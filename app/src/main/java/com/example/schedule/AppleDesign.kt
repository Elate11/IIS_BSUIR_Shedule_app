package com.example.schedule

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Apple Design System tokens & components based on official Apple design language:
 * - Single interactive accent: Action Blue #0066cc (Focus Blue #0071e3, Sky Link Blue #2997ff on dark)
 * - Surfaces: Pure White #ffffff, Parchment #f5f5f7, Pearl #fafafc, Near-Black Tiles #272729 / #2a2a2c / #252527
 * - Ink: Near-Black Ink #1d1d1f on light surfaces, BodyOnDark #ffffff on dark, muted tones #7a7a7a / #86868b
 * - Typography: SF Pro Display & Text hierarchy with negative tracking at display sizes, weight 600 headlines, body 17sp (weight 500 absent)
 * - Shapes: Rounded pill (9999dp), utility rects (8dp), capsules (11dp), utility cards (18dp)
 * - Elevation: Single whisper-soft shadow / 1px hairlines / frosted blur, no decorative gradients
 * - Micro-interaction: scale(0.95f) on button active/press state
 */
object AppleTokens {
    // Brand & Interactive Accent
    val ActionBlue = Color(0xFF0066CC)         // {colors.primary} #0066cc
    val FocusBlue = Color(0xFF0071E3)          // {colors.primary-focus} #0071e3
    val SkyLinkBlue = Color(0xFF2997FF)        // {colors.primary-on-dark} #2997ff

    // Light Surfaces
    val CanvasWhite = Color(0xFFFFFFFF)        // {colors.canvas} #ffffff
    val Parchment = Color(0xFFF5F5F7)          // {colors.canvas-parchment} #f5f5f7
    val SurfacePearl = Color(0xFFFAFAFC)       // {colors.surface-pearl} #fafafc

    // Dark Surfaces
    val Tile1 = Color(0xFF272729)              // {colors.surface-tile-1} #272729
    val Tile2 = Color(0xFF2A2A2C)              // {colors.surface-tile-2} #2a2a2c
    val Tile3 = Color(0xFF252527)              // {colors.surface-tile-3} #252527
    val PureBlack = Color(0xFF000000)          // {colors.surface-black} #000000

    // Controls
    val TranslucentChip = Color(0xA3D2D2D7)    // rgba(210, 210, 215, 0.64)

    // Text & Ink
    val Ink = Color(0xFF1D1D1F)                // {colors.ink} #1d1d1f
    val BodyOnDark = Color(0xFFFFFFFF)         // {colors.body-on-dark} #ffffff
    val BodyMutedDark = Color(0xFFCCCCCC)      // {colors.body-muted} #cccccc
    val InkMuted80 = Color(0xFF333333)         // {colors.ink-muted-80} #333333
    val InkMuted48 = Color(0xFF7A7A7A)         // {colors.ink-muted-48} #7a7a7a
    val InkSecondary = Color(0xFF86868B)       // Apple standard secondary

    // Hairlines & Borders
    val HairlineLight = Color(0x1F000000)      // ~rgba(0,0,0,0.08) hairline #e0e0e0
    val HairlineDark = Color(0x1FFFFFFF)       // ~rgba(255,255,255,0.12)
    val DividerSoftLight = Color(0xFFF0F0F0)   // {colors.divider-soft} #f0f0f0
    val DividerSoftDark = Color(0x14FFFFFF)    // subtle divider on dark

    // Radii
    val RadiusNone = 0.dp
    val RadiusXs = 5.dp
    val RadiusSm = 8.dp                        // {rounded.sm} 8px
    val RadiusMd = 11.dp                       // {rounded.md} 11px
    val RadiusLg = 18.dp                       // {rounded.lg} 18px (utility card)
    val RadiusPill = 9999.dp                   // {rounded.pill} full capsule
}

object AppleTypography {
    // SF Pro Display: Display sizes >= 17sp use negative letter spacing ("Apple tight")
    // Weight ladder: 300, 400, 600, 700 (500 is intentionally absent)
    fun heroDisplay(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 42.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
        lineHeight = 46.sp,
        fontFamily = fontFamily
    )

    fun displayLg(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
        lineHeight = 36.sp,
        fontFamily = fontFamily
    )

    fun displayMd(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.35).sp,
        lineHeight = 30.sp,
        fontFamily = fontFamily
    )

    fun lead(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 26.sp,
        fontFamily = fontFamily
    )

    fun tagline(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
        fontFamily = fontFamily
    )

    fun bodyStrong(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        lineHeight = 22.sp,
        fontFamily = fontFamily
    )

    fun body(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.3).sp,
        lineHeight = 25.sp,
        fontFamily = fontFamily
    )

    fun caption(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.2).sp,
        lineHeight = 18.sp,
        fontFamily = fontFamily
    )

    fun captionStrong(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        lineHeight = 18.sp,
        fontFamily = fontFamily
    )

    fun buttonUtility(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.2).sp,
        lineHeight = 18.sp,
        fontFamily = fontFamily
    )

    fun finePrint(fontFamily: FontFamily? = null) = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.1).sp,
        lineHeight = 15.sp,
        fontFamily = fontFamily
    )

    fun heroDisplayLarge(fontFamily: FontFamily? = null) = displayLg(fontFamily)
    fun callout(fontFamily: FontFamily? = null) = bodyStrong(fontFamily)
    fun subheadline(fontFamily: FontFamily? = null) = tagline(fontFamily)
    fun footnote(fontFamily: FontFamily? = null) = caption(fontFamily)
}

/**
 * Modifier implementing Apple's system-wide micro-interaction: scale(0.95f) on active press
 */
@Composable
fun Modifier.applePressEffect(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "applePressScale"
    )
    return this.scale(scale)
}

/**
 * Primary Blue Pill CTA {component.button-primary}
 * Capsule shape, Action Blue background, white text, 11px x 22px padding, scale(0.95) on press
 */
@Composable
fun AppleButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkTheme: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "btnScale"
    )

    val bgColor = if (!enabled) {
        if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    } else {
        if (isDarkTheme) AppleTokens.FocusBlue else AppleTokens.ActionBlue
    }

    val contentColor = if (!enabled) {
        if (isDarkTheme) AppleTokens.InkMuted48 else AppleTokens.InkMuted48
    } else {
        Color.White
    }

    Surface(
        onClick = {
            AppHapticManager.playClick()
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(AppleTokens.RadiusPill),
        color = bgColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = AppleTypography.captionStrong().copy(fontSize = 15.sp),
                color = contentColor
            )
        }
    }
}

/**
 * Secondary Pill CTA {component.button-secondary-pill}
 * Capsule shape, transparent background with 1px Action Blue border, Action Blue text
 */
@Composable
fun AppleButtonSecondaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "secBtnScale"
    )

    val blueColor = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue

    Surface(
        onClick = {
            AppHapticManager.playClick()
            onClick()
        },
        shape = RoundedCornerShape(AppleTokens.RadiusPill),
        color = Color.Transparent,
        border = BorderStroke(1.dp, blueColor),
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = blueColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = AppleTypography.captionStrong().copy(fontSize = 15.sp),
                color = blueColor
            )
        }
    }
}

/**
 * Compact Utility Button {component.button-dark-utility}
 * 8px radius, Near-Black ink fill, white text
 */
@Composable
fun AppleButtonUtility(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "utilityBtnScale"
    )

    val bgColor = if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.Ink
    val textColor = Color.White

    Surface(
        onClick = {
            AppHapticManager.playClick()
            onClick()
        },
        shape = RoundedCornerShape(AppleTokens.RadiusSm),
        color = bgColor,
        contentColor = textColor,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(34.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = AppleTypography.buttonUtility(),
                color = textColor
            )
        }
    }
}

/**
 * Apple Store Utility Card {component.store-utility-card}
 * 18px radius ({rounded.lg}), Pure White in light or Tile 1/2 in dark, 1px hairline border, no loud shadows
 */
@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    cardBg: Color = if (isDarkTheme) AppleTokens.Tile1 else AppleTokens.CanvasWhite,
    cardBorder: Color = if (isDarkTheme) AppleTokens.HairlineDark else AppleTokens.HairlineLight,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    hasProductShadow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgColor = cardBg
    val borderColor = cardBorder
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "cardScale"
    )

    val cardShape = RoundedCornerShape(AppleTokens.RadiusLg)

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (hasProductShadow) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = cardShape,
                        spotColor = Color(0x2E000000),
                        ambientColor = Color(0x14000000)
                    )
                } else Modifier
            )
            .clip(cardShape)
            .background(bgColor)
            .border(1.dp, borderColor, cardShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            AppHapticManager.playClick()
                            onClick()
                        }
                    )
                } else Modifier
            )
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Apple Search Input {component.search-input}
 * Full pill (9999dp), 44dp height, 1px subtle hairline, leading search icon, muted placeholder
 */
@Composable
fun AppleSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    inputBg: Color? = null,
    inputBorder: Color? = null,
    inputTextColor: Color? = null,
    inputIconColor: Color? = null,
    onDone: (() -> Unit)? = null
) {
    val bgColor = inputBg ?: if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.CanvasWhite
    val borderColor = inputBorder ?: if (isDarkTheme) AppleTokens.HairlineDark else AppleTokens.HairlineLight
    val textColor = inputTextColor ?: if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink
    val iconColor = inputIconColor ?: if (isDarkTheme) AppleTokens.BodyMutedDark else AppleTokens.InkSecondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(AppleTokens.RadiusPill))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(AppleTokens.RadiusPill))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = AppleTypography.body().copy(fontSize = 15.sp),
                        color = iconColor
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = AppleTypography.body().copy(fontSize = 15.sp, color = textColor),
                    cursorBrush = SolidColor(if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = if (onDone != null) androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Filter Chip / Configurator Option Chip {component.configurator-option-chip}
 * Rounded pill shape, quiet surface, Action Blue border/fill when selected
 */
@Composable
fun AppleOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    accentColor: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "chipScale"
    )

    val chipShape = RoundedCornerShape(AppleTokens.RadiusPill)
    val activeColor = accentColor ?: if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue

    val (bgColor, textColor, borderColor) = when {
        isSelected -> Triple(
            activeColor,
            Color.White,
            Color.Transparent
        )
        isDarkTheme -> Triple(
            AppleTokens.Tile2,
            AppleTokens.BodyOnDark,
            AppleTokens.HairlineDark
        )
        else -> Triple(
            AppleTokens.CanvasWhite,
            AppleTokens.Ink,
            AppleTokens.HairlineLight
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(chipShape)
            .background(bgColor)
            .border(1.dp, borderColor, chipShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    AppHapticManager.playClick()
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppleTypography.captionStrong(),
            color = textColor
        )
    }
}

/**
 * Navigation Bar item with Apple's clean aesthetic
 */
@Composable
fun AppleTabBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "tabScale"
    )

    val activeColor = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue
    val inactiveColor = if (isDarkTheme) AppleTokens.InkSecondary else AppleTokens.InkMuted48

    Column(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    AppHapticManager.playClick()
                    onClick()
                }
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = AppleTypography.finePrint().copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Floating Vertical Date Slider in Thin Air {component.vertical-date-rail}
 * Numbers and weekdays float directly in the air without a background box/panel.
 * Top and bottom edges dissolve and blur into transparency via a vertical gradient mask.
 */
@Composable
fun AppleVerticalDateRail(
    days: List<Int>,
    weekDays: List<String>,
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    selectedColor: Color? = null,
    todayIndex: Int = 7
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Smoothly center the selected day in view
    LaunchedEffect(selectedDayIndex) {
        val target = (selectedDayIndex - 3).coerceAtLeast(0)
        listState.animateScrollToItem(target)
    }

    // Pure floating numbers in the air with dissolved top and bottom
    Box(
        modifier = modifier
            .width(50.dp)
            .fillMaxHeight()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Top and bottom dissolved in the air (fade out into transparent)
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.14f to Color.Black,
                        0.86f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            items(days.size) { index ->
                val isSelected = index == selectedDayIndex
                val isToday = index == todayIndex
                val dayNum = days[index]
                val weekDay = weekDays[index].uppercase()

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 0.90f
                        isSelected -> 1.18f
                        else -> 1f
                    },
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
                    label = "railItemScale"
                )

                val activeColor = selectedColor ?: if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue

                // In the air: selected is prominent theme accent; unselected is soft translucent text
                val textColor = when {
                    isSelected -> activeColor
                    isDarkTheme -> Color.White.copy(alpha = 0.40f)
                    else -> Color.Black.copy(alpha = 0.35f)
                }
                val subTextColor = when {
                    isSelected -> activeColor.copy(alpha = 0.85f)
                    isDarkTheme -> Color.White.copy(alpha = 0.25f)
                    else -> Color.Black.copy(alpha = 0.22f)
                }

                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(52.dp)
                        .scale(scale)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                AppHapticManager.playClick()
                                onDaySelected(index)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = weekDay,
                            style = AppleTypography.finePrint().copy(
                                fontSize = if (isSelected) 11.sp else 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.4.sp
                            ),
                            color = subTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = dayNum.toString(),
                            style = AppleTypography.heroDisplay().copy(
                                fontSize = if (isSelected) 22.sp else 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = (-0.4).sp
                            ),
                            color = textColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 4.5.dp else 3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor else textColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data model for Apple Radial Navigation Wheel
 */
data class RadialNavMenuItem(
    val title: String,
    val icon: ImageVector,
    val index: Int
)

/**
 * Apple Touch-Hold Charging Aura (Кольцо зарядки под пальцем при зажатии)
 * Renders a glowing breathing Apple aura right under the finger while holding.
 */
@Composable
fun AppleTouchChargingAura(
    touchOffset: androidx.compose.ui.geometry.Offset?,
    progress: Float,
    isDarkTheme: Boolean = true
) {
    if (touchOffset == null || progress <= 0.02f) return

    val density = androidx.compose.ui.platform.LocalDensity.current
    val xDp = with(density) { touchOffset.x.toDp() }
    val yDp = with(density) { touchOffset.y.toDp() }

    val auraSize = 28.dp + (46.dp * progress)
    val activeColor = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue
    val auraAlpha = (progress * 0.95f).coerceIn(0f, 0.95f)

    Box(
        modifier = Modifier
            .offset(x = xDp - (auraSize / 2), y = yDp - (auraSize / 2))
            .size(auraSize)
            .clip(CircleShape)
            .background(activeColor.copy(alpha = 0.15f * progress))
            .border(
                (1.5f + (progress * 1.2f)).dp,
                activeColor.copy(alpha = auraAlpha),
                CircleShape
            )
    )
}

/**
 * Apple Radial Circular Menu (Кружочек навигации)
 * Appears exactly under the user's finger upon long-pressing anywhere on the screen.
 * Blossoms outward with an explosive Apple shockwave ripple and spring physics.
 */
@Composable
fun AppleRadialMenu(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    items: List<RadialNavMenuItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    isDarkTheme: Boolean = true,
    accentColor: Color? = null,
    touchPosition: androidx.compose.ui.geometry.Offset? = null
) {
    if (!isOpen) return

    var bloomTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        bloomTriggered = true
    }

    val enterScale by animateFloatAsState(
        targetValue = if (bloomTriggered) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 420f),
        label = "radialScale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (bloomTriggered) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "radialAlpha"
    )

    // Explosive Bloom Physics: satellite discs shoot outward from the finger center
    val bloomProgress by animateFloatAsState(
        targetValue = if (bloomTriggered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 280f),
        label = "satelliteBloom"
    )

    // Shockwave ripple ring expanding outward
    val shockwaveProgress by animateFloatAsState(
        targetValue = if (bloomTriggered) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(480, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "shockwaveRipple"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp.dp
    val screenHeightDp = config.screenHeightDp.dp

    val menuDiameterDp = 300.dp
    val halfSizeDp = menuDiameterDp / 2

    val targetCenterXDp = with(density) { touchPosition?.x?.toDp() } ?: (screenWidthDp / 2)
    val targetCenterYDp = with(density) { touchPosition?.y?.toDp() } ?: (screenHeightDp / 2)

    val clampedXDp = targetCenterXDp.coerceIn(halfSizeDp + 8.dp, (screenWidthDp - halfSizeDp - 8.dp).coerceAtLeast(halfSizeDp + 8.dp))
    val clampedYDp = targetCenterYDp.coerceIn(halfSizeDp + 24.dp, (screenHeightDp - halfSizeDp - 24.dp).coerceAtLeast(halfSizeDp + 24.dp))

    val activeColor = accentColor ?: if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue

    // Full screen dimmed frosted overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f * enterAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // Radial container positioned EXACTLY under the finger
        Box(
            modifier = Modifier
                .offset(x = clampedXDp - halfSizeDp, y = clampedYDp - halfSizeDp)
                .size(menuDiameterDp)
                .scale(enterScale)
                .alpha(enterAlpha),
            contentAlignment = Alignment.Center
        ) {
            // Expanding Shockwave Aura Ring from under the finger
            if (shockwaveProgress < 0.98f) {
                val waveRadius = 32.dp + (116.dp * shockwaveProgress)
                val waveAlpha = (1f - shockwaveProgress).coerceIn(0f, 1f) * 0.75f
                Box(
                    modifier = Modifier
                        .size(waveRadius * 2)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            activeColor.copy(alpha = waveAlpha),
                            CircleShape
                        )
                )
            }

            // Subtle frosted circular guide ring
            Box(
                modifier = Modifier
                    .size(216.dp)
                    .clip(CircleShape)
                    .border(
                        1.dp,
                        if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                        CircleShape
                    )
            )

            // Center Disc (tap to close)
            val centerInteraction = remember { MutableInteractionSource() }
            val isCenterPressed by centerInteraction.collectIsPressedAsState()
            val centerScale by animateFloatAsState(
                targetValue = if (isCenterPressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "centerScale"
            )

            val centerBg = if (isDarkTheme) Color(0xFF2C2C2E).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
            val centerBorder = if (isDarkTheme) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(centerScale)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.35f),
                        ambientColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(CircleShape)
                    .background(centerBg)
                    .border(1.dp, centerBorder, CircleShape)
                    .clickable(
                        interactionSource = centerInteraction,
                        indication = null,
                        onClick = {
                            AppHapticManager.playClick()
                            onDismiss()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Закрыть",
                    tint = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else AppleTokens.Ink,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Satellite discs blossoming dynamically from the center
            val targetRadiusDp = 112f
            val currentRadiusDp = targetRadiusDp * bloomProgress
            val count = items.size
            items.forEachIndexed { i, item ->
                val angleRad = (-Math.PI / 2.0) + (i * 2.0 * Math.PI / count)
                val xOffset = (currentRadiusDp * kotlin.math.cos(angleRad)).toFloat()
                val yOffset = (currentRadiusDp * kotlin.math.sin(angleRad)).toFloat()

                val isSelected = item.index == selectedIndex
                val itemInteraction = remember { MutableInteractionSource() }
                val isItemPressed by itemInteraction.collectIsPressedAsState()
                val itemScale by animateFloatAsState(
                    targetValue = when {
                        isItemPressed -> 0.90f
                        isSelected -> 1.08f
                        else -> 1.0f
                    },
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                    label = "itemScale_$i"
                )

                val itemBg = if (isSelected) activeColor else if (isDarkTheme) Color(0xFF222224).copy(alpha = 0.96f) else Color(0xFFFAFAFC).copy(alpha = 0.96f)
                val itemBorder = if (isSelected) activeColor.copy(alpha = 0.6f) else if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
                val iconColor = if (isSelected) Color.White else if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink

                // Kinetic angular whip during flight
                val spinAngle = (1f - bloomProgress.coerceIn(0f, 1f)) * (if (i % 2 == 0) 35f else -35f)
                val bloomFade = (bloomProgress * 1.2f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .offset(x = xOffset.dp, y = yOffset.dp)
                        .scale(itemScale * bloomFade)
                        .graphicsLayer { rotationZ = spinAngle },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(
                                    elevation = if (isSelected) 14.dp else 6.dp,
                                    shape = CircleShape,
                                    spotColor = if (isSelected) activeColor.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.35f),
                                    ambientColor = if (isSelected) activeColor.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.10f)
                                )
                                .clip(CircleShape)
                                .background(itemBg)
                                .border(1.dp, itemBorder, CircleShape)
                                .clickable(
                                    interactionSource = itemInteraction,
                                    indication = null,
                                    onClick = {
                                        AppHapticManager.playClick()
                                        onSelectIndex(item.index)
                                        onDismiss()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = iconColor,
                                modifier = Modifier.size(27.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppleTokens.RadiusPill))
                                .background(
                                    if (isSelected) activeColor.copy(alpha = 0.40f)
                                    else Color(0xD018181A)
                                )
                                .border(
                                    0.5.dp,
                                    if (isSelected) activeColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(AppleTokens.RadiusPill)
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = AppleTypography.finePrint().copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}


