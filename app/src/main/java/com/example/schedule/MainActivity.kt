@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.schedule

import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request

import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.padding
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.foundation.horizontalScroll
import dev.chrisbanes.haze.haze
import androidx.compose.ui.window.Dialog
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColor
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.animation.AnimatedVisibility
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.content.Intent
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.combinedClickable

val vt323FontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.example.schedule.R.font.vt323))



@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkClient.init(applicationContext)
        setContent {
            MaterialTheme {
                MinimalistApp()
            }
        }
    }
}

data class Particle(
    val x: Float,
    val y: Float,
    val alpha: Float = 1f,
    val size: Float,
    val vx: Float,
    val vy: Float
)

// Matrix digital rain background effect
@Composable
fun MatrixRainLayer(content: @Composable () -> Unit) {
    val matrixGreen = Color(0xFF00FF41)
    val matrixChars = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン0123456789ABCDEFZ<>[]{}|"

    data class RainDrop(
        val x: Float,
        val y: Float,
        val speed: Float,
        val length: Int,
        val chars: List<Char>
    )

    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    val fontSize = 16f
    val colWidth = fontSize * 1.2f

    var drops by remember { mutableStateOf(listOf<RainDrop>()) }

    LaunchedEffect(screenWidth, screenHeight) {
        if (screenWidth == 0f) return@LaunchedEffect
        val colCount = (screenWidth / colWidth).toInt().coerceAtLeast(1)
        drops = List(colCount) { i ->
            val len = (8..24).random()
            RainDrop(
                x = i * colWidth,
                y = (-screenHeight * (0.5f + kotlin.random.Random.nextFloat())),
                speed = 3f + kotlin.random.Random.nextFloat() * 5f,
                length = len,
                chars = List(len) { matrixChars.random() }
            )
        }
    }

    // Animate
    LaunchedEffect(drops.isNotEmpty()) {
        if (drops.isEmpty()) return@LaunchedEffect
        while (true) {
            androidx.compose.runtime.withFrameMillis { _ ->
                drops = drops.map { drop ->
                    val newY = drop.y + drop.speed
                    val resetY = if (newY > screenHeight + drop.length * fontSize) {
                        -drop.length * fontSize - kotlin.random.Random.nextFloat() * screenHeight * 0.5f
                    } else newY
                    val newChars = drop.chars.toMutableList()
                    // Randomly mutate a character in the tail
                    if (kotlin.random.Random.nextFloat() < 0.1f) {
                        val idx = kotlin.random.Random.nextInt(drop.chars.size)
                        newChars[idx] = matrixChars.random()
                    }
                    drop.copy(y = resetY, chars = newChars)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            screenWidth = coordinates.size.width.toFloat()
            screenHeight = coordinates.size.height.toFloat()
        }) {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = fontSize * density
                typeface = android.graphics.Typeface.MONOSPACE
            }
            drops.forEach { drop ->
                drop.chars.forEachIndexed { i, char ->
                    val charY = drop.y + i * fontSize
                    if (charY < 0 || charY > size.height) return@forEachIndexed
                    val isHead = i == drop.chars.size - 1
                    val tailAlpha = (i.toFloat() / drop.chars.size)
                    val color = if (isHead) {
                        matrixGreen.copy(alpha = 1f)
                    } else {
                        matrixGreen.copy(alpha = tailAlpha * 0.7f)
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        paint.color = android.graphics.Color.argb(
                            (color.alpha * 255).toInt(),
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                        )
                        drawText(char.toString(), drop.x, charY, paint)
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun ParticleTrailLayer(isDarkTheme: Boolean, particleSizeMultiplier: Float, customParticleColor: Color?, content: @Composable () -> Unit) {
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    val particleColor = customParticleColor ?: if (isDarkTheme) Color.White else Color.Black
    val currentMultiplier by rememberUpdatedState(particleSizeMultiplier)

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            if (particles.isNotEmpty()) {
                particles = particles.mapNotNull { p ->
                    val newAlpha = p.alpha - 0.02f
                    if (newAlpha <= 0f) null
                    else p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        alpha = newAlpha
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val changes = event.changes
                        if (changes.any { it.pressed }) {
                            val positions = changes.filter { it.pressed }.map { it.position }
                            val newParticles = positions.flatMap { pos ->
                                List(8) {
                                    Particle(
                                        x = pos.x + (Math.random() - 0.5f).toFloat() * 60f,
                                        y = pos.y + (Math.random() - 0.5f).toFloat() * 60f,
                                        size = (Math.random() * 15 + 10).toFloat(), // Unscaled size
                                        vx = (Math.random() - 0.5f).toFloat() * 12f,
                                        vy = (Math.random() - 0.5f).toFloat() * 12f
                                    )
                                }
                            }
                            particles = (particles + newParticles).takeLast(300)
                        }
                    }
                }
            }
    ) {
        content()

        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = particleColor.copy(alpha = p.alpha * 0.3f),
                    radius = p.size * currentMultiplier,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MinimalistApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val groupPrefs = remember { context.getSharedPreferences("group_prefs", android.content.Context.MODE_PRIVATE) }
    var selectedSubgroup by remember { mutableStateOf(groupPrefs.getInt("subgroup", 0)) }

    var isDarkTheme by remember { mutableStateOf(sharedPreferences.getBoolean("isDarkTheme", true)) }

    var particlesEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("particlesEnabled", true)) }
    var particleSizeMultiplier by remember { mutableStateOf(sharedPreferences.getFloat("particleSizeMultiplier", 1f)) }
    var transitionsEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("transitionsEnabled", true)) }
    var transitionType by remember { mutableStateOf(TransitionType.valueOf(sharedPreferences.getString("transitionType", "Slide") ?: "Slide")) }
    var transitionSpeedMultiplier by remember { mutableStateOf(sharedPreferences.getFloat("transitionSpeedMultiplier", 1f)) }
    val GoogleSans = remember {
        androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(com.example.schedule.R.font.google_sans_regular, androidx.compose.ui.text.font.FontWeight.Normal),
            androidx.compose.ui.text.font.Font(com.example.schedule.R.font.google_sans_bold, androidx.compose.ui.text.font.FontWeight.Bold)
        )
    }
    var fontFamily: androidx.compose.ui.text.font.FontFamily by remember { mutableStateOf(GoogleSans) }
    var textSizeMultiplier by remember { mutableStateOf(sharedPreferences.getFloat("textSizeMultiplier", 1.25f).let { if (it == 1f) 1.25f else it }) }
    var selectedGroup by remember { mutableStateOf(sharedPreferences.getString("login_group", "114001") ?: "114001") }

    var isLoggedIn by remember { mutableStateOf(sharedPreferences.getBoolean("is_logged_in", false)) }
    if (!isLoggedIn) {
        MinLoginScreen(
            MinBg = Color(0xFF101116),
            MinBorder = Color(0xFF222222),
            MinTextPrimary = Color(0xFFEEEEEE),
            MinTextSecondary = Color(0xFF888888),
            isDarkTheme = true,
            onLoginSuccess = { gradebook, token ->
                val realGroup = sharedPreferences.getString("login_group", null)
                val groupStr = realGroup ?: (if (gradebook.length >= 6) gradebook.take(6) else gradebook)
                sharedPreferences.edit().putBoolean("is_logged_in", true)
                     .putString("gradebook", gradebook)
                     .putString("auth_token", token)
                     .putString("selectedGroup", groupStr)
                     .apply()
                selectedGroup = groupStr
                isLoggedIn = true
            }
        )
        return
    }
    var customPrimaryColor by remember { mutableStateOf(sharedPreferences.getInt("customPrimaryColor", -1).let { if (it == -1) null else Color(it) }) }
    var customBackgroundColor by remember { mutableStateOf(sharedPreferences.getInt("customBackgroundColor", -1).let { if (it == -1) null else Color(it) }) }
    var customParticleColor by remember { mutableStateOf(sharedPreferences.getInt("customParticleColor", -1).let { if (it == -1) null else Color(it) }) }

    androidx.compose.runtime.LaunchedEffect(isDarkTheme) { sharedPreferences.edit().putBoolean("isDarkTheme", isDarkTheme).apply() }


    androidx.compose.runtime.LaunchedEffect(particlesEnabled) { sharedPreferences.edit().putBoolean("particlesEnabled", particlesEnabled).apply() }
    androidx.compose.runtime.LaunchedEffect(particleSizeMultiplier) { sharedPreferences.edit().putFloat("particleSizeMultiplier", particleSizeMultiplier).apply() }
    androidx.compose.runtime.LaunchedEffect(transitionsEnabled) { sharedPreferences.edit().putBoolean("transitionsEnabled", transitionsEnabled).apply() }
    androidx.compose.runtime.LaunchedEffect(transitionType) { sharedPreferences.edit().putString("transitionType", transitionType.name).apply() }
    androidx.compose.runtime.LaunchedEffect(transitionSpeedMultiplier) { sharedPreferences.edit().putFloat("transitionSpeedMultiplier", transitionSpeedMultiplier).apply() }
    androidx.compose.runtime.LaunchedEffect(customPrimaryColor) { sharedPreferences.edit().putInt("customPrimaryColor", customPrimaryColor?.toArgb() ?: -1).apply() }
    androidx.compose.runtime.LaunchedEffect(customBackgroundColor) { sharedPreferences.edit().putInt("customBackgroundColor", customBackgroundColor?.toArgb() ?: -1).apply() }
    androidx.compose.runtime.LaunchedEffect(customParticleColor) { sharedPreferences.edit().putInt("customParticleColor", customParticleColor?.toArgb() ?: -1).apply() }
    androidx.compose.runtime.LaunchedEffect(textSizeMultiplier) { sharedPreferences.edit().putFloat("textSizeMultiplier", textSizeMultiplier).apply() }
    androidx.compose.runtime.LaunchedEffect(selectedGroup) { sharedPreferences.edit().putString("selectedGroup", selectedGroup).apply() }
    
    var bgMode by remember { mutableStateOf(sharedPreferences.getString("bgMode", "Solid") ?: "Solid") }
    var bgImageUri by remember { mutableStateOf(sharedPreferences.getString("bgImageUri", null)) }
    var bgBlur by remember { mutableStateOf(sharedPreferences.getFloat("bgBlur", 0f)) }
    var bgDim by remember { mutableStateOf(sharedPreferences.getFloat("bgDim", 0f)) }
    var bgEmoji by remember { mutableStateOf(sharedPreferences.getString("bgEmoji", "😎") ?: "😎") }
    
    androidx.compose.runtime.LaunchedEffect(bgMode) { sharedPreferences.edit().putString("bgMode", bgMode).apply() }
    androidx.compose.runtime.LaunchedEffect(bgImageUri) { sharedPreferences.edit().putString("bgImageUri", bgImageUri).apply() }
    androidx.compose.runtime.LaunchedEffect(bgBlur) { sharedPreferences.edit().putFloat("bgBlur", bgBlur).apply() }
    androidx.compose.runtime.LaunchedEffect(bgDim) { sharedPreferences.edit().putFloat("bgDim", bgDim).apply() }
    androidx.compose.runtime.LaunchedEffect(bgEmoji) { sharedPreferences.edit().putString("bgEmoji", bgEmoji).apply() }

    var styleType by remember { mutableStateOf(
        when (sharedPreferences.getString("styleType", "Minimal")) {
            "Techno" -> StyleType.Techno
            else -> StyleType.Minimal
        }
    ) }
    androidx.compose.runtime.LaunchedEffect(styleType) { sharedPreferences.edit().putString("styleType", styleType.name).apply() }

    val isTechnoStyle = styleType == StyleType.Techno

    val actualBg = if (isTechnoStyle) Color(0xFF000000)
        else customBackgroundColor ?: if (isDarkTheme) Color(0xFF111111) else Color(0xFFFAFAFA)

    val MinBg = if (isTechnoStyle) Color.Transparent
        else if (bgMode == "Solid") actualBg else Color.Transparent

    val MinCardBg = if (isTechnoStyle) Color.Transparent else Color.Transparent

    val MinBorder = if (isTechnoStyle) Color(0xFF00FF41).copy(alpha = 0.25f) else Color.Transparent

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val animatedPrimaryColor by infiniteTransition.animateColor(
        initialValue = androidx.compose.ui.graphics.Color(0xFF1A237E),
        targetValue = androidx.compose.ui.graphics.Color(0xFF1A237E),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 20000
                androidx.compose.ui.graphics.Color(0xFF1A237E) at 0
                androidx.compose.ui.graphics.Color(0xFF004D40) at 6666
                androidx.compose.ui.graphics.Color(0xFF311B92) at 13332
                androidx.compose.ui.graphics.Color(0xFF1A237E) at 20000
            },
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "primaryTextColor"
    )

    val animatedSecondaryColor by infiniteTransition.animateColor(
        initialValue = androidx.compose.ui.graphics.Color(0xFF3949AB),
        targetValue = androidx.compose.ui.graphics.Color(0xFF3949AB),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 20000
                androidx.compose.ui.graphics.Color(0xFF3949AB) at 0
                androidx.compose.ui.graphics.Color(0xFF00695C) at 6666
                androidx.compose.ui.graphics.Color(0xFF512DA8) at 13332
                androidx.compose.ui.graphics.Color(0xFF3949AB) at 20000
            },
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "secondaryTextColor"
    )

    val MinTextPrimary = if (styleType == StyleType.Techno) Color(0xFF00FF41)
        else customPrimaryColor ?: if (isDarkTheme) Color(0xFFEEEEEE) else Color(0xFF111111)

    val MinTextSecondary = if (styleType == StyleType.Techno) Color(0xFF00AA22)
        else if (isDarkTheme) Color(0xFFCCCCCC) else Color(0xFF444444)
    
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is android.app.Activity) break
                context = context.baseContext
            }
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
            }
        }
    }
    val MinAccent = MinTextPrimary

    val items = listOf("Расписание", "Оценки", "Заметки", "Профиль")
    val icons = listOf(Icons.Outlined.DateRange, Icons.Outlined.CheckCircle, Icons.Outlined.Edit, Icons.Outlined.Person)
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()
    val selectedItem = pagerState.currentPage

    // Multi-level Back Navigation System:
    // Level 1: If on Оценки (1), Заметки (2), or Профиль (3), return to Расписание (0)
    // Level 0: Double-tap back on Schedule tab to exit cleanly
    var lastBackPressTime by remember { mutableStateOf(0L) }
    androidx.activity.compose.BackHandler(enabled = true) {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                if (transitionsEnabled) {
                    pagerState.animateScrollToPage(
                        page = 0,
                        animationSpec = tween((400 / transitionSpeedMultiplier).toInt())
                    )
                } else {
                    pagerState.scrollToPage(0)
                }
            }
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = now
                android.widget.Toast.makeText(context, "Нажмите назад еще раз для выхода", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val appContent = @Composable {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
            if (bgMode == "Gallery" && bgImageUri != null) {
                coil.compose.AsyncImage(
                    model = bgImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().let { if (bgBlur > 0f) it.blur((bgBlur * 50).dp) else it },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                if (bgDim > 0f) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDim)))
                }
            } else if (bgMode == "Solid" || (bgMode == "Gallery" && bgImageUri == null)) {
                Box(modifier = Modifier.fillMaxSize().background(if (styleType == StyleType.Techno) Color.Transparent else actualBg))

            } else if (bgMode == "Gradient" && styleType != StyleType.Techno) {
                DynamicGradientBackground(accentColor = MinAccent, bgColor = if (isDarkTheme) Color(0xFF101116) else Color(0xFFFAFAFA), isDarkTheme = isDarkTheme)
                if (bgDim > 0f) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDim)))
                }
            }
            // Techno: scanline overlay
            if (styleType == StyleType.Techno) {
                ScanlineOverlay()
            }
            }
            
            Scaffold(
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (styleType == StyleType.Techno) {
                                // ASCII terminal nav bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .background(Color(0xFF0D0D0D).copy(alpha = 0.97f))
                                        .border(1.dp, MinTextPrimary.copy(alpha = 0.8f)),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val labels = listOf("[SCH]", "[MRK]", "[NTS]", "[PRF]")
                                    items.forEachIndexed { index, _ ->
                                        val isSelected = selectedItem == index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .border(if (isSelected) 1.dp else 0.dp, if (isSelected) MinTextPrimary else Color.Transparent)
                                                .clickable {
                                                    coroutineScope.launch { pagerState.scrollToPage(index) }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isSelected) "> ${labels[index]}" else labels[index],
                                                fontFamily = vt323FontFamily,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MinTextPrimary else MinTextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                            NavigationBar(
                                containerColor = actualBg.copy(alpha = 0.85f),
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .height(72.dp).clip(RoundedCornerShape(36.dp))
                            ) {
                                items.forEachIndexed { index, item ->
                                    val isSelected = selectedItem == index
                                    
                                    NavigationBarItem(
                                        icon = { Icon(icons[index], contentDescription = item, modifier = Modifier.size(24.dp)) },
                                        label = null,
                                        selected = isSelected,
                                        onClick = { 
                                            coroutineScope.launch {
                                                if (transitionsEnabled) {
                                                    pagerState.animateScrollToPage(
                                                        page = index,
                                                        animationSpec = tween((500 / transitionSpeedMultiplier).toInt())
                                                    )
                                                } else {
                                                    pagerState.scrollToPage(index)
                                                }
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color.Transparent,
                                            selectedIconColor = MinTextPrimary,
                                            unselectedIconColor = MinTextSecondary,
                                            selectedTextColor = MinTextPrimary,
                                            unselectedTextColor = MinTextSecondary
                                        )
                                    )
                                }
                            }
                            } // end else (Minimal nav bar)
                            if (styleType == StyleType.Minimal) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                items.indices.forEach { index ->
                                    val isSelected = selectedItem == index
                                    val color by androidx.compose.animation.animateColorAsState(if (isSelected) MinTextPrimary else MinTextSecondary.copy(alpha = 0.3f))
                                    val size by androidx.compose.animation.core.animateDpAsState(if (isSelected) 6.dp else 4.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(size)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }
                            }
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color.Transparent)) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (transitionsEnabled) {
                                when (transitionType) {
                                    TransitionType.Slide -> { } // Default slide behavior
                                    TransitionType.Fade -> {
                                        alpha = (1f - kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                                        translationX = pageOffset * size.width
                                    }
                                    TransitionType.Scale -> {
                                        val s = 1f - (kotlin.math.abs(pageOffset) * 0.2f)
                                        scaleX = s
                                        scaleY = s
                                        alpha = (1f - kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                                        translationX = pageOffset * size.width
                                    }
                                    TransitionType.Cube -> {
                                        rotationY = pageOffset * 90f
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                            pivotFractionX = if (pageOffset > 0) 0f else 1f,
                                            pivotFractionY = 0.5f
                                        )
                                    }
                                    TransitionType.Flip -> {
                                        rotationY = pageOffset * 180f
                                        alpha = if (kotlin.math.abs(pageOffset) > 0.5f) 0f else 1f
                                        translationX = pageOffset * size.width
                                    }
                                }
                            } else {
                                translationX = pageOffset * size.width
                                alpha = if (pageOffset == 0f) 1f else 0f
                            }
                        }
                    ) {
                        when (page) {
                            0 -> MinScheduleScreen(MinBg, actualBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, MinAccent, isDarkTheme, selectedGroup, { selectedGroup = it }, selectedSubgroup, { selectedSubgroup = it; groupPrefs.edit().putInt("subgroup", it).apply() })
                            1 -> MinMarksScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, MinAccent, isDarkTheme, onBack = {})
                            2 -> MinNotesScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, MinAccent, selectedGroup = selectedGroup)
                            3 -> MinProfileScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, isDarkTheme, MinAccent, particlesEnabled, particleSizeMultiplier, transitionsEnabled, transitionType, transitionSpeedMultiplier, fontFamily, textSizeMultiplier, bgMode, bgImageUri, bgBlur, bgDim, bgEmoji, customParticleColor, { isDarkTheme = !isDarkTheme; customPrimaryColor = null; customBackgroundColor = null; customParticleColor = null; sharedPreferences.edit().remove("customPrimaryColor").remove("customBackgroundColor").remove("customParticleColor").apply() }, { customPrimaryColor = it }, { particlesEnabled = it }, { particleSizeMultiplier = it }, { transitionsEnabled = it }, { transitionType = it }, { transitionSpeedMultiplier = it }, { fontFamily = it }, { textSizeMultiplier = it }, { customPrimaryColor = it }, { 
                                customBackgroundColor = it
                                if (it != null) {
                                    isDarkTheme = it.luminance() < 0.5f
                                }
                            }, { bgMode = it }, { bgImageUri = it }, { bgBlur = it }, { bgDim = it }, { bgEmoji = it }, { customParticleColor = it }, { styleType = it })
                        }
                    }
                }
            }
        }
    }
    } // closes appContent lambda

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val currentTypography = androidx.compose.material3.MaterialTheme.typography
    val activeFontFamily = if (styleType == StyleType.Techno) vt323FontFamily else fontFamily
    val appTypography = androidx.compose.material3.Typography(
        displayLarge = currentTypography.displayLarge.copy(fontFamily = activeFontFamily),
        displayMedium = currentTypography.displayMedium.copy(fontFamily = activeFontFamily),
        displaySmall = currentTypography.displaySmall.copy(fontFamily = activeFontFamily),
        headlineLarge = currentTypography.headlineLarge.copy(fontFamily = activeFontFamily),
        headlineMedium = currentTypography.headlineMedium.copy(fontFamily = activeFontFamily),
        headlineSmall = currentTypography.headlineSmall.copy(fontFamily = activeFontFamily),
        titleLarge = currentTypography.titleLarge.copy(fontFamily = activeFontFamily),
        titleMedium = currentTypography.titleMedium.copy(fontFamily = activeFontFamily),
        titleSmall = currentTypography.titleSmall.copy(fontFamily = activeFontFamily),
        bodyLarge = currentTypography.bodyLarge.copy(fontFamily = activeFontFamily),
        bodyMedium = currentTypography.bodyMedium.copy(fontFamily = activeFontFamily),
        bodySmall = currentTypography.bodySmall.copy(fontFamily = activeFontFamily),
        labelLarge = currentTypography.labelLarge.copy(fontFamily = activeFontFamily),
        labelMedium = currentTypography.labelMedium.copy(fontFamily = activeFontFamily),
        labelSmall = currentTypography.labelSmall.copy(fontFamily = activeFontFamily)
    )

    CompositionLocalProvider(
        LocalStyleType provides styleType,
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density = currentDensity.density, fontScale = currentDensity.fontScale * textSizeMultiplier),
        androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = activeFontFamily)
    ) {
        androidx.compose.material3.MaterialTheme(typography = appTypography) {
            if (styleType == StyleType.Techno) {
                MatrixRainLayer { appContent() }
            } else {
                appContent()
            }
        }
    }
}

@Composable
fun rememberDeviceTilt(onShake: (() -> Unit)? = null): Pair<Float, Float> {
    val context = androidx.compose.ui.platform.LocalContext.current
    var rawTiltX by remember { mutableStateOf(0f) }
    var rawTiltY by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val sensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        
        var lastShakeTime = 0L
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                event?.let {
                    if (it.sensor.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                        val ax = it.values[0]
                        val ay = it.values[1]
                        val az = it.values[2]

                        // Normalized tilt between -1f and 1f
                        rawTiltX = (-ax / 9.81f).coerceIn(-1f, 1f)
                        rawTiltY = ((ay - 4.5f) / 9.81f).coerceIn(-1f, 1f)

                        // Feature 6: Shake detection for quick return to today
                        val gForce = kotlin.math.sqrt(ax * ax + ay * ay + az * az) / 9.81f
                        if (gForce > 2.4f) {
                            val now = System.currentTimeMillis()
                            if (now - lastShakeTime > 1200L) {
                                lastShakeTime = now
                                onShake?.invoke()
                            }
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val animatedTiltX by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rawTiltX,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = 220f
        ),
        label = "tiltX"
    )
    val animatedTiltY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rawTiltY,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = 220f
        ),
        label = "tiltY"
    )

    return Pair(animatedTiltX, animatedTiltY)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MinScheduleScreen(MinBg: Color, actualBg: Color, MinCardBg: Color, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, MinAccent: Color, isDarkTheme: Boolean, selectedGroup: String, onGroupSelected: (String) -> Unit, selectedSubgroup: Int, onSubgroupChange: (Int) -> Unit, displayTitle: String? = null) {
    var showScheduleSettings by remember { mutableStateOf(false) }
        val carouselDays = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7)
        val list = mutableListOf<Pair<Int, String>>()
        val weekDayNames = arrayOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
        for (i in 0..90) {
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val w = weekDayNames[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            list.add(d to w)
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        list
    }
    
    var selectedDayIndex by remember { mutableStateOf(7) }

    
    val days = carouselDays.map { it.first }
    val weekDays = carouselDays.map { it.second }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val dataManager = remember { DataManager(context) }
    val prefs = remember { context.getSharedPreferences("group_prefs", android.content.Context.MODE_PRIVATE) }
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }

    var baseCurrentWeek by remember { mutableStateOf(1) }

    val validDayIndices = remember(lessons, baseCurrentWeek, displayTitle) {
        if (displayTitle == null) return@remember (0..90).toList()
        val valid = mutableListOf<Int>()
        for (index in 0..90) {
            val offset = index - 7
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_MONTH, offset)
            var dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
            if (dow == 0) dow = 7
            
            var w = baseCurrentWeek
            val wCal = java.util.Calendar.getInstance()
            if (offset > 0) {
                for (i in 1..offset) {
                    wCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                    if (wCal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) w = (w % 4) + 1
                }
            } else if (offset < 0) {
                for (i in 1..(-offset)) {
                    if (wCal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) { w = w - 1; if (w == 0) w = 4 }
                    wCal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                }
            }
            
            val hasLessons = lessons.any { it.dayOfWeek == dow && (it.weeks.isEmpty() || it.weeks.contains(w)) }
            if (hasLessons || index == 7) valid.add(index)
        }
        valid
    }
    
    val currentWeek = remember(baseCurrentWeek, selectedDayIndex) {
        if (baseCurrentWeek == 0) return@remember 0
        var w = baseCurrentWeek
        val cal = java.util.Calendar.getInstance()
        val offset = selectedDayIndex - 7
        if (offset > 0) {
            for (i in 1..offset) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) {
                    w = (w % 4) + 1
                }
            }
        } else if (offset < 0) {
            for (i in 1..(-offset)) {
                if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) {
                    w = w - 1
                    if (w == 0) w = 4
                }
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
            }
        }
        w
    }

    val selectedDayOfWeek = remember(selectedDayIndex) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7 + selectedDayIndex)
        var dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        if (dow == 0) dow = 7
        dow
    }

    LaunchedEffect(selectedGroup) {
        val week = BsuirApi.getCurrentWeek() ?: 0
        baseCurrentWeek = week
        
        val response = if (selectedGroup.any { it.isLetter() }) {
            BsuirApi.getEmployeeSchedule(selectedGroup)
        } else {
            BsuirApi.getGroupSchedule(selectedGroup)
        }
        
        val newLessons = mutableListOf<Lesson>()
        response?.schedules?.forEach { (dayName, dayLessons) ->
            val dow = when(dayName.lowercase()) {
                "понедельник" -> 1; "вторник" -> 2; "среда" -> 3; "четверг" -> 4; "пятница" -> 5; "суббота" -> 6; "воскресенье" -> 7; else -> 0
            }
            dayLessons.forEach { bl ->
                val teacher = bl.employees?.firstOrNull()
                newLessons.add(Lesson(
                    startTime = bl.startLessonTime ?: "",
                    endTime = bl.endLessonTime ?: "",
                    title = bl.subject ?: "Предмет",
                    details = ", ауд. ",
                    isActive = false,
                    progress = null,
                    subjectFullName = bl.subjectFullName ?: "",
                    lessonType = bl.lessonTypeAbbrev ?: "",
                    teacherName = teacher?.lastName?.let { " .." } ?: "",
                    teacherFullName = teacher?.fullName ?: "",
                    teacherPhoto = teacher?.photoLink ?: "", teacherUrlId = teacher?.urlId ?: "",
                    auditory = bl.auditories?.joinToString() ?: "",
                    subgroup = bl.numSubgroup ?: 0,
                    weeks = bl.weekNumber ?: emptyList(),
                    dayOfWeek = dow
                ))
            }
        }
        response?.exams?.forEach { bl ->
           val teacher = bl.employees?.firstOrNull()
           newLessons.add(Lesson(
               startTime = bl.startLessonTime ?: "",
               endTime = bl.endLessonTime ?: "",
                    title = bl.subject ?: "Предмет",
                    details = "ЛК, ауд. ",
               isActive = false,
               progress = null,
               subjectFullName = bl.subjectFullName ?: "",
                    lessonType = "ЛК",
               teacherName = teacher?.lastName?.let { " .." } ?: "",
               teacherFullName = teacher?.fullName ?: "",
               teacherPhoto = teacher?.photoLink ?: "", teacherUrlId = teacher?.urlId ?: "",
               auditory = bl.auditories?.joinToString() ?: "",
               subgroup = bl.numSubgroup ?: 0,
               weeks = listOf(1, 2, 3, 4),
               dayOfWeek = 0
           ))
        }
        
        if (newLessons.isNotEmpty()) {
            dataManager.saveLessons(newLessons)
            lessons = newLessons
        } else {
            // Load from cache instead of mock data
            val cached = dataManager.getLessons()
            // If cache is exactly the mock data, don't use it
            if (cached.size == 3 && cached[0].title == "Матан") {
                lessons = emptyList()
            } else {
                lessons = cached
            }
        }
    }

    val filteredLessons = remember(lessons, selectedSubgroup, selectedDayOfWeek, currentWeek) {
        lessons.filter { 
            (it.subgroup == 0 || it.subgroup == selectedSubgroup || selectedSubgroup == 0) &&
            (it.dayOfWeek == selectedDayOfWeek || it.dayOfWeek == 0) &&
            (it.weeks.isEmpty() || it.weeks.contains(currentWeek))
        }
    }
    var selectedLessonForSheet by remember { mutableStateOf<Lesson?>(null) }
    var noteLessonToAdd by remember { mutableStateOf<Lesson?>(null) }
    var teacherScheduleData by remember { mutableStateOf<Pair<String, String>?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val (tiltX, tiltY) = rememberDeviceTilt(onShake = {
        selectedDayIndex = 7
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    })
    val notesPrefs = remember { context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE) }
    val subjectsWithNotes = remember(notesPrefs.getString("notes_data", "")) {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
            (com.google.gson.Gson().fromJson<List<Note>>(notesPrefs.getString("notes_data", "[]"), type) ?: emptyList<Note>()).map { it.subject }.toSet()
        } catch (e: Exception) { emptySet<String>() }
    }

    val blurRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selectedLessonForSheet != null) 16.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "blur"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selectedLessonForSheet != null) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "scale"
    )

    val styleType = LocalStyleType.current
    val isDialogOpen = selectedLessonForSheet != null || showScheduleSettings || noteLessonToAdd != null

    androidx.activity.compose.BackHandler(enabled = isDialogOpen || teacherScheduleData != null) {
        if (selectedLessonForSheet != null) {
            selectedLessonForSheet = null
        } else if (noteLessonToAdd != null) {
            noteLessonToAdd = null
        } else if (showScheduleSettings) {
            showScheduleSettings = false
        } else if (teacherScheduleData != null) {
            teacherScheduleData = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().let { if (isDialogOpen) it.blur(16.dp) else it }) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (styleType == StyleType.Techno) {
                    rotationX = (-tiltY * 3.5f).coerceIn(-6f, 6f)
                    rotationY = (tiltX * 4.5f).coerceIn(-7f, 7f)
                    cameraDistance = 10f * density
                }
            },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val weekText = if (currentWeek == 0) "КАНИКУЛЫ" else "$currentWeek УЧЕБНАЯ НЕДЕЛЯ"
                    val weekTextTechno = if (currentWeek == 0) "> КАНИКУЛЫ_" else "> $currentWeek УЧЕБНАЯ НЕДЕЛЯ_"
                    Text(if (styleType == StyleType.Techno) weekTextTechno else weekText, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary, letterSpacing = if (styleType == StyleType.Techno) 1.sp else 2.sp, fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null)
                    androidx.compose.material3.IconButton(
                        onClick = { showScheduleSettings = true },
                        modifier = Modifier.align(Alignment.CenterEnd).size(24.dp)
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MinTextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val context2 = androidx.compose.ui.platform.LocalContext.current
                var expanded by remember { mutableStateOf(false) }
                var inputText by remember { mutableStateOf(selectedGroup) }
                var groupHistory by remember {
                    mutableStateOf(
                        prefs.getStringSet("group_history", emptySet())?.toList() ?: emptyList()
                    )
                }

                // Header row: group number + arrow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (styleType == StyleType.Techno) "> _" else (displayTitle ?: selectedGroup),
                            fontSize = 33.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MinTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MinTextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Expandable panel
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle.Default.copy(
                                color = MinTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null
                            ),
                            placeholder = {
                                Text(
                                    "Введите номер группы или преподавателя",
                                    color = MinTextSecondary.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            },
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = MinCardBg,
                                unfocusedContainerColor = MinCardBg,
                                focusedIndicatorColor = MinAccent,
                                unfocusedIndicatorColor = MinBorder,
                                cursorColor = MinAccent
                            ),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (inputText.isNotBlank()) {
                                        onGroupSelected(inputText)
                                        val newHistory = (listOf(inputText) + groupHistory)
                                            .distinct()
                                            .take(5)
                                        groupHistory = newHistory
                                        prefs.edit().putStringSet("group_history", newHistory.toSet()).apply()
                                        expanded = false
                                    }
                                }
                            )
                        )

                        // Subgroup selector
                        Spacer(modifier = Modifier.height(10.dp))
                        // Assuming MinSubgroupSegmented is defined elsewhere in the file
                        
                        // History list
                        if (groupHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            groupHistory.forEach { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            inputText = group
                                            onGroupSelected(group)
                                            expanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MinTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        group,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MinTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val newHistory = groupHistory.filter { it != group }
                                            groupHistory = newHistory
                                            prefs.edit().putStringSet("group_history", newHistory.toSet()).apply()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Удалить",
                                            tint = MinTextSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
            val hPadding = (screenWidth / 2) - 24.dp
            val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = selectedDayIndex)

            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            LaunchedEffect(listState) {
                var lastIndex = listState.firstVisibleItemIndex
                androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
                    if (index != lastIndex) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        lastIndex = index
                    }
                }
            }

            @Suppress("OPT_IN_USAGE")
            val defaultFling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)
            val flingBehavior = remember(defaultFling, listState) {
                object : androidx.compose.foundation.gestures.FlingBehavior {
                    override suspend fun androidx.compose.foundation.gestures.ScrollScope.performFling(initialVelocity: Float): Float {
                        if (kotlin.math.abs(initialVelocity) > 4000f) {
                            listState.animateScrollToItem(7)
                            selectedDayIndex = 7
                            return 0f
                        }
                        return with(defaultFling) {
                            performFling(initialVelocity)
                        }
                    }
                }
            }

            LaunchedEffect(selectedDayIndex) {
                val arrIndexForSelected = validDayIndices.indexOf(selectedDayIndex).coerceAtLeast(0)
                listState.animateScrollToItem(arrIndexForSelected)
            }

            val transition = rememberInfiniteTransition(label = "glassShimmer")
            val shimmerAlpha by transition.animateFloat(
                initialValue = 0.18f,
                targetValue = 0.45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmerAlpha"
            )

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = hPadding),
                state = listState,
                flingBehavior = flingBehavior
            ) {
                items(validDayIndices.size) { arrIndex ->
                    val index = validDayIndices[arrIndex]
                    val day = weekDays[index]
                    val isSelected = index == selectedDayIndex
                    val isToday = index == 7
                    
                    // Smooth spring morphing for selection
                    val animProgress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    )
                    val safeProgress = animProgress.coerceIn(0f, 1f)
                    
                    val targetTextColor = when {
                        styleType == StyleType.Techno -> if (isSelected) Color(0xFF00FF41) else Color(0xFF00FF41).copy(alpha = 0.7f)
                        isSelected -> if (isDarkTheme) Color.White else MinAccent
                        isToday -> MinTextPrimary
                        isDarkTheme -> Color.White.copy(alpha = 0.90f)
                        else -> Color.Black.copy(alpha = 0.90f)
                    }
                    val textColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(250))

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val scale = 1f + 0.08f * safeProgress
                                scaleX = scale
                                scaleY = scale
                                translationX = tiltX * 4.5.dp.toPx() * safeProgress
                                translationY = tiltY * 3.5.dp.toPx() * safeProgress
                            }
                            .width(58.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .drawWithContent {
                                if (safeProgress > 0.01f) {
                                    val w = size.width
                                    val h = size.height
                                    val r = 22.dp.toPx()
                                    val cornerR = androidx.compose.ui.geometry.CornerRadius(r, r)
                                    val lightOffsetX = tiltX * 20.dp.toPx()
                                    val lightOffsetY = tiltY * 16.dp.toPx()

                                    // 1. Ambient Subsurface Glass Bloom Glow
                                    drawRoundRect(
                                        color = MinAccent.copy(alpha = ((if (isDarkTheme) 0.35f else 0.45f) * safeProgress).coerceIn(0f, 1f)),
                                        topLeft = androidx.compose.ui.geometry.Offset(-3.dp.toPx() + lightOffsetX * 0.15f, -3.dp.toPx() + lightOffsetY * 0.15f),
                                        size = androidx.compose.ui.geometry.Size(w + 6.dp.toPx(), h + 6.dp.toPx()),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r + 3.dp.toPx(), r + 3.dp.toPx())
                                    )

                                    // 2. Multi-stop Directional 3D Liquid Glass Body (135 deg refraction)
                                    val baseGlassBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = if (isDarkTheme) {
                                            listOf(
                                                Color.White.copy(alpha = (0.32f * safeProgress).coerceIn(0f, 1f)),
                                                MinAccent.copy(alpha = (0.26f * safeProgress).coerceIn(0f, 1f)),
                                                Color.White.copy(alpha = (0.08f * safeProgress).coerceIn(0f, 1f)),
                                                MinAccent.copy(alpha = (0.18f * safeProgress).coerceIn(0f, 1f))
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = (0.96f * safeProgress).coerceIn(0f, 1f)),
                                                MinAccent.copy(alpha = (0.28f * safeProgress).coerceIn(0f, 1f)),
                                                Color.White.copy(alpha = (0.75f * safeProgress).coerceIn(0f, 1f)),
                                                MinAccent.copy(alpha = (0.22f * safeProgress).coerceIn(0f, 1f))
                                            )
                                        },
                                        start = androidx.compose.ui.geometry.Offset(lightOffsetX, lightOffsetY),
                                        end = androidx.compose.ui.geometry.Offset(w + lightOffsetX, h + lightOffsetY)
                                    )
                                    drawRoundRect(
                                        brush = baseGlassBrush,
                                        cornerRadius = cornerR
                                    )

                                    // 3. Upper 3D Convex Dome Specular Glare (Apple visionOS curved lens)
                                    val convexDomeHighlight = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = ((if (isDarkTheme) 0.85f else 0.99f) * safeProgress).coerceIn(0f, 1f)),
                                            Color.White.copy(alpha = ((if (isDarkTheme) 0.28f else 0.52f) * safeProgress).coerceIn(0f, 1f)),
                                            Color.Transparent
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(w * 0.5f + lightOffsetX, (h * 0.28f + lightOffsetY).coerceIn(0f, h * 0.7f)),
                                        radius = (w * 0.70f).coerceAtLeast(10f)
                                    )
                                    drawRoundRect(
                                        brush = convexDomeHighlight,
                                        size = androidx.compose.ui.geometry.Size(w, h * 0.55f),
                                        cornerRadius = cornerR
                                    )

                                    // 4. Bottom Depth / Caustic Shadow
                                    val bottomCaustic = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MinAccent.copy(alpha = (0.30f * safeProgress).coerceIn(0f, 1f))
                                        ),
                                        startY = (h * 0.52f + lightOffsetY * 0.25f).coerceIn(0f, h),
                                        endY = h
                                    )
                                    drawRoundRect(
                                        brush = bottomCaustic,
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.52f),
                                        size = androidx.compose.ui.geometry.Size(w, h * 0.48f),
                                        cornerRadius = cornerR
                                    )

                                    // 5. Dynamic Prismatic Light Wave / Shimmer Beam
                                    val shimmerBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = (shimmerAlpha * safeProgress).coerceIn(0f, 1f)),
                                            Color.Transparent
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(lightOffsetX * 0.5f, lightOffsetY * 0.5f),
                                        end = androidx.compose.ui.geometry.Offset(w + lightOffsetX * 0.5f, h + lightOffsetY * 0.5f)
                                    )
                                    drawRoundRect(
                                        brush = shimmerBrush,
                                        cornerRadius = cornerR
                                    )

                                    // 6. Prismatic Chromatic Bevel Rim (Diamond Cut Edge)
                                    val rimChromaticColor = androidx.compose.ui.graphics.Color(
                                        red = ((MinAccent.red + tiltX * 0.15f).coerceIn(0f, 1f)),
                                        green = ((MinAccent.green + tiltY * 0.15f).coerceIn(0f, 1f)),
                                        blue = ((MinAccent.blue - tiltX * 0.12f).coerceIn(0f, 1f)),
                                        alpha = (0.85f * safeProgress).coerceIn(0f, 1f)
                                    )
                                    val rimBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = (1.0f * safeProgress).coerceIn(0f, 1f)),
                                            rimChromaticColor,
                                            MinAccent.copy(alpha = (0.90f * safeProgress).coerceIn(0f, 1f)),
                                            Color.White.copy(alpha = ((if (isDarkTheme) 0.40f else 0.80f) * safeProgress).coerceIn(0f, 1f))
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(lightOffsetX, 0f),
                                        end = androidx.compose.ui.geometry.Offset(w - lightOffsetX, h)
                                    )
                                    drawRoundRect(
                                        brush = rimBrush,
                                        cornerRadius = cornerR,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
                                    )
                                }
                                drawContent()
                            }
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { 
                                selectedDayIndex = index
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = days[index].toString(),
                                fontSize = 24.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = textColor,
                                fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = day.uppercase(),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textColor,
                                fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null
                            )
                        }
                    }
                }
            }
        }

        if (filteredLessons.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Отличный день!",
                        color = MinTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "На сегодня пар не найдено",
                        color = MinTextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            filteredLessons.forEachIndexed { idx, lesson ->
                item {
                    val parallaxDepth = 1f + (idx % 3) * 0.35f
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                rotationX = (-tiltY * 3.2f * parallaxDepth).coerceIn(-6f, 6f)
                                rotationY = (tiltX * 4.2f * parallaxDepth).coerceIn(-7f, 7f)
                                cameraDistance = 12f * density
                                translationX = tiltX * 6.dp.toPx() * parallaxDepth
                                translationY = tiltY * 4.5.dp.toPx() * parallaxDepth
                            }
                    ) {
                        MinLessonCard(
                            lesson = lesson,
                            MinTextPrimary = MinTextPrimary,
                            MinTextSecondary = MinTextSecondary,
                            MinBorder = MinBorder,
                            MinAccent = MinAccent,
                            MinCardBg = MinCardBg,
                            hasNotes = subjectsWithNotes.contains(lesson.title),
                            onClick = { selectedLessonForSheet = lesson },
                            onLongClick = { noteLessonToAdd = lesson },
                            onTeacherClick = { urlId, name -> teacherScheduleData = urlId to name }
                        )
                    }
                }
            }
        }
        
        if (styleType == StyleType.Techno) {
            item {
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(800),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "BlinkingMatrixText"
                )
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    val asciiArt = """
                  _        _      
  _ __ ___   __ _| |_ _ __(_)_  __
 | '_ ` _ \ / _` | __| '__| \ \/ /
 | | | | | | (_| | |_| |  | |>  < 
 |_| |_| |_|\__,_|\__|_|  |_/_/\_\
 
  _                   
 | |__   __ _ ___     
 | '_ \ / _` / __|    
 | | | | (_| \__ \    
 |_| |_|\__,_|___/    
 
  _   _  ___  _   _   
 | | | |/ _ \| | | |  
 | |_| | (_) | |_| |  
  \__, |\___/ \__,_|  
  |___/               
                    """.trimIndent()
                    Text(
                        text = asciiArt,
                        color = Color(0xFF00FF41).copy(alpha = alpha),
                        fontFamily = vt323FontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.material3.LocalTextStyle.current.copy(lineHeight = 12.sp)
                    )
                }
            }
        }
    }
        if (noteLessonToAdd != null) {
        var noteText by remember { mutableStateOf("") }
        val dateTuple = remember(selectedDayIndex) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_MONTH, -7 + selectedDayIndex)
            Triple(cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.YEAR))
        }
        val dateString = String.format("%02d.%02d.%04d", dateTuple.first, dateTuple.second + 1, dateTuple.third)

        AlertDialog(
            onDismissRequest = { noteLessonToAdd = null },
                title = { Text("Заметка: " + noteLessonToAdd!!.title, color = androidx.compose.ui.graphics.Color.White, fontSize = 30.sp) },
            text = {
                Column {
                    Text(dateString, color = androidx.compose.ui.graphics.Color(0xFFEEEEEE), fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Введите заметку...", fontSize = 24.sp) },
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 24.sp, color = androidx.compose.ui.graphics.Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteText.isNotBlank()) {
                        val newNote = Note(subject = noteLessonToAdd!!.title, text = noteText, date = dateString)
                        val notesPrefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                        val currentNotesRaw = notesPrefs.getString("notes_data", "[]") ?: "[]"
                        val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
                        val currentNotes: List<Note> = com.google.gson.Gson().fromJson(currentNotesRaw, type) ?: emptyList()
                        val updatedNotes = currentNotes + newNote
                        notesPrefs.edit().putString("notes_data", com.google.gson.Gson().toJson(updatedNotes)).apply()
                        noteLessonToAdd = null
                    }
                }) {
                    Text("Сохранить", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteLessonToAdd = null }) { Text("Отмена", color = androidx.compose.ui.graphics.Color.White, fontSize = 20.sp) }
            },
            containerColor = MinCardBg
        )
    }

    if (selectedLessonForSheet != null) {
        val lesson = selectedLessonForSheet!!
        AlertDialog(
            onDismissRequest = { selectedLessonForSheet = null },
            title = {
                Text(lesson.subjectFullName.ifEmpty { lesson.title }, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Тип занятия", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFEEEEEE), textAlign = TextAlign.Center)
                    Text(lesson.details, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Подгруппа", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFEEEEEE), textAlign = TextAlign.Center)
                    Text(if (lesson.subgroup == 0) "Вся группа" else "${lesson.subgroup} подгруппа", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Учебные недели", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFEEEEEE), textAlign = TextAlign.Center)
                    Text(lesson.weeks.joinToString(", "), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                    
                    val upcomingDates = remember(lesson, baseCurrentWeek) {
                        val dates = mutableListOf<Pair<Int, String>>()
                        val cal = java.util.Calendar.getInstance()
                        for (offset in 1..75) {
                            cal.time = java.util.Date()
                            cal.add(java.util.Calendar.DAY_OF_MONTH, offset)
                            var dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
                            if (dow == 0) dow = 7
                            
                            val todayCal = java.util.Calendar.getInstance()
                            var w = baseCurrentWeek
                            for (i in 1..offset) {
                                todayCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                                if (todayCal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) {
                                    w = (w % 4) + 1
                                }
                            }
                            
                            if (dow == lesson.dayOfWeek && (lesson.weeks.isEmpty() || lesson.weeks.contains(w))) {
                                val dateStr = String.format("%02d.%02d", cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH)+1)
                                dates.add((7 + offset) to dateStr)
                                if (dates.size == 3) break
                            }
                        }
                        dates
                    }
                    if (upcomingDates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ближайшие занятия", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFEEEEEE), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            upcomingDates.forEach { (targetIndex, dateStr) ->
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).border(2.dp, MinTextPrimary, RoundedCornerShape(8.dp)).clickable {
                                    selectedDayIndex = targetIndex
                                    selectedLessonForSheet = null
                                }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(dateStr, fontSize = 20.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    }

    if (teacherScheduleData != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { teacherScheduleData = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MinScheduleScreen(MinBg, actualBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, MinAccent, isDarkTheme, teacherScheduleData!!.first, {}, selectedSubgroup, {}, teacherScheduleData!!.second)
                androidx.compose.material3.IconButton(
                    onClick = { teacherScheduleData = null },
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MinTextPrimary)
                }
            }
        }
    }
    
    if (showScheduleSettings) {
        AlertDialog(
            onDismissRequest = { showScheduleSettings = false },
            title = {
                Text("Настройки расписания", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Подгруппа", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Все", "1 подгр.", "2 подгр.").forEachIndexed { index, title ->
                            val isSelected = selectedSubgroup == index
                            Box(modifier = Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(Color.Transparent).border(2.dp, if (isSelected) androidx.compose.ui.graphics.Color.White else MinBorder, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).clickable { 
                                onSubgroupChange(index)
                            }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(title, fontSize = 24.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun MinLessonCard(lesson: Lesson, MinTextPrimary: Color, MinTextSecondary: Color, MinBorder: Color, MinAccent: Color, MinCardBg: Color, hasNotes: Boolean = false, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}, onTeacherClick: (String, String) -> Unit = { _, _ -> }) {
    val styleType = LocalStyleType.current
    val isTechno = styleType == StyleType.Techno
    val monoFont = if (isTechno) vt323FontFamily else null
    val technoAccent = Color(0xFF00FF41) // matrix green
    val technoCardBg = Color(0xFF0A0A0A)
    Box(
        modifier = if (isTechno) Modifier.fillMaxWidth().border(1.dp, Color(0xFF00FF41).copy(alpha = 0.3f), androidx.compose.ui.graphics.RectangleShape).padding(horizontal = 12.dp) else Modifier.fillMaxWidth()
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.width(60.dp)) {
            AutoSizeText(lesson.startTime, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = if (isTechno) technoAccent else MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple, fontFamily = monoFont))
            Spacer(modifier = Modifier.height(4.dp))
            AutoSizeText(lesson.endTime, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (isTechno) technoAccent.copy(alpha=0.6f) else MinTextSecondary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple, fontFamily = monoFont))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        val typeColor = when {
            lesson.lessonType.contains("ЛК", ignoreCase = true) -> Color(0xFF4CAF50)
            lesson.lessonType.contains("ПЗ", ignoreCase = true) -> Color(0xFFFF9800)
            lesson.lessonType.contains("ЛР", ignoreCase = true) -> Color(0xFF2196F3)
            else -> if (lesson.isActive) MinAccent else MinBorder
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (lesson.progress != null) 90.dp else 50.dp)
                .clip(if (isTechno) androidx.compose.ui.graphics.RectangleShape else androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                .background(if (isTechno) technoAccent else typeColor)
        )
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lesson.teacherPhoto.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = lesson.teacherPhoto,
                                contentDescription = "Teacher photo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(72.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable {
                                    if (lesson.teacherUrlId.isNotEmpty()) {
                                        onTeacherClick(lesson.teacherUrlId, lesson.teacherFullName)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        val displayTitle = if (lesson.subgroup != 0) "${lesson.title} (${lesson.subgroup} подгруппа)" else lesson.title
                        AutoSizeText(displayTitle, modifier = Modifier.weight(1f, fill = false), maxLines = 2, fontSize = 21.sp, fontWeight = if(lesson.isActive) FontWeight.ExtraBold else FontWeight.Bold, color = if (isTechno) technoAccent else MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple, fontFamily = monoFont))
                        if (hasNotes) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier.size(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, tint = MinAccent, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(lesson.details, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (isTechno) technoAccent.copy(alpha=0.7f) else MinTextSecondary, fontFamily = monoFont, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple))
                }
                

            }
            
            val progress = lesson.progress
            if (progress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    val totalWidth = size.width
                    
                    val w1 = totalWidth * (45f / 95f)
                    val g = totalWidth * (5f / 95f)
                    val w2 = totalWidth * (45f / 95f)
                    
                    val barBg = MinTextSecondary.copy(alpha = 0.2f)
                    drawRoundRect(color = barBg, size = androidx.compose.ui.geometry.Size(w1, size.height), cornerRadius = cornerRadius)
                    drawRoundRect(color = barBg, topLeft = Offset(w1 + g, 0f), size = androidx.compose.ui.geometry.Size(w2, size.height), cornerRadius = cornerRadius)
                    
                    val progressTime = progress * 95f
                    if (progressTime > 0f) {
                        val p1 = progressTime.coerceAtMost(45f) / 45f
                        drawRoundRect(color = MinTextPrimary, size = androidx.compose.ui.geometry.Size(w1 * p1, size.height), cornerRadius = cornerRadius)
                    }
                    if (progressTime > 50f) {
                        val p2 = (progressTime - 50f).coerceAtMost(45f) / 45f
                        drawRoundRect(color = MinTextPrimary, topLeft = Offset(w1 + g, 0f), size = androidx.compose.ui.geometry.Size(w2 * p2, size.height), cornerRadius = cornerRadius)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val progressTime = progress * 95f
                val statusText = when {
                    progressTime < 45f -> "До перерыва: ${((45f - progressTime).toInt())} мин"
                    progressTime < 50f -> "Перерыв: еще ${((50f - progressTime).toInt())} мин"
                    progressTime < 95f -> "До конца пары: ${((95f - progressTime).toInt())} мин"
                    else -> "Пара окончена"
                }
                Text(statusText, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = if (isTechno) technoAccent else MinAccent, fontFamily = monoFont)
            }
        }
    }
    } // end techno Box
}

data class SubjectMarks(val name: String, val marks: List<Int>)

data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subject: String,
    val text: String,
    val date: String
)

@Composable
fun MinMarksScreen(MinBg: androidx.compose.ui.graphics.Color, MinCardBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, currentAccent: androidx.compose.ui.graphics.Color, isDarkTheme: Boolean = true, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var marksData by remember { mutableStateOf<Map<String, List<Int>>>(emptyMap()) }
    var scheduleSubjects by remember { mutableStateOf<List<String>>(emptyList()) }
    var statusText by remember { mutableStateOf("Загрузка...") }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", "") ?: ""
                val loginGroup = prefs.getString("login_group", "") ?: ""

                val cachedSubjectsStr = prefs.getString("cached_marks_subjects", null)
                if (cachedSubjectsStr != null) {
                    try {
                        val arr = org.json.JSONArray(cachedSubjectsStr)
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) list.add(arr.getString(i))
                        withContext(kotlinx.coroutines.Dispatchers.Main) { scheduleSubjects = list }
                    } catch (e: Exception) {}
                }

                if (loginGroup.isNotBlank()) {
                    try {
                        val scheduleResp = if (loginGroup.any { it.isLetter() }) {
                            com.example.schedule.BsuirApi.getEmployeeSchedule(loginGroup)
                        } else {
                            com.example.schedule.BsuirApi.getGroupSchedule(loginGroup)
                        }
                        if (scheduleResp != null) {
                            val subs = mutableSetOf<String>()
                            scheduleResp.schedules?.values?.flatten()?.forEach { lesson ->
                                val subj = lesson.subject
                                if (!subj.isNullOrBlank() && subj != "null") subs.add(subj)
                            }
                            val sortedSubs = subs.sorted()
                            prefs.edit().putString("cached_marks_subjects", org.json.JSONArray(sortedSubs).toString()).apply()
                            withContext(kotlinx.coroutines.Dispatchers.Main) { scheduleSubjects = sortedSubs }
                        }
                    } catch (e: Exception) {}
                }
                val parseBody = { b: String ->
                    val jsonArray = if (b.trim().startsWith("[")) org.json.JSONArray(b) else org.json.JSONArray()
                    val mData = mutableMapOf<String, MutableList<Int>>()
                    for (i in 0 until jsonArray.length()) {
                        val entry = jsonArray.optJSONObject(i) ?: continue
                        val student = entry.optJSONObject("student")
                        val lessonsArray = student?.optJSONArray("lessons")
                        
                        val items = mutableListOf<org.json.JSONObject>()
                        if (lessonsArray != null) {
                            for (j in 0 until lessonsArray.length()) {
                                lessonsArray.optJSONObject(j)?.let { items.add(it) }
                            }
                        } else if (entry.has("lessonNameAbbrev") || entry.has("marks")) {
                            items.add(entry)
                        }
                        
                        for (lesson in items) {
                            val subj = lesson.optString("lessonNameAbbrev", "Неизвестно")
                            val list = mData.getOrPut(subj) { mutableListOf() }
                            val marksArr = lesson.optJSONArray("marks")
                            if (marksArr != null) {
                                for (k in 0 until marksArr.length()) {
                                    val mark = marksArr.optInt(k, -1)
                                    if (mark > 0) {
                                        list.add(mark)
                                    }
                                }
                            }
                        }
                    }
                    mData
                }

                val cachedBody = prefs.getString("cached_marks", null)
                val lastUpdate = prefs.getLong("cached_marks_time", 0L)
                val shouldUpdate = (System.currentTimeMillis() - lastUpdate) > 86400_000L

                if (cachedBody != null) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        try {
                            marksData = parseBody(cachedBody)
                            statusText = ""
                            isLoading = false
                        } catch (e: Exception) {}
                    }
                }

                if (shouldUpdate || cachedBody == null) {
                    val client = com.example.schedule.NetworkClient.client
                    val request = okhttp3.Request.Builder()
                        .url("https://iis.bsuir.by/api/v1/omissions")
                        .addHeader("Cookie", token)
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string()
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (response.isSuccessful && body != null) {
                                prefs.edit().putString("cached_marks", body).putLong("cached_marks_time", System.currentTimeMillis()).apply()
                                try {
                                    marksData = parseBody(body)
                                    statusText = ""
                                } catch (e: Exception) {
                                    if (cachedBody == null) {
                                        statusText = "Ошибка парсинга: ${e.message}"
                                    }
                                }
                            } else {
                                if (cachedBody == null) {
                                    if (response.code == 404 || response.code == 403) {
                                        statusText = ""
                                    } else {
                                        statusText = "Ошибка: ${response.code}"
                                    }
                                }
                            }
                            isLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    statusText = "Исключение: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MinBg)) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    "Текущие отметки",
                    fontSize = 26.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = MinTextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(32.dp))
            
            if (isLoading || statusText.isNotEmpty()) {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.Text(statusText, fontSize = 16.sp, color = MinTextSecondary)
                }
            } else if (scheduleSubjects.isEmpty() && marksData.isEmpty()) {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.Text("Нет текущих отметок", fontSize = 16.sp, color = MinTextSecondary)
                }
            } else {
                val allKeys = (scheduleSubjects + marksData.keys).distinct().sorted()
                for (subj in allKeys) {
                    val marksList = marksData[subj] ?: emptyList()
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(MinCardBg).padding(16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                            androidx.compose.material3.Text(subj, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            for (mark in marksList) {
                                val markColor = when (mark) {
                                    9, 10 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    7, 8 -> androidx.compose.ui.graphics.Color(0xFF8BC34A)
                                    5, 6 -> androidx.compose.ui.graphics.Color(0xFFFFC107)
                                    4 -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                                    else -> androidx.compose.ui.graphics.Color(0xFFF44336)
                                }
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(markColor),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.Text(mark.toString(), color = androidx.compose.ui.graphics.Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(4.dp))
                            }
                            if (marksList.isEmpty()) {
                                androidx.compose.material3.Text("—", fontSize = 24.sp, color = MinTextSecondary.copy(alpha = 0.5f))
                            }
                        }
                        if (marksList.isNotEmpty()) {
                            val average = marksList.average()
                            val avgColor = if (isDarkTheme) { if (average >= 8) androidx.compose.ui.graphics.Color(0xFF4CAF50) else if (average >= 6) androidx.compose.ui.graphics.Color(0xFFFF9800) else androidx.compose.ui.graphics.Color(0xFFF44336) } else { if (average >= 8) androidx.compose.ui.graphics.Color(0xFF2E7D32) else if (average >= 6) androidx.compose.ui.graphics.Color(0xFFE65100) else androidx.compose.ui.graphics.Color(0xFFB71C1C) }
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                            androidx.compose.material3.Text(String.format("%.1f", average), fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = avgColor)
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(48.dp))
        }
    }
}

data class RealOmissionLesson(
    val subject: String,
    val lessonType: String,
    val dateString: String,
    val hours: Int,
    val isRespectful: Boolean,
    val note: String,
    val teacher: String,
    val monthName: String
)

data class RealExcuseDocument(
    val title: String,
    val note: String,
    val hoursClosed: Int,
    val dates: List<String>,
    val subjects: List<String>
)

@Composable
fun MinAbsencesScreen(MinBg: androidx.compose.ui.graphics.Color, MinCardBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, currentAccent: androidx.compose.ui.graphics.Color, isDarkTheme: Boolean = true, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var totalHours by remember { mutableStateOf(0) }
    var unexcusedHours by remember { mutableStateOf(0) }
    var monthlyData by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var monthlyUnexcusedData by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var allLessons by remember { mutableStateOf<List<RealOmissionLesson>>(emptyList()) }
    var excuseDocuments by remember { mutableStateOf<List<RealExcuseDocument>>(emptyList()) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Загрузка данных из ИИС БГУИР...") }
    var isLoading by remember { mutableStateOf(true) }
    var isNotAuthorized by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", "") ?: ""
                if (token.isBlank()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isNotAuthorized = true
                        statusText = "Войдите в аккаунт ИИС БГУИР в профиле, чтобы загрузить ваши реальные пропуски и справки."
                        isLoading = false
                    }
                    return@withContext
                }

                val client = com.example.schedule.NetworkClient.client
                val request = okhttp3.Request.Builder()
                    .url("https://iis.bsuir.by/api/v1/omissions")
                    .addHeader("Cookie", token)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (response.isSuccessful && body != null) {
                            try {
                                val jsonArray = if (body.trim().startsWith("[")) org.json.JSONArray(body) else org.json.JSONArray()
                                var tHours = 0
                                var uHours = 0
                                val mData = mutableMapOf<String, Int>()
                                val muData = mutableMapOf<String, Int>()
                                val parsedLessons = mutableListOf<RealOmissionLesson>()

                                for (i in 0 until jsonArray.length()) {
                                    val entry = jsonArray.optJSONObject(i) ?: continue
                                    val student = entry.optJSONObject("student")
                                    val lessonsArray = student?.optJSONArray("lessons")
                                    
                                    val items = mutableListOf<org.json.JSONObject>()
                                    if (lessonsArray != null) {
                                        for (j in 0 until lessonsArray.length()) {
                                            lessonsArray.optJSONObject(j)?.let { items.add(it) }
                                        }
                                    } else if (entry.has("gradeBookOmissions") || entry.has("missedHours") || entry.has("nameLesson") || entry.has("subject")) {
                                        items.add(entry)
                                    }
                                    
                                    for (lesson in items) {
                                        val omissions = lesson.optInt("gradeBookOmissions", lesson.optInt("missedHours", lesson.optInt("omissions", 0)))
                                        val isRespectful = lesson.optBoolean("isRespectfulOmission", lesson.optBoolean("respectful", false))
                                        val dateString = lesson.optString("dateString", lesson.optString("date", ""))
                                        val subject = lesson.optString("nameLesson", lesson.optString("subject", lesson.optString("lessonName", "Учебное занятие")))
                                        val lessonType = lesson.optString("lessonType", lesson.optString("type", "ПЗ"))
                                        val note = lesson.optString("note", lesson.optString("reason", lesson.optString("comment", "")))
                                        val teacher = lesson.optString("teacher", lesson.optString("employee", ""))

                                        tHours += omissions
                                        if (!isRespectful) uHours += omissions
                                        
                                        var mName = "Другое"
                                        if (dateString.length >= 5) {
                                            val parts = dateString.split(".", "-")
                                            val monthIndex = if (parts.size >= 2) {
                                                if (parts[0].length == 4) parts[1].toIntOrNull() else parts[1].toIntOrNull()
                                            } else null
                                            if (monthIndex != null && monthIndex in 1..12) {
                                                val monthNames = arrayOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
                                                mName = monthNames[monthIndex - 1]
                                                mData[mName] = (mData[mName] ?: 0) + omissions
                                                if (!isRespectful) {
                                                    muData[mName] = (muData[mName] ?: 0) + omissions
                                                }
                                            }
                                        }

                                        if (omissions > 0) {
                                            parsedLessons.add(
                                                RealOmissionLesson(
                                                    subject = subject,
                                                    lessonType = lessonType,
                                                    dateString = dateString,
                                                    hours = omissions,
                                                    isRespectful = isRespectful,
                                                    note = note,
                                                    teacher = teacher,
                                                    monthName = mName
                                                )
                                            )
                                        }
                                    }
                                }

                                // Extract real excuse documents from respectful omissions
                                val respectfulList = parsedLessons.filter { it.isRespectful }
                                val docsMap = mutableMapOf<String, MutableList<RealOmissionLesson>>()
                                for (rl in respectfulList) {
                                    val key = if (rl.note.isNotBlank()) rl.note else "Справка / Заявление в деканат"
                                    docsMap.getOrPut(key) { mutableListOf() }.add(rl)
                                }

                                val parsedDocs = docsMap.map { (key, list) ->
                                    val docHours = list.sumOf { it.hours }
                                    val docDates = list.map { it.dateString }.distinct().take(4)
                                    val docSubjects = list.map { it.subject }.distinct().take(3)
                                    RealExcuseDocument(
                                        title = if (key.contains("справ", ignoreCase = true) || key.contains("095", ignoreCase = true)) "Медицинская справка" else if (key.contains("заявл", ignoreCase = true)) "Заявление деканата" else "Оправдательный документ",
                                        note = key,
                                        hoursClosed = docHours,
                                        dates = docDates,
                                        subjects = docSubjects
                                    )
                                }

                                totalHours = tHours
                                unexcusedHours = uHours
                                monthlyData = mData
                                monthlyUnexcusedData = muData
                                allLessons = parsedLessons
                                excuseDocuments = parsedDocs
                                statusText = ""
                                isLoading = false
                            } catch (e: Exception) {
                                statusText = "Ошибка парсинга: ${e.message}"
                                isLoading = false
                            }
                        } else {
                            if (response.code == 401 || response.code == 403) {
                                isNotAuthorized = true
                                statusText = "Сессия ИИС БГУИР устарела. Пожалуйста, перезайдите в профиле."
                            } else {
                                statusText = "Ошибка сервера: ${response.code} ${response.message}"
                            }
                            isLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    statusText = "Ошибка сети: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    val excusedHours = (totalHours - unexcusedHours).coerceAtLeast(0)
    val filteredByMonthLessons = remember(allLessons, selectedMonth) {
        if (selectedMonth == null) allLessons else allLessons.filter { it.monthName == selectedMonth }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(MinBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = androidx.compose.ui.Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                androidx.compose.material3.Text(
                    "Пропуски и справки",
                    fontSize = 26.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = MinTextPrimary
                )
            }
        }

        if (isLoading) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(250.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = currentAccent)
                }
            }
        } else if (isNotAuthorized) {
            item {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MinCardBg)
                        .border(1.dp, MinBorder, RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = currentAccent,
                        modifier = androidx.compose.ui.Modifier.size(42.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(14.dp))
                    androidx.compose.material3.Text(
                        "Авторизация в ИИС БГУИР",
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MinTextPrimary
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        statusText,
                        fontSize = 13.sp,
                        color = MinTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Stats Overview Cards
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    // Unexcused Card
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MinCardBg)
                            .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            "$unexcusedHours ч",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = currentAccent
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            "Без ув. причины",
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MinTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Excused (Closed by docs) Card
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MinCardBg)
                            .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            "$excusedHours ч",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            "По справкам",
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MinTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Total Card
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MinCardBg)
                            .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            "$totalHours ч",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            color = MinTextPrimary
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            "Всего часов",
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MinTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Interactive Monthly Chart
            item {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MinCardBg)
                        .border(1.dp, MinBorder, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            "График по месяцам",
                            fontSize = 18.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MinTextPrimary
                        )
                        if (selectedMonth != null) {
                            val selTotal = monthlyData[selectedMonth] ?: 0
                            val selUn = monthlyUnexcusedData[selectedMonth] ?: 0
                            androidx.compose.material3.Text(
                                "$selectedMonth: $selTotal ч ($selUn без ув.) [Сброс]",
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = currentAccent,
                                modifier = androidx.compose.ui.Modifier.clickable { selectedMonth = null }
                            )
                        }
                    }
                    
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))

                    val academicMonths = listOf("Сен", "Окт", "Ноя", "Дек", "Янв", "Фев", "Мар", "Апр", "Май", "Июн")
                    val maxVal = monthlyData.values.maxOrNull()?.coerceAtLeast(10) ?: 10

                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.Bottom
                    ) {
                        academicMonths.forEach { m ->
                            val t = monthlyData[m] ?: 0
                            val u = monthlyUnexcusedData[m] ?: 0
                            val isSel = selectedMonth == m
                            val heightRatio = if (t > 0) (t.toFloat() / maxVal).coerceIn(0.12f, 1f) else 0.05f

                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedMonth = if (selectedMonth == m) null else m
                                    },
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
                            ) {
                                if (t > 0) {
                                    androidx.compose.material3.Text(
                                        "$t",
                                        fontSize = 10.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = if (isSel) currentAccent else MinTextSecondary
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                                }

                                // Stacked Bar
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .width(if (isSel) 22.dp else 16.dp)
                                        .height((120 * heightRatio).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                        .background(if (t == 0) MinBorder.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                ) {
                                    if (u > 0 && t > 0) {
                                        val unexcusedRatio = (u.toFloat() / t).coerceIn(0f, 1f)
                                        androidx.compose.foundation.layout.Box(
                                            modifier = androidx.compose.ui.Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(unexcusedRatio)
                                                .align(androidx.compose.ui.Alignment.BottomCenter)
                                                .background(currentAccent)
                                        )
                                    }
                                }

                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                androidx.compose.material3.Text(
                                    m,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = if (isSel) currentAccent else MinTextSecondary
                                )
                            }
                        }
                    }

                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                    // Legend
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))
                            androidx.compose.material3.Text("Уважительные (справки)", fontSize = 11.sp, color = MinTextSecondary)
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(18.dp))
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(currentAccent)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))
                            androidx.compose.material3.Text("Без ув. причины", fontSize = 11.sp, color = MinTextSecondary)
                        }
                    }
                }
            }

            // Real Excusing Documents Section ("Оправдательные документы")
            item {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            "Оправдательные документы",
                            fontSize = 19.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MinTextPrimary
                        )
                        androidx.compose.material3.Text(
                            "Памятка",
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = currentAccent,
                            modifier = androidx.compose.ui.Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showGuidelines = !showGuidelines }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (excuseDocuments.isEmpty() && totalHours > 0) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MinCardBg)
                                .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                "Все пропуски на текущий момент без уважительной причины. Сдайте справку в деканат для списания часов.",
                                fontSize = 13.sp,
                                color = MinTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else if (totalHours == 0) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MinCardBg)
                                .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                "Отлично! У вас 0 пропусков в базе данных ИИС БГУИР.",
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        excuseDocuments.forEach { doc ->
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MinCardBg)
                                    .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    androidx.compose.foundation.layout.Box(
                                        modifier = androidx.compose.ui.Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.15f)),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        androidx.compose.material3.Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                            modifier = androidx.compose.ui.Modifier.size(22.dp)
                                        )
                                    }
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                        androidx.compose.material3.Text(
                                            doc.title,
                                            fontSize = 15.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MinTextPrimary
                                        )
                                        androidx.compose.material3.Text(
                                            doc.note,
                                            fontSize = 12.sp,
                                            color = MinTextSecondary
                                        )
                                    }
                                    androidx.compose.foundation.layout.Box(
                                        modifier = androidx.compose.ui.Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        androidx.compose.material3.Text(
                                            "${doc.hoursClosed} ч списано",
                                            fontSize = 11.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                        )
                                    }
                                }

                                if (doc.subjects.isNotEmpty()) {
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                    androidx.compose.material3.Text(
                                        "Предметы: " + doc.subjects.joinToString(", "),
                                        fontSize = 11.sp,
                                        color = MinTextSecondary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Guidelines block
                    AnimatedVisibility(visible = showGuidelines) {
                        androidx.compose.foundation.layout.Column(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MinCardBg)
                                .border(1.dp, currentAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "Памятка подачи документов в деканат:",
                                fontSize = 15.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = currentAccent
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                            androidx.compose.material3.Text(
                                "1. Медицинская справка должна быть заверена в 33-й поликлинике и сдана в деканат в течение 3 рабочих дней после закрытия.\n" +
                                "2. Заявления по семейным обстоятельствам подаются заранее на имя декана факультета.\n" +
                                "3. После внесения справки сотрудником деканата часы в ИИС БГУИР автоматически пересчитываются в категорию уважительных.",
                                fontSize = 13.sp,
                                color = MinTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Real Detailed Missed Lessons List
            if (filteredByMonthLessons.isNotEmpty()) {
                item {
                    androidx.compose.material3.Text(
                        if (selectedMonth != null) "Пропуски за $selectedMonth" else "Детализация пропущенных занятий",
                        fontSize = 19.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MinTextPrimary
                    )
                }

                items(filteredByMonthLessons.size) { index ->
                    val lesson = filteredByMonthLessons[index]
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MinCardBg)
                            .border(1.dp, MinBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = androidx.compose.ui.Modifier.weight(1f)
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MinBorder.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                    androidx.compose.material3.Text(
                                        lesson.lessonType,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MinTextPrimary
                                    )
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                                androidx.compose.material3.Text(
                                    lesson.subject,
                                    fontSize = 14.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MinTextPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            
                            androidx.compose.material3.Text(
                                "${lesson.hours} ч",
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                color = if (lesson.isRespectful) androidx.compose.ui.graphics.Color(0xFF4CAF50) else currentAccent
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Text(
                                lesson.dateString + if (lesson.teacher.isNotBlank()) " • ${lesson.teacher}" else "",
                                fontSize = 12.sp,
                                color = MinTextSecondary
                            )
                            androidx.compose.material3.Text(
                                if (lesson.isRespectful) "Уважительная" else "Без уважительной",
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = if (lesson.isRespectful) androidx.compose.ui.graphics.Color(0xFF4CAF50) else currentAccent
                            )
                        }

                        if (lesson.note.isNotBlank()) {
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                            androidx.compose.material3.Text(
                                "Основание: ${lesson.note}",
                                fontSize = 11.sp,
                                color = MinTextSecondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinGroupScreen(MinBg: androidx.compose.ui.graphics.Color, MinCardBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, isDarkTheme: Boolean = true, onBack: () -> Unit) {
    var students by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", null)
                if (token == null) {
                    errorMessage = "Требуется авторизация"
                    isLoading = false
                    return@withContext
                }

                val client = com.example.schedule.NetworkClient.client
                val request = okhttp3.Request.Builder()
                    .url("https://iis.bsuir.by/api/v1/grade-book/group-students")
                    .addHeader("Cookie", token)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (response.isSuccessful && body != null) {
                            try {
                                val list = mutableListOf<String>()
                                val jsonArray = org.json.JSONArray(body)
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.optJSONObject(i)
                                    val fio = obj?.optString("fio")
                                    if (!fio.isNullOrEmpty()) list.add(fio)
                                }
                                students = list
                                isLoading = false
                            } catch (e: Exception) {
                                errorMessage = "Ошибка данных: ${e.message}"
                                isLoading = false
                            }
                        } else {
                            errorMessage = "Ошибка ${response.code}: Нет доступа к списку группы"
                            isLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    errorMessage = "Ошибка сети: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MinBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        item {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.KeyboardArrowLeft, contentDescription = "Назад", tint = MinTextPrimary, modifier = androidx.compose.ui.Modifier.size(32.dp).clickable { onBack() })
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                androidx.compose.material3.Text("Список группы", fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = MinTextPrimary)
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = MinTextPrimary)
                }
            }
        } else if (errorMessage != null) {
            item {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.Text(errorMessage ?: "", color = androidx.compose.ui.graphics.Color(0xFFF44336), fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else if (students.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.Text("Список пуст", color = MinTextSecondary, fontSize = 18.sp)
                }
            }
        } else {
            items(students.size) { index ->
                val FIO = students[index]
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MinCardBg)
                        .border(1.dp, MinBorder, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MinTextPrimary.copy(alpha = 0.1f)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Text("${index + 1}", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                    androidx.compose.material3.Text(FIO, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = MinTextPrimary)
                }
            }
        }
    }
}

@Composable
fun MinStudyScreen(MinBg: androidx.compose.ui.graphics.Color, MinCardBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, currentAccent: androidx.compose.ui.graphics.Color, isDarkTheme: Boolean = true, onBack: () -> Unit) {
    var showMarksheets by remember { mutableStateOf(false) }

    if (showMarksheets) {
        androidx.activity.compose.BackHandler(onBack = { showMarksheets = false })
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MinBg)) {
            MinGenericApiScreen("Заказ ведомостей", "https://iis.bsuir.by/api/v1/mark-sheet-requests", MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent) { showMarksheets = false }
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MinBg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 40.dp)
        ) {
            item {
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Назад", tint = MinTextPrimary, modifier = androidx.compose.ui.Modifier.size(32.dp).clickable { onBack() })
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    androidx.compose.material3.Text("Учеба", fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = MinTextPrimary)
                }
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(32.dp))
            }
            item {
                MinListAction("Заказ ведомостей", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.List) {
                    showMarksheets = true
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MinGradebookScreen(MinBg: androidx.compose.ui.graphics.Color, MinCardBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, isDarkTheme: Boolean = true, onBack: () -> Unit) {
    var semesters by remember { mutableStateOf<List<Pair<String, List<Pair<String, String>>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", null)
                if (token == null) {
                    errorMessage = "Необходима авторизация"
                    isLoading = false
                    return@withContext
                }

                val parseBody = { bodyStr: String ->
                    val jsonObj = org.json.JSONObject(bodyStr)
                    val markPages = jsonObj.optJSONObject("markPages")
                    val parsedSemesters = mutableListOf<Pair<Int, Pair<String, List<Pair<String, String>>>>>()
                    if (markPages != null) {
                        val keys = markPages.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val page = markPages.optJSONObject(key)
                            if (page != null) {
                                val marksArray = page.optJSONArray("marks") ?: org.json.JSONArray()
                                val subjects = mutableListOf<Pair<String, String>>()
                                for (i in 0 until marksArray.length()) {
                                    val markObj = marksArray.optJSONObject(i)
                                    if (markObj != null) {
                                        val subject = markObj.optString("subject", "Неизвестно")
                                        val rawMark = markObj.optString("mark", "")
                                        val formOfControl = markObj.optString("formOfControl", "")
                                        val mark = if (rawMark.isBlank() || rawMark == "null") {
                                            if (formOfControl.isNotBlank()) "— ($formOfControl)" else "—"
                                        } else {
                                            rawMark
                                        }
                                        subjects.add(subject to mark)
                                    }
                                }
                                val semNum = key.toIntOrNull() ?: 1
                                parsedSemesters.add(semNum to ("Семестр $semNum" to subjects))
                            }
                        }
                    }
                    parsedSemesters.sortBy { it.first }
                    parsedSemesters.map { it.second }
                }

                val cachedBody = prefs.getString("cached_gradebook", null)
                if (cachedBody != null) {
                    withContext(Dispatchers.Main) {
                        try {
                            semesters = parseBody(cachedBody)
                            errorMessage = null
                            isLoading = false
                        } catch (e: Exception) {}
                    }
                }

                val client = com.example.schedule.NetworkClient.client
                val request = okhttp3.Request.Builder()
                    .url("https://iis.bsuir.by/api/v1/markbook")
                    .addHeader("Cookie", token)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            prefs.edit().putString("cached_gradebook", body).apply()
                            try {
                                semesters = parseBody(body)
                                errorMessage = null
                                isLoading = false
                            } catch (e: Exception) {
                                if (cachedBody == null) {
                                    errorMessage = "Ошибка парсинга данных: ${e.message}"
                                }
                                isLoading = false
                            }
                        } else {
                            if (cachedBody == null) {
                                if (response.code == 404 || response.code == 403) {
                                    errorMessage = null
                                } else {
                                    errorMessage = "Ошибка загрузки: ${response.code}"
                                }
                            }
                            isLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Ошибка сети: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(MinBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Назад", tint = MinTextPrimary, modifier = Modifier.size(32.dp).clickable { onBack() })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Зачетка", fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = MinTextPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = MinTextPrimary)
                }
            }
        } else if (errorMessage != null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(errorMessage ?: "", color = androidx.compose.ui.graphics.Color(0xFFF44336), fontSize = 18.sp)
                }
            }
        } else if (semesters.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Нет данных о сессиях", color = MinTextSecondary, fontSize = 18.sp)
                }
            }
        } else {
            semesters.forEach { (semesterName, subjects) ->
                item {
                    Text(semesterName, fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(MinCardBg)
                            .border(1.dp, MinBorder, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        subjects.forEachIndexed { index, (subject, mark) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    subject,
                                    fontSize = 22.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = MinTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                val markColor = when {
                                    mark.startsWith("—") -> MinTextSecondary
                                    mark in listOf("9", "10", "Зачет", "зач", "зач.") -> if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                    mark in listOf("7", "8") -> if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF2196F3) else androidx.compose.ui.graphics.Color(0xFF1565C0)
                                    mark in listOf("4", "5", "6") -> if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFFFF9800) else androidx.compose.ui.graphics.Color(0xFFE65100)
                                    else -> if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFFF44336) else androidx.compose.ui.graphics.Color(0xFFB71C1C)
                                }
                                Text(
                                    mark,
                                    fontSize = 24.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = markColor
                                )
                            }
                            if (index < subjects.size - 1) {
                                androidx.compose.material3.HorizontalDivider(color = MinBorder.copy(alpha=0.5f), thickness = 1.dp)
                            }
                        }
                        
                        val validMarks = subjects.mapNotNull { it.second.toIntOrNull() }
                        if (validMarks.isNotEmpty()) {
                            val average = validMarks.average()
                            val avgColor = if (isDarkTheme) { if (average >= 8) androidx.compose.ui.graphics.Color(0xFF4CAF50) else if (average >= 6) androidx.compose.ui.graphics.Color(0xFFFF9800) else androidx.compose.ui.graphics.Color(0xFFF44336) } else { if (average >= 8) androidx.compose.ui.graphics.Color(0xFF2E7D32) else if (average >= 6) androidx.compose.ui.graphics.Color(0xFFE65100) else androidx.compose.ui.graphics.Color(0xFFB71C1C) }

                            androidx.compose.material3.HorizontalDivider(color = MinBorder.copy(alpha=0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(avgColor.copy(alpha = 0.1f)).padding(12.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Средний балл", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                                Text(String.format("%.2f", average), fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = avgColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MinMarksScreen(MinBg: Color, MinCardBg: Color, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, isDarkTheme: Boolean = true) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dataManager = remember { DataManager(context) }
    val mockData = remember { dataManager.getMarks() }
    
    val styleType = LocalStyleType.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Оценки", fontSize = 33.sp, fontWeight = FontWeight.ExtraBold, color = MinTextPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(mockData.size) { index ->
            val subject = mockData[index]
            val average = if (subject.marks.isNotEmpty()) subject.marks.average() else 0.0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(MinCardBg)
                    .border(1.dp, MinBorder, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (subject.name.length > 20) subject.name.take(17) + "..." else subject.name,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MinTextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (subject.marks.isNotEmpty()) {
                        val avgColor = if (isDarkTheme) { if (average >= 8) Color(0xFF4CAF50) else if (average >= 6) Color(0xFFFF9800) else Color(0xFFF44336) } else { if (average >= 8) Color(0xFF2E7D32) else if (average >= 6) Color(0xFFE65100) else Color(0xFFB71C1C) }
                        Box(
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(avgColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(String.format(java.util.Locale.US, "%.1f", average), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = avgColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subject.marks.forEach { mark ->
                        val markColor = if (isDarkTheme) { if (mark >= 8) Color(0xFF4CAF50) else if (mark >= 6) Color(0xFFFF9800) else Color(0xFFF44336) } else { if (mark >= 8) Color(0xFF2E7D32) else if (mark >= 6) Color(0xFFE65100) else Color(0xFFB71C1C) }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(markColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mark.toString(), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = markColor)
                        }
                    }
                    if (subject.marks.isEmpty()) {
                        Text("Нет оценок", fontSize = 19.sp, color = MinTextSecondary)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MinNotesScreen(MinBg: Color, MinCardBg: Color, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, MinAccent: Color, selectedGroup: String = "114001") {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE) }
    val gson = remember { com.google.gson.Gson() }

    var predefinedSubjects by remember { mutableStateOf<List<String>>(emptyList()) }
    
    androidx.compose.runtime.LaunchedEffect(selectedGroup) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val appPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val targetGroup = if (selectedGroup.isNotBlank()) selectedGroup else appPrefs.getString("login_group", "114001") ?: "114001"
                val token = appPrefs.getString("auth_token", "") ?: ""

                val sessionSubjects = mutableSetOf<String>()

                // 1. Fetch from group schedule (сессия и расписание текущего семестра)
                if (targetGroup.isNotBlank()) {
                    try {
                        val scheduleResp = if (targetGroup.any { it.isLetter() }) {
                            com.example.schedule.BsuirApi.getEmployeeSchedule(targetGroup)
                        } else {
                            com.example.schedule.BsuirApi.getGroupSchedule(targetGroup)
                        }
                        
                        scheduleResp?.exams?.forEach { examLesson ->
                            val sName = examLesson.subject?.takeIf { it.isNotBlank() } ?: examLesson.subjectFullName
                            if (!sName.isNullOrBlank()) sessionSubjects.add(sName.trim())
                        }

                        scheduleResp?.schedules?.forEach { (_, dayLessons) ->
                            dayLessons.forEach { lesson ->
                                val sName = lesson.subject?.takeIf { it.isNotBlank() } ?: lesson.subjectFullName
                                if (!sName.isNullOrBlank()) sessionSubjects.add(sName.trim())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 2. Fetch from student markbook (все экзамены, зачеты и курсовые текущего семестра)
                val parseMarkbookCurrentSemester: (String) -> Unit = { bodyStr ->
                    try {
                        val jsonObj = org.json.JSONObject(bodyStr)
                        val markPages = jsonObj.optJSONObject("markPages")
                        if (markPages != null) {
                            val semesterNumbers = mutableListOf<Int>()
                            val keys = markPages.keys()
                            while (keys.hasNext()) {
                                keys.next().toIntOrNull()?.let { semesterNumbers.add(it) }
                            }
                            
                            val sortedSems = semesterNumbers.sortedDescending()
                            for (semNum in sortedSems) {
                                val semObj = markPages.optJSONObject(semNum.toString())
                                val marksArray = semObj?.optJSONArray("marks") ?: org.json.JSONArray()
                                if (marksArray.length() > 0) {
                                    for (i in 0 until marksArray.length()) {
                                        val markObj = marksArray.optJSONObject(i) ?: continue
                                        val subject = markObj.optString("subject", "").trim()
                                        if (subject.isNotBlank()) {
                                            sessionSubjects.add(subject)
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val cachedMarkbook = appPrefs.getString("cached_gradebook", null)
                if (cachedMarkbook != null) {
                    parseMarkbookCurrentSemester(cachedMarkbook)
                }

                if (token.isNotBlank()) {
                    try {
                        val client = com.example.schedule.NetworkClient.client
                        val request = okhttp3.Request.Builder()
                            .url("https://iis.bsuir.by/api/v1/markbook")
                            .addHeader("Cookie", token)
                            .get()
                            .build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                appPrefs.edit().putString("cached_gradebook", body).apply()
                                parseMarkbookCurrentSemester(body)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (sessionSubjects.isNotEmpty()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        predefinedSubjects = sessionSubjects.toList().sorted()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var selectedSubject by remember { mutableStateOf("Все") }
    var noteText by remember { mutableStateOf("") }

    val todayCal = remember { java.util.Calendar.getInstance() }
    var selectedDay by remember { mutableStateOf(todayCal.get(java.util.Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableStateOf(todayCal.get(java.util.Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(todayCal.get(java.util.Calendar.YEAR)) }
    var viewMonth by remember { mutableStateOf(todayCal.get(java.util.Calendar.MONTH)) }
    var viewYear by remember { mutableStateOf(todayCal.get(java.util.Calendar.YEAR)) }

    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }

    // Reload notes every time this composable becomes active
    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
            notes = com.google.gson.Gson().fromJson<List<Note>>(prefs.getString("notes_data", "[]"), type) ?: emptyList()
        } catch (e: Exception) { notes = emptyList() }
    }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    var isCalendarExpanded by remember { mutableStateOf(false) }
    var radialExpanded by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = noteToDelete != null || radialExpanded || isCalendarExpanded) {
        if (noteToDelete != null) {
            noteToDelete = null
        } else if (radialExpanded) {
            radialExpanded = false
        } else if (isCalendarExpanded) {
            isCalendarExpanded = false
        }
    }

    fun formatDate(d: Int, m: Int, y: Int) = "%02d.%02d.%04d".format(d, m + 1, y)

    fun scheduleAlarm(context: android.content.Context, note: Note, d: Int, m: Int, y: Int) {
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(context, NoteReminderReceiver::class.java).apply {
            putExtra("SUBJECT", note.subject)
            putExtra("TEXT", note.text)
        }
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, y)
            set(java.util.Calendar.MONTH, m)
            set(java.util.Calendar.DAY_OF_MONTH, d)
            add(java.util.Calendar.DAY_OF_MONTH, -1)
            set(java.util.Calendar.HOUR_OF_DAY, 18)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, note.id.hashCode(), intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } catch (e: SecurityException) { }
        }
    }

    fun saveNotes(list: List<Note>) { notes = list; prefs.edit().putString("notes_data", gson.toJson(list)).apply() }

    val allSubjects = remember(notes, predefinedSubjects) { (predefinedSubjects + notes.map { it.subject }.distinct()).distinct() }
    val subjects = listOf("Все") + allSubjects
    val highlightedDates = remember(notes, selectedSubject) {
        (if (selectedSubject == "Все") notes else notes.filter { it.subject == selectedSubject }).map { it.date }.toSet()
    }
    val subjectNotes = remember(notes, selectedSubject) {
        (if (selectedSubject == "Все") notes else notes.filter { it.subject == selectedSubject }).sortedByDescending { it.date }
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Удалить заметку?", color = androidx.compose.ui.graphics.Color.White) },
            text = { Text("Удержите для удаления. Действие нельзя отменить.", color = androidx.compose.ui.graphics.Color(0xFFEEEEEE)) },
            confirmButton = {
                TextButton(onClick = { saveNotes(notes.filter { it.id != noteToDelete!!.id }); noteToDelete = null }) {
                    Text("Удалить", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Отмена", color = androidx.compose.ui.graphics.Color.White) }
            },
            containerColor = Color.Transparent
        )
    }

    val styleType = LocalStyleType.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Заметки", fontSize = 33.sp, fontWeight = FontWeight.ExtraBold, color = MinTextPrimary)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().zIndex(10f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {

                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                            .clickable { radialExpanded = !radialExpanded }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedSubject, color = MinTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(if (radialExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MinTextSecondary)
                        }
                    }
                    DropdownMenu(
                        expanded = radialExpanded,
                        onDismissRequest = { radialExpanded = false },
                        modifier = Modifier.background(MinBg)
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject, color = if (subject == selectedSubject) MinAccent else MinTextPrimary, fontWeight = FontWeight.Bold) },
                                onClick = { selectedSubject = subject; radialExpanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MinBorder, RoundedCornerShape(16.dp))
                        .clickable { isCalendarExpanded = !isCalendarExpanded }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDate(selectedDay, selectedMonth, selectedYear), color = MinTextPrimary, fontWeight = FontWeight.Bold)
                        Icon(if (isCalendarExpanded) Icons.Outlined.KeyboardArrowLeft else Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MinTextSecondary)
                    }
                }
            }

            AnimatedVisibility(visible = isCalendarExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    MinNotesCalendar(
                        viewMonth = viewMonth, viewYear = viewYear,
                        selectedDay = selectedDay, selectedMonth = selectedMonth, selectedYear = selectedYear,
                        highlightedDates = highlightedDates,
                        MinTextPrimary = MinTextPrimary, MinTextSecondary = MinTextSecondary,
                        MinBorder = MinBorder, MinAccent = MinAccent, MinBg = MinBg,
                        onDaySelected = { d, m, y -> selectedDay = d; selectedMonth = m; selectedYear = y; isCalendarExpanded = false },
                        onPrevMonth = { if (viewMonth == 0) { viewMonth = 11; viewYear-- } else viewMonth-- },
                        onNextMonth = { if (viewMonth == 11) { viewMonth = 0; viewYear++ } else viewMonth++ }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Введите заметку...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MinTextPrimary,
                    unfocusedTextColor = MinTextPrimary,
                    cursorColor = MinTextPrimary,
                    focusedBorderColor = MinTextPrimary,
                    unfocusedBorderColor = MinBorder,
                    focusedPlaceholderColor = MinTextSecondary,
                    unfocusedPlaceholderColor = MinTextSecondary
                ),
                maxLines = 6
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.TextButton(
                onClick = {
                    if (noteText.isNotBlank()) {
                        val subjectForNote = if (selectedSubject == "Все") (allSubjects.firstOrNull() ?: "Общее") else selectedSubject
                        val newNote = Note(subject = subjectForNote, text = noteText, date = formatDate(selectedDay, selectedMonth, selectedYear))
                        saveNotes(notes + newNote)
                        scheduleAlarm(context, newNote, selectedDay, selectedMonth, selectedYear)
                        noteText = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MinAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
            }
        }
        if (subjectNotes.isNotEmpty()) {
            item {
                Text("Сохранённые", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary, letterSpacing = 2.sp)
            }
            items(subjectNotes, key = { it.id }) { note ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                        .combinedClickable(onClick = {}, onLongClick = { noteToDelete = note })
                        .padding(16.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(note.subject, modifier = Modifier.weight(1f, fill = false), maxLines = 1, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MinAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(note.date, fontSize = 17.sp, color = MinTextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(note.text, fontSize = 20.sp, color = MinTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun MinNotesCalendar(
    viewMonth: Int, viewYear: Int,
    selectedDay: Int, selectedMonth: Int, selectedYear: Int,
    highlightedDates: Set<String>,
    MinTextPrimary: Color, MinTextSecondary: Color,
    MinBorder: Color, MinAccent: Color, MinBg: Color,
    onDaySelected: (Int, Int, Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthNames = listOf("Январь","Февраль","Март","Апрель","Май","Июнь","Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
    val dayNames = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
    val cal = remember(viewYear, viewMonth) { java.util.Calendar.getInstance().apply { set(viewYear, viewMonth, 1) } }
    val firstDayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, MinBorder, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onPrevMonth() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = null, tint = MinTextPrimary, modifier = Modifier.size(20.dp))
            }
            Text("${monthNames[viewMonth]} $viewYear", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple))
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onNextMonth() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MinTextPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 16.sp, color = MinTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val rows = (firstDayOfWeek + daysInMonth + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - firstDayOfWeek + 1
                    val isValid = dayNum in 1..daysInMonth
                    val dateStr = if (isValid) "%02d.%02d.%04d".format(dayNum, viewMonth + 1, viewYear) else ""
                    val isHighlighted = isValid && highlightedDates.contains(dateStr)
                    val isSelected = isValid && dayNum == selectedDay && viewMonth == selectedMonth && viewYear == selectedYear
                    Box(
                        modifier = Modifier
                            .weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape)
                            .background(if (isHighlighted && !isSelected) MinTextPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .then(if (isSelected) Modifier.border(1.dp, MinTextPrimary, CircleShape) else Modifier)
                            .then(if (isValid) Modifier.clickable { onDaySelected(dayNum, viewMonth, viewYear) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isValid) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    dayNum.toString(),
                                    fontSize = 17.sp,
                                    fontWeight = if (isSelected || isHighlighted) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = MinTextPrimary
                                )
                                if (isHighlighted && !isSelected) {
                                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MinAccent))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinProfileScreen(MinBg: Color, MinCardBg: Color, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, isDarkTheme: Boolean, currentAccent: Color, particlesEnabled: Boolean, particleSizeMultiplier: Float, transitionsEnabled: Boolean, transitionType: TransitionType, transitionSpeedMultiplier: Float, fontFamily: androidx.compose.ui.text.font.FontFamily, textSizeMultiplier: Float, bgMode: String, bgImageUri: String?, bgBlur: Float, bgDim: Float, bgEmoji: String, customParticleColor: Color?, onThemeToggle: () -> Unit, onAccentChange: (Color) -> Unit, onParticlesToggle: (Boolean) -> Unit, onParticleSizeChange: (Float) -> Unit, onTransitionsToggle: (Boolean) -> Unit, onTransitionTypeChange: (TransitionType) -> Unit, onTransitionSpeedChange: (Float) -> Unit, onFontChange: (androidx.compose.ui.text.font.FontFamily) -> Unit, onTextSizeChange: (Float) -> Unit, onPrimaryColorChange: (Color?) -> Unit, onBackgroundColorChange: (Color?) -> Unit, onBgModeChange: (String) -> Unit, onBgImageUriChange: (String?) -> Unit, onBgBlurChange: (Float) -> Unit, onBgDimChange: (Float) -> Unit, onBgEmojiChange: (String) -> Unit, onParticleColorChange: (Color?) -> Unit, onStyleChange: (StyleType) -> Unit) {
    var showCustomization by remember { mutableStateOf(false) }
    var showGradebook by remember { mutableStateOf(false) }
    var showMarks by remember { mutableStateOf(false) }
    var showGroupScreen by remember { mutableStateOf(false) }
    var showStudyScreen by remember { mutableStateOf(false) }
    var showAnnouncements by remember { mutableStateOf(false) }
    var showDormitory by remember { mutableStateOf(false) }
    var showBenefits by remember { mutableStateOf(false) }
    var showPenalties by remember { mutableStateOf(false) }
    var showAbsences by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var userFio by remember { mutableStateOf("Загрузка...") }
    var userDesc by remember { mutableStateOf("Загрузка...") }
    var userPhoto by remember { mutableStateOf("") }
    var userGroup by remember { mutableStateOf("Загрузка...") }
    var userBirthDate by remember { mutableStateOf("Не указана") }
    var showGroupDescToggle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(4000)
            showGroupDescToggle = !showGroupDescToggle
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""
        val cachedFio = prefs.getString("login_fio", null)
        val cachedGroup = prefs.getString("login_group", null)
        val cachedPhoto = prefs.getString("login_photo", null)
        val cachedDesc = prefs.getString("login_desc", null)

        if (cachedFio != null) {
            userFio = cachedFio
            userDesc = cachedDesc ?: cachedGroup ?: "БГУИР"
            userPhoto = cachedPhoto ?: ""
            userGroup = cachedGroup ?: "Неизвестно"

            if (cachedGroup != null) {
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val client = com.example.schedule.NetworkClient.client
                        val request = NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/student-groups")
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val jsonArray = org.json.JSONArray(body)
                            for (i in 0 until jsonArray.length()) {
                                val groupObj = jsonArray.optJSONObject(i)
                                if (groupObj?.optString("name") == cachedGroup) {
                                    val fac = groupObj.optString("facultyAbbrev", "")
                                    val spec = groupObj.optString("specialityAbbrev", "")
                                    val crs = groupObj.optInt("course", 0)
                                    val desc = buildString {
                                        if (fac.isNotEmpty()) append("$fac ")
                                        if (spec.isNotEmpty()) append("$spec ")
                                        if (crs > 0) append("$crs курс")
                                    }.trim()
                                    if (desc.isNotEmpty()) {
                                        prefs.edit().putString("login_desc", desc).apply()
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            userDesc = desc
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        if (token.isNotEmpty()) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val cachedProfile = prefs.getString("cached_profile", null)
                suspend fun parseProfile(body: String) {
                    android.util.Log.d("API_RESPONSE", "Markbook API Response: $body")
                    try {
                        val json = org.json.JSONObject(body)
                        val student = json.optJSONObject("student")
                        if (student != null) {
                            val lastName = student.optString("lastName", "")
                            val firstName = student.optString("firstName", "")
                            val middleName = student.optString("middleName", "")
                            val fioStr = student.optString("fio", "")
                            val calculatedFio = if (fioStr.isNotEmpty()) fioStr else listOf(lastName, firstName, middleName).filter { it.isNotEmpty() }.joinToString(" ")
                            val fio = if (calculatedFio.isNotBlank()) calculatedFio else cachedFio ?: "Студент"
                            val faculty = student.optString("facultyAbbrev", "")
                            val course = student.optInt("course", 1)
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                userFio = fio
                                userDesc = if (faculty.isNotEmpty()) "$faculty, $course курс" else cachedGroup ?: "Студент БГУИР"
                                userPhoto = cachedPhoto ?: ""
                            }
                        }
                    } catch (e: Exception) {}
                }

                try {
                    val client = com.example.schedule.NetworkClient.client
                    val request = okhttp3.Request.Builder()
                        .url("https://iis.bsuir.by/api/v1/markbook")
                        .addHeader("Cookie", token)
                        .build()
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            prefs.edit().putString("cached_profile", body).apply()
                            parseProfile(body)
                        } else if (cachedProfile != null) {
                            parseProfile(cachedProfile)
                        } else {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                userFio = "Студент"
                                userDesc = "БГУИР"
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (cachedProfile != null) {
                        parseProfile(cachedProfile)
                    } else {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            userFio = "Студент"
                            userDesc = "БГУИР (офлайн)"
                        }
                    }
                }
            }
        } else {
            userFio = "Гость"
            userDesc = "Войдите в аккаунт"
        }
    }

    if (showCustomization) {
        androidx.activity.compose.BackHandler(onBack = { showCustomization = false })
        MinCustomizationView(MinBg, MinBorder, MinTextPrimary, MinTextSecondary, isDarkTheme, currentAccent, particlesEnabled, particleSizeMultiplier, transitionsEnabled, transitionType, transitionSpeedMultiplier, fontFamily, textSizeMultiplier, bgMode, bgImageUri, bgBlur, bgDim, bgEmoji, customParticleColor, onThemeToggle, onAccentChange, onParticlesToggle, onParticleSizeChange, onTransitionsToggle, onTransitionTypeChange, onTransitionSpeedChange, onFontChange, onTextSizeChange, onPrimaryColorChange, onBackgroundColorChange, onBgModeChange, onBgImageUriChange, onBgBlurChange, onBgDimChange, onBgEmojiChange, onParticleColorChange, onStyleChange) {
            showCustomization = false
        }
    } else if (showGradebook) {
        androidx.activity.compose.BackHandler(onBack = { showGradebook = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinGradebookScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, isDarkTheme) {
                showGradebook = false
            }
        }
    } else if (showMarks) {
        androidx.activity.compose.BackHandler(onBack = { showMarks = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinMarksScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
                showMarks = false
            }
        }
    } else if (showAbsences) {
        androidx.activity.compose.BackHandler(onBack = { showAbsences = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinAbsencesScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
                showAbsences = false
            }
        }
    } else if (showGroupScreen) {
        androidx.activity.compose.BackHandler(onBack = { showGroupScreen = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinGroupScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, isDarkTheme) {
                showGroupScreen = false
            }
        }
    } else if (showStudyScreen) {
        androidx.activity.compose.BackHandler(onBack = { showStudyScreen = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinStudyScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
                showStudyScreen = false
            }
        }
    } else if (showAnnouncements) {
        androidx.activity.compose.BackHandler(onBack = { showAnnouncements = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinGenericApiScreen("Объявления", "https://iis.bsuir.by/api/v1/announcements?page=0&size=20", MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent) { showAnnouncements = false }
        }
    } else if (showPenalties) {
        androidx.activity.compose.BackHandler(onBack = { showPenalties = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinGenericApiScreen("Взыскания", "https://iis.bsuir.by/api/v1/student-discipline-penalties", MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent) { showPenalties = false }
        }
    } else if (showDormitory) {
        androidx.activity.compose.BackHandler(onBack = { showDormitory = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinGenericApiScreen("Общежитие", "https://iis.bsuir.by/api/v1/dormitory-queue-application", MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent) { showDormitory = false }
        }
    } else if (showBenefits) {
        androidx.activity.compose.BackHandler(onBack = { showBenefits = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinGenericApiScreen("Льготы", "https://iis.bsuir.by/api/v1/dormitory-queue-application/privileges", MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent) { showBenefits = false }
        }
    } else {
        val isDialogOpen = false
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().let { if (isDialogOpen) it.blur(16.dp) else it }) {
                val styleType = LocalStyleType.current
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 40.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri -> photoUri = uri }

                    Box(contentAlignment = Alignment.BottomEnd) {
                        val finalPhoto: Any? = photoUri ?: if (userPhoto.isNotEmpty()) userPhoto else null
                        if (finalPhoto != null) {
                            if (finalPhoto is android.net.Uri) {
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(finalPhoto),
                                    contentDescription = null,
                                    modifier = Modifier.size(200.dp).clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else if (finalPhoto is String) {
                                var bitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                                try {
                                    val base64String = if (finalPhoto.contains(",")) finalPhoto.substringAfter(",") else finalPhoto
                                    val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bmp != null) {
                                        bitmap = bmp.asImageBitmap()
                                    }
                                } catch (e: Exception) {}
                                
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(200.dp).clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.avatar),
                                        contentDescription = null,
                                        modifier = Modifier.size(200.dp).clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.avatar),
                                contentDescription = null,
                                modifier = Modifier.size(200.dp).clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEEEEE))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Изменить фото",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(userFio.replace(" ", "\n"), fontSize = 33.sp, fontWeight = FontWeight.ExtraBold, color = MinTextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.animation.Crossfade(
                            targetState = showGroupDescToggle,
                            modifier = Modifier.fillMaxWidth(),
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
                        ) { showGroup ->
                            Text(
                                text = if (showGroup) "Группа $userGroup" else userDesc,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MinTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }

        item {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MinCardBg).padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                MinStat("Пропуски (ч)", "0 ч", MinTextPrimary, MinTextSecondary) { showAbsences = true }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                MinListAction("Объявления", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.Info) { showAnnouncements = true }
                MinListAction("Общежитие", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.Home) { showDormitory = true }
                MinListAction("Льготы", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.Star) { showBenefits = true }
                MinListAction("Отметки", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.CheckCircle) { showMarks = true }
                MinListAction("Зачетка", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.List) { showGradebook = true }

                MinListAction("Группа", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.Person) { showGroupScreen = true }
                MinListAction("Взыскания", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.Warning) { showPenalties = true }
                MinListAction("Кастомизация", MinBorder, MinTextPrimary, MinTextSecondary, icon = Icons.Outlined.Settings, isLast = true) { showCustomization = true }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            val localContext = androidx.compose.ui.platform.LocalContext.current
            Text("Выйти", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), modifier = Modifier.clickable {
                val prefs = localContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                val activity = localContext as? android.app.Activity
                activity?.finish()
                val intent = android.content.Intent(localContext, MainActivity::class.java)
                localContext.startActivity(intent)
            })
        }
        }
            }


            if (showAnnouncements) InfoDialog("Объявления", "В данный момент объявлений нет.", MinBg, MinTextPrimary, MinTextSecondary) { showAnnouncements = false }
            if (showDormitory) InfoDialog("Общежитие", "Функция находится в разработке.", MinBg, MinTextPrimary, MinTextSecondary) { showDormitory = false }
            if (showBenefits) InfoDialog("Льготы", "Функция находится в разработке.", MinBg, MinTextPrimary, MinTextSecondary) { showBenefits = false }
            if (showPenalties) InfoDialog("Взыскания", "У вас нет активных взысканий.", MinBg, MinTextPrimary, MinTextSecondary) { showPenalties = false }
        }
    }
}


@Composable
fun MinGenericApiScreen(
    title: String,
    url: String,
    MinBg: androidx.compose.ui.graphics.Color, MinCardBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, currentAccent: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit
) {
    var itemsList by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var rawText by remember { mutableStateOf("Загрузка...") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val cacheKey = "cache_${url.hashCode()}"
    
    LaunchedEffect(url) {
        val cachedBody = prefs.getString(cacheKey, null)
        val parser = { bodyStr: String ->
            val list = mutableListOf<Map<String, String>>()
            val jsonStr = bodyStr.trim()
            
            fun flattenJson(obj: org.json.JSONObject): Map<String, String> {
                val map = mutableMapOf<String, String>()
                val keys = obj.keys()
                while(keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.opt(k)
                    if (v is org.json.JSONObject) {
                        val subKeys = v.keys()
                        while(subKeys.hasNext()) {
                            val sk = subKeys.next()
                            map["$k.$sk"] = v.optString(sk)
                        }
                    } else if (v !is org.json.JSONArray && v != null && v.toString() != "null") {
                        map[k] = v.toString()
                    }
                }
                return map
            }

            if (jsonStr.startsWith("[")) {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) list.add(flattenJson(obj))
                }
            } else if (jsonStr.startsWith("{")) {
                val obj = org.json.JSONObject(jsonStr)
                val contentArray = obj.optJSONArray("content") ?: obj.optJSONArray("items")
                if (contentArray != null) {
                    for (i in 0 until contentArray.length()) {
                        val item = contentArray.optJSONObject(i)
                        if (item != null) list.add(flattenJson(item))
                    }
                } else {
                    list.add(flattenJson(obj))
                }
            }
            list
        }

        if (cachedBody != null) {
            try {
                itemsList = parser(cachedBody)
                rawText = ""
            } catch (e: Exception) {}
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val client = com.example.schedule.NetworkClient.client
                val request = NetworkClient.buildGetRequest(url, token)
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        prefs.edit().putString(cacheKey, body).apply()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            try {
                                itemsList = parser(body)
                                rawText = ""
                            } catch (e: Exception) {
                                rawText = "Ошибка парсинга:\n$body"
                            }
                        }
                    } else if (cachedBody == null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (response.code == 404 || response.code == 403 || response.code == 405) {
                                rawText = ""
                            } else {
                                rawText = "Ошибка: ${response.code}\n${body ?: ""}"
                            }
                        }
                    }
                }
            } catch(e: Exception) {
                if (cachedBody == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        rawText = "Исключение: ${e.message}"
                    }
                }
            }
        }
    }
    
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MinBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        item {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.KeyboardArrowLeft, contentDescription = "Назад", tint = MinTextPrimary, modifier = androidx.compose.ui.Modifier.size(32.dp).clickable { onBack() })
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                androidx.compose.material3.Text(title, fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = MinTextPrimary)
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        }
        if (rawText.isNotEmpty() && itemsList.isEmpty()) {
            item {
                if (rawText == "Загрузка...") {
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = currentAccent)
                    }
                } else {
                    androidx.compose.material3.Text(rawText, color = MinTextPrimary)
                }
            }
        } else if (itemsList.isEmpty() && rawText.isEmpty()) {
            item {
                if (title == "Объявления") {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(":(", fontSize = 80.sp, color = MinTextSecondary.copy(alpha = 0.5f), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                        androidx.compose.material3.Text("пока нет объявлений", fontSize = 20.sp, color = MinTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    }
                } else if (title == "Взыскания") {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(":(", fontSize = 80.sp, color = MinTextSecondary.copy(alpha = 0.5f), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                        androidx.compose.material3.Text("вы не очень активны", fontSize = 20.sp, color = MinTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    }
                } else if (title == "Льготы") {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(":(", fontSize = 80.sp, color = MinTextSecondary.copy(alpha = 0.5f), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                        androidx.compose.material3.Text("льгот нет", fontSize = 20.sp, color = MinTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    }
                } else if (title == "Заказ ведомостей") {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(":(", fontSize = 80.sp, color = MinTextSecondary.copy(alpha = 0.5f), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                        androidx.compose.material3.Text("ведомостей нет", fontSize = 20.sp, color = MinTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    }
                } else {
                    androidx.compose.material3.Text("Нет данных", color = MinTextSecondary)
                }
            }
        } else {
            items(itemsList.size) { index ->
                val item = itemsList[index]
                val translateStatus: (String) -> String = {
                    when (it.uppercase()) {
                        "PENDING" -> "В ожидании"
                        "APPROVED" -> "Одобрено"
                        "REJECTED" -> "Отклонено"
                        "IN_REVIEW" -> "На рассмотрении"
                        "VERIFIED" -> "Проверено"
                        "ACCEPTED" -> "Принято"
                        "ACTIVE" -> "Активно"
                        "CONFIRMED" -> "Подтверждено"
                        "ATTACHED" -> "Прикреплено"
                        "ADD" -> "Добавлено"
                        "VERIFY" -> "На проверке"
                        "WAITING" -> "В очереди"
                        else -> it
                    }
                }
                
                if (title == "Общежитие") {
                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(MinCardBg).padding(16.dp)) {
                        val dateRaw = item["dateOfApplication"] ?: item["date"] ?: item["createdAt"] ?: item.entries.firstOrNull { it.value.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*")) }?.value ?: ""
                        val date = if (dateRaw.length >= 10) dateRaw.take(10) else dateRaw
                        
                        val actionRaw = item["status"] ?: item["action"] ?: item["status.name"] ?: item["state"] ?: item["statusName"] ?: item.entries.firstOrNull { it.key.contains("status", true) || it.key.contains("state", true) }?.value ?: "Заявление зарегистрировано"
                        val action = translateStatus(actionRaw)
                        
                        val dorm = item["dormitory"] ?: item["dormitoryName"] ?: ""
                        val room = item["room"] ?: item["roomNumber"] ?: ""
                        val note = item["note"] ?: item["comment"] ?: ""
                        
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            if (date.isNotBlank()) {
                                androidx.compose.material3.Text(date, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = MinTextPrimary)
                            } else {
                                androidx.compose.material3.Text("Дата не указана", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = MinTextSecondary)
                            }
                            androidx.compose.material3.Text(action, fontSize = 16.sp, color = currentAccent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        
                        if (dorm.isNotBlank() && dorm != "null") {
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                            androidx.compose.material3.Text("Общежитие: $dorm", fontSize = 15.sp, color = MinTextPrimary)
                        }
                        if (room.isNotBlank() && room != "null") {
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                            androidx.compose.material3.Text("Комната: $room", fontSize = 15.sp, color = MinTextPrimary)
                        }
                        if (note.isNotBlank() && note != "null") {
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                            androidx.compose.material3.Text("Примечание: $note", fontSize = 14.sp, color = MinTextSecondary)
                        }
                    }
                } else if (title == "Льготы") {
                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(MinCardBg).padding(16.dp)) {
                        val yearRaw = item["issueYear"] ?: item["year"] ?: item["date"]?.take(4) ?: item["createdAt"]?.take(4) ?: item.entries.firstOrNull { it.key.contains("year", true) }?.value ?: ""
                        val yearStr = if (yearRaw.isNotBlank()) yearRaw else "Год не указан"
                        
                        val isOutOfTurn = item["outOfTurn"] == "true" || item["isOutOfTurn"] == "true" || item["privilege.isOutOfTurn"] == "true" || item["privilege.outOfTurn"] == "true" || item.entries.any { it.value.contains("внеочередное", true) || it.key.contains("outOfTurn", true) && it.value == "true" }
                        val priorityText = item.values.firstOrNull { it.contains("очередное", true) }?.lowercase() ?: if (isOutOfTurn) "внеочередное" else ""
                        
                        val suffix = if (priorityText.isNotBlank()) "$priorityText получение общежития" else "получение общежития"
                        
                        androidx.compose.material3.Text("$yearStr - $suffix", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                    }
                } else {
                    val displayTitle = item["name"] ?: item["title"] ?: item["type"] ?: "Запись"
                    val displayDate = item["date"] ?: item["issued"] ?: item["createdAt"]
                    
                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(MinCardBg).padding(16.dp)) {
                        androidx.compose.material3.Text(displayTitle, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                        if (displayDate != null && displayDate.isNotBlank()) {
                            androidx.compose.material3.Text(displayDate.take(10), fontSize = 12.sp, color = currentAccent)
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        
                        val content = item["content"] ?: item["reason"]
                        if (content != null && content.isNotBlank()) {
                            androidx.compose.material3.Text(content, fontSize = 14.sp, color = MinTextPrimary)
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        }
                        
                        for ((k, v) in item) {
                            val skipKeys = listOf("name", "title", "type", "date", "issued", "createdAt", "content", "reason", "id", "url")
                            if (k !in skipKeys && v.isNotBlank()) {
                                val translatedKey = when (k) {
                                    "employee.fio" -> "Преподаватель"
                                    "status" -> "Статус"
                                    "number" -> "Номер"
                                    "amount" -> "Сумма"
                                    "department.name" -> "Отдел"
                                    else -> k.replace(Regex("([A-Z])"), " $1").replace(".", " ").replaceFirstChar { it.uppercase() }
                                }
                                androidx.compose.material3.Text("$translatedKey: $v", fontSize = 14.sp, color = MinTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoDialog(
    title: String,
    content: String,
    MinBg: androidx.compose.ui.graphics.Color,
    MinTextPrimary: androidx.compose.ui.graphics.Color,
    MinTextSecondary: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MinBg,
        title = {
            androidx.compose.material3.Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MinTextPrimary
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = content,
                fontSize = 16.sp,
                color = MinTextSecondary,
                lineHeight = 24.sp
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Закрыть", color = MinTextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    )
}

@Composable
fun MinCustomizationView(
    MinBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, isDarkTheme: Boolean, currentAccent: androidx.compose.ui.graphics.Color, particlesEnabled: Boolean, particleSizeMultiplier: Float, transitionsEnabled: Boolean, transitionType: TransitionType, transitionSpeedMultiplier: Float, fontFamily: androidx.compose.ui.text.font.FontFamily, textSizeMultiplier: Float, bgMode: String, bgImageUri: String?, bgBlur: Float, bgDim: Float, bgEmoji: String, customParticleColor: androidx.compose.ui.graphics.Color?, onThemeToggle: () -> Unit, onAccentChange: (androidx.compose.ui.graphics.Color) -> Unit, onParticlesToggle: (Boolean) -> Unit, onParticleSizeChange: (Float) -> Unit, onTransitionsToggle: (Boolean) -> Unit, onTransitionTypeChange: (TransitionType) -> Unit, onTransitionSpeedChange: (Float) -> Unit, onFontChange: (androidx.compose.ui.text.font.FontFamily) -> Unit, onTextSizeChange: (Float) -> Unit, onPrimaryColorChange: (androidx.compose.ui.graphics.Color?) -> Unit, onBackgroundColorChange: (androidx.compose.ui.graphics.Color?) -> Unit, onBgModeChange: (String) -> Unit, onBgImageUriChange: (String?) -> Unit, onBgBlurChange: (Float) -> Unit, onBgDimChange: (Float) -> Unit, onBgEmojiChange: (String) -> Unit, onParticleColorChange: (androidx.compose.ui.graphics.Color?) -> Unit, onStyleChange: (StyleType) -> Unit, onBack: () -> Unit
) {
    var showPrimaryStrip by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showBackgroundStrip by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAccentStrip by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showPrimaryStrip) {
        ColorPickerDialog(
            initialColor = MinTextPrimary,
            onColorSelected = { onPrimaryColorChange(it) },
            onDismiss = { showPrimaryStrip = false }
        )
    }
    if (showBackgroundStrip) {
        ColorPickerDialog(
            initialColor = MinBg,
            onColorSelected = { onBackgroundColorChange(it) },
            onDismiss = { showBackgroundStrip = false }
        )
    }
    if (showAccentStrip) {
        ColorPickerDialog(
            initialColor = currentAccent,
            onColorSelected = { onAccentChange(it) },
            onDismiss = { showAccentStrip = false }
        )
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(MinBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        item {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.KeyboardArrowLeft, contentDescription = "Назад", tint = MinTextPrimary, modifier = androidx.compose.ui.Modifier.size(32.dp).clickable { onBack() })
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                androidx.compose.material3.Text("Кастомизация", fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = MinTextPrimary)
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }


        item {
            androidx.compose.material3.Text("Стиль интерфейса", fontSize = 19.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextSecondary)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                StyleType.values().forEach { style ->
                    val isSelected = style == LocalStyleType.current
                    androidx.compose.material3.Button(
                        onClick = { onStyleChange(style) },
                        modifier = androidx.compose.ui.Modifier.weight(1f).height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MinBg,
                            contentColor = MinTextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MinTextPrimary else MinBorder)
                    ) {
                        androidx.compose.material3.Text(style.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
            androidx.compose.material3.HorizontalDivider(color = MinBorder, thickness = 1.dp)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }

        item {
            androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    androidx.compose.material3.Text("Тема", fontSize = 19.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(48.dp).border(1.dp, MinTextSecondary.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(if (isDarkTheme) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White).clickable { onThemeToggle() })
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text(if (isDarkTheme) "Темная" else "Светлая", fontSize = 17.sp, color = MinTextSecondary)
                }

                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    androidx.compose.material3.Text("Основной", fontSize = 19.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(48.dp).border(1.dp, MinTextSecondary.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(MinTextPrimary).clickable { showPrimaryStrip = true })
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text("Сброс", fontSize = 17.sp, color = MinTextSecondary, modifier = androidx.compose.ui.Modifier.clickable { onPrimaryColorChange(null) })
                }
    
                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    androidx.compose.material3.Text("Фон", fontSize = 19.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(48.dp).border(1.dp, MinTextSecondary.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(MinBg).clickable { showBackgroundStrip = true })
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text("Сброс", fontSize = 17.sp, color = MinTextSecondary, modifier = androidx.compose.ui.Modifier.clickable { onBackgroundColorChange(null) })
                }
            }

    
            Spacer(modifier = Modifier.height(24.dp))
            Text("Сочетания цветов", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val customPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
                var savedCustomPalettesStr by remember { mutableStateOf(customPrefs.getString("saved_palettes", "") ?: "") }
                val savedCustomPalettes = remember(savedCustomPalettesStr) {
                    savedCustomPalettesStr.split(";").filter { it.isNotEmpty() }.mapNotNull { 
                        try {
                            val parts = it.split(",")
                            Pair(Color(parts[0].toULong()), Color(parts[1].toULong()))
                        } catch (e: Exception) { null }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MinTextSecondary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { 
                            val newStr = if (savedCustomPalettesStr.isEmpty()) "${currentAccent.value},${MinBg.value}" else "$savedCustomPalettesStr;${currentAccent.value},${MinBg.value}"
                            customPrefs.edit().putString("saved_palettes", newStr).apply()
                            savedCustomPalettesStr = newStr
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add custom", tint = MinTextPrimary)
                }

                savedCustomPalettes.forEach { palette ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.second)
                            .border(1.dp, MinTextSecondary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                onPrimaryColorChange(palette.first)
                                onBackgroundColorChange(palette.second)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(palette.first))
                    }
                }

                val recommendedPalettes = listOf(
                    // Минималистичный
                    Pair(Color(0xFFE53935), Color(0xFFFFEBEE)),
                    Pair(Color(0xFF43A047), Color(0xFFE8F5E9)),
                    Pair(Color(0xFF1E88E5), Color(0xFFE3F2FD)),
                    Pair(Color(0xFFFFB300), Color(0xFF333333)),
                    Pair(Color(0xFF00E5FF), Color(0xFF111111)),
                    Pair(Color(0xFFE0E0E0), Color(0xFF000000)),
                    // Техно / Linux
                    Pair(Color(0xFF00FF41), Color(0xFF0D0D0D)),   // классик терминал
                    Pair(Color(0xFF00FFFF), Color(0xFF001020)),   // циан киберпанк
                    Pair(Color(0xFFFF6600), Color(0xFF120800)),   // amber терминал
                    Pair(Color(0xFFBD93F9), Color(0xFF1E1535)),   // dracula purple
                    Pair(Color(0xFF50FA7B), Color(0xFF0A1628))    // dracula green
                )
                recommendedPalettes.forEach { palette ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.second)
                            .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                onPrimaryColorChange(palette.first)
                                onBackgroundColorChange(palette.second)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(palette.first))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MinBorder, thickness = 1.dp)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Пользовательский фон", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Solid" to "Сплошной", "Gallery" to "Фото", "Gradient" to "Градиент").forEach { (mode, name) ->
                    androidx.compose.material3.TextButton(
                        onClick = { onBgModeChange(mode) },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = if (bgMode == mode) MinTextPrimary else MinTextSecondary
                        )
                    ) {
                        Text(name, fontWeight = if (bgMode == mode) FontWeight.ExtraBold else FontWeight.Bold)
                    }
                }
            }
            
            if (bgMode == "Gallery" || bgMode == "Gradient") {
                Spacer(modifier = Modifier.height(16.dp))
                if (bgMode == "Gallery") {
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        if (uri != null) onBgImageUriChange(uri.toString())
                    }
                    OutlinedButton(onClick = { launcher.launch("image/*") }, border = androidx.compose.foundation.BorderStroke(2.dp, MinTextPrimary), colors = ButtonDefaults.outlinedButtonColors(contentColor = MinTextPrimary)) { Text("Выбрать фото", color = MinTextPrimary, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (bgMode != "Gradient") {
                    Text("Размытие", color = MinTextPrimary)
                    Slider(value = bgBlur, onValueChange = { onBgBlurChange(it) })
                }
                Text("Затемнение", color = MinTextPrimary)
                Slider(value = bgDim, onValueChange = { onBgDimChange(it) })
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MinBorder, thickness = 1.dp)
        }



        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Отображение", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Размер текста: ${String.format("%.1f", textSizeMultiplier)}x", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple))
            Slider(
                value = textSizeMultiplier,
                onValueChange = { onTextSizeChange(it) },
                valueRange = 0.8f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = MinTextPrimary,
                    activeTrackColor = MinTextPrimary,
                    inactiveTrackColor = MinBorder
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MinBorder, thickness = 1.dp)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MinBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Анимации переходов", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple))
                Switch(
                    checked = transitionsEnabled,
                    onCheckedChange = { onTransitionsToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinBg,
                        checkedTrackColor = MinTextPrimary,
                        uncheckedThumbColor = MinTextPrimary,
                        uncheckedTrackColor = MinTextSecondary.copy(alpha = 0.3f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            
            if (transitionsEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TransitionType.values().forEach { type ->
                        TransitionDemoBox(type, transitionSpeedMultiplier, isSelected = (transitionType == type), onClick = { onTransitionTypeChange(type) }, MinTextPrimary, MinBorder)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Скорость переходов: ${String.format("%.1f", transitionSpeedMultiplier)}x", fontSize = 19.sp, color = MinTextSecondary)
                Slider(
                    value = transitionSpeedMultiplier,
                    onValueChange = { onTransitionSpeedChange(it) },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = MinTextPrimary,
                        activeTrackColor = MinTextPrimary,
                        inactiveTrackColor = MinBorder
                    )
                )
            }
        }
    }
}

@Composable
fun TransitionDemoBox(type: TransitionType, speedMultiplier: Float, isSelected: Boolean, onClick: () -> Unit, MinTextPrimary: Color, MinBorder: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((1500 / speedMultiplier).toInt()),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier
             .size(40.dp)
             .clip(RoundedCornerShape(8.dp))
             .clickable { onClick() }
             .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MinTextPrimary else MinBorder, RoundedCornerShape(8.dp))
             .padding(4.dp)
        ) {
             Box(modifier = Modifier.fillMaxSize().background(MinBorder))
             Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                 when (type) {
                     TransitionType.Slide -> { this.translationX = (1f - progress) * size.width }
                     TransitionType.Fade -> { this.alpha = progress }
                     TransitionType.Scale -> { 
                         val s = 0.8f + (progress * 0.2f)
                         this.scaleX = s
                         this.scaleY = s
                         this.alpha = progress
                     }
                     TransitionType.Cube -> {
                         this.rotationY = (1f - progress) * 90f
                         this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                     }
                     TransitionType.Flip -> {
                         this.rotationY = (1f - progress) * 180f
                         this.alpha = if (progress < 0.5f) 0f else 1f
                     }
                 }
             }.background(MinTextPrimary))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(type.title, fontSize = 17.sp, color = if(isSelected) MinTextPrimary else MinBorder, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MoonIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val path1 = androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        val path2 = androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(size.width * 0.3f, -size.height * 0.1f, size.width * 1.3f, size.height * 0.9f))
        }
        val moonPath = androidx.compose.ui.graphics.Path().apply {
            op(path1, path2, androidx.compose.ui.graphics.PathOperation.Difference)
        }
        drawPath(moonPath, color = tint)
    }
}

@Composable
fun MinStat(label: String, value: String, MinTextPrimary: Color, MinTextSecondary: Color, onClick: (() -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal=16.dp, vertical=8.dp) else Modifier.padding(horizontal=16.dp, vertical=8.dp)) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = MinTextPrimary)
    }
}

@Composable
fun MinListAction(label: String, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, isLast: Boolean = false, onClick: () -> Unit = {}) {
    val padding = Modifier.padding(vertical = 16.dp)
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }.then(padding)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MinTextPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple))
            }
            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MinTextSecondary)
        }
        if (!isLast) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MinBorder, thickness = 1.dp)
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }

    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(currentColor, RoundedCornerShape(8.dp)))
                Spacer(modifier = Modifier.height(24.dp))

                Canvas(modifier = Modifier.fillMaxWidth().height(60.dp).pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        hue = (x / size.width.toFloat()) * 360f
                    }
                }.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        hue = (x / size.width.toFloat()) * 360f
                    }
                }) {
                    val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                    )
                    drawRect(brush)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(60.dp).pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val fraction = x / size.width.toFloat()
                        if (fraction <= 0.5f) {
                            value = fraction * 2f
                            saturation = 1f
                        } else {
                            value = 1f
                            saturation = 1f - (fraction - 0.5f) * 2f
                        }
                    }
                }.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        val fraction = x / size.width.toFloat()
                        if (fraction <= 0.5f) {
                            value = fraction * 2f
                            saturation = 1f
                        } else {
                            value = 1f
                            saturation = 1f - (fraction - 0.5f) * 2f
                        }
                    }
                }) {
                    val pureColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color.Black, pureColor, Color.White)
                    )
                    drawRect(brush)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("ОТМЕНА", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("ВЫБРАТЬ", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        onColorSelected(currentColor)
                        onDismiss()
                    }.padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun SplashScreen(MinBg: Color, MinTextPrimary: Color, onSplashFinished: () -> Unit) {
    val scale = remember { androidx.compose.animation.core.Animatable(0.5f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1.2f,
                    animationSpec = tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 400, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 800)
                )
            }
        }
        kotlinx.coroutines.delay(400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MinBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                tint = MinTextPrimary,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        alpha = alpha.value
                    )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SCHEDULE",
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MinTextPrimary,
                letterSpacing = 8.sp,
                modifier = Modifier.graphicsLayer(
                    alpha = alpha.value,
                    translationY = (1f - alpha.value) * 50f
                )
            )
        }
    }


}

enum class TransitionType(val title: String) { Slide("Слайд"), Fade("Выцветание"), Scale("Масштаб"), Cube("Куб"), Flip("Вращение") }


enum class StyleType(val title: String) { Minimal("Минимализм"), Techno("Техно") }
val LocalStyleType = androidx.compose.runtime.compositionLocalOf { StyleType.Minimal }

@Composable
fun ScanlineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineStep = 4f
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color.Black.copy(alpha = 0.13f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, lineStep / 2f)
            )
            y += lineStep
        }
    }
}

@Composable
fun DynamicGradientBackground(accentColor: Color, bgColor: Color, isDarkTheme: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart),
        label = "angle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val vibrantBg = if (isDarkTheme) Color(0xFF0A001A) else Color(0xFFFFEBEB)
        drawRect(color = vibrantBg)

        val t = angle * kotlin.math.PI.toFloat() / 180f

        val cx1 = size.width * (0.5f + 0.3f * kotlin.math.sin(t))
        val cy1 = size.height * (0.5f + 0.3f * kotlin.math.cos(t * 0.8f))
        val r1 = size.width * (0.8f + 0.2f * kotlin.math.sin(t * 1.5f))
        val color1 = if (isDarkTheme) Color(0xFFFF007F).copy(alpha = 0.5f) else Color(0xFFFF6B6B).copy(alpha = 0.5f)

        val cx2 = size.width * (0.5f + 0.4f * kotlin.math.cos(t * 1.2f))
        val cy2 = size.height * (0.5f + 0.4f * kotlin.math.sin(t * 0.9f))
        val r2 = size.width * (0.9f + 0.2f * kotlin.math.cos(t * 1.3f))
        val color2 = if (isDarkTheme) Color(0xFF7000FF).copy(alpha = 0.5f) else Color(0xFF4ECDC4).copy(alpha = 0.5f)

        val cx3 = size.width * (0.5f + 0.35f * kotlin.math.sin(t * 0.7f))
        val cy3 = size.height * (0.5f + 0.35f * kotlin.math.cos(t * 1.1f))
        val r3 = size.width * (0.85f + 0.15f * kotlin.math.sin(t * 1.4f))
        val color3 = if (isDarkTheme) Color(0xFF00E5FF).copy(alpha = 0.4f) else Color(0xFFFFD93D).copy(alpha = 0.5f)

        val cx4 = size.width * (0.5f + 0.2f * kotlin.math.cos(t * 1.5f))
        val cy4 = size.height * (0.5f + 0.2f * kotlin.math.sin(t * 1.2f))
        val r4 = size.width * 0.7f
        val color4 = accentColor.copy(alpha = 0.4f)

        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(cx1, cy1),
                radius = r1
            ),
            radius = r1,
            center = androidx.compose.ui.geometry.Offset(cx1, cy1)
        )

        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(cx2, cy2),
                radius = r2
            ),
            radius = r2,
            center = androidx.compose.ui.geometry.Offset(cx2, cy2)
        )

        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(color3, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(cx3, cy3),
                radius = r3
            ),
            radius = r3,
            center = androidx.compose.ui.geometry.Offset(cx3, cy3)
        )
        
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(color4, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(cx4, cy4),
                radius = r4
            ),
            radius = r4,
            center = androidx.compose.ui.geometry.Offset(cx4, cy4)
        )
    }
}






@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    minFontSize: androidx.compose.ui.unit.TextUnit = 8.sp
) {
    var textSize by remember(text) { mutableStateOf(fontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        fontSize = textSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        lineHeight = lineHeight,
        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        style = style,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && textSize.value > minFontSize.value) {
                textSize = (textSize.value - 1f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}

















@Composable
fun ColorWheel(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, onColorSelected: (androidx.compose.ui.graphics.Color) -> Unit) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val offset = change.position
                    val width = size.width.toFloat()
                    val x = offset.x.coerceIn(0f, width)
                    val fraction = x / width
                    val hue = fraction * 360f
                    val hsv = floatArrayOf(hue, 1f, 1f)
                    onColorSelected(androidx.compose.ui.graphics.Color(android.graphics.Color.HSVToColor(hsv)))
                }
            }
            .clickable {
                // A single click could also pick color
            }
    ) {
        val linearGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
            colors = listOf(
                androidx.compose.ui.graphics.Color.Red,
                androidx.compose.ui.graphics.Color.Yellow,
                androidx.compose.ui.graphics.Color.Green,
                androidx.compose.ui.graphics.Color.Cyan,
                androidx.compose.ui.graphics.Color.Blue,
                androidx.compose.ui.graphics.Color.Magenta,
                androidx.compose.ui.graphics.Color.Red
            )
        )
        drawRect(brush = linearGradient)
    }
}


@Composable
fun MinLoginScreen(MinBg: Color, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, isDarkTheme: Boolean, onLoginSuccess: (String, String) -> Unit) {
    var gradebookNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            var contextActivity = view.context
            while (contextActivity is android.content.ContextWrapper) {
                if (contextActivity is android.app.Activity) break
                contextActivity = contextActivity.baseContext
            }
            val window = (contextActivity as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        DynamicGradientBackground(accentColor = Color(0xFF8B5CF6), bgColor = MinBg, isDarkTheme = isDarkTheme)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Добро пожаловать", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = MinTextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 46.sp)
            Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = gradebookNumber,
                    onValueChange = { gradebookNumber = it },
                    label = { Text("Номер зачетки", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MinTextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MinTextPrimary,
                        unfocusedTextColor = MinTextPrimary,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = MinBorder,
                        focusedLabelColor = Color(0xFF8B5CF6),
                        unfocusedLabelColor = MinTextSecondary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    trailingIcon = {
                        val icon = if (passwordVisible) androidx.compose.material.icons.Icons.Outlined.Visibility else androidx.compose.material.icons.Icons.Outlined.VisibilityOff
                        androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            androidx.compose.material3.Icon(icon, contentDescription = null, tint = MinTextSecondary)
                        }
                    },
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MinTextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MinTextPrimary,
                        unfocusedTextColor = MinTextPrimary,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = MinBorder,
                        focusedLabelColor = Color(0xFF3B82F6),
                        unfocusedLabelColor = MinTextSecondary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color(0xFFFF6B6B), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        if (gradebookNumber.isBlank() || password.isBlank()) {
                            errorMessage = "Пожалуйста, заполните все поля"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch(Dispatchers.IO) {
                            try {
                                NetworkClient.init(context)
                                val loginJson = org.json.JSONObject()
                                loginJson.put("username", gradebookNumber.trim())
                                loginJson.put("password", password)
                                val body = loginJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                val request = Request.Builder()
                                    .url("https://iis.bsuir.by/api/v1/auth/login")
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                                    .addHeader("Accept", "application/json, text/plain, */*")
                                    .post(body)
                                    .build()
                                NetworkClient.client.newCall(request).execute().use { response ->
                                    val responseBody = response.body?.string()
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        if (response.isSuccessful) {
                                            android.util.Log.d("API_RESPONSE", "Login Response JSON: $responseBody")
                                            var extractedToken = ""
                                            if (responseBody != null) {
                                                try {
                                                    val json = org.json.JSONObject(responseBody)
                                                    prefs.edit().apply {
                                                        putString("login_fio", json.optString("fio"))
                                                        putString("login_group", json.optString("group"))
                                                        putString("login_photo", json.optString("photoUrl"))
                                                    }.apply()
                                                } catch (e: Exception) {}
                                            }
                                            val cookies = response.headers("Set-Cookie")
                                            for (cookie in cookies) {
                                                if (cookie.contains("SESSION") || cookie.contains("JSESSIONID") || cookie.contains("jwt") || cookie.contains("token")) {
                                                    extractedToken = cookie.substringBefore(";")
                                                    break
                                                }
                                            }
                                            if (extractedToken.isEmpty()) extractedToken = responseBody ?: ""
                                            // Save password for auto re-login on 401
                                            prefs.edit()
                                                .putString("saved_password", password)
                                                .putString("gradebook_number", gradebookNumber)
                                                .apply()
                                            onLoginSuccess(gradebookNumber, extractedToken)
                                        } else {
                                            when (response.code) {
                                                400, 401, 403, 404 -> errorMessage = "Неверный логин или пароль"
                                                500, 502, 503 -> errorMessage = "Ошибка сервера (${response.code}). Попробуйте позже."
                                                else -> errorMessage = "Ошибка: ${response.code} ${response.message}"
                                            }
                                        }
                                    }
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMessage = "Отсутствует подключение к сети: ${e.message}"
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMessage = "Непредвиденная ошибка: ${e.localizedMessage}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6), 
                        contentColor = Color.White, 
                        disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Войти", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }


fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri, filename: String): String? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.filesDir, filename)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return android.net.Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
