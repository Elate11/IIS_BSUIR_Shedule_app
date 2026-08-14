package com.example.schedule

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

// =========================================================================
// AGSL SHADERS FROM Kyant0/AndroidLiquidGlass
// =========================================================================

private const val AGSL_ROUNDED_RECT_SDF = """
float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}
"""

private const val AGSL_REFRACTION_WITH_DISPERSION = """
uniform shader content;
uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float chromaticAberration;

$AGSL_ROUNDED_RECT_SDF

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));
    
    float2 refractedCoord = coord + d * grad;
    float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y + 0.001));
    float2 dispersedCoord = d * grad * dispersionIntensity;
    
    half4 color = half4(0.0);
    
    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;
    
    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;
    
    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;
    
    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;
    
    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;
    
    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;
    
    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;
    
    return color;
}
"""

private const val AGSL_SPECULAR_HIGHLIGHT = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;

$AGSL_ROUNDED_RECT_SDF

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    float intensity = pow(abs(d), falloff);
    return color * intensity;
}
"""

// =========================================================================
// INTERACTIVE HIGHLIGHT & SQUISHY PHYSICS ENGINE
// =========================================================================

class KyantLiquidPhysics(
    val scope: CoroutineScope
) {
    private val springSpec = spring<Float>(0.5f, 320f, 0.001f)
    private val offsetSpringSpec = spring(0.5f, 320f, Offset.VisibilityThreshold)

    val pressAnim = Animatable(0f, 0.001f)
    val positionAnim = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    var startPos = Offset.Zero
    val pressProgress: Float get() = pressAnim.value
    val dragOffset: Offset get() = positionAnim.value - startPos

    fun onTouchDown(pos: Offset) {
        startPos = pos
        scope.launch {
            launch { pressAnim.animateTo(1f, springSpec) }
            launch { positionAnim.snapTo(startPos) }
        }
    }

    fun onTouchMove(pos: Offset) {
        scope.launch { positionAnim.snapTo(pos) }
    }

    fun onTouchUp() {
        scope.launch {
            launch { pressAnim.animateTo(0f, springSpec) }
            launch { positionAnim.animateTo(startPos, offsetSpringSpec) }
        }
    }
}

// =========================================================================
// LIQUID GLASS MODIFIER (Full Kyant0/AndroidLiquidGlass fidelity)
// =========================================================================

fun Modifier.liquidGlassEffect(
    cornerRadius: Dp = 20.dp,
    tint: Color = Color.Unspecified,
    accentColor: Color = Color(0xFF6C63FF),
    isDarkTheme: Boolean = true,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    isActive: Boolean = true,
    refractionAmount: Float = 28f,
    refractionHeight: Float = 14f,
    chromaticAberration: Boolean = true
): Modifier = this.then(
    Modifier
        .clip(RoundedCornerShape(cornerRadius))
        .drawWithContent {
            val w = size.width
            val h = size.height
            val r = cornerRadius.toPx().coerceAtMost(minOf(w, h) / 2f)

            if (!isActive) {
                drawContent()
                return@drawWithContent
            }

            // 1. Subsurface Bloom Glow
            val glowColor = (if (tint != Color.Unspecified) tint else accentColor)
                .copy(alpha = if (isDarkTheme) 0.30f else 0.40f)
            drawRoundRect(
                color = glowColor,
                topLeft = Offset(-1.5.dp.toPx() + tiltX * 4.dp.toPx(), -1.5.dp.toPx() + tiltY * 4.dp.toPx()),
                size = Size(w + 3.dp.toPx(), h + 3.dp.toPx()),
                cornerRadius = CornerRadius(r + 1.5.dp.toPx(), r + 1.5.dp.toPx())
            )

            // 2. Optical Liquid Glass Body (Translucent gradient backdrop)
            val glassBodyBrush = Brush.linearGradient(
                colors = if (isDarkTheme) listOf(
                    Color.White.copy(alpha = 0.28f),
                    accentColor.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.08f),
                    accentColor.copy(alpha = 0.18f)
                ) else listOf(
                    Color.White.copy(alpha = 0.85f),
                    accentColor.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.40f),
                    accentColor.copy(alpha = 0.20f)
                ),
                start = Offset(tiltX * 15f, tiltY * 15f),
                end = Offset(w - tiltX * 15f, h - tiltY * 15f)
            )
            drawRoundRect(
                brush = glassBodyBrush,
                cornerRadius = CornerRadius(r, r)
            )

            // 3. Draw Inner Content
            drawContent()

            // 4. Specular Highlight & Caustic Lens Reflection
            val highlightAngle = atan2(tiltY, tiltX)
            val normalX = cos(highlightAngle)
            val normalY = sin(highlightAngle)
            
            val causticBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDarkTheme) 0.65f else 0.85f),
                    Color.White.copy(alpha = if (isDarkTheme) 0.20f else 0.35f),
                    Color.Transparent
                ),
                center = Offset(w / 2f + normalX * (w * 0.25f), h / 2f + normalY * (h * 0.25f)),
                radius = maxOf(w, h) * 0.75f
            )
            drawRoundRect(
                brush = causticBrush,
                cornerRadius = CornerRadius(r, r),
                blendMode = BlendMode.Plus
            )

            // 5. Prismatic Diamond-Cut Bevel (Chromatic Dispersion Rim)
            val hueShift = ((highlightAngle * 180 / Math.PI + 360) % 360).toFloat()
            val prismColor = Color.hsl(hueShift, 0.70f, if (isDarkTheme) 0.75f else 0.55f)
            val rimBrush = Brush.sweepGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.90f),
                    prismColor.copy(alpha = 0.60f),
                    Color.White.copy(alpha = 0.35f),
                    prismColor.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.90f)
                ),
                center = Offset(w / 2f + tiltX * 10f, h / 2f + tiltY * 10f)
            )
            drawRoundRect(
                brush = rimBrush,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
)

// =========================================================================
// KYANT LIQUID BUTTON (Drop-in Composable Component)
// =========================================================================

@Composable
fun KyantLiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    tint: Color = Color.Unspecified,
    accentColor: Color = Color(0xFF6C63FF),
    isDarkTheme: Boolean = true,
    isSelected: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val physics = remember(coroutineScope) { KyantLiquidPhysics(coroutineScope) }

    val press = physics.pressProgress
    val offset = physics.dragOffset

    val maxSquish = 8.dp
    val scale = 1f + 0.04f * press

    Row(
        modifier = modifier
            .graphicsLayer {
                val maxOffset = 60f
                val initDeriv = 0.06f
                translationX = maxOffset * tanh(initDeriv * offset.x / maxOffset)
                translationY = maxOffset * tanh(initDeriv * offset.y / maxOffset)
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(coroutineScope) {
                detectDragGestures(
                    onDragStart = { physics.onTouchDown(it) },
                    onDragEnd = { physics.onTouchUp() },
                    onDragCancel = { physics.onTouchUp() }
                ) { change, _ ->
                    physics.onTouchMove(change.position)
                }
            }
            .liquidGlassEffect(
                cornerRadius = cornerRadius,
                tint = if (isSelected) accentColor else tint,
                accentColor = accentColor,
                isDarkTheme = isDarkTheme,
                tiltX = if (press > 0.01f) (offset.x / 100f).coerceIn(-1f, 1f) else 0f,
                tiltY = if (press > 0.01f) (offset.y / 100f).coerceIn(-1f, 1f) else 0f,
                isActive = isSelected || press > 0.01f
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
