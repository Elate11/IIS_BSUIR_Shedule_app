@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.schedule

import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request

import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.padding
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.style.TextOverflow

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


fun getBsuirWeekForCalendarDay(selectedDayIndex: Int): Pair<Int, Int> {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_MONTH, -7 + selectedDayIndex)

    var dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    if (dow == 0) dow = 7

    val year = cal.get(java.util.Calendar.YEAR)
    val month = cal.get(java.util.Calendar.MONTH)
    val sep1Year = if (month < java.util.Calendar.AUGUST) year - 1 else year

    val sep1Cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, sep1Year)
        set(java.util.Calendar.MONTH, java.util.Calendar.SEPTEMBER)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 12)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    var sep1Dow = sep1Cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    if (sep1Dow == 0) sep1Dow = 7
    sep1Cal.add(java.util.Calendar.DAY_OF_MONTH, -(sep1Dow - 1))

    val targetMonday = (cal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, 12)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        add(java.util.Calendar.DAY_OF_MONTH, -(dow - 1))
    }

    val diffMillis = targetMonday.timeInMillis - sep1Cal.timeInMillis
    val diffWeeks = (diffMillis / (7L * 24 * 60 * 60 * 1000)).toInt()
    val weekNum = ((diffWeeks % 4 + 4) % 4) + 1
    return Pair(dow, weekNum)
}

data class NextLessonTarget(
    val day: Int,
    val month0Based: Int,
    val year: Int,
    val hour: Int,
    val minute: Int,
    val dateFormatted: String,
    val timeFormatted: String,
    val lessonType: String
)

fun findNextLessonForSubject(
    context: android.content.Context,
    group: String,
    subject: String,
    subgroup: Int = 0,
    baseCalendar: java.util.Calendar = java.util.Calendar.getInstance(),
    afterTime: String? = null
): NextLessonTarget? {
    val dm = DataManager(context)
    val allLessons = dm.getLessonsForTarget(group).ifEmpty { dm.getLessons() }
    val cleanSubj = subject.trim()
    if (allLessons.isEmpty() || cleanSubj.isBlank() || cleanSubj == "Общее" || cleanSubj == "Все") return null
    val subjectLessons = allLessons.filter { l ->
        l.title.equals(cleanSubj, ignoreCase = true) &&
            (subgroup == 0 || l.subgroup == 0 || l.subgroup == subgroup)
    }
    if (subjectLessons.isEmpty()) return null

    // Helper to calculate BSUIR DOW (1=Mon..7=Sun) and Week (1..4) for any Calendar date
    fun getWeekInfo(cal: java.util.Calendar): Pair<Int, Int> {
        var dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        if (dow == 0) dow = 7

        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH)
        val sep1Year = if (month < java.util.Calendar.AUGUST) year - 1 else year

        val sep1Cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, sep1Year)
            set(java.util.Calendar.MONTH, java.util.Calendar.SEPTEMBER)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        var sep1Dow = sep1Cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        if (sep1Dow == 0) sep1Dow = 7
        sep1Cal.add(java.util.Calendar.DAY_OF_MONTH, -(sep1Dow - 1))

        val targetMonday = (cal.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            add(java.util.Calendar.DAY_OF_MONTH, -(dow - 1))
        }

        val diffMillis = targetMonday.timeInMillis - sep1Cal.timeInMillis
        val diffWeeks = (diffMillis / (7L * 24 * 60 * 60 * 1000)).toInt()
        val weekNum = ((diffWeeks % 4 + 4) % 4) + 1
        return Pair(dow, weekNum)
    }

    // 1. If afterTime is specified, first check if there is another lesson later TODAY (after afterTime)
    if (!afterTime.isNullOrBlank()) {
        val (baseDow, baseWk) = getWeekInfo(baseCalendar)
        val laterToday = subjectLessons
            .filter { l ->
                l.dayOfWeek == baseDow &&
                    (l.weeks.isEmpty() || l.weeks.contains(baseWk)) &&
                    l.startTime > afterTime
            }
            .sortedWith(
                compareBy<Lesson> { if (it.lessonType.contains("ЛР", true) || it.lessonType.contains("ПЗ", true)) 0 else 1 }
                    .thenBy { it.startTime }
            )
            .firstOrNull()

        if (laterToday != null) {
            val d = baseCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            val m = baseCalendar.get(java.util.Calendar.MONTH)
            val y = baseCalendar.get(java.util.Calendar.YEAR)
            val timeParts = laterToday.startTime.split(":")
            val h = timeParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
            val min = timeParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            return NextLessonTarget(
                day = d,
                month0Based = m,
                year = y,
                hour = h,
                minute = min,
                dateFormatted = "%02d.%02d.%04d".format(d, m + 1, y),
                timeFormatted = "%02d:%02d".format(h, min),
                lessonType = laterToday.lessonType
            )
        }
    }

    // 2. Otherwise search STRICTLY FUTURE days starting from baseCalendar + 1 day
    for (offset in 1..60) {
        val nextCal = (baseCalendar.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_MONTH, offset)
        }
        val (dow, wk) = getWeekInfo(nextCal)
        val matching = subjectLessons
            .filter { it.dayOfWeek == dow && (it.weeks.isEmpty() || it.weeks.contains(wk)) }
            .sortedWith(
                compareBy<Lesson> { if (it.lessonType.contains("ЛР", true) || it.lessonType.contains("ПЗ", true)) 0 else 1 }
                    .thenBy { it.startTime }
            )
            .firstOrNull()
        if (matching != null) {
            val cal = nextCal
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val m = cal.get(java.util.Calendar.MONTH)
            val y = cal.get(java.util.Calendar.YEAR)
            val timeParts = matching.startTime.split(":")
            val h = timeParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
            val min = timeParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            return NextLessonTarget(
                day = d,
                month0Based = m,
                year = y,
                hour = h,
                minute = min,
                dateFormatted = "%02d.%02d.%04d".format(d, m + 1, y),
                timeFormatted = "%02d:%02d".format(h, min),
                lessonType = matching.lessonType
            )
        }
    }
    return null
}

fun fetchAndStoreStudentProfile(context: android.content.Context) {
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val client = NetworkClient.client

    // 1. Try /api/v1/profiles/personal-cv
    try {
        val req = NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/profiles/personal-cv")
        client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
                val body = resp.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = org.json.JSONObject(body)
                    val fio = json.optString("fio", "").ifBlank {
                        val fn = json.optString("firstName", "")
                        val ln = json.optString("lastName", "")
                        val mn = json.optString("middleName", "")
                        "$ln $fn $mn".trim()
                    }
                    val group = json.optString("studentGroup", "").ifBlank {
                        json.optString("group", "").ifBlank {
                            json.optJSONObject("studentGroup")?.optString("name", "") ?: ""
                        }
                    }
                    val photo = json.optString("photoUrl", "").ifBlank { json.optString("photoLink", "") }
                    val faculty = json.optString("faculty", "").ifBlank { json.optString("facultyAbbrev", "") }
                    val course = json.optInt("course", 0)

                    val edit = prefs.edit()
                    if (fio.isNotBlank()) edit.putString("login_fio", fio).putString("user_fio", fio)
                    if (group.isNotBlank()) {
                        edit.putString("login_group", group)
                        edit.putString("selectedGroup", group)
                    }
                    if (photo.isNotBlank()) edit.putString("login_photo", photo)
                    if (faculty.isNotBlank() || course > 0) {
                        edit.putString("login_desc", "$faculty${if (course > 0) ", $course курс" else ""}".trim())
                    }
                    edit.apply()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // 2. Try /api/v1/markbook if group is still empty or to supplement data
    val currentSavedGroup = prefs.getString("login_group", "") ?: ""
    if (currentSavedGroup.isBlank()) {
        try {
            val req = NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/markbook")
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = org.json.JSONObject(body)
                        val student = json.optJSONObject("student")
                        val group = student?.optString("group", "")?.ifBlank {
                            student.optString("studentGroup", "")
                        } ?: json.optJSONObject("markBookGroupDto")?.optString("studentGroup", "") ?: json.optString("studentGroup", "")

                        val fio = student?.optString("fio", "")?.ifBlank {
                            val fn = student.optString("firstName", "")
                            val ln = student.optString("lastName", "")
                            val mn = student.optString("middleName", "")
                            "$ln $fn $mn".trim()
                        } ?: ""

                        val edit = prefs.edit()
                        if (group.isNotBlank()) {
                            edit.putString("login_group", group)
                            edit.putString("selectedGroup", group)
                        }
                        if (fio.isNotBlank()) edit.putString("login_fio", fio).putString("user_fio", fio)
                        edit.apply()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 3. Try /api/v1/omissions if still missing
    val afterGroup = prefs.getString("login_group", "") ?: ""
    if (afterGroup.isBlank()) {
        try {
            val req = NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/omissions")
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = org.json.JSONObject(body)
                        val group = json.optString("studentGroup", "")
                        if (group.isNotBlank()) {
                            prefs.edit().putString("login_group", group).putString("selectedGroup", group).apply()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


object AppHapticManager {
    var isEnabled: Boolean = true
    var strength: Float = 0.6f
    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    fun playClick() {
        val ctx = appContext ?: return
        if (!isEnabled || strength <= 0.05f) return
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val duration = (12 + (strength * 28)).toLong()
                    val amplitude = (strength * 255).toInt().coerceIn(1, 255)
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate((25 * strength).toLong().coerceAtLeast(1L))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}







@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        NetworkClient.init(applicationContext)
        BsuirApi.initCache(applicationContext)
        AppHapticManager.init(applicationContext)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        AppHapticManager.isEnabled = prefs.getBoolean("vibration_enabled", true)
        AppHapticManager.strength = prefs.getFloat("vibration_strength", 0.6f)
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
fun AutoResizedText(
    text: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    minFontSize: androidx.compose.ui.unit.TextUnit = 9.sp,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    maxLines: Int = 1,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current
) {
    var resizedFontSize by androidx.compose.runtime.remember(text, fontSize) { 
        androidx.compose.runtime.mutableStateOf(fontSize) 
    }
    var readyToDraw by androidx.compose.runtime.remember(text, fontSize) { 
        androidx.compose.runtime.mutableStateOf(false) 
    }

    androidx.compose.material3.Text(
        text = text,
        color = color,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        fontSize = resizedFontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = false,
        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        style = style,
        onTextLayout = { result ->
            if (result.didOverflowWidth || result.didOverflowHeight) {
                if (resizedFontSize.value > minFontSize.value) {
                    resizedFontSize = (resizedFontSize.value - 1f).sp
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
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

object IntroVideoPreviewTrigger {
    var requestReplay by mutableStateOf(false)
}

@Composable
fun BsuirIntroVideoSplash(
    onFinished: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isFadingOut by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableFloatStateOf(0f) }

    val fadeAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFadingOut) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 360,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        finishedListener = {
            if (isFadingOut) onFinished()
        },
        label = "introFadeAlpha"
    )
    val fadeScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFadingOut) 1.04f else 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 360,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "introFadeScale"
    )

    // 60fps animation clock + safety fallback timer
    LaunchedEffect(Unit) {
        val startNanos = System.nanoTime()
        while (!isFadingOut) {
            androidx.compose.runtime.withFrameNanos { now ->
                elapsedSec = ((now - startNanos) / 1_000_000_000f).coerceAtLeast(0f)
                if (elapsedSec >= 4.0f && !isFadingOut) {
                    isFadingOut = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = fadeAlpha
                scaleX = fadeScale
                scaleY = fadeScale
            }
            .background(Color(0xFF101116))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                if (!isFadingOut) {
                    isFadingOut = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Instant 60fps vector/canvas backdrop (spotlight, sonar radio-waves, particles & glowing logo)
        // Guarantees 0ms visual start even while hardware MediaPlayer warms up
        val t = elapsedSec
        val logoScale = remember(t) {
            val u = (t / 0.82f).coerceIn(0f, 1f)
            val s = 1.15f
            val u1 = u - 1f
            val back = u1 * u1 * ((s + 1f) * u1 + s) + 1f
            0.84f + 0.16f * back
        }
        val logoAlpha = remember(t) {
            ((t / 0.45f).coerceIn(0f, 1f))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val bloomAlpha = ((t / 0.7f).coerceIn(0f, 1f)) * 0.32f
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2997FF).copy(alpha = bloomAlpha),
                        Color(0xFF12326B).copy(alpha = bloomAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(cx, cy)
            )

            // Expanding sonar / radio wave rings from top-right of logo
            val waveOrigin = Offset(cx + 38.dp.toPx(), cy - 22.dp.toPx())
            for (wIdx in 0..2) {
                val wStart = 0.16f + wIdx * 0.22f
                if (t > wStart) {
                    val wt = ((t - wStart) / 1.35f).coerceIn(0f, 1f)
                    if (wt < 1f) {
                        val easeOut = 1f - (1f - wt) * (1f - wt) * (1f - wt)
                        val rad = 24.dp.toPx() + easeOut * 195.dp.toPx()
                        val ringAlpha = (1f - wt) * (1f - wt) * 0.34f
                        drawCircle(
                            color = Color(0xFF38A3FF).copy(alpha = ringAlpha),
                            radius = rad,
                            center = waveOrigin,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // Crisp tinted BSUIR logo in the center
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.bsuir_logo),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF38A3FF)),
            modifier = Modifier
                .width(168.dp)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha = logoAlpha
                }
        )

        // 2. Hardware MP4 video layer (R.raw.intro_video)
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.view.TextureView(ctx).apply {
                    isOpaque = false
                    alpha = 0f
                    var mediaPlayer: android.media.MediaPlayer? = null

                    fun applyCenterCropTransform(viewWidth: Int, viewHeight: Int, videoW: Int, videoH: Int) {
                        if (viewWidth <= 0 || viewHeight <= 0 || videoW <= 0 || videoH <= 0) return
                        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
                        val videoRatio = videoW.toFloat() / videoH.toFloat()
                        val scaleX: Float
                        val scaleY: Float
                        if (videoRatio > viewRatio) {
                            scaleX = videoRatio / viewRatio
                            scaleY = 1f
                        } else {
                            scaleX = 1f
                            scaleY = viewRatio / videoRatio
                        }
                        val matrix = android.graphics.Matrix()
                        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
                        setTransform(matrix)
                    }

                    surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            try {
                                val afd = ctx.resources.openRawResourceFd(com.example.schedule.R.raw.intro_video) ?: return
                                val mp = android.media.MediaPlayer()
                                mediaPlayer = mp
                                mp.setSurface(android.view.Surface(surfaceTexture))
                                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                mp.isLooping = false
                                mp.setVolume(0f, 0f)
                                mp.setOnVideoSizeChangedListener { _, vw, vh ->
                                    applyCenterCropTransform(width, height, vw, vh)
                                }
                                mp.setOnInfoListener { _, what, _ ->
                                    if (what == android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                        alpha = 1f
                                    }
                                    false
                                }
                                mp.setOnPreparedListener { player ->
                                    applyCenterCropTransform(width, height, player.videoWidth, player.videoHeight)
                                    player.start()
                                }
                                mp.setOnCompletionListener {
                                    isFadingOut = true
                                }
                                mp.setOnErrorListener { _, _, _ ->
                                    false
                                }
                                mp.prepareAsync()
                            } catch (_: Exception) {}
                        }

                        override fun onSurfaceTextureSizeChanged(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            mediaPlayer?.let { mp ->
                                applyCenterCropTransform(width, height, mp.videoWidth, mp.videoHeight)
                            }
                        }

                        override fun onSurfaceTextureDestroyed(surfaceTexture: android.graphics.SurfaceTexture): Boolean {
                            try {
                                mediaPlayer?.stop()
                            } catch (_: Exception) {}
                            try {
                                mediaPlayer?.release()
                            } catch (_: Exception) {}
                            mediaPlayer = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: android.graphics.SurfaceTexture) {
                            if (alpha < 1f) alpha = 1f
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MinimalistApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var showIntroVideo by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(sharedPreferences.getBoolean("intro_video_enabled", true))
    }
    LaunchedEffect(IntroVideoPreviewTrigger.requestReplay) {
        if (IntroVideoPreviewTrigger.requestReplay) {
            showIntroVideo = true
            IntroVideoPreviewTrigger.requestReplay = false
        }
    }
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
    var isLoggedIn by remember { mutableStateOf(sharedPreferences.getBoolean("is_logged_in", false)) }
    var selectedGroup by remember { mutableStateOf(
        sharedPreferences.getString("selectedGroup", null)?.takeIf { it != "114001" && it.isNotBlank() }
            ?: sharedPreferences.getString("login_group", null)?.takeIf { it != "114001" && it.isNotBlank() }
            ?: "310101"
    ) }

    androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                fetchAndStoreStudentProfile(context)
                val syncedGroup = sharedPreferences.getString("login_group", null)?.takeIf { it.isNotBlank() }
                if (!syncedGroup.isNullOrBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        selectedGroup = syncedGroup
                    }
                }
            }
        }
    }
    if (!isLoggedIn) {
        Box(modifier = Modifier.fillMaxSize()) {
            MinLoginScreen(
                MinBg = Color(0xFF101116),
                MinBorder = Color(0xFF222222),
                MinTextPrimary = Color(0xFFEEEEEE),
                MinTextSecondary = Color(0xFF888888),
                isDarkTheme = true,
                onLoginSuccess = { gradebook, token ->
                    if (token.isBlank()) {
                        // Guest mode: clear all previous private data so personal grades/FIO are never exposed
                        sharedPreferences.edit()
                            .putBoolean("is_logged_in", true)
                            .remove("auth_token")
                            .remove("login_fio")
                            .remove("login_group")
                            .remove("login_photo")
                            .remove("login_desc")
                            .remove("cached_academic_progress")
                            .remove("cached_gradebook")
                            .remove("cached_profile")
                            .remove("gradebook_number")
                            .remove("saved_password")
                            .apply()
                        selectedGroup = sharedPreferences.getString("selectedGroup", "510902") ?: "510902"
                        isLoggedIn = true
                    } else {
                        val realGroup = sharedPreferences.getString("login_group", null)
                        val groupStr = if (!realGroup.isNullOrBlank()) realGroup else (if (gradebook.length >= 6) gradebook.take(6) else gradebook)
                        sharedPreferences.edit().putBoolean("is_logged_in", true)
                             .putString("gradebook", gradebook)
                             .putString("auth_token", token)
                             .putString("selectedGroup", groupStr)
                             .putString("login_group", groupStr)
                             .apply()
                        selectedGroup = groupStr
                        isLoggedIn = true
                    }
                }
            )
            if (showIntroVideo) {
                BsuirIntroVideoSplash(onFinished = { showIntroVideo = false })
            }
        }
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
    androidx.compose.runtime.LaunchedEffect(customPrimaryColor) {
        sharedPreferences.edit().putInt("customPrimaryColor", customPrimaryColor?.toArgb() ?: -1).apply()
        ScheduleWidgetProvider.updateAllWidgets(context)
    }
    androidx.compose.runtime.LaunchedEffect(customBackgroundColor) {
        sharedPreferences.edit().putInt("customBackgroundColor", customBackgroundColor?.toArgb() ?: -1).apply()
        ScheduleWidgetProvider.updateAllWidgets(context)
    }
    androidx.compose.runtime.LaunchedEffect(customParticleColor) { sharedPreferences.edit().putInt("customParticleColor", customParticleColor?.toArgb() ?: -1).apply() }
    androidx.compose.runtime.LaunchedEffect(textSizeMultiplier) { sharedPreferences.edit().putFloat("textSizeMultiplier", textSizeMultiplier).apply() }
    androidx.compose.runtime.LaunchedEffect(selectedGroup) {
        sharedPreferences.edit().putString("selectedGroup", selectedGroup).apply()
        ScheduleWidgetProvider.updateAllWidgets(context)
    }
    androidx.compose.runtime.LaunchedEffect(isDarkTheme) {
        sharedPreferences.edit().putBoolean("isDarkTheme", isDarkTheme).apply()
        ScheduleWidgetProvider.updateAllWidgets(context)
    }
    
    var bgMode by remember { mutableStateOf(sharedPreferences.getString("bgMode", "Solid") ?: "Solid") }
    var bgImageUri by remember {
        val saved = sharedPreferences.getString("bgImageUri", null)
        val defaultBgFile = java.io.File(context.filesDir, "custom_background.png")
        val initialUri = if (saved != null && java.io.File(saved).exists()) {
            saved
        } else if (defaultBgFile.exists()) {
            defaultBgFile.absolutePath
        } else {
            saved
        }
        mutableStateOf(initialUri)
    }
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
        else customBackgroundColor ?: if (isDarkTheme) AppleTokens.Tile1 else AppleTokens.Parchment

    val MinBg = if (isTechnoStyle) Color.Transparent
        else if (bgMode == "Solid") actualBg else Color.Transparent

    val MinCardBg = if (isTechnoStyle) Color.Transparent 
        else if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.CanvasWhite

    val MinBorder = if (isTechnoStyle) Color(0xFF00FF41).copy(alpha = 0.25f) 
        else if (isDarkTheme) AppleTokens.HairlineDark else AppleTokens.HairlineLight

    val MinTextPrimary = if (styleType == StyleType.Techno) Color(0xFF00FF41)
        else customPrimaryColor ?: if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink

    val MinTextSecondary = if (styleType == StyleType.Techno) Color(0xFF00AA22)
        else if (isDarkTheme) AppleTokens.BodyMutedDark else AppleTokens.InkSecondary
    
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
    // Apple signature single interactive blue accent: Action Blue (#0066cc) / Sky Link Blue on dark (#2997ff)
    val MinAccent = if (styleType == StyleType.Techno) Color(0xFF00FF41)
        else customPrimaryColor ?: if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
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
        var showRadialMenu by remember { mutableStateOf(false) }
        var radialTouchPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
        var touchChargeOffset by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
        val touchChargeAnim = remember { androidx.compose.animation.core.Animatable(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            touchChargeOffset = down.position
                            val job = coroutineScope.launch {
                                touchChargeAnim.snapTo(0f)
                                touchChargeAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 480, easing = LinearEasing)
                                )
                            }
                            val result = withTimeoutOrNull(480L) {
                                while (true) {
                                    val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        return@withTimeoutOrNull false
                                    }
                                    val dist = kotlin.math.hypot((change.position.x - down.position.x).toDouble(), (change.position.y - down.position.y).toDouble())
                                    if (dist > 55.0) {
                                        return@withTimeoutOrNull false
                                    }
                                }
                                true
                            }
                            val isLongPress = (result == null)
                            job.cancel()
                            if (isLongPress) {
                                AppHapticManager.playClick()
                                radialTouchPosition = down.position
                                showRadialMenu = true
                                while (true) {
                                    val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                            touchChargeOffset = null
                            coroutineScope.launch {
                                touchChargeAnim.snapTo(0f)
                            }
                        }
                    }
                }
        ) {
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
                bottomBar = {},
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color.Transparent)) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
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
                                        val s = (1f - (kotlin.math.abs(pageOffset) * 0.25f)).coerceIn(0f, 1f)
                                        scaleX = s
                                        scaleY = s
                                        alpha = (1f - kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                                        translationX = pageOffset * size.width
                                    }
                                    TransitionType.Cube -> {
                                        cameraDistance = 12f * density
                                        val clampedOffset = pageOffset.coerceIn(-1f, 1f)
                                        rotationY = clampedOffset * 90f
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                            pivotFractionX = if (clampedOffset > 0) 0f else 1f,
                                            pivotFractionY = 0.5f
                                        )
                                        alpha = (1f - kotlin.math.abs(clampedOffset) * 0.4f).coerceIn(0f, 1f)
                                    }
                                    TransitionType.Flip -> {
                                        cameraDistance = 12f * density
                                        val clampedOffset = pageOffset.coerceIn(-1f, 1f)
                                        rotationY = clampedOffset * 180f
                                        alpha = if (kotlin.math.abs(clampedOffset) > 0.5f) 0f else 1f
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

        AppleTouchChargingAura(
            touchOffset = touchChargeOffset,
            progress = touchChargeAnim.value,
            isDarkTheme = isDarkTheme,
            accentColor = MinAccent
        )

        AppleRadialMenu(
            isOpen = showRadialMenu,
            onDismiss = { showRadialMenu = false },
            items = listOf(
                RadialNavMenuItem("Расписание", Icons.Outlined.DateRange, 0),
                RadialNavMenuItem("Оценки", Icons.Outlined.CheckCircle, 1),
                RadialNavMenuItem("Заметки", Icons.Outlined.Edit, 2),
                RadialNavMenuItem("Профиль", Icons.Outlined.Person, 3)
            ),
            selectedIndex = pagerState.currentPage,
            onSelectIndex = { index ->
                if (index != pagerState.currentPage) {
                    coroutineScope.launch {
                        if (transitionsEnabled) {
                            val duration = ((420f / transitionSpeedMultiplier.coerceAtLeast(0.5f)).toInt()).coerceIn(150, 1000)
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = tween(
                                    durationMillis = duration,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                )
                            )
                        } else {
                            pagerState.scrollToPage(index)
                        }
                    }
                }
            },
            isDarkTheme = isDarkTheme,
            accentColor = MinAccent,
            touchPosition = radialTouchPosition
        )
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (styleType == StyleType.Techno) {
                    MatrixRainLayer { appContent() }
                } else {
                    appContent()
                }
                if (showIntroVideo) {
                    BsuirIntroVideoSplash(onFinished = { showIntroVideo = false })
                }
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
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = 300f
        ),
        label = "tiltX"
    )
    val animatedTiltY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rawTiltY,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = 300f
        ),
        label = "tiltY"
    )

    return Pair(animatedTiltX, animatedTiltY)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MinScheduleScreen(MinBg: Color, actualBg: Color, MinCardBg: Color, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, MinAccent: Color, isDarkTheme: Boolean, selectedGroup: String, onGroupSelected: (String) -> Unit, selectedSubgroup: Int, onSubgroupChange: (Int) -> Unit, displayTitle: String? = null) {
    var showScheduleSettings by remember { mutableStateOf(false) }
    var inlineTeacherData by remember { mutableStateOf<Pair<String, String>?>(null) }
    val effectiveGroup = inlineTeacherData?.first ?: selectedGroup
    val effectiveTitle = inlineTeacherData?.second ?: displayTitle

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
    val schedulePrefs = remember { context.getSharedPreferences("schedule_prefs", android.content.Context.MODE_PRIVATE) }
    var isDateRailVisible by remember { mutableStateOf(schedulePrefs.getBoolean("date_rail_visible", true)) }
    var lessons by remember(effectiveGroup) {
        val localTarget = dataManager.getLessonsForTarget(effectiveGroup)
        val initial = if (localTarget.isNotEmpty()) localTarget else if (inlineTeacherData == null) dataManager.getLessons() else emptyList()
        mutableStateOf(initial)
    }
    var isSessionMode by remember { mutableStateOf(false) }
    var sessionExams by remember(effectiveGroup) { mutableStateOf(dataManager.getExams(effectiveGroup)) }
    var sessionPeriod by remember(effectiveGroup) { mutableStateOf(dataManager.getSessionPeriod(effectiveGroup)) }

    var allGroups by remember { mutableStateOf(BsuirApi.getCachedAllGroups()) }
    var allEmployees by remember { mutableStateOf(BsuirApi.getCachedAllEmployees()) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            BsuirApi.initCache(context)
            val grps = BsuirApi.getAllGroups()
            val emps = BsuirApi.getAllEmployees()
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (grps.isNotEmpty()) allGroups = grps
                if (emps.isNotEmpty()) allEmployees = emps
            }
        }
    }

    val validDayIndices = remember { (0..90).toList() }
    val dayInfo = remember(selectedDayIndex) { getBsuirWeekForCalendarDay(selectedDayIndex) }
    val selectedDayOfWeek = dayInfo.first
    val currentWeek = dayInfo.second

    fun parseScheduleResponse(resp: BsuirScheduleResponse?): Pair<List<Lesson>, List<Lesson>> {
        val newLessons = mutableListOf<Lesson>()
        val schedMap = (if (!resp?.schedules.isNullOrEmpty()) resp?.schedules else resp?.nextSchedules) ?: emptyMap()
        schedMap.forEach { (dayName, dayLessons) ->
            val dn = dayName.lowercase().trim()
            val dow = if ("пон" in dn || "mon" in dn) 1
                else if ("втор" in dn || "tue" in dn) 2
                else if ("сред" in dn || "wed" in dn) 3
                else if ("чет" in dn || "thu" in dn) 4
                else if ("пят" in dn || "fri" in dn) 5
                else if ("суб" in dn || "sat" in dn) 6
                else if ("вос" in dn || "sun" in dn) 7
                else 0
            dayLessons.forEach { bl ->
                val teacher = bl.employees?.firstOrNull()
                val teacherShort = if (teacher != null) {
                    val ln = teacher.lastName ?: ""
                    val fn = teacher.firstName?.take(1)?.let { "$it." } ?: ""
                    val mn = teacher.middleName?.take(1)?.let { "$it." } ?: ""
                    "$ln $fn$mn".trim()
                } else ""
                val aud = bl.auditories?.filter { it.isNotBlank() }?.joinToString(", ") ?: ""
                val subjectTitle = bl.subject?.takeIf { it.isNotBlank() }
                    ?: bl.note?.takeIf { it.isNotBlank() }
                    ?: bl.subjectFullName?.takeIf { it.isNotBlank() }
                    ?: if (bl.announcement == true) "Объявление" else "Занятие"
                val lType = bl.lessonTypeAbbrev?.takeIf { it.isNotBlank() }
                    ?: if (bl.announcement == true) "Инфо" else ""
                val parts = mutableListOf<String>()
                if (lType.isNotBlank()) parts.add(lType)
                if (teacherShort.isNotBlank()) parts.add(teacherShort)
                if (aud.isNotBlank()) parts.add(aud)
                if (!bl.note.isNullOrBlank() && subjectTitle != bl.note) {
                    parts.add(bl.note)
                }
                val detailsStr = if (parts.isNotEmpty()) parts.joinToString(" • ") else ""

                val groupsList = bl.studentGroups?.mapNotNull { it.name?.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                newLessons.add(Lesson(
                    startTime = bl.startLessonTime ?: "",
                    endTime = bl.endLessonTime ?: "",
                    title = subjectTitle,
                    details = detailsStr,
                    isActive = false,
                    progress = null,
                    subjectFullName = bl.subjectFullName ?: subjectTitle,
                    lessonType = lType,
                    teacherName = teacherShort,
                    teacherFullName = teacher?.fullName ?: teacherShort,
                    teacherPhoto = teacher?.photoLink?.ifBlank { null }
                        ?: (teacher?.id?.let { "https://iis.bsuir.by/api/v1/employees/photo/" } ?: ""),
                    teacherUrlId = teacher?.urlId ?: "",
                    auditory = aud,
                    subgroup = bl.numSubgroup ?: 0,
                    weeks = bl.weekNumber ?: emptyList(),
                    dayOfWeek = dow,
                    dateLesson = bl.dateLesson,
                    note = bl.note,
                    studentGroups = groupsList
                ))
            }
        }
        val parsedExams = mutableListOf<Lesson>()
        resp?.exams?.forEach { bl ->
            val teacher = bl.employees?.firstOrNull()
            val teacherShort = if (teacher != null) {
                val ln = teacher.lastName ?: ""
                val fn = teacher.firstName?.take(1)?.let { "$it." } ?: ""
                val mn = teacher.middleName?.take(1)?.let { "$it." } ?: ""
                "$ln $fn$mn".trim()
            } else ""
            val aud = bl.auditories?.filter { it.isNotBlank() }?.joinToString(", ") ?: ""
            val lType = bl.lessonTypeAbbrev?.takeIf { it.isNotBlank() } ?: "Экзамен"
            val parts = mutableListOf<String>()
            parts.add(lType)
            if (teacherShort.isNotBlank()) parts.add(teacherShort)
            if (aud.isNotBlank()) parts.add(aud)
            val detailsStr = parts.joinToString(" • ")
            val examGroups = bl.studentGroups?.mapNotNull { it.name?.trim() }?.filter { it.isNotBlank() } ?: emptyList()

            parsedExams.add(Lesson(
                startTime = bl.startLessonTime ?: "",
                endTime = bl.endLessonTime ?: "",
                title = bl.subject?.takeIf { it.isNotBlank() } ?: bl.subjectFullName ?: "Экзамен",
                details = detailsStr,
                isActive = false,
                progress = null,
                subjectFullName = bl.subjectFullName ?: "",
                lessonType = lType,
                teacherName = teacherShort,
                teacherFullName = teacher?.fullName ?: teacherShort,
                teacherPhoto = teacher?.photoLink?.ifBlank { null }
                        ?: (teacher?.id?.let { "https://iis.bsuir.by/api/v1/employees/photo/" } ?: ""),
                teacherUrlId = teacher?.urlId ?: "",
                auditory = aud,
                subgroup = bl.numSubgroup ?: 0,
                weeks = bl.weekNumber ?: listOf(1, 2, 3, 4),
                dayOfWeek = 0,
                dateLesson = bl.dateLesson,
                note = bl.note,
                studentGroups = examGroups
            ))
        }
        return newLessons to parsedExams
    }

    LaunchedEffect(effectiveGroup) {
        BsuirApi.initCache(context)
        val realLoginGrp = prefs.getString("login_group", null)?.takeIf { it.isNotBlank() && it != "114001" }
        val targetGrp = if (effectiveGroup == "114001" && realLoginGrp != null) realLoginGrp else effectiveGroup
        if (targetGrp != effectiveGroup && inlineTeacherData == null) {
            onGroupSelected(targetGrp)
            return@LaunchedEffect
        }

        // 1. Instant local load from device memory (DataManager & BsuirApi disk cache)
        val cachedTargetLessons = dataManager.getLessonsForTarget(targetGrp)
        if (cachedTargetLessons.isNotEmpty()) {
            lessons = cachedTargetLessons
        } else {
            val diskResp = withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (effectiveGroup.any { it.isLetter() }) {
                    BsuirApi.getCachedEmployeeSchedule(effectiveGroup)
                } else {
                    BsuirApi.getCachedGroupSchedule(effectiveGroup)
                }
            }
            if (diskResp != null) {
                val (diskLessons, diskExams) = parseScheduleResponse(diskResp)
                if (diskLessons.isNotEmpty()) {
                    lessons = diskLessons
                    dataManager.saveLessonsForTarget(targetGrp, diskLessons)
                }
                if (diskExams.isNotEmpty()) {
                    sessionExams = diskExams
                    dataManager.saveExams(targetGrp, diskExams)
                }
            }
        }
        sessionExams = dataManager.getExams(targetGrp)
        sessionPeriod = dataManager.getSessionPeriod(targetGrp)

        // 2. Background network sync (updates device memory seamlessly when online)
        val response = if (effectiveGroup.any { it.isLetter() }) {
            BsuirApi.getEmployeeSchedule(effectiveGroup)
        } else {
            BsuirApi.getGroupSchedule(effectiveGroup)
        }

        val (newLessons, parsedExams) = parseScheduleResponse(response)

        if (response != null) {
            val startEx = response.startExamsDate ?: ""
            val endEx = response.endExamsDate ?: ""
            dataManager.saveSessionPeriod(targetGrp, startEx, endEx)
            sessionPeriod = startEx to endEx

            dataManager.saveExams(targetGrp, parsedExams)
            sessionExams = parsedExams
        } else {
            sessionExams = dataManager.getExams(targetGrp)
            sessionPeriod = dataManager.getSessionPeriod(targetGrp)
        }

        if (newLessons.isNotEmpty()) {
            dataManager.saveLessonsForTarget(targetGrp, newLessons)
            lessons = newLessons
            // Pre-warm teacher schedules in background and store in device memory for offline use
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val teachers = newLessons.mapNotNull { it.teacherUrlId.takeIf { id -> id.isNotBlank() } }.distinct()
                    teachers.forEach { uid ->
                        BsuirApi.getOrFetchEmployeeSchedule(uid)
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        } else if (lessons.isEmpty()) {
            val cached = dataManager.getLessonsForTarget(targetGrp).ifEmpty { dataManager.getLessons() }
            lessons = cached
        }
    }

    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val filteredSessionExams = remember(sessionExams, selectedSubgroup) {
        val list = if (selectedSubgroup == 0) sessionExams else sessionExams.filter { it.subgroup == 0 || it.subgroup == selectedSubgroup }
        list.sortedWith(
            compareBy<Lesson> { lesson ->
                lesson.dateLesson?.let { dStr ->
                    try {
                        val parts = dStr.split(".")
                        if (parts.size == 3) {
                            "${parts[2]}${parts[1]}${parts[0]}"
                        } else dStr
                    } catch (e: Exception) { dStr }
                } ?: ""
            }.thenBy { it.startTime }
        )
    }

    val groupedExams = remember(filteredSessionExams) {
        filteredSessionExams.groupBy { it.dateLesson?.trim() ?: "Без даты" }
    }

    val filteredLessons = remember(lessons, selectedSubgroup, selectedDayOfWeek, currentWeek, selectedDayIndex, currentTimeMillis) {
        val nowCal = java.util.Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val nowMinutes = nowCal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + nowCal.get(java.util.Calendar.MINUTE)
        val nowSeconds = nowCal.get(java.util.Calendar.SECOND)
        val nowTotalSeconds = nowMinutes * 60 + nowSeconds
        val isTodaySelected = (selectedDayIndex == 7)

        lessons.filter { 
            (it.subgroup == 0 || it.subgroup == selectedSubgroup || selectedSubgroup == 0) &&
            it.dayOfWeek == selectedDayOfWeek &&
            (it.weeks.isEmpty() || it.weeks.contains(currentWeek))
        }.map { lesson ->
            if (isTodaySelected) {
                try {
                    val sParts = lesson.startTime.split(":").map { it.trim().toInt() }
                    val eParts = lesson.endTime.split(":").map { it.trim().toInt() }
                    val sSec = (sParts[0] * 60 + sParts[1]) * 60
                    val eSec = (eParts[0] * 60 + eParts[1]) * 60
                    val durSec = (eSec - sSec).coerceAtLeast(60)
                    if (nowTotalSeconds in sSec until eSec) {
                        val prog = ((nowTotalSeconds - sSec).toFloat() / durSec.toFloat()).coerceIn(0f, 1f)
                        lesson.copy(isActive = true, progress = prog)
                    } else {
                        lesson.copy(isActive = false, progress = null)
                    }
                } catch (e: Exception) {
                    lesson.copy(isActive = false, progress = null)
                }
            } else {
                lesson.copy(isActive = false, progress = null)
            }
        }
    }
    var selectedLessonForSheet by remember { mutableStateOf<Lesson?>(null) }
    var noteLessonToAdd by remember { mutableStateOf<Lesson?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val (tiltX, tiltY) = rememberDeviceTilt(onShake = {
        selectedDayIndex = 7
        AppHapticManager.playClick()
    })
    val notesPrefs = remember { context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE) }
    var scheduleNotesVersion by remember { mutableIntStateOf(0) }
    val allScheduleNotes = remember(notesPrefs.getString("notes_data", ""), scheduleNotesVersion, selectedDayIndex) {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
            com.google.gson.Gson().fromJson<List<Note>>(notesPrefs.getString("notes_data", "[]"), type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
    val subjectsWithNotes = remember(allScheduleNotes) {
        allScheduleNotes.map { it.subject }.toSet()
    }
    val selectedDateFormatted = remember(selectedDayIndex) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7 + selectedDayIndex)
        "%02d.%02d.%04d".format(
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.YEAR)
        )
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

    androidx.activity.compose.BackHandler(enabled = isDialogOpen || inlineTeacherData != null) {
        if (selectedLessonForSheet != null) {
            selectedLessonForSheet = null
        } else if (noteLessonToAdd != null) {
            noteLessonToAdd = null
        } else if (showScheduleSettings) {
            showScheduleSettings = false
        } else if (inlineTeacherData != null) {
            inlineTeacherData = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().let { if (isDialogOpen) it.blur(16.dp) else it }) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
            .padding(end = if (isSessionMode || !isDateRailVisible) 16.dp else 46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (styleType == StyleType.Techno) {
                    rotationX = (-tiltY * 3.5f).coerceIn(-6f, 6f)
                    rotationY = (tiltX * 4.5f).coerceIn(-7f, 7f)
                    cameraDistance = 10f * density
                }
            },
        contentPadding = PaddingValues(start = 20.dp, end = 2.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val weekText = if (isSessionMode) "Сессия" else if (currentWeek == 0) "Каникулы" else "$currentWeek учебная неделя"
                    val weekTextTechno = if (isSessionMode) "> СЕССИЯ_" else if (currentWeek == 0) "> КАНИКУЛЫ_" else "> $currentWeek УЧЕБНАЯ НЕДЕЛЯ_"
                    Text(if (styleType == StyleType.Techno) weekTextTechno else weekText, fontSize = if (styleType == StyleType.Techno) 19.sp else 15.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary, letterSpacing = if (styleType == StyleType.Techno) 1.sp else 0.sp, fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val context2 = androidx.compose.ui.platform.LocalContext.current
                var expanded by remember { mutableStateOf(false) }
                var inputText by remember { mutableStateOf(selectedGroup) }
                var groupHistory by remember {
                    mutableStateOf(
                        prefs.getStringSet("group_history", emptySet())?.toList() ?: emptyList()
                    )
                }

                // Header row: group number or teacher name + back button
                if (inlineTeacherData != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.IconButton(
                            onClick = { inlineTeacherData = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowLeft,
                                contentDescription = "Назад к группе",
                                tint = MinAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = inlineTeacherData!!.second,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MinTextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                } else {
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
                                text = if (styleType == StyleType.Techno) "> ${effectiveTitle ?: selectedGroup}_" else (effectiveTitle ?: selectedGroup),
                                fontSize = 33.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MinTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                }

                // Expandable panel
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val fieldBg = if (isDarkTheme) Color(0xFF1C1D24) else Color(0xFFFFFFFF)
                        val fieldBorder = if (isDarkTheme) MinAccent.copy(alpha = 0.40f) else MinAccent.copy(alpha = 0.55f)
                        
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = if (isDarkTheme) 8.dp else 3.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    spotColor = MinAccent.copy(alpha = 0.25f),
                                    ambientColor = MinAccent.copy(alpha = 0.10f)
                                ),
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MinTextPrimary,
                                unfocusedTextColor = MinTextPrimary,
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg,
                                focusedBorderColor = MinAccent,
                                unfocusedBorderColor = fieldBorder,
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

                        val trimmedInput = inputText.trim()
                        val matchingGroups = remember(trimmedInput, allGroups) {
                            if (trimmedInput.isEmpty()) emptyList()
                            else allGroups.filter { it.name?.contains(trimmedInput, ignoreCase = true) == true }.take(5)
                        }

                        val matchingEmployees = remember(trimmedInput, allEmployees) {
                            if (trimmedInput.length < 2) emptyList()
                            else allEmployees.filter { 
                                it.fullName.contains(trimmedInput, ignoreCase = true) || 
                                (it.lastName != null && it.lastName.contains(trimmedInput, ignoreCase = true)) ||
                                (it.urlId != null && it.urlId.contains(trimmedInput, ignoreCase = true))
                            }.take(5)
                        }

                        // Autocomplete suggestions
                        if (matchingGroups.isNotEmpty() || matchingEmployees.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Подсказки",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MinAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            
                            matchingGroups.forEach { grp ->
                                val gName = grp.name ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            AppHapticManager.playClick()
                                            onGroupSelected(gName)
                                            val newHistory = (listOf(gName) + groupHistory).distinct().take(5)
                                            groupHistory = newHistory
                                            prefs.edit().putStringSet("group_history", newHistory.toSet()).apply()
                                            expanded = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = MinAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        gName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MinTextPrimary
                                    )
                                    if (!grp.facultyAbbrev.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "(${grp.facultyAbbrev} • ${grp.course ?: 1} курс)",
                                            fontSize = 12.sp,
                                            color = MinTextSecondary
                                        )
                                    }
                                }
                            }

                            matchingEmployees.forEach { emp ->
                                val eFullName = emp.fullName
                                val eUrlId = emp.urlId ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            AppHapticManager.playClick()
                                            inlineTeacherData = eUrlId to eFullName
                                            val newHistory = (listOf(eFullName) + groupHistory).distinct().take(5)
                                            groupHistory = newHistory
                                            prefs.edit().putStringSet("group_history", newHistory.toSet()).apply()
                                            expanded = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = MinAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            eFullName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MinTextPrimary
                                        )
                                        if (!emp.degree.isNullOrBlank()) {
                                            Text(
                                                emp.degree,
                                                fontSize = 11.sp,
                                                color = MinTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // History list
                        if (groupHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Недавние",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MinTextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            groupHistory.forEach { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            AppHapticManager.playClick()
                                            inputText = group
                                            onGroupSelected(group)
                                            expanded = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
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
                                        fontSize = 14.sp,
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

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "Все", 1 to "1 подгр.", 2 to "2 подгр.").forEach { (sg, label) ->
                        AppleOptionChip(
                            label = label,
                            isSelected = selectedSubgroup == sg,
                            onClick = { onSubgroupChange(sg) },
                            isDarkTheme = isDarkTheme,
                            accentColor = MinAccent,
                            unselectedBg = MinCardBg,
                            unselectedTextColor = MinTextPrimary,
                            selectedTextColor = MinCardBg,
                            borderColorOverride = MinBorder
                        )
                    }
                }
            }
        }

        if (isSessionMode) {
            if (sessionPeriod.first.isNotBlank() || sessionPeriod.second.isNotBlank()) {
                item {
                    val pStart = sessionPeriod.first.ifBlank { "?" }
                    val pEnd = sessionPeriod.second.ifBlank { "?" }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppleTokens.RadiusMd))
                            .background(if (isDarkTheme) AppleTokens.Tile1 else AppleTokens.CanvasWhite)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                tint = MinAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Период сессии: $pStart – $pEnd",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MinTextPrimary
                            )
                        }
                    }
                }
            }

            if (filteredSessionExams.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, start = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Экзамены не найдены",
                            color = if (styleType == StyleType.Techno) Color(0xFF00FF41) else MinTextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val statusText = if (sessionPeriod.first.isNotBlank() && sessionPeriod.second.isNotBlank()) {
                            "Сессия: с ${sessionPeriod.first} по ${sessionPeriod.second}\nРасписание ещё не опубликовано деканатом"
                        } else {
                            "Для выбранной группы нет назначенных экзаменов"
                        }
                        Text(
                            text = statusText,
                            color = if (styleType == StyleType.Techno) Color(0xFF00FF41).copy(alpha = 0.7f) else MinTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                groupedExams.forEach { (dateStr, examsForDay) ->
                    item {
                        val formattedHeader = remember(dateStr) { formatExamDateHeader(dateStr) }
                        Text(
                            text = formattedHeader,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MinTextPrimary,
                            modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    examsForDay.forEach { exam ->
                        item {
                            SessionExamCard(
                                exam = exam,
                                MinTextPrimary = MinTextPrimary,
                                MinTextSecondary = MinTextSecondary,
                                MinAccent = MinAccent,
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    AppHapticManager.playClick()
                                    selectedLessonForSheet = exam
                                },
                                onTeacherClick = { urlId, name -> inlineTeacherData = urlId to name }
                            )
                        }
                    }
                }
            }
        } else if (filteredLessons.isEmpty()) {
            item {
                val selectedCal = remember(selectedDayIndex) {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -7 + selectedDayIndex)
                    cal
                }
                val month = selectedCal.get(java.util.Calendar.MONTH)
                val dayOfMonth = selectedCal.get(java.util.Calendar.DAY_OF_MONTH)
                val isSummerVacation = (month == java.util.Calendar.JULY || month == java.util.Calendar.AUGUST)
                val isSunday = (selectedDayOfWeek == 7)

                val mainStatus = "Отличный день"
                val subStatus = "Пар не найдено"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        mainStatus,
                        color = if (styleType == StyleType.Techno) Color(0xFF00FF41) else MinTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        subStatus,
                        color = if (styleType == StyleType.Techno) Color(0xFF00FF41).copy(alpha = 0.7f) else MinTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            filteredLessons.forEachIndexed { idx, lesson ->
                item {
                    val parallaxDepth = 1f + (idx % 3) * 0.35f
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                if (styleType == StyleType.Techno) {
                                    rotationX = (-tiltY * 3.2f * parallaxDepth).coerceIn(-6f, 6f)
                                    rotationY = (tiltX * 4.2f * parallaxDepth).coerceIn(-7f, 7f)
                                    cameraDistance = 12f * density
                                    translationX = tiltX * 6.dp.toPx() * parallaxDepth
                                    translationY = tiltY * 4.5.dp.toPx() * parallaxDepth
                                }
                            }
                    ) {
                        val matchedNotePreview = remember(allScheduleNotes, lesson.title, selectedDateFormatted) {
                            val forSubject = allScheduleNotes.filter { it.subject.equals(lesson.title, ignoreCase = true) }
                            val exactDay = forSubject.firstOrNull { it.date == selectedDateFormatted }
                            val pinned = forSubject.firstOrNull { it.isPinned }
                            (exactDay ?: pinned)?.text
                        }
                        MinLessonCard(
                            lesson = lesson,
                            MinTextPrimary = MinTextPrimary,
                            MinTextSecondary = MinTextSecondary,
                            MinBorder = MinBorder,
                            MinAccent = MinAccent,
                            MinCardBg = MinCardBg,
                            hasNotes = subjectsWithNotes.contains(lesson.title),
                            notePreview = matchedNotePreview,
                            isDarkTheme = isDarkTheme,
                            currentTimeMillis = currentTimeMillis,
                            onClick = {
                                AppHapticManager.playClick()
                                selectedLessonForSheet = lesson
                            },
                            onLongClick = { noteLessonToAdd = lesson },
                            onTeacherClick = { urlId, name -> inlineTeacherData = urlId to name }
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
        val targetLesson = noteLessonToAdd!!
        var noteText by remember(targetLesson) { mutableStateOf("") }
        var isPinnedQuick by remember(targetLesson) { mutableStateOf(false) }
        val selectedDayCal = remember(selectedDayIndex) {
            java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_MONTH, -7 + selectedDayIndex)
            }
        }
        val currentDayStr = String.format("%02d.%02d.%04d", selectedDayCal.get(java.util.Calendar.DAY_OF_MONTH), selectedDayCal.get(java.util.Calendar.MONTH) + 1, selectedDayCal.get(java.util.Calendar.YEAR))
        val nextLessonTarget = remember(targetLesson, effectiveGroup, selectedSubgroup, selectedDayCal) {
            findNextLessonForSubject(
                context = context,
                group = effectiveGroup,
                subject = targetLesson.title,
                subgroup = selectedSubgroup,
                baseCalendar = selectedDayCal,
                afterTime = targetLesson.startTime.ifBlank { null }
            )
        }
        var targetDateStr by remember(targetLesson, currentDayStr) {
            mutableStateOf(currentDayStr)
        }
        var targetTimeStr by remember(targetLesson) {
            mutableStateOf(targetLesson.startTime.ifBlank { "09:00" })
        }

        Dialog(onDismissRequest = { noteLessonToAdd = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MinCardBg)
                    .border(1.dp, MinBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Заметка • ${targetLesson.title}",
                        color = MinTextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { noteLessonToAdd = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Закрыть", tint = MinTextSecondary)
                    }
                }

                // Quick target date selector: Next Pair vs Selected Day
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (nextLessonTarget != null) {
                        val isNextSel = targetDateStr == nextLessonTarget.dateFormatted
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isNextSel) MinAccent.copy(alpha = 0.16f) else MinTextPrimary.copy(alpha = 0.06f))
                                .border(1.dp, if (isNextSel) MinAccent else MinBorder, CircleShape)
                                .clickable {
                                    AppHapticManager.playClick()
                                    targetDateStr = nextLessonTarget.dateFormatted
                                    targetTimeStr = nextLessonTarget.timeFormatted
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = if (isNextSel) MinAccent else MinTextSecondary, modifier = Modifier.size(15.dp))
                            Text(
                                "К след. паре (${nextLessonTarget.dateFormatted.take(5)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isNextSel) MinAccent else MinTextPrimary
                            )
                        }
                    }

                    val isCurrentDaySel = targetDateStr == currentDayStr
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isCurrentDaySel) MinAccent.copy(alpha = 0.16f) else MinTextPrimary.copy(alpha = 0.06f))
                            .border(1.dp, if (isCurrentDaySel) MinAccent else MinBorder, CircleShape)
                            .clickable {
                                AppHapticManager.playClick()
                                targetDateStr = currentDayStr
                                targetTimeStr = targetLesson.startTime.ifBlank { "09:00" }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = if (isCurrentDaySel) MinAccent else MinTextSecondary, modifier = Modifier.size(14.dp))
                        Text(
                            "Этот день (${currentDayStr.take(5)})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrentDaySel) MinAccent else MinTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isPinnedQuick) Color(0xFFFF9500).copy(alpha = 0.16f) else MinTextPrimary.copy(alpha = 0.06f))
                            .border(1.dp, if (isPinnedQuick) Color(0xFFFF9500) else MinBorder, CircleShape)
                            .clickable {
                                AppHapticManager.playClick()
                                isPinnedQuick = !isPinnedQuick
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Outlined.PushPin, contentDescription = null, tint = if (isPinnedQuick) Color(0xFFFF9500) else MinTextSecondary, modifier = Modifier.size(14.dp))
                        Text(
                            "Закрепить",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPinnedQuick) Color(0xFFFF9500) else MinTextPrimary
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Что нужно сделать к паре...", color = MinTextSecondary, fontSize = 15.sp) },
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 16.sp, color = MinTextPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinAccent,
                        unfocusedBorderColor = MinBorder,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = MinAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MinTextPrimary.copy(alpha = 0.08f))
                            .clickable { noteLessonToAdd = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Отмена", color = MinTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (noteText.isNotBlank()) MinAccent else MinTextPrimary.copy(alpha = 0.08f))
                            .clickable(enabled = noteText.isNotBlank()) {
                                AppHapticManager.playClick()
                                val resolvedType = when {
                                    targetLesson.lessonType.contains("ЛР", true) -> "ЛР"
                                    targetLesson.lessonType.contains("ПЗ", true) -> "ПЗ"
                                    targetLesson.lessonType.contains("ЛК", true) -> "ЛК"
                                    else -> targetLesson.lessonType.take(4).trim()
                                }
                                val newNote = Note(
                                    subject = targetLesson.title,
                                    text = noteText.trim(),
                                    date = targetDateStr,
                                    time = targetTimeStr,
                                    isPinned = isPinnedQuick,
                                    lessonType = resolvedType
                                )
                                val currentNotesRaw = notesPrefs.getString("notes_data", "[]") ?: "[]"
                                val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
                                val currentNotes: List<Note> = com.google.gson.Gson().fromJson(currentNotesRaw, type) ?: emptyList()
                                val updatedNotes = currentNotes + newNote
                                notesPrefs.edit().putString("notes_data", com.google.gson.Gson().toJson(updatedNotes)).apply()
                                scheduleNotesVersion++
                                noteLessonToAdd = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Сохранить",
                            color = if (noteText.isNotBlank()) MinCardBg else MinTextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (selectedLessonForSheet != null) {
        val lesson = selectedLessonForSheet!!
        var displayedGroups by remember(lesson) {
            mutableStateOf(lesson.studentGroups)
        }
        LaunchedEffect(lesson) {
            if (lesson.teacherUrlId.isNotBlank()) {
                val resolved = BsuirApi.resolveAllGroupsForLesson(
                    teacherUrlId = lesson.teacherUrlId,
                    dayOfWeekInt = lesson.dayOfWeek,
                    startTime = lesson.startTime,
                    subjectTitle = lesson.title,
                    lessonType = lesson.lessonType,
                    fallbackGroups = lesson.studentGroups,
                    currentGroup = selectedGroup,
                    dateLesson = lesson.dateLesson
                )
                if (resolved.isNotEmpty() && resolved != displayedGroups) {
                    displayedGroups = resolved
                }
            }
        }

        AlertDialog(
            onDismissRequest = { selectedLessonForSheet = null },
            title = {
                val fullTitle = lesson.subjectFullName.ifEmpty { lesson.title }
                Text(
                    text = fullTitle,
                    fontSize = if (fullTitle.length > 35) 21.sp else if (fullTitle.length > 20) 24.sp else 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 28.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (lesson.lessonType.isNotBlank()) {
                        Text("Тип занятия", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                        Text(lesson.lessonType, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (lesson.teacherFullName.isNotBlank() || lesson.teacherName.isNotBlank()) {
                        Text("Преподаватель", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                        val tName = lesson.teacherFullName.ifBlank { lesson.teacherName }
                        Text(
                            tName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lesson.teacherUrlId.isNotBlank()) MinAccent else androidx.compose.ui.graphics.Color.White,
                            textAlign = TextAlign.Center,
                            modifier = if (lesson.teacherUrlId.isNotBlank()) Modifier.clickable {
                                inlineTeacherData = lesson.teacherUrlId to tName
                                selectedLessonForSheet = null
                            } else Modifier
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (lesson.auditory.isNotBlank()) {
                        Text("Аудитория", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                        Text("ауд. ${lesson.auditory}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text("Время и подгруппа", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                    Text("${lesson.startTime} — ${lesson.endTime} • ${if (lesson.subgroup == 0) "Вся группа" else "${lesson.subgroup} подгруппа"}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (lesson.isActive) {
                        val sheetTimeBreakdown = remember(currentTimeMillis, lesson.startTime, lesson.endTime) {
                            try {
                                val nowCal = java.util.Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
                                val nowSec = nowCal.get(java.util.Calendar.HOUR_OF_DAY) * 3600 +
                                             nowCal.get(java.util.Calendar.MINUTE) * 60 +
                                             nowCal.get(java.util.Calendar.SECOND)

                                val sParts = lesson.startTime.split(":").map { it.trim().toInt() }
                                val eParts = lesson.endTime.split(":").map { it.trim().toInt() }
                                val startSec = sParts[0] * 3600 + sParts[1] * 60
                                val endSec = eParts[0] * 3600 + eParts[1] * 60

                                val totalSec = (endSec - startSec).coerceAtLeast(60)
                                val elapsedSec = (nowSec - startSec).coerceIn(0, totalSec)
                                val remainingSec = (endSec - nowSec).coerceAtLeast(0)
                                val remMinutes = (remainingSec + 59) / 60

                                val isStandardPair = totalSec >= 70 * 60
                                val halfDurationSec = if (isStandardPair) 40 * 60 else (totalSec - 5 * 60) / 2
                                val breakDurationSec = 5 * 60
                                val breakStartSec = halfDurationSec
                                val breakEndSec = breakStartSec + breakDurationSec

                                val isBreak: Boolean
                                val statusText: String

                                if (isStandardPair) {
                                    if (elapsedSec < breakStartSec) {
                                        val toBreakSec = breakStartSec - elapsedSec
                                        val toBreakMin = (toBreakSec + 59) / 60
                                        isBreak = false
                                        statusText = if (toBreakSec < 60) {
                                            "До конца пары: $remMinutes мин (перерыв через $toBreakSec сек)"
                                        } else {
                                            "До конца пары: $remMinutes мин (перерыв через $toBreakMin мин)"
                                        }
                                    } else if (elapsedSec < breakEndSec) {
                                        val breakRemSec = breakEndSec - elapsedSec
                                        val bMin = breakRemSec / 60
                                        val bSec = breakRemSec % 60
                                        val bStr = if (bMin > 0) "$bMin мин $bSec сек" else "$bSec сек"
                                        isBreak = true
                                        statusText = "Перерыв $bStr · До конца пары: $remMinutes мин"
                                    } else {
                                        isBreak = false
                                        statusText = if (remMinutes <= 3) {
                                            val rSec = remainingSec % 60
                                            "2-я часть • До конца пары: $remMinutes мин ($rSec сек)"
                                        } else {
                                            "2-я часть • До конца пары: $remMinutes мин"
                                        }
                                    }
                                } else {
                                    isBreak = false
                                    statusText = "До конца пары: $remMinutes мин"
                                }

                                val calculatedProg = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                                Triple(statusText, isBreak, calculatedProg)
                            } catch (e: Exception) {
                                val rem = ((1f - (lesson.progress ?: 0f)) * 85).toInt().coerceAtLeast(1)
                                Triple("До конца пары: $rem мин", false, lesson.progress ?: 0f)
                            }
                        }

                        val (stText, isBrk, actProg) = sheetTimeBreakdown
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBrk) androidx.compose.ui.graphics.Color(0xFFFF9500) else MinAccent,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    val barBg = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f)
                                    val barColor = if (isBrk) androidx.compose.ui.graphics.Color(0xFFFF9500) else MinAccent
                                    drawRoundRect(color = barBg, size = size, cornerRadius = cornerRadius)
                                    val activeWidth = size.width * actProg.coerceIn(0f, 1f)
                                    if (activeWidth > 0f) {
                                        drawRoundRect(color = barColor, size = androidx.compose.ui.geometry.Size(activeWidth, size.height), cornerRadius = cornerRadius)
                                    }
                                    val notchX = size.width * (40f / 85f)
                                    drawRoundRect(
                                        color = androidx.compose.ui.graphics.Color(0xFF16171D),
                                        topLeft = androidx.compose.ui.geometry.Offset(notchX, 0f),
                                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (displayedGroups.isNotEmpty()) {
                        Text("Группы на занятии", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            displayedGroups.forEach { grpName ->
                                val isCurrent = grpName == selectedGroup
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isCurrent) MinAccent else MinAccent.copy(alpha = 0.16f))
                                        .clickable {
                                            onGroupSelected(grpName)
                                            selectedLessonForSheet = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = grpName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Bold,
                                        color = if (isCurrent) androidx.compose.ui.graphics.Color.White else MinAccent
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (lesson.dateLesson != null) {
                        Text("Дата", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                        Text(formatExamDateHeader(lesson.dateLesson), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!lesson.note.isNullOrBlank()) {
                            Text("Примечание", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                            Text(
                                text = lesson.note,
                                fontSize = 15.sp,
                                color = MinAccent,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    } else {
                        Text("Учебные недели", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                        Text(lesson.weeks.joinToString(", "), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center)
                        
                        val upcomingDates = remember(lesson) {
                            val dates = mutableListOf<Pair<Int, String>>()
                            val cal = java.util.Calendar.getInstance()
                            for (offset in 1..75) {
                                val info = getBsuirWeekForCalendarDay(7 + offset)
                                val dow = info.first
                                val w = info.second
                                if (dow == lesson.dayOfWeek && (lesson.weeks.isEmpty() || lesson.weeks.contains(w))) {
                                    val itemCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, offset) }
                                    val dateStr = String.format("%02d.%02d", itemCal.get(java.util.Calendar.DAY_OF_MONTH), itemCal.get(java.util.Calendar.MONTH) + 1)
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

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MinAccent)
                                .clickable {
                                    AppHapticManager.playClick()
                                    val target = lesson
                                    selectedLessonForSheet = null
                                    noteLessonToAdd = target
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+ Добавить заметку к паре",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MinCardBg
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    }

    // -------------------------------------------------------------
    // Ergonomic Side Vertical Date Slider (Apple Floating Date Rail)
    // -------------------------------------------------------------
    if (!isSessionMode) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val railWidthPx = with(density) { 80.dp.toPx() }
        val railOffsetPx by animateFloatAsState(
            targetValue = if (isDateRailVisible) 0f else railWidthPx,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
            label = "dateRailOffset"
        )

        // 1. Sliding Date Rail Container (swipes right to hide)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .offset { androidx.compose.ui.unit.IntOffset(x = kotlin.math.round(railOffsetPx).toInt(), y = 0) }
                .padding(top = 16.dp, bottom = 40.dp, end = 2.dp)
                .pointerInput(isDateRailVisible) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            var totalX = 0f
                            var totalY = 0f
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val dx = change.position.x - change.previousPosition.x
                                val dy = change.position.y - change.previousPosition.y
                                totalX += dx
                                totalY += dy
                                if (totalX > 25f && kotlin.math.abs(totalX) > kotlin.math.abs(totalY) && isDateRailVisible) {
                                    change.consume()
                                    AppHapticManager.playClick()
                                    isDateRailVisible = false
                                    schedulePrefs.edit().putBoolean("date_rail_visible", false).apply()
                                    break
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            AppleVerticalDateRail(
                days = days,
                weekDays = weekDays,
                selectedDayIndex = selectedDayIndex,
                onDaySelected = { index ->
                    selectedDayIndex = index
                    AppHapticManager.playClick()
                },
                isDarkTheme = isDarkTheme,
                selectedColor = MinAccent,
                todayIndex = 7
            )
        }

        // 2. Edge return trigger zone & subtle Apple handle when hidden (swipes left or taps to return)
        AnimatedVisibility(
            visible = !isDateRailVisible,
            enter = androidx.compose.animation.fadeIn(tween(200)),
            exit = androidx.compose.animation.fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(isDateRailVisible) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                var totalX = 0f
                                var totalY = 0f
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val dx = change.position.x - change.previousPosition.x
                                    val dy = change.position.y - change.previousPosition.y
                                    totalX += dx
                                    totalY += dy
                                    if (totalX < -25f && kotlin.math.abs(totalX) > kotlin.math.abs(totalY) && !isDateRailVisible) {
                                        change.consume()
                                        AppHapticManager.playClick()
                                        isDateRailVisible = true
                                        schedulePrefs.edit().putBoolean("date_rail_visible", true).apply()
                                        break
                                    }
                                }
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            AppHapticManager.playClick()
                            isDateRailVisible = true
                            schedulePrefs.edit().putBoolean("date_rail_visible", true).apply()
                        }
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Subtle Apple edge pill handle
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(width = 4.dp, height = 48.dp)
                        .clip(RoundedCornerShape(AppleTokens.RadiusPill))
                        .background(
                            if (isDarkTheme) Color.White.copy(alpha = 0.35f)
                            else Color.Black.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }

    val hasSession = isSessionMode || isSessionPeriodActive(sessionPeriod.first, sessionPeriod.second, sessionExams)
    if (hasSession) {
        val fabInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val fabPressed by fabInteraction.collectIsPressedAsState()
        val fabScale by animateFloatAsState(
            targetValue = if (fabPressed) 0.94f else 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "sessionFabScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 28.dp, end = 20.dp)
                .scale(fabScale)
                .shadow(
                    elevation = if (isDarkTheme) 14.dp else 8.dp,
                    shape = RoundedCornerShape(AppleTokens.RadiusPill),
                    spotColor = MinAccent.copy(alpha = 0.45f),
                    ambientColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(AppleTokens.RadiusPill))
                .background(if (isSessionMode) (if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.CanvasWhite) else MinAccent)
                .clickable(
                    interactionSource = fabInteraction,
                    indication = null,
                    onClick = {
                        AppHapticManager.playClick()
                        isSessionMode = !isSessionMode
                    }
                )
                .padding(horizontal = 18.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isSessionMode) Icons.Outlined.CalendarToday else Icons.Outlined.School,
                    contentDescription = if (isSessionMode) "Расписание" else "Сессия",
                    tint = if (isSessionMode) MinAccent else Color.White,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = if (isSessionMode) "Расписание" else "Сессия",
                    style = AppleTypography.bodyStrong().copy(fontSize = 14.5.sp),
                    color = if (isSessionMode) MinTextPrimary else Color.White
                )
            }
        }
    }
    
    if (showScheduleSettings) {
        val transition = rememberInfiniteTransition(label = "glassSettingsShimmer")
        val shimmerAlpha by transition.animateFloat(
            initialValue = 0.20f,
            targetValue = 0.50f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "settingsShimmerAlpha"
        )

        AlertDialog(
            onDismissRequest = { showScheduleSettings = false },
            title = {
                Text(
                    "Настройки расписания",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Подгруппа", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    val subgroupOptions = listOf("Все", "1 подгр.", "2 подгр.")
                    val targetSubgroup = selectedSubgroup.coerceIn(0, 2)
                    val animatedSubgroup by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = targetSubgroup.toFloat(),
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.52f,
                            stiffness = 200f
                        ),
                        label = "subgroupDroplet"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        val itemHeightDp = 54.dp
                        val itemSpacingDp = 10.dp
                        val stepDp = itemHeightDp + itemSpacingDp
                        val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { stepDp.toPx() }

                        // 1. Sliding Outline Box
                        if (styleType != StyleType.Techno) {
                            val velocity = (targetSubgroup - animatedSubgroup)
                            val absVel = kotlin.math.abs(velocity).coerceIn(0f, 2.5f)

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationY = animatedSubgroup * stepPx
                                        scaleY = 1f + 0.30f * absVel
                                        scaleX = 1f / kotlin.math.sqrt(1f + 0.30f * absVel)
                                    }
                                    .fillMaxWidth()
                                    .height(itemHeightDp)
                                    .background(Color.Transparent, RoundedCornerShape(16.dp))
                                    .border(2.dp, MinAccent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            )
                        }

                        // 2. Subgroup options track
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(itemSpacingDp)
                        ) {
                            subgroupOptions.forEachIndexed { index, title ->
                                val isSelected = selectedSubgroup == index
                                val targetTextColor = when {
                                    styleType == StyleType.Techno -> if (isSelected) Color(0xFF00FF41) else Color(0xFF00FF41).copy(alpha = 0.7f)
                                    isSelected -> Color.White
                                    else -> Color.White.copy(alpha = 0.6f)
                                }
                                val textColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(250))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(itemHeightDp)
                                        .clip(RoundedCornerShape(if (styleType == StyleType.Techno) 6.dp else 16.dp))
                                        .then(
                                            if (styleType == StyleType.Techno) {
                                                Modifier
                                            } else {
                                                Modifier.background(Color.Transparent)
                                            }
                                        )
                                        .drawWithContent {
                                            if (styleType == StyleType.Techno) {
                                                val r = 6.dp.toPx()
                                                val cornerR = androidx.compose.ui.geometry.CornerRadius(r, r)
                                                if (isSelected) {
                                                    drawRoundRect(
                                                        color = Color(0xFF00FF41).copy(alpha = 0.18f),
                                                        cornerRadius = cornerR
                                                    )
                                                    drawRoundRect(
                                                        color = Color(0xFF00FF41),
                                                        cornerRadius = cornerR,
                                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                                    )
                                                } else {
                                                    drawRoundRect(
                                                        color = Color(0xFF00FF41).copy(alpha = 0.3f),
                                                        cornerRadius = cornerR,
                                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                                    )
                                                }
                                            }
                                            drawContent()
                                        }
                                        .clickable {
                                            AppHapticManager.playClick()
                                            onSubgroupChange(index)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        title,
                                        fontSize = 22.sp,
                                        color = textColor,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Bold,
                                        fontFamily = if (styleType == StyleType.Techno) vt323FontFamily else null
                                    )
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
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun MinLessonCard(
    lesson: Lesson,
    MinTextPrimary: Color,
    MinTextSecondary: Color,
    MinBorder: Color,
    MinAccent: Color,
    MinCardBg: Color,
    hasNotes: Boolean = false,
    notePreview: String? = null,
    isDarkTheme: Boolean = true,
    currentTimeMillis: Long = System.currentTimeMillis(),
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onTeacherClick: (String, String) -> Unit = { _, _ -> }
) {
    val styleType = LocalStyleType.current
    val isTechno = styleType == StyleType.Techno
    val monoFont = if (isTechno) vt323FontFamily else null
    val technoAccent = Color(0xFF00FF41)

    val cardBg = if (isTechno) Color(0xFF0A0A0A) else if (isDarkTheme) AppleTokens.Tile1 else AppleTokens.CanvasWhite
    val cardBorder = if (isTechno) Color(0xFF00FF41).copy(alpha = 0.3f) else if (isDarkTheme) AppleTokens.HairlineDark else AppleTokens.HairlineLight
    val cardShape = RoundedCornerShape(if (isTechno) 6.dp else AppleTokens.RadiusLg)

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "lessonCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(cardShape)
            .background(Color.Transparent)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    AppHapticManager.playClick()
                    onClick()
                },
                onLongClick = {
                    AppHapticManager.playClick()
                    onLongClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Time Column
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = lesson.startTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTechno) technoAccent else if (lesson.isActive) MinAccent else MinTextPrimary,
                    fontFamily = monoFont,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lesson.endTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isTechno) technoAccent.copy(alpha = 0.6f) else MinTextSecondary,
                    fontFamily = monoFont,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Subtle Pill / Indicator for Lesson Type
            val lessonTypeBadge = when {
                lesson.lessonType.contains("ЛК", ignoreCase = true) -> "ЛК"
                lesson.lessonType.contains("ПЗ", ignoreCase = true) -> "ПЗ"
                lesson.lessonType.contains("ЛР", ignoreCase = true) -> "ЛР"
                lesson.lessonType.contains("Экзамен", ignoreCase = true) -> "ЭКЗ"
                else -> lesson.lessonType.take(3)
            }
            if (lessonTypeBadge.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (lesson.isActive) MinAccent
                            else MinCardBg
                        )
                        .border(1.dp, if (lesson.isActive) Color.Transparent else MinBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lessonTypeBadge,
                        style = AppleTypography.finePrint().copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (lesson.isActive) MinCardBg else MinTextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 3. Central Column: Title, Teacher & Progress
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                val displayTitle = if (lesson.subgroup != 0) "${lesson.title} (${lesson.subgroup} п.)" else lesson.title
                Text(
                    text = displayTitle,
                    style = AppleTypography.bodyStrong().copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isTechno) technoAccent else MinTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                val teacherDisplayName = remember(lesson.teacherName, lesson.teacherFullName) {
                    val src = if (lesson.teacherFullName.isNotBlank()) lesson.teacherFullName else lesson.teacherName
                    val parts = src.split(" ").filter { it.isNotBlank() }
                    if (parts.size >= 3) {
                        val fInit = parts[1].firstOrNull()?.let { "$it." } ?: ""
                        val mInit = parts[2].firstOrNull()?.let { "$it." } ?: ""
                        "${parts[0]} $fInit $mInit".trim()
                    } else if (parts.size == 2) {
                        val fInit = parts[1].firstOrNull()?.let { "$it." } ?: ""
                        "${parts[0]} $fInit".trim()
                    } else {
                        src
                    }
                }

                val line2Text = teacherDisplayName

                if (line2Text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = line2Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isTechno) technoAccent.copy(alpha = 0.8f) else MinTextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!notePreview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isTechno) technoAccent else MinAccent)
                        )
                        Text(
                            text = notePreview,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isTechno) technoAccent else MinAccent,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

            }

            // 4. Auditory chip (without "ауд.")
            val cleanAuditory = remember(lesson.auditory, lesson.title, lesson.lessonType) {
                val raw = lesson.auditory.trim()
                when {
                    raw.isNotBlank() -> {
                        raw.replace(Regex("^(ауд\\.?\\s*|aud\\.?\\s*)", RegexOption.IGNORE_CASE), "").trim()
                    }
                    lesson.title.contains("Физ", ignoreCase = true) || lesson.lessonType.contains("Физ", ignoreCase = true) -> "спортзал"
                    else -> "—"
                }
            }
            if (cleanAuditory.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MinCardBg)
                        .border(1.dp, MinBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cleanAuditory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isTechno) technoAccent else MinTextPrimary,
                        fontFamily = monoFont,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

fun isSessionPeriodActive(startExamsDate: String?, endExamsDate: String?, exams: List<Lesson>): Boolean {
    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
    val now = java.util.Calendar.getInstance()
    now.set(java.util.Calendar.HOUR_OF_DAY, 0)
    now.set(java.util.Calendar.MINUTE, 0)
    now.set(java.util.Calendar.SECOND, 0)
    now.set(java.util.Calendar.MILLISECOND, 0)
    val todayMillis = now.timeInMillis

    var startMillis: Long? = null
    var endMillis: Long? = null

    if (!startExamsDate.isNullOrBlank()) {
        try {
            startMillis = sdf.parse(startExamsDate.trim())?.time
        } catch (_: Exception) {}
    }

    if (!endExamsDate.isNullOrBlank()) {
        try {
            endMillis = sdf.parse(endExamsDate.trim())?.time?.plus(24 * 60 * 60 * 1000L - 1)
        } catch (_: Exception) {}
    }

    if (startMillis == null && exams.isNotEmpty()) {
        val examDates = exams.mapNotNull { ex ->
            ex.dateLesson?.let {
                try { sdf.parse(it.trim())?.time } catch (_: Exception) { null }
            }
        }
        startMillis = examDates.minOrNull()
        endMillis = examDates.maxOrNull()?.plus(24 * 60 * 60 * 1000L - 1)
    }

    if (startMillis == null) return false

    return if (endMillis != null) {
        todayMillis in startMillis..endMillis
    } else {
        todayMillis >= startMillis
    }
}

fun formatExamDateHeader(dateStr: String): String {
    if (dateStr.isBlank() || dateStr == "Без даты") return "Без даты"
    return try {
        val inFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("ru"))
        val date = inFormat.parse(dateStr.trim())
        if (date != null) {
            val outFormat = java.text.SimpleDateFormat("d MMMM, EEEE", java.util.Locale("ru"))
            val res = outFormat.format(date)
            res.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("ru")) else it.toString() }
        } else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun SessionExamCard(
    exam: Lesson,
    MinTextPrimary: Color,
    MinTextSecondary: Color,
    MinAccent: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onTeacherClick: ((String, String) -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(AppleTokens.RadiusLg)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "examCardScale"
    )

    val isExamType = exam.lessonType.contains("Экзамен", ignoreCase = true)
    val isConsultation = exam.lessonType.contains("Консульт", ignoreCase = true)
    val isCredit = exam.lessonType.contains("Зачет", ignoreCase = true) || exam.lessonType.contains("Дифф", ignoreCase = true)

    val badgeColor = when {
        isExamType -> if (isDarkTheme) Color(0xFFFF453A).copy(alpha = 0.20f) else Color(0xFFFF3B30).copy(alpha = 0.12f)
        isConsultation -> if (isDarkTheme) Color(0xFF0A84FF).copy(alpha = 0.20f) else Color(0xFF007AFF).copy(alpha = 0.12f)
        isCredit -> if (isDarkTheme) Color(0xFFFF9F0A).copy(alpha = 0.20f) else Color(0xFFFF9500).copy(alpha = 0.12f)
        else -> if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.SurfacePearl
    }
    val badgeTextColor = when {
        isExamType -> if (isDarkTheme) Color(0xFFFF6961) else Color(0xFFD70015)
        isConsultation -> if (isDarkTheme) Color(0xFF64D2FF) else Color(0xFF0066CC)
        isCredit -> if (isDarkTheme) Color(0xFFFFB340) else Color(0xFFC97800)
        else -> if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(cardShape)
            .background(Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    AppHapticManager.playClick()
                    onClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Time Column
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = exam.startTime.ifBlank { "—" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MinTextPrimary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                if (exam.endTime.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exam.endTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MinTextSecondary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 2. Type badge pill
            val badgeLabel = when {
                isExamType -> "Экзамен"
                isConsultation -> "Консультация"
                isCredit -> if (exam.lessonType.contains("Дифф", ignoreCase = true)) "Дифф. зачет" else "Зачет"
                exam.lessonType.isNotBlank() -> exam.lessonType
                else -> "Экзамен"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppleTokens.RadiusPill))
                    .background(badgeColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeLabel,
                    style = AppleTypography.finePrint().copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = badgeTextColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 3. Subject and Teacher
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val displayTitle = if (exam.subgroup != 0) "${exam.title} (${exam.subgroup} п.)" else exam.title
                Text(
                    text = displayTitle,
                    style = AppleTypography.bodyStrong().copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MinTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                if (exam.teacherName.isNotBlank() || exam.teacherFullName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val teacherDisplay = exam.teacherName.ifBlank { exam.teacherFullName }
                    Text(
                        text = teacherDisplay,
                        style = AppleTypography.subheadline().copy(fontSize = 13.sp),
                        color = MinTextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            enabled = onTeacherClick != null && exam.teacherUrlId.isNotBlank(),
                            onClick = {
                                if (onTeacherClick != null && exam.teacherUrlId.isNotBlank()) {
                                    AppHapticManager.playClick()
                                    onTeacherClick(exam.teacherUrlId, exam.teacherFullName.ifBlank { exam.teacherName })
                                }
                            }
                        )
                    )
                }

                if (!exam.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exam.note,
                        fontSize = 11.5.sp,
                        color = MinAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Auditory chip
            val cleanAuditory = remember(exam.auditory, exam.title, exam.lessonType) {
                val raw = exam.auditory.trim()
                when {
                    raw.isNotBlank() -> raw.replace(Regex("^(ауд\\.?\\s*|aud\\.?\\s*)", RegexOption.IGNORE_CASE), "").trim()
                    exam.title.contains("Физ", ignoreCase = true) || exam.lessonType.contains("Физ", ignoreCase = true) -> "спортзал"
                    else -> "—"
                }
            }
            if (cleanAuditory.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppleTokens.RadiusSm))
                        .background(if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.SurfacePearl)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cleanAuditory,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MinTextPrimary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

data class SubjectMarks(val name: String, val marks: List<Int>)

data class NoteAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: String,
    val name: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0L
)

data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subject: String,
    val text: String,
    val date: String,
    val time: String? = null,
    val isEvent: Boolean = false,
    val isPinned: Boolean = false,
    val lessonType: String = "",
    val attachments: List<NoteAttachment> = emptyList(),
    val calendarEventId: Long? = null
)

data class AcademicMarkInfo(
    val mark: Int,
    val taskNumber: Int? = null,
    val lessonType: String? = null,
    val dateString: String? = null,
    val controlPoint: String? = null
)

data class AcademicSubjectProgress(
    val id: Int? = null,
    val name: String,
    val abbrev: String = name,
    val totalLabs: Int = 0,
    val submittedLabs: Int = 0,
    val marks: List<AcademicMarkInfo> = emptyList(),
    val averageMark: Double? = null,
    val hasLabs: Boolean = totalLabs > 0
)

@Composable
fun AppleSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    isDarkTheme: Boolean = false,
    accentColor: Color? = null,
    trackColor: Color? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val trackBg = trackColor ?: if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
    val thumbBg = if (isDarkTheme) Color(0xFF2C2C2E) else Color.White
    val textActive = accentColor ?: if (isDarkTheme) Color.White else AppleTokens.Ink
    val textInactive = if (isDarkTheme) Color.White.copy(alpha = 0.55f) else AppleTokens.InkMuted48

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(trackBg)
            .padding(2.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(if (isSelected) thumbBg else Color.Transparent)
                        .then(
                            if (isSelected) {
                                androidx.compose.ui.Modifier.shadow(
                                    elevation = 2.dp,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                    spotColor = Color.Black.copy(alpha = 0.12f),
                                    ambientColor = Color.Black.copy(alpha = 0.05f)
                                )
                            } else androidx.compose.ui.Modifier
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                AppHapticManager.playClick()
                                onSelectIndex(index)
                            }
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = title,
                        style = AppleTypography.subheadline().copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        color = if (isSelected) textActive else textInactive,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun MinMarksScreen(
    MinBg: androidx.compose.ui.graphics.Color,
    MinCardBg: androidx.compose.ui.graphics.Color,
    MinBorder: androidx.compose.ui.graphics.Color,
    MinTextPrimary: androidx.compose.ui.graphics.Color,
    MinTextSecondary: androidx.compose.ui.graphics.Color,
    currentAccent: androidx.compose.ui.graphics.Color,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var subjectsList by remember { mutableStateOf<List<AcademicSubjectProgress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var studentRating by remember { mutableStateOf<Double?>(null) }
    var studentRatingPosition by remember { mutableStateOf<Int?>(null) }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Все, 1: Лабораторные, 2: С оценками
    var searchQuery by remember { mutableStateOf("") }
    var isAuthorized by remember { mutableStateOf(false) }
    var currentGroup by remember { mutableStateOf("") }

    androidx.activity.compose.BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()

    val loadData: (Boolean) -> Unit = remember(context) {
        { isUserRefresh ->
            coroutineScope.launch {
                if (isUserRefresh) isRefreshing = true else isLoading = true
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        val token = prefs.getString("auth_token", "") ?: ""
                        val loginGroup = prefs.getString("login_group", "").takeIf { !it.isNullOrBlank() }
                            ?: prefs.getString("selectedGroup", "310101") ?: "310101"

                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isAuthorized = token.isNotBlank()
                            currentGroup = loginGroup
                        }

                        // Version check for cache invalidation when lesson-type breakdown and taskCount are updated
                        val cachedVersion = prefs.getInt("academic_progress_version", 1)
                        if (cachedVersion < 4) {
                            prefs.edit().remove("cached_academic_progress").putInt("academic_progress_version", 4).apply()
                        }

                        // 1. Try to load cached academic progress only if authorized
                        if (token.isNotBlank()) {
                            val cachedJson = prefs.getString("cached_academic_progress", null)
                            if (cachedJson != null && subjectsList.isEmpty()) {
                                try {
                                    val type = object : com.google.gson.reflect.TypeToken<List<AcademicSubjectProgress>>() {}.type
                                    val cached: List<AcademicSubjectProgress>? = com.google.gson.Gson().fromJson(cachedJson, type)
                                    if (!cached.isNullOrEmpty()) {
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            subjectsList = cached
                                            isLoading = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        // Helper to extract student rating
                        val extractRatingFromJson: (String) -> Unit = { bodyStr ->
                            try {
                                val trimmed = bodyStr.trim()
                                if (trimmed.startsWith("{")) {
                                    val root = org.json.JSONObject(trimmed)
                                    if (root.has("rating")) {
                                        val r = root.optDouble("rating")
                                        if (r > 0.0) studentRating = r
                                    }
                                    if (root.has("place") || root.has("position")) {
                                        val p = root.optInt("place", root.optInt("position", 0))
                                        if (p > 0) studentRatingPosition = p
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                        if (token.isNotBlank()) {
                            listOfNotNull(
                                prefs.getString("cached_gradebook", null),
                                prefs.getString("cached_profile", null)
                            ).forEach { extractRatingFromJson(it) }
                        }

                        var fetchedFromApi = false

                        // 2. Fetch from BSUIR IIS personal-rating endpoint if token is present
                        if (token.isNotBlank()) {
                            try {
                                val ratingResp = com.example.schedule.BsuirApi.getPersonalRating(token)
                                if (ratingResp?.subjects != null && ratingResp.subjects.isNotEmpty()) {
                                    val deadlinesMap = ratingResp.deadlines?.associate { (it.termHoursId ?: -1) to (it.taskCount ?: it.labCount ?: 0) } ?: emptyMap()
                                    val parsed = ratingResp.subjects.map { subj ->
                                        val sName = subj.name ?: subj.abbrev ?: ""
                                        val sAbbrev = subj.abbrev ?: subj.name ?: ""

                                        val labTypes = subj.lessonTypes?.filter {
                                            (it.id ?: 0) == 4 || it.abbrev?.contains("ЛР", ignoreCase = true) == true || it.abbrev?.contains("лаб", ignoreCase = true) == true
                                        } ?: emptyList()

                                        var totalLabs = 0
                                        for (lt in labTypes) {
                                            val dlCount = deadlinesMap[lt.termHoursId] ?: 0
                                            if (dlCount > totalLabs) totalLabs = dlCount
                                        }
                                        if (totalLabs == 0 && labTypes.isNotEmpty()) {
                                            val allTaskNumbers = labTypes.flatMap { lt ->
                                                lt.lessons?.flatMap { l -> (l.marks?.mapNotNull { it.taskNumber } ?: emptyList()) } ?: emptyList()
                                            }
                                            totalLabs = if (allTaskNumbers.isNotEmpty()) allTaskNumbers.maxOrNull() ?: 4 else {
                                                labTypes.sumOf { it.lessons?.size ?: 0 }.coerceIn(2, 8)
                                            }
                                        }

                                        val submittedTasks = mutableSetOf<Int>()
                                        var genericSubmitted = 0
                                        for (lt in labTypes) {
                                            lt.lessons?.forEach { l ->
                                                val validMarks = l.marks?.filter { (it.mark ?: 0) > 0 } ?: emptyList()
                                                if (validMarks.isNotEmpty()) {
                                                    validMarks.forEach { m ->
                                                        if ((m.taskNumber ?: 0) > 0) submittedTasks.add(m.taskNumber!!)
                                                        else genericSubmitted++
                                                    }
                                                }
                                            }
                                        }
                                        val submittedLabs = (if (submittedTasks.isNotEmpty()) submittedTasks.size else genericSubmitted).coerceAtMost(if (totalLabs > 0) totalLabs else 99)

                                        val marksList = mutableListOf<AcademicMarkInfo>()
                                        subj.lessonTypes?.forEach { lt ->
                                            val rawAbbrev = lt.abbrev?.trim() ?: ""
                                            val ltId = lt.id ?: 0
                                            val resolvedType = when {
                                                ltId == 4 || rawAbbrev.contains("ЛР", ignoreCase = true) || rawAbbrev.contains("лаб", ignoreCase = true) -> "ЛР"
                                                ltId == 2 || rawAbbrev.contains("ПЗ", ignoreCase = true) || rawAbbrev.contains("практ", ignoreCase = true) -> "ПЗ"
                                                ltId == 1 || rawAbbrev.contains("ЛК", ignoreCase = true) || rawAbbrev.contains("лек", ignoreCase = true) -> "ЛК"
                                                ltId == 3 || rawAbbrev.contains("СЕМ", ignoreCase = true) || rawAbbrev.contains("сем", ignoreCase = true) -> "СЕМ"
                                                rawAbbrev.contains("КП", ignoreCase = true) || rawAbbrev.contains("КР", ignoreCase = true) -> "КП"
                                                rawAbbrev.isNotBlank() -> rawAbbrev
                                                else -> "ПЗ"
                                            }

                                            lt.lessons?.forEach { l ->
                                                l.marks?.forEach { m ->
                                                    val mVal = m.mark ?: 0
                                                    if (mVal > 0) {
                                                        val finalType = if ((m.taskNumber ?: 0) > 0) "ЛР" else resolvedType
                                                        marksList.add(
                                                            AcademicMarkInfo(
                                                                mark = mVal,
                                                                taskNumber = m.taskNumber,
                                                                lessonType = finalType,
                                                                dateString = l.dateString,
                                                                controlPoint = l.controlPoint
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        val avg = if (marksList.isNotEmpty()) marksList.map { it.mark }.average() else null

                                        AcademicSubjectProgress(
                                            id = subj.id,
                                            name = sName,
                                            abbrev = sAbbrev,
                                            totalLabs = totalLabs,
                                            submittedLabs = submittedLabs,
                                            marks = marksList,
                                            averageMark = avg
                                        )
                                    }

                                    if (parsed.isNotEmpty()) {
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            subjectsList = parsed.sortedBy { it.name }
                                        }
                                        prefs.edit().putString("cached_academic_progress", com.google.gson.Gson().toJson(parsed)).apply()
                                        fetchedFromApi = true
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // Also try markbook for rating / GPA
                            try {
                                val markbookJson = com.example.schedule.BsuirApi.getMarkBook(token)
                                if (!markbookJson.isNullOrBlank()) {
                                    extractRatingFromJson(markbookJson)
                                    val mObj = org.json.JSONObject(markbookJson)
                                    if (mObj.has("averageMark")) {
                                        val mbGpa = mObj.optDouble("averageMark")
                                        if (mbGpa > 0.0 && studentRating == null) {
                                            studentRating = mbGpa
                                        }
                                    }
                                }
                            } catch (e: Exception) {}

                            // Also try group rating
                            if (studentRatingPosition == null && loginGroup.isNotBlank()) {
                                for (ratingUrl in listOf(
                                    "https://iis.bsuir.by/api/v1/rating/last?studentGroup=$loginGroup",
                                    "https://iis.bsuir.by/api/v1/rating/group?studentGroup=$loginGroup"
                                )) {
                                    try {
                                        val rReq = com.example.schedule.NetworkClient.buildGetRequest(ratingUrl, token)
                                        com.example.schedule.NetworkClient.client.newCall(rReq).execute().use { rResp ->
                                            val rBody = rResp.body?.string()
                                            if (rResp.isSuccessful && !rBody.isNullOrBlank()) {
                                                extractRatingFromJson(rBody)
                                            }
                                        }
                                    } catch (e: Exception) {}
                                    if (studentRatingPosition != null) break
                                }
                            }
                        }

                        // 3. Fallback: If not fetched from API and subjectsList is still empty, load from group schedule
                        if (!fetchedFromApi && subjectsList.isEmpty() && loginGroup.isNotBlank()) {
                            try {
                                val schedResp = if (loginGroup.any { it.isLetter() }) {
                                    com.example.schedule.BsuirApi.getEmployeeSchedule(loginGroup)
                                } else {
                                    com.example.schedule.BsuirApi.getGroupSchedule(loginGroup)
                                }

                                val scheduleSubjects = mutableMapOf<String, Boolean>()
                                val schedules = (if (!schedResp?.schedules.isNullOrEmpty()) schedResp?.schedules else schedResp?.nextSchedules) ?: emptyMap()
                                schedules.forEach { (_, dayLessons) ->
                                    dayLessons.forEach { l ->
                                        val sName = l.subject?.takeIf { it.isNotBlank() } ?: l.subjectFullName ?: ""
                                        if (sName.isNotBlank()) {
                                            val isLab = l.lessonTypeAbbrev?.contains("ЛР", ignoreCase = true) == true
                                            val existingHasLab = scheduleSubjects[sName] ?: false
                                            scheduleSubjects[sName] = existingHasLab || isLab
                                        }
                                    }
                                }
                                schedResp?.exams?.forEach { exam ->
                                    val sName = exam.subject?.takeIf { it.isNotBlank() } ?: exam.subjectFullName ?: ""
                                    if (sName.isNotBlank() && !scheduleSubjects.containsKey(sName)) {
                                        scheduleSubjects[sName] = false
                                    }
                                }

                                if (scheduleSubjects.isNotEmpty()) {
                                    val fallbackList = scheduleSubjects.map { (name, hasLabs) ->
                                        AcademicSubjectProgress(
                                            name = name,
                                            abbrev = name,
                                            totalLabs = if (hasLabs) 6 else 0,
                                            submittedLabs = 0,
                                            marks = emptyList(),
                                            averageMark = null
                                        )
                                    }.sortedBy { it.name }

                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        subjectsList = fallbackList
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isLoading = false
                            isRefreshing = false
                        }
                    } catch (e: Exception) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isLoading = false
                            isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData(false)
    }

    val overallGpa = remember(subjectsList) {
        val graded = subjectsList.mapNotNull { it.averageMark }
        if (graded.isNotEmpty()) graded.average() else 0.0
    }
    val totalAssignedLabs = remember(subjectsList) {
        subjectsList.sumOf { it.totalLabs }
    }
    val totalSubmittedLabs = remember(subjectsList) {
        subjectsList.sumOf { it.submittedLabs }
    }
    val labsCompletionPercent = remember(totalAssignedLabs, totalSubmittedLabs) {
        if (totalAssignedLabs > 0) ((totalSubmittedLabs.toFloat() / totalAssignedLabs.toFloat()) * 100f).toInt() else 0
    }
    val gradedSubjectsCount = remember(subjectsList) {
        subjectsList.count { it.marks.isNotEmpty() }
    }
    val labSubjectsCount = remember(subjectsList) {
        subjectsList.count { it.hasLabs }
    }

    val filteredSubjects = remember(subjectsList, searchQuery) {
        if (searchQuery.isBlank()) subjectsList else {
            subjectsList.filter { subj ->
                subj.name.contains(searchQuery, ignoreCase = true) || subj.abbrev.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val appleBg = MinBg
    val appleTextPrimary = MinTextPrimary
    val appleTextSecondary = MinTextSecondary
    val appleAccent = currentAccent

    val refreshRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "refreshAnim"
    )

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(appleBg)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 28.dp,
                bottom = 48.dp
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header with Title & Refresh Action
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Успеваемость",
                            style = AppleTypography.heroDisplayLarge().copy(
                                fontSize = 32.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = appleTextPrimary
                        )
                    }

                    // Refresh Button
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                AppHapticManager.playClick()
                                loadData(true)
                            },
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.Refresh,
                            contentDescription = "Обновить",
                            tint = appleAccent,
                            modifier = androidx.compose.ui.Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = if (isRefreshing) refreshRotation else 0f }
                        )
                    }
                }
            }

            // 2. Hero KPI Cards: Overall GPA + Laboratory Progress
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Средний балл
                    AppleCard(
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        isDarkTheme = isDarkTheme,
                        cardBg = Color.Transparent,
                        cardBorder = Color.Transparent,
                        hasProductShadow = false
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Analytics,
                                contentDescription = null,
                                tint = appleTextSecondary,
                                modifier = androidx.compose.ui.Modifier.size(16.dp)
                            )
                            androidx.compose.material3.Text(
                                text = "Средний балл",
                                style = AppleTypography.finePrint().copy(
                                    fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                ),
                                color = appleTextSecondary
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                        androidx.compose.material3.Text(
                            text = if (overallGpa > 0.0) String.format(java.util.Locale.US, "%.2f", overallGpa) else "—",
                            style = AppleTypography.heroDisplayLarge().copy(
                                fontSize = 32.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            color = appleTextPrimary
                        )

                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .background(appleAccent.copy(alpha = if (isDarkTheme) 0.16f else 0.10f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val badgeLabel = when {
                                overallGpa >= 8.0 -> "Отлично"
                                overallGpa >= 6.5 -> "Хорошо"
                                overallGpa >= 5.0 -> "Удовлетвор."
                                else -> "Нет оценок"
                            }
                            androidx.compose.material3.Text(
                                text = if (gradedSubjectsCount > 0) "$badgeLabel · $gradedSubjectsCount предм." else badgeLabel,
                                style = AppleTypography.finePrint().copy(
                                    fontSize = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                ),
                                color = appleAccent
                            )
                        }
                    }

                    // Card 2: Лабораторные работы
                    AppleCard(
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        isDarkTheme = isDarkTheme,
                        cardBg = Color.Transparent,
                        cardBorder = Color.Transparent,
                        hasProductShadow = false
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = appleTextSecondary,
                                modifier = androidx.compose.ui.Modifier.size(16.dp)
                            )
                            androidx.compose.material3.Text(
                                text = "Лабораторные",
                                style = AppleTypography.finePrint().copy(
                                    fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                ),
                                color = appleTextSecondary
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.Bottom
                        ) {
                            androidx.compose.material3.Text(
                                text = "$totalSubmittedLabs",
                                style = AppleTypography.heroDisplayLarge().copy(
                                    fontSize = 32.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                ),
                                color = appleTextPrimary
                            )
                            androidx.compose.material3.Text(
                                text = " / $totalAssignedLabs",
                                style = AppleTypography.callout().copy(
                                    fontSize = 17.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                ),
                                color = appleTextSecondary,
                                modifier = androidx.compose.ui.Modifier.padding(bottom = 3.dp, start = 3.dp)
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(10.dp))

                        // Apple capsule progress bar (themed accent)
                        val labProgressFraction = if (totalAssignedLabs > 0) {
                            (totalSubmittedLabs.toFloat() / totalAssignedLabs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                .background(if (isDarkTheme) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f))
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxWidth(fraction = labProgressFraction)
                                    .fillMaxHeight()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                    .background(appleAccent)
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                        androidx.compose.material3.Text(
                            text = if (labProgressFraction >= 1f) "Все сданы (100%)" else "$labsCompletionPercent% сдано",
                            style = AppleTypography.footnote().copy(
                                fontSize = 11.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            color = appleAccent
                        )
                    }
                }
            }

            // 3. Authorization banner (if offline/guest)
            if (!isAuthorized) {
                item {
                    AppleCard(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        isDarkTheme = isDarkTheme,
                        cardBg = Color.Transparent,
                        cardBorder = Color.Transparent,
                        hasProductShadow = false
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.Top,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(appleAccent.copy(alpha = 0.15f)),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Lock,
                                    contentDescription = "Авторизация",
                                    tint = appleAccent,
                                    modifier = androidx.compose.ui.Modifier.size(18.dp)
                                )
                            }
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier.weight(1f)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Синхронизация с ИИС БГУИР",
                                    style = AppleTypography.subheadline().copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                    color = appleTextPrimary
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(2.dp))
                                androidx.compose.material3.Text(
                                    text = "Войдите в профиле в свой аккаунт ИИС БГУИР, чтобы загрузить персональные отметки и точное количество сданных лабораторных.",
                                    style = AppleTypography.footnote().copy(fontSize = 12.5.sp),
                                    color = appleTextSecondary
                                )
                            }
                        }
                    }
                }
            }



            // 5. Search Bar (if list is not empty)
            if (subjectsList.size > 3) {
                item {
                    AppleSearchInput(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Поиск",
                        isDarkTheme = isDarkTheme,
                        inputBg = MinCardBg,
                        inputBorder = MinBorder,
                        inputTextColor = appleTextPrimary,
                        inputIconColor = appleTextSecondary,
                        cursorColor = appleAccent
                    )
                }
            }

            // 6. Loading Indicator
            if (isLoading && subjectsList.isEmpty()) {
                item {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = appleAccent,
                            modifier = androidx.compose.ui.Modifier.size(36.dp)
                        )
                    }
                }
            } else if (filteredSubjects.isEmpty()) {
                item {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = if (searchQuery.isNotBlank()) "Ничего не найдено" else "Нет предметов",
                                style = AppleTypography.subheadline().copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                color = appleTextPrimary
                            )
                            androidx.compose.material3.Text(
                                text = if (searchQuery.isNotBlank()) "Попробуйте изменить поисковый запрос" else "Данные по предметам пока отсутствуют",
                                style = AppleTypography.footnote(),
                                color = appleTextSecondary
                            )
                        }
                    }
                }
            } else {
                // 7. Subject Cards List
                items(filteredSubjects.size) { idx ->
                    val subj = filteredSubjects[idx]
                    AppleCard(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        isDarkTheme = isDarkTheme,
                        cardBg = Color.Transparent,
                        cardBorder = Color.Transparent,
                        hasProductShadow = false
                    ) {
                        // Top row: Subject Title & Average Mark Pill
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            val shortName = subj.abbrev.takeIf { it.isNotBlank() } ?: subj.name
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier.weight(1f)
                            ) {
                                androidx.compose.material3.Text(
                                    text = shortName,
                                    style = AppleTypography.subheadline().copy(
                                        fontSize = 17.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    ),
                                    color = appleTextPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))

                            // Average pill
                            if (subj.averageMark != null) {
                                val avgVal = subj.averageMark
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.Text(
                                        text = String.format(java.util.Locale.US, "%.1f", avgVal),
                                        style = AppleTypography.subheadline().copy(
                                            fontSize = 14.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        ),
                                        color = appleAccent
                                    )
                                }
                            } else {
                                androidx.compose.material3.Text(
                                    text = "—",
                                    style = AppleTypography.subheadline().copy(
                                        fontSize = 14.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                    ),
                                    color = appleTextSecondary.copy(alpha = 0.45f)
                                )
                            }
                        }

                        // Middle row: Lab Progress Scale (if subject has labs)
                        if (subj.hasLabs) {
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(10.dp))
                            androidx.compose.foundation.layout.Row(
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Лабораторные: ${subj.submittedLabs} из ${subj.totalLabs} сдано",
                                    style = AppleTypography.footnote().copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    ),
                                    color = appleTextSecondary
                                )

                                val labFrac = (subj.submittedLabs.toFloat() / subj.totalLabs.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                                val labPercent = (labFrac * 100f).toInt()
                                androidx.compose.material3.Text(
                                    text = "$labPercent%",
                                    style = AppleTypography.finePrint().copy(
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    ),
                                    color = appleAccent
                                )
                            }

                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))

                            // Apple capsule progress bar (themed accent)
                            val subFrac = (subj.submittedLabs.toFloat() / subj.totalLabs.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                    .background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .fillMaxWidth(fraction = subFrac)
                                        .fillMaxHeight()
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                        .background(appleAccent)
                                )
                            }
                        }

                        // Bottom row: Marks divided by lesson types (ЛР, ПЗ, ЛК)
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                        if (subj.marks.isNotEmpty()) {
                            val marksByLessonType = remember(subj.marks) {
                                val groups = subj.marks.groupBy { mark ->
                                    val raw = mark.lessonType?.trim()?.uppercase() ?: ""
                                    when {
                                        raw.contains("ЛР") || (mark.taskNumber != null && mark.taskNumber > 0) -> "ЛР"
                                        raw.contains("ПЗ") || raw.contains("ПРАКТ") -> "ПЗ"
                                        raw.contains("ЛК") || raw.contains("ЛЕК") -> "ЛК"
                                        raw.contains("СЕМ") -> "СЕМ"
                                        raw.contains("КП") || raw.contains("КР") -> "КП"
                                        raw.isNotBlank() -> raw
                                        else -> "ПЗ"
                                    }
                                }
                                val order = listOf("ЛР", "ПЗ", "ЛК", "СЕМ", "КП")
                                groups.entries.sortedBy { entry ->
                                    val idx = order.indexOf(entry.key)
                                    if (idx >= 0) idx else 99
                                }
                            }

                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                            ) {
                                marksByLessonType.forEach { (typeKey, marksInGroup) ->
                                    androidx.compose.foundation.layout.Row(
                                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        // Category Pill Badge (no background, no border)
                                        androidx.compose.foundation.layout.Box(
                                            modifier = androidx.compose.ui.Modifier
                                                .width(36.dp)
                                                .height(26.dp),
                                            contentAlignment = androidx.compose.ui.Alignment.CenterStart
                                        ) {
                                            androidx.compose.material3.Text(
                                                text = typeKey,
                                                style = AppleTypography.finePrint().copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                ),
                                                color = appleAccent,
                                                maxLines = 1
                                            )
                                        }

                                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))

                                        // Horizontal scroll of marks for this specific lesson type
                                        androidx.compose.foundation.layout.Row(
                                            modifier = androidx.compose.ui.Modifier
                                                .weight(1f)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            marksInGroup.forEach { markInfo ->
                                                val mVal = markInfo.mark

                                                androidx.compose.foundation.layout.Box(
                                                    modifier = androidx.compose.ui.Modifier
                                                        .size(32.dp),
                                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                                ) {
                                                    androidx.compose.material3.Text(
                                                        text = mVal.toString(),
                                                        style = AppleTypography.subheadline().copy(
                                                            fontSize = 16.sp,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                        ),
                                                        color = appleTextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            androidx.compose.material3.Text(
                                text = "Нет текущих отметок",
                                style = AppleTypography.finePrint().copy(
                                    fontSize = 12.5.sp
                                ),
                                color = appleTextSecondary.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
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

                val request = com.example.schedule.NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/omissions", token)
                var omissionsLoaded = false
                try {
                    com.example.schedule.NetworkClient.executeWithAuth(request, context).use { response ->
                        val body = response.body?.string()
                        val nowCal = java.util.Calendar.getInstance()
                        val curMonthNum = nowCal.get(java.util.Calendar.MONTH) + 1 // 1..12
                        val curYearNum = nowCal.get(java.util.Calendar.YEAR)
                        val monthNamesAll = arrayOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")

                        if (response.isSuccessful && !body.isNullOrBlank()) {
                            try {
                                val jsonArray = if (body.trim().startsWith("[")) {
                                    org.json.JSONArray(body)
                                } else if (body.trim().startsWith("{")) {
                                    val obj = org.json.JSONObject(body)
                                    obj.optJSONArray("omissions") ?: obj.optJSONArray("items") ?: org.json.JSONArray()
                                } else {
                                    org.json.JSONArray()
                                }

                                var tHours = 0
                                var uHoursCurrentMonth = 0
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
                                        val dateString = lesson.optString("dateString", lesson.optString("date", "")).takeIf { it != "null" } ?: ""
                                        val subject = lesson.optString("nameLesson", lesson.optString("subject", lesson.optString("lessonName", "Учебное занятие"))).takeIf { it != "null" } ?: "Учебное занятие"
                                        val lessonType = lesson.optString("lessonType", lesson.optString("type", "ПЗ")).takeIf { it != "null" } ?: "ПЗ"
                                        val note = lesson.optString("note", lesson.optString("reason", lesson.optString("comment", ""))).takeIf { it != "null" } ?: ""
                                        val teacher = lesson.optString("teacher", lesson.optString("employee", "")).takeIf { it != "null" } ?: ""

                                        var mName = "Другое"
                                        var lessonMonthIndex: Int? = null
                                        if (dateString.length >= 5) {
                                            val parts = dateString.split(".", "-")
                                            lessonMonthIndex = if (parts.size >= 2) {
                                                if (parts[0].length == 4) parts[1].toIntOrNull() else parts[1].toIntOrNull()
                                            } else null
                                            if (lessonMonthIndex != null && lessonMonthIndex in 1..12) {
                                                mName = monthNamesAll[lessonMonthIndex - 1]
                                            }
                                        }

                                        tHours += omissions
                                        if (!isRespectful && (lessonMonthIndex == null || lessonMonthIndex == curMonthNum)) {
                                            uHoursCurrentMonth += omissions
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

                                // Extract excuse documents from respectful omissions
                                val respectfulList = parsedLessons.filter { it.isRespectful }
                                val docsMap = mutableMapOf<String, MutableList<RealOmissionLesson>>()
                                for (rl in respectfulList) {
                                    val key = if (rl.note.isNotBlank()) rl.note else "Справка / Заявление в деканат"
                                    docsMap.getOrPut(key) { mutableListOf() }.add(rl)
                                }

                                val parsedDocs = docsMap.map { (key, list) ->
                                    val docHours = list.sumOf { it.hours }
                                    val docDates = list.map { it.dateString }.filter { it.isNotBlank() }.distinct().take(4)
                                    val docSubjects = list.map { it.subject }.filter { it.isNotBlank() }.distinct().take(3)
                                    RealExcuseDocument(
                                        title = if (key.contains("справ", ignoreCase = true) || key.contains("095", ignoreCase = true)) "Медицинская справка" else if (key.contains("заявл", ignoreCase = true)) "Заявление деканата" else "Оправдательный документ",
                                        note = key,
                                        hoursClosed = docHours,
                                        dates = docDates,
                                        subjects = docSubjects
                                    )
                                }.toMutableList()

                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    prefs.edit().putInt("cached_unexcused_hours", uHoursCurrentMonth).apply()
                                    totalHours = tHours
                                    unexcusedHours = uHoursCurrentMonth
                                    allLessons = parsedLessons
                                    if (parsedDocs.isNotEmpty()) {
                                        excuseDocuments = (parsedDocs + excuseDocuments).distinctBy { it.title + it.note }
                                    }
                                    statusText = ""
                                    isLoading = false
                                    isNotAuthorized = false
                                }
                                omissionsLoaded = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else if (response.code in listOf(401, 403)) {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isNotAuthorized = true
                                statusText = "Сессия ИИС БГУИР устарела. Пожалуйста, перезайдите в профиле."
                                isLoading = false
                            }
                            return@withContext
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Additionally fetch /api/v1/omissions-by-student (real medical certificates & excuse records)
                try {
                    val certReq = com.example.schedule.NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/omissions-by-student", token)
                    com.example.schedule.NetworkClient.executeWithAuth(certReq, context).use { certResp ->
                        val certBody = certResp.body?.string()
                        if (certResp.isSuccessful && !certBody.isNullOrBlank()) {
                            val extraDocs = mutableListOf<RealExcuseDocument>()
                            val certJson = if (certBody.trim().startsWith("{")) org.json.JSONObject(certBody) else org.json.JSONObject()
                            val dtoList = certJson.optJSONArray("omissionDtoList") 
                                ?: certJson.optJSONArray("omissions")
                                ?: if (certBody.trim().startsWith("[")) org.json.JSONArray(certBody) else null

                            if (dtoList != null && dtoList.length() > 0) {
                                for (k in 0 until dtoList.length()) {
                                    val item = dtoList.optJSONObject(k) ?: continue
                                    val name = item.optString("name", item.optString("title", "Справка")).takeIf { it != "null" && it.isNotBlank() } ?: "Справка"
                                    val note = item.optString("note", item.optString("reason", "")).takeIf { it != "null" } ?: ""
                                    val dFrom = (item.optString("dateFrom", item.optString("dateStart", "")).takeIf { it != "null" } ?: "").take(10)
                                    val dTo = (item.optString("dateTo", item.optString("dateEnd", "")).takeIf { it != "null" } ?: "").take(10)
                                    val term = item.optString("term", item.optString("semester", "")).takeIf { it != "null" } ?: ""
                                    val hrs = item.optInt("omissionCount", item.optInt("hours", item.optInt("hoursClosed", 0)))
                                    val rangeStr = if (dFrom.isNotBlank() && dTo.isNotBlank()) "С $dFrom по $dTo" else dFrom
                                    extraDocs.add(
                                        RealExcuseDocument(
                                            title = name,
                                            note = listOf(rangeStr, note, if (term.isNotBlank()) "$term семестр" else "").filter { it.isNotBlank() }.joinToString(" • "),
                                            hoursClosed = hrs,
                                            dates = listOfNotNull(dFrom.takeIf { it.isNotBlank() }, dTo.takeIf { it.isNotBlank() }),
                                            subjects = emptyList()
                                        )
                                    )
                                }
                            }
                            if (extraDocs.isNotEmpty()) {
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    excuseDocuments = (extraDocs + excuseDocuments).distinctBy { it.title + it.note }
                                    isNotAuthorized = false
                                    isLoading = false
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                // Also fetch /api/v1/certificate to see any active or issued certificates from BSUIR
                try {
                    val userCertsReq = com.example.schedule.NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/certificate", token)
                    com.example.schedule.NetworkClient.executeWithAuth(userCertsReq, context).use { uResp ->
                        val uBody = uResp.body?.string()
                        if (uResp.isSuccessful && !uBody.isNullOrBlank() && uBody.trim().startsWith("[")) {
                            val uArr = org.json.JSONArray(uBody.trim())
                            val uDocs = mutableListOf<RealExcuseDocument>()
                            for (k in 0 until uArr.length()) {
                                val item = uArr.optJSONObject(k) ?: continue
                                val certType = item.optString("certificateType", "Справка об обучении")
                                val certPlace = item.optString("placeOfSubmission", item.optString("place", ""))
                                val issueDate = item.optString("dateIssue", item.optString("dateOrder", "")).take(10)
                                val status = item.optString("status", "")
                                val noteParts = listOfNotNull(
                                    issueDate.takeIf { it.isNotBlank() }?.let { "Выдана $it" },
                                    certPlace.takeIf { it.isNotBlank() }?.let { "По месту: $it" },
                                    status.takeIf { it.isNotBlank() }
                                )
                                uDocs.add(
                                    RealExcuseDocument(
                                        title = if (certType.isNotBlank()) certType else "Справка студента",
                                        note = noteParts.joinToString(" • "),
                                        hoursClosed = 0,
                                        dates = listOfNotNull(issueDate.takeIf { it.isNotBlank() }),
                                        subjects = emptyList()
                                    )
                                )
                            }
                            if (uDocs.isNotEmpty()) {
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    excuseDocuments = (excuseDocuments + uDocs).distinctBy { it.title + it.note }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                // Additionally fetch /api/v1/disrespectful-omissions-by-student to ensure exact disrespectful hours
                try {
                    val nowCal = java.util.Calendar.getInstance()
                    val curMonthNum = nowCal.get(java.util.Calendar.MONTH) + 1
                    val curYearNum = nowCal.get(java.util.Calendar.YEAR)
                    val disReq = com.example.schedule.NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/disrespectful-omissions-by-student", token)
                    com.example.schedule.NetworkClient.executeWithAuth(disReq, context).use { disResp ->
                        val disBody = disResp.body?.string()
                        if (disResp.isSuccessful && !disBody.isNullOrBlank() && disBody.trim().startsWith("[")) {
                            val arr = org.json.JSONArray(disBody.trim())
                            var curMonthDisSum = 0
                            var allDisSum = 0
                            for (k in 0 until arr.length()) {
                                val obj = arr.optJSONObject(k) ?: continue
                                val hrs = obj.optInt("omissionCount", obj.optInt("hours", 0))
                                val mNum = obj.optInt("month", -1)
                                val dt = obj.optString("date", "").takeIf { it != "null" } ?: ""

                                if (hrs > 0) {
                                    allDisSum += hrs
                                }

                                val isThisMonth = when {
                                    mNum in 1..12 -> mNum == curMonthNum
                                    dt.length >= 7 -> {
                                        val parts = dt.split(".", "-")
                                        val parsedM = if (parts.size >= 2) parts[1].toIntOrNull() else null
                                        val parsedY = if (parts.size >= 3) (if (parts[0].length == 4) parts[0].toIntOrNull() else parts[2].toIntOrNull()) else curYearNum
                                        parsedM == curMonthNum && parsedY == curYearNum
                                    }
                                    else -> false
                                }
                                if (isThisMonth && hrs > 0) {
                                    curMonthDisSum += hrs
                                }
                            }

                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (curMonthDisSum > 0 || unexcusedHours == 0) {
                                    unexcusedHours = curMonthDisSum
                                    prefs.edit().putInt("cached_unexcused_hours", curMonthDisSum).apply()
                                }
                                if (totalHours == 0 && allDisSum > 0) {
                                    totalHours = allDisSum
                                } else if (totalHours < unexcusedHours) {
                                    totalHours = unexcusedHours
                                }
                                isNotAuthorized = false
                                isLoading = false
                            }
                        }
                    }
                } catch (_: Exception) {}

                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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
            // Stats Overview Cards (without background or border)
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    // Unexcused Card (Current month only)
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            "$unexcusedHours ч",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = currentAccent
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            "За этот месяц",
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
                            .padding(vertical = 8.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            "$excusedHours ч",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = currentAccent
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
                            .padding(vertical = 8.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            "$totalHours ч",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = currentAccent
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            "Всего за семестр",
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MinTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Real Excusing Documents Section ("Оправдательные документы")
            item {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Text(
                        "Оправдательные документы",
                        fontSize = 19.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MinTextPrimary
                    )

                    if (excuseDocuments.isEmpty() && totalHours > 0) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = androidx.compose.ui.Alignment.CenterStart
                        ) {
                            androidx.compose.material3.Text(
                                "Все пропуски на текущий момент без уважительной причины. Сдайте справку в деканат для списания часов.",
                                fontSize = 13.sp,
                                color = MinTextSecondary
                            )
                        }
                    } else if (excuseDocuments.isEmpty() && totalHours == 0) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = androidx.compose.ui.Alignment.CenterStart
                        ) {
                            androidx.compose.material3.Text(
                                "Отлично! У вас 0 пропусков за этот месяц.",
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF4CC38A) else androidx.compose.ui.graphics.Color(0xFF16A34A))
                            )
                        }
                    } else {
                        excuseDocuments.forEachIndexed { idx, doc ->
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF4CC38A) else androidx.compose.ui.graphics.Color(0xFF16A34A)),
                                        modifier = androidx.compose.ui.Modifier.size(22.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                        androidx.compose.material3.Text(
                                            doc.title,
                                            fontSize = 15.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = MinTextPrimary
                                        )
                                        androidx.compose.material3.Text(
                                            doc.note,
                                            fontSize = 12.sp,
                                            color = MinTextSecondary
                                        )
                                    }
                                    if (doc.hoursClosed > 0) {
                                        androidx.compose.material3.Text(
                                            "${doc.hoursClosed} ч списано",
                                            fontSize = 12.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF4CC38A) else androidx.compose.ui.graphics.Color(0xFF16A34A))
                                        )
                                    }
                                }

                                if (doc.subjects.isNotEmpty()) {
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                                    androidx.compose.material3.Text(
                                        "Предметы: " + doc.subjects.joinToString(", "),
                                        fontSize = 11.sp,
                                        color = MinTextSecondary.copy(alpha = 0.8f)
                                    )
                                }
                                if (idx < excuseDocuments.size - 1) {
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(10.dp))
                                    androidx.compose.material3.HorizontalDivider(color = MinBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                                }
                            }
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
                            .padding(vertical = 8.dp)
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
                                androidx.compose.material3.Text(
                                    lesson.lessonType,
                                    fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MinTextSecondary
                                )
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
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = if (lesson.isRespectful) (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF4CC38A) else androidx.compose.ui.graphics.Color(0xFF16A34A)) else currentAccent
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
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
                                color = if (lesson.isRespectful) (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF4CC38A) else androidx.compose.ui.graphics.Color(0xFF16A34A)) else currentAccent
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

data class GroupCuratorInfo(
    val fio: String,
    val position: String = "Куратор",
    val phone: String = "",
    val email: String = "",
    val urlId: String = ""
)

data class GroupStudentInfo(
    val fio: String,
    val position: Int,
    val role: String? = null,
    val averageMark: Double? = null,
    val rating: Double? = null,
    val isCurrentUser: Boolean = false
)

@Composable
fun MinGroupScreen(
    MinBg: androidx.compose.ui.graphics.Color,
    MinCardBg: androidx.compose.ui.graphics.Color,
    MinBorder: androidx.compose.ui.graphics.Color,
    MinTextPrimary: androidx.compose.ui.graphics.Color,
    MinTextSecondary: androidx.compose.ui.graphics.Color,
    currentAccent: androidx.compose.ui.graphics.Color = MinTextPrimary,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    var students by remember { mutableStateOf<List<GroupStudentInfo>>(emptyList()) }
    var curator by remember { mutableStateOf<GroupCuratorInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val styleType = LocalStyleType.current
    val isTechno = styleType == StyleType.Techno
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val userGroup = remember { prefs.getString("login_group", "")?.ifBlank { prefs.getString("selectedGroup", "") ?: "" } ?: "" }

    fun parseUserGroupInfo(jsonStr: String, currentUserFio: String): Pair<GroupCuratorInfo?, List<GroupStudentInfo>> {
        try {
            val root = org.json.JSONObject(jsonStr.trim())
            var cur: GroupCuratorInfo? = null
            val curObj = root.optJSONObject("studentGroupCuratorDto")
            if (curObj != null) {
                val cFio = curObj.optString("fio", "").trim()
                if (cFio.isNotBlank()) {
                    cur = GroupCuratorInfo(
                        fio = cFio,
                        position = curObj.optString("position", "Куратор").ifBlank { "Куратор" },
                        phone = curObj.optString("phone", "").trim(),
                        email = curObj.optString("email", "").trim(),
                        urlId = curObj.optString("urlId", "").trim()
                    )
                }
            }
            val studentsList = mutableListOf<GroupStudentInfo>()
            val arr = root.optJSONArray("groupInfoStudentDto")
            if (arr != null && arr.length() > 0) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val fio = item.optString("fio", "").trim()
                    if (fio.isBlank()) continue
                    val role = item.optString("position", "").trim()
                    val isCur = currentUserFio.isNotBlank() && (
                        fio.equals(currentUserFio, ignoreCase = true) ||
                        fio.contains(currentUserFio, ignoreCase = true) ||
                        currentUserFio.contains(fio, ignoreCase = true)
                    )
                    studentsList.add(
                        GroupStudentInfo(
                            fio = fio,
                            position = i + 1,
                            role = role.takeIf { it.isNotBlank() },
                            isCurrentUser = isCur
                        )
                    )
                }
            }
            return Pair(cur, studentsList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(null, emptyList())
    }

    fun extractStudentsFromAnySource(jsonStr: String, currentUserFio: String): List<GroupStudentInfo> {
        val list = mutableListOf<GroupStudentInfo>()
        val setOfFios = mutableSetOf<String>()
        val studentRatings = mutableMapOf<String, Pair<Double?, Double?>>()
        try {
            val trimmed = jsonStr.trim()
            val arraysToProcess = mutableListOf<org.json.JSONArray>()

            if (trimmed.startsWith("[")) {
                try { arraysToProcess.add(org.json.JSONArray(trimmed)) } catch (e: Exception) {}
            } else if (trimmed.startsWith("{")) {
                try {
                    val root = org.json.JSONObject(trimmed)
                    val keys = listOf("students", "rating", "items", "groupStudents", "grades", "list", "omissions", "studentGroups", "markPages")
                    for (k in keys) {
                        val arr = root.optJSONArray(k)
                        if (arr != null) arraysToProcess.add(arr)
                    }
                    val it = root.keys()
                    while (it.hasNext()) {
                        val k = it.next()
                        val arr = root.optJSONArray(k)
                        if (arr != null && !arraysToProcess.contains(arr)) {
                            arraysToProcess.add(arr)
                        }
                    }
                } catch (e: Exception) {}
            }

            for (arr in arraysToProcess) {
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    if (item is String && item.isNotBlank() && !item.all { it.isDigit() }) {
                        setOfFios.add(item.trim())
                    } else if (item is org.json.JSONObject) {
                        val st = item.optJSONObject("student")
                        var fio = item.optString("fio", "").ifBlank {
                            item.optString("fullName", "").ifBlank {
                                item.optString("studentFio", "").ifBlank {
                                    item.optString("name", "").ifBlank {
                                        st?.optString("fio", "") ?: st?.optString("fullName", "") ?: ""
                                    }
                                }
                            }
                        }
                        if (fio.isBlank()) {
                            val fn = st?.optString("firstName", "") ?: item.optString("firstName", "")
                            val ln = st?.optString("lastName", "") ?: item.optString("lastName", "")
                            val mn = st?.optString("middleName", "") ?: item.optString("middleName", "")
                            if (ln.isNotBlank()) fio = "$ln $fn $mn".trim()
                        }

                        if (fio.isNotBlank() && !fio.all { it.isDigit() }) {
                            setOfFios.add(fio.trim())
                            val avg = if (item.has("averageMark")) item.optDouble("averageMark") else if (st?.has("averageMark") == true) st.optDouble("averageMark") else null
                            val ratingVal = if (item.has("rating")) item.optDouble("rating") else if (st?.has("rating") == true) st.optDouble("rating") else null
                            if (avg != null || ratingVal != null) {
                                studentRatings[fio.trim()] = (avg to ratingVal)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var pos = 1
        for (fio in setOfFios.sorted()) {
            val ratings = studentRatings[fio]
            val isCur = currentUserFio.isNotBlank() && (fio.contains(currentUserFio, ignoreCase = true) || currentUserFio.contains(fio, ignoreCase = true))
            list.add(
                GroupStudentInfo(
                    fio = fio,
                    position = pos++,
                    averageMark = ratings?.first?.takeIf { it > 0.0 },
                    rating = ratings?.second?.takeIf { it > 0.0 },
                    isCurrentUser = isCur
                )
            )
        }
        return list
    }

    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun performFetch(forceReauth: Boolean = false) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            var token = prefs.getString("auth_token", "") ?: ""
            val currentFio = prefs.getString("login_fio", "")?.ifBlank { prefs.getString("user_fio", "") ?: "" } ?: ""
            val gradebookNum = prefs.getString("gradebook_number", "") ?: ""
            val savedPass = prefs.getString("saved_password", "") ?: ""

            // 1. Immediately check local cached sources
            if (students.isEmpty()) {
                val cachedUserGroup = prefs.getString("cached_user_group_info", null)
                if (!cachedUserGroup.isNullOrBlank()) {
                    val (cachedCur, cachedSts) = parseUserGroupInfo(cachedUserGroup, currentFio)
                    if (cachedSts.isNotEmpty() || cachedCur != null) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (cachedCur != null) curator = cachedCur
                            if (cachedSts.isNotEmpty()) students = cachedSts
                            isLoading = false
                        }
                    }
                }
                if (students.isEmpty()) {
                    val cachedGroup = prefs.getString("cached_group_students", null)
                    val cachedMarks = prefs.getString("cached_marks", null)
                    val cachedProfile = prefs.getString("cached_profile", null)

                    for (cachedJson in listOfNotNull(cachedGroup, cachedMarks, cachedProfile)) {
                        val parsedCache = extractStudentsFromAnySource(cachedJson, currentFio)
                        if (parsedCache.isNotEmpty()) {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                students = parsedCache
                                isLoading = false
                            }
                            break
                        }
                    }
                }
            }

            // Function to perform re-login if token expired or missing
            suspend fun doRelogin(): String? {
                if (gradebookNum.isBlank() || savedPass.isBlank()) return null
                try {
                    val loginJson = org.json.JSONObject().apply {
                        put("username", gradebookNum.trim())
                        put("password", savedPass)
                    }
                    val body = loginJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val req = okhttp3.Request.Builder()
                        .url("https://iis.bsuir.by/api/v1/auth/login")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .addHeader("Accept", "application/json, text/plain, */*")
                        .post(body)
                        .build()
                    com.example.schedule.NetworkClient.client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            var newToken = ""
                            val cookies = resp.headers("Set-Cookie")
                            for (cookie in cookies) {
                                if (cookie.contains("SESSION") || cookie.contains("JSESSIONID") || cookie.contains("jwt") || cookie.contains("token")) {
                                    newToken = cookie.substringBefore(";")
                                    break
                                }
                            }
                            if (newToken.isEmpty()) newToken = resp.body?.string() ?: ""
                            if (newToken.isNotEmpty()) {
                                prefs.edit().putString("auth_token", newToken).apply()
                                return newToken
                            }
                        }
                    }
                } catch (e: Exception) {}
                return null
            }

            if (forceReauth || token.isBlank()) {
                val fresh = doRelogin()
                if (fresh != null) token = fresh
            }

            var fetchedSuccessfully = false
            val urlsToTry = mutableListOf<String>()
            // Real official BSUIR endpoint returning curator + all 30 students:
            urlsToTry.add("https://iis.bsuir.by/api/v1/student-groups/user-group-info")
            if (userGroup.isNotBlank()) {
                urlsToTry.add("https://iis.bsuir.by/api/v1/student-groups/user-group-info?studentGroup=$userGroup")
            }
            urlsToTry.add("https://iis.bsuir.by/api/v1/grade-book/group-students")
            urlsToTry.add("https://iis.bsuir.by/api/v1/omissions")
            if (userGroup.isNotBlank()) {
                urlsToTry.add("https://iis.bsuir.by/api/v1/rating/group?studentGroup=$userGroup")
                urlsToTry.add("https://iis.bsuir.by/api/v1/rating/last?studentGroup=$userGroup")
            }

            for (attempt in 1..2) {
                for (url in urlsToTry) {
                    try {
                        val client = com.example.schedule.NetworkClient.client
                        val request = okhttp3.Request.Builder()
                            .url(url)
                            .addHeader("Cookie", token)
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .addHeader("Accept", "application/json, text/plain, */*")
                            .build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string()
                            if (response.code in listOf(401, 403) && attempt == 1) {
                                val refreshedToken = doRelogin()
                                if (refreshedToken != null) {
                                    token = refreshedToken
                                }
                            } else if (response.isSuccessful && !body.isNullOrBlank()) {
                                if (body.contains("groupInfoStudentDto") || body.contains("studentGroupCuratorDto")) {
                                    val (newCur, newSts) = parseUserGroupInfo(body, currentFio)
                                    if (newSts.isNotEmpty() || newCur != null) {
                                        prefs.edit().putString("cached_user_group_info", body).apply()
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            if (newCur != null) curator = newCur
                                            if (newSts.isNotEmpty()) students = newSts
                                            errorMessage = null
                                            isLoading = false
                                            isRefreshing = false
                                        }
                                        fetchedSuccessfully = true
                                        return@use
                                    }
                                }
                                val parsed = extractStudentsFromAnySource(body, currentFio)
                                if (parsed.size >= 1) {
                                    prefs.edit().putString("cached_group_students", body).apply()
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        students = parsed
                                        errorMessage = null
                                        isLoading = false
                                        isRefreshing = false
                                    }
                                    fetchedSuccessfully = true
                                    return@use
                                }
                            }
                        }
                    } catch (e: Exception) {}
                    if (fetchedSuccessfully) break
                }
                if (fetchedSuccessfully) break
            }

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                isLoading = false
                isRefreshing = false
                if (!fetchedSuccessfully && students.isEmpty()) {
                    if (currentFio.isNotBlank()) {
                        students = listOf(GroupStudentInfo(fio = currentFio, position = 1, isCurrentUser = true))
                        errorMessage = null
                    } else {
                        errorMessage = "Не удалось загрузить список группы"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        performFetch()
    }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) {
            students
        } else {
            students.filter { it.fio.contains(searchQuery, ignoreCase = true) || (it.role != null && it.role.contains(searchQuery, ignoreCase = true)) }
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MinBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 36.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Header with Back Button and Title
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = androidx.compose.ui.Modifier.size(32.dp).clickable { onBack() }
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                androidx.compose.material3.Text(
                    if (isTechno) "> СПИСОК ГРУППЫ_" else "Список группы",
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MinTextPrimary,
                    fontFamily = if (isTechno) vt323FontFamily else null
                )
                if (userGroup.isNotBlank()) {
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(currentAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        androidx.compose.material3.Text(
                            userGroup,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = currentAccent,
                            fontFamily = if (isTechno) vt323FontFamily else null
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))
                val scope = rememberCoroutineScope()
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Outlined.Refresh,
                    contentDescription = "Обновить",
                    tint = if (isRefreshing) currentAccent else MinTextSecondary,
                    modifier = androidx.compose.ui.Modifier.size(24.dp).clickable {
                        if (!isRefreshing) {
                            isRefreshing = true
                            isLoading = true
                            scope.launch { performFetch(forceReauth = true) }
                        }
                    }
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            // Search Bar
            if (students.isNotEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(if (isTechno) Color(0xFF0A0A0A) else MinCardBg)
                        .border(
                            1.dp,
                            if (isTechno) Color(0xFF00FF41).copy(alpha = 0.4f) else MinBorder,
                            androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MinTextSecondary,
                            modifier = androidx.compose.ui.Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(10.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MinTextPrimary,
                                fontSize = 15.sp,
                                fontFamily = if (isTechno) vt323FontFamily else null
                            ),
                            singleLine = true,
                            modifier = androidx.compose.ui.Modifier.weight(1f).padding(vertical = 8.dp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    androidx.compose.material3.Text(
                                        "Поиск",
                                        color = MinTextSecondary.copy(alpha = 0.6f),
                                        fontSize = 15.sp,
                                        fontFamily = if (isTechno) vt323FontFamily else null
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Outlined.Close,
                                contentDescription = "Очистить",
                                tint = MinTextSecondary,
                                modifier = androidx.compose.ui.Modifier.size(18.dp).clickable { searchQuery = "" }
                            )
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
            }
        }

        // Curator Card Section
        if (curator != null) {
            item {
                val cur = curator!!
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        // Top row with Curator avatar & info
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            // Avatar circle with initials
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier
                                    .size(44.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                val initials = cur.fio.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                                androidx.compose.material3.Text(
                                    initials.ifBlank { "К" },
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = currentAccent
                                )
                            }

                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))

                            androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                androidx.compose.material3.Text(
                                    "Куратор группы",
                                    fontSize = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = currentAccent
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(2.dp))
                                androidx.compose.material3.Text(
                                    cur.fio,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MinTextPrimary
                                )
                                if (cur.position.isNotBlank() && cur.position != "Куратор") {
                                    androidx.compose.material3.Text(
                                        cur.position,
                                        fontSize = 12.sp,
                                        color = MinTextSecondary
                                    )
                                }
                            }
                        }

                        // Action buttons row for phone & email - clean without background/border
                        androidx.compose.foundation.layout.Column(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            if (cur.phone.isNotBlank()) {
                                val formattedPhone = if (cur.phone.length == 13 && cur.phone.startsWith("+375")) {
                                    "${cur.phone.substring(0, 4)} (${cur.phone.substring(4, 6)}) ${cur.phone.substring(6, 9)}-${cur.phone.substring(9, 11)}-${cur.phone.substring(11, 13)}"
                                } else cur.phone

                                androidx.compose.foundation.layout.Row(
                                    modifier = androidx.compose.ui.Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${cur.phone}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {}
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Outlined.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF34C759),
                                        modifier = androidx.compose.ui.Modifier.size(20.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                        androidx.compose.material3.Text(
                                            "Телефон куратора",
                                            fontSize = 11.sp,
                                            color = MinTextSecondary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                        )
                                        androidx.compose.material3.Text(
                                            formattedPhone,
                                            fontSize = 15.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MinTextPrimary
                                        )
                                    }
                                    androidx.compose.material3.Icon(
                                        Icons.Outlined.Call,
                                        contentDescription = "Позвонить",
                                        tint = currentAccent,
                                        modifier = androidx.compose.ui.Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (cur.email.isNotBlank()) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = androidx.compose.ui.Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:${cur.email}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {}
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Outlined.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = androidx.compose.ui.Modifier.size(20.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                        androidx.compose.material3.Text(
                                            "Электронная почта",
                                            fontSize = 11.sp,
                                            color = MinTextSecondary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                        )
                                        androidx.compose.material3.Text(
                                            cur.email,
                                            fontSize = 15.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MinTextPrimary
                                        )
                                    }
                                    androidx.compose.material3.Icon(
                                        Icons.Outlined.Send,
                                        contentDescription = "Написать",
                                        tint = currentAccent,
                                        modifier = androidx.compose.ui.Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Student counter subheader
        if (students.isNotEmpty()) {
            item {
                androidx.compose.material3.Text(
                    "Студентов в группе: ${filteredStudents.size}${if (searchQuery.isNotEmpty()) " (из ${students.size})" else ""}",
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = MinTextSecondary,
                    fontFamily = if (isTechno) vt323FontFamily else null
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
            }
        }

        if (isLoading && students.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = currentAccent)
                }
            }
        } else if (errorMessage != null && students.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        errorMessage ?: "Ошибка",
                        color = androidx.compose.ui.graphics.Color(0xFFF44336),
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontFamily = if (isTechno) vt323FontFamily else null
                    )
                }
            }
        } else if (filteredStudents.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        if (searchQuery.isNotEmpty()) "Никого не найдено" else "Список пуст",
                        color = MinTextSecondary,
                        fontSize = 16.sp,
                        fontFamily = if (isTechno) vt323FontFamily else null
                    )
                }
            }
        } else {
            items(filteredStudents.size) { index ->
                val student = filteredStudents[index]

                val roleBadgeInfo = when {
                    student.role == null -> null
                    student.role.contains("Староста", ignoreCase = true) && !student.role.contains("Зам", ignoreCase = true) ->
                        Pair("Староста", Color(0xFFFFB800))
                    student.role.contains("Зам", ignoreCase = true) || student.role.contains("Заместитель", ignoreCase = true) ->
                        Pair("Зам. старосты", Color(0xFFFF9500))
                    student.role.contains("Спорт", ignoreCase = true) ->
                        Pair("Спорторг", Color(0xFF34C759))
                    student.role.contains("Культ", ignoreCase = true) ->
                        Pair("Культорг", Color(0xFFAF52DE))
                    student.role.contains("Проф", ignoreCase = true) ->
                        Pair("Профорг", Color(0xFF007AFF))
                    student.role.contains("БРСМ", ignoreCase = true) ->
                        Pair("БРСМ", Color(0xFFFF3B30))
                    else ->
                        Pair(student.role, currentAccent)
                }

                // Clean list item without box background under name
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Position number
                    androidx.compose.material3.Text(
                        "${student.position}",
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (student.isCurrentUser) currentAccent else MinTextSecondary.copy(alpha = 0.5f),
                        modifier = androidx.compose.ui.Modifier.width(28.dp),
                        fontFamily = if (isTechno) vt323FontFamily else null
                    )

                    // Student Name & Role Badge
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Text(
                                student.fio,
                                fontSize = 15.sp,
                                fontWeight = if (student.isCurrentUser) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
                                color = if (isTechno) Color(0xFF00FF41) else if (student.isCurrentUser) currentAccent else MinTextPrimary,
                                fontFamily = if (isTechno) vt323FontFamily else null
                            )
                            if (student.isCurrentUser) {
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                        .background(currentAccent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        "ВЫ",
                                        fontSize = 10.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (roleBadgeInfo != null) {
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(2.dp))
                            androidx.compose.material3.Text(
                                roleBadgeInfo.first,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = roleBadgeInfo.second,
                                fontFamily = if (isTechno) vt323FontFamily else null
                            )
                        }
                    }

                    // Average mark / Rating badge if present
                    if (student.averageMark != null && student.averageMark > 0.0) {
                        val markColor = when {
                            isTechno -> Color(0xFF00FF41)
                            student.averageMark >= 8.0 -> if (isDarkTheme) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                            student.averageMark >= 6.0 -> if (isDarkTheme) Color(0xFFFF9800) else Color(0xFFE65100)
                            else -> if (isDarkTheme) Color(0xFFF44336) else Color(0xFFC62828)
                        }

                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(markColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                String.format("%.1f", student.averageMark),
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = markColor,
                                fontFamily = if (isTechno) vt323FontFamily else null
                            )
                        }
                    }
                }

                // Subtle divider between student rows
                if (index < filteredStudents.size - 1) {
                    androidx.compose.material3.HorizontalDivider(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f),
                        thickness = 0.5.dp,
                        modifier = androidx.compose.ui.Modifier.padding(start = 28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MinStudyScreen(MinBg: androidx.compose.ui.graphics.Color, MinCardBg: androidx.compose.ui.graphics.Color, MinBorder: androidx.compose.ui.graphics.Color, MinTextPrimary: androidx.compose.ui.graphics.Color, MinTextSecondary: androidx.compose.ui.graphics.Color, currentAccent: androidx.compose.ui.graphics.Color, isDarkTheme: Boolean = true, onBack: () -> Unit) {
    MinStudyCertificatesAndSheetsScreen(
        MinBg = MinBg,
        MinCardBg = MinCardBg,
        MinBorder = MinBorder,
        MinTextPrimary = MinTextPrimary,
        MinTextSecondary = MinTextSecondary,
        currentAccent = currentAccent,
        isDarkTheme = isDarkTheme,
        onBack = onBack
    )
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
                                errorMessage = null
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
                Text("Зачетка", fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = MinTextPrimary)
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
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
                                    mark in listOf("9", "10", "Зачет", "зач", "зач.") -> if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF4CC38A) else androidx.compose.ui.graphics.Color(0xFF2E7D32)
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
                            val avgColor = if (isDarkTheme) { if (average >= 8) androidx.compose.ui.graphics.Color(0xFF4CC38A) else if (average >= 6) androidx.compose.ui.graphics.Color(0xFFFF9800) else androidx.compose.ui.graphics.Color(0xFFF44336) } else { if (average >= 8) androidx.compose.ui.graphics.Color(0xFF2E7D32) else if (average >= 6) androidx.compose.ui.graphics.Color(0xFFE65100) else androidx.compose.ui.graphics.Color(0xFFB71C1C) }

                            androidx.compose.material3.HorizontalDivider(color = MinBorder.copy(alpha=0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Средний балл", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary)
                                Text(String.format("%.2f", average), fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = avgColor)
                            }
                        }
                    }
                }
            }
        }
    }
}



fun copyAttachmentToStorage(context: android.content.Context, uri: android.net.Uri): NoteAttachment? {
    try {
        var fileName = "attachment"
        var fileSize = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: "attachment"
                    if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                }
            }
        } catch (e: Exception) {}

        val mimeType = context.contentResolver.getType(uri) ?: when {
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".webp", true) -> "image/webp"
            fileName.endsWith(".docx", true) || fileName.endsWith(".doc", true) -> "application/msword"
            fileName.endsWith(".txt", true) -> "text/plain"
            else -> "*/*"
        }

        val attachmentsDir = java.io.File(context.filesDir, "note_attachments").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val savedFile = java.io.File(attachmentsDir, "${System.currentTimeMillis()}_$safeName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            savedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return NoteAttachment(
            id = java.util.UUID.randomUUID().toString(),
            uri = android.net.Uri.fromFile(savedFile).toString(),
            name = fileName,
            mimeType = mimeType,
            sizeBytes = if (fileSize > 0) fileSize else savedFile.length()
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun openAttachmentFile(context: android.content.Context, attachment: NoteAttachment) {
    try {
        val fileUri = android.net.Uri.parse(attachment.uri)
        val targetUri = if (fileUri.scheme == "file") {
            val file = java.io.File(fileUri.path ?: "")
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            fileUri
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, attachment.mimeType.ifBlank { "*/*" })
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fileUri = android.net.Uri.parse(attachment.uri)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "*/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            android.widget.Toast.makeText(context, "Не удалось открыть файл: ${attachment.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f МБ", mb)
    else String.format(java.util.Locale.US, "%.0f КБ", kb)
}

@Composable
fun NotionAttachmentChip(
    attachment: NoteAttachment,
    isDarkTheme: Boolean,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val chipBg = if (isDarkTheme) Color(0xFF22232C) else Color(0xFFEFEFF4)
    val chipBorder = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFD8D8E0)
    val ext = attachment.name.substringAfterLast('.', "").lowercase()

    val iconVector = when {
        ext in listOf("png", "jpg", "jpeg", "webp", "gif", "svg") -> Icons.Outlined.Image
        ext in listOf("pdf") -> Icons.Outlined.PictureAsPdf
        ext in listOf("doc", "docx", "txt", "rtf", "odt") -> Icons.Outlined.Description
        else -> Icons.Outlined.InsertDriveFile
    }

    val iconTint = when {
        ext in listOf("png", "jpg", "jpeg", "webp") -> Color(0xFF30B0C7)
        ext in listOf("pdf") -> Color(0xFFFF453A)
        ext in listOf("doc", "docx", "txt") -> Color(0xFF007AFF)
        else -> Color(0xFFFF9F0A)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(10.dp))
            .clickable {
             AppHapticManager.playClick()
             onClick()
         }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            iconVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.widthIn(max = 140.dp)) {
            Text(
                attachment.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color(0xFFEEEEEE) else Color(0xFF1C1C1E),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (attachment.sizeBytes > 0) {
                Text(
                    formatFileSize(attachment.sizeBytes),
                    fontSize = 10.sp,
                    color = if (isDarkTheme) Color(0xFFAAAAAA) else Color(0xFF8E8E93)
                )
            }
        }
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Удалить файл",
                tint = if (isDarkTheme) Color(0xFFAAAAAA) else Color(0xFF8E8E93),
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() }
                    .padding(1.dp)
            )
        }
    }
}

fun parseNoteDateMillis(dateStr: String): Long {
    return try {
        val parts = dateStr.split(".")
        if (parts.size == 3) {
            val d = parts[0].toInt()
            val m = parts[1].toInt() - 1
            val y = parts[2].toInt()
            val cal = java.util.Calendar.getInstance()
            cal.set(y, m, d, 12, 0, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } else 0L
    } catch (e: Exception) { 0L }
}

fun calculateReminderTime(timing: String, d: Int, m: Int, y: Int, hour: Int = 9, minute: Int = 0, customHours: Int = 3): Long {
    val cal = java.util.Calendar.getInstance()
    when {
        timing == "custom_hours" || timing.startsWith("custom_hours_") -> {
            val hrs = if (timing.startsWith("custom_hours_")) timing.removePrefix("custom_hours_").toIntOrNull() ?: customHours else customHours
            cal.set(y, m, d, hour, minute, 0)
            cal.add(java.util.Calendar.HOUR_OF_DAY, -hrs)
        }
        timing == "1_day_before_18" -> {
            cal.set(y, m, d, 18, 0, 0)
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        timing == "1_day_before_21" -> {
            cal.set(y, m, d, 21, 0, 0)
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        timing == "same_day_08" -> {
            cal.set(y, m, d, 8, 0, 0)
        }
        timing == "same_day_09" -> {
            cal.set(y, m, d, 9, 0, 0)
        }
        timing == "2_hours_before" -> {
            cal.set(y, m, d, hour, minute, 0)
            cal.add(java.util.Calendar.HOUR_OF_DAY, -2)
        }
        timing == "1_hour_before" -> {
            cal.set(y, m, d, hour, minute, 0)
            cal.add(java.util.Calendar.HOUR_OF_DAY, -1)
        }
        timing == "15_min_before" -> {
            cal.set(y, m, d, hour, minute, 0)
            cal.add(java.util.Calendar.MINUTE, -15)
        }
        else -> {
            cal.set(y, m, d, 18, 0, 0)
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
    }
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

object GoogleCalendarSync {
    fun getPrimaryCalendarId(context: android.content.Context): Long? {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val projection = arrayOf(
            android.provider.CalendarContract.Calendars._ID,
            android.provider.CalendarContract.Calendars.IS_PRIMARY,
            android.provider.CalendarContract.Calendars.ACCOUNT_TYPE
        )
        try {
            context.contentResolver.query(
                android.provider.CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                var primaryId: Long? = null
                var googleId: Long? = null
                var firstId: Long? = null
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val isPrimary = cursor.getInt(1) == 1
                    val accountType = cursor.getString(2)
                    if (firstId == null) firstId = id
                    if (isPrimary) primaryId = id
                    if (accountType == "com.google") googleId = id
                }
                return primaryId ?: googleId ?: firstId
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun syncEvent(context: android.content.Context, note: Note, d: Int, m: Int, y: Int): Long? {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return null
        }
        try {
            val calId = getPrimaryCalendarId(context) ?: return null
            val startCal = java.util.Calendar.getInstance().apply {
                set(y, m, d, 9, 0, 0)
            }
            val endCal = java.util.Calendar.getInstance().apply {
                set(y, m, d, 10, 30, 0)
            }

            val values = android.content.ContentValues().apply {
                put(android.provider.CalendarContract.Events.DTSTART, startCal.timeInMillis)
                put(android.provider.CalendarContract.Events.DTEND, endCal.timeInMillis)
                put(android.provider.CalendarContract.Events.TITLE, "${note.subject}: ${note.text.lines().firstOrNull() ?: ""}")
                put(android.provider.CalendarContract.Events.DESCRIPTION, note.text)
                put(android.provider.CalendarContract.Events.CALENDAR_ID, calId)
                put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }

            if (note.calendarEventId != null) {
                try {
                    val updateUri = android.content.ContentUris.withAppendedId(android.provider.CalendarContract.Events.CONTENT_URI, note.calendarEventId)
                    val rows = context.contentResolver.update(updateUri, values, null, null)
                    if (rows > 0) return note.calendarEventId
                } catch (e: Exception) {}
            }

            val uri = context.contentResolver.insert(android.provider.CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deleteEvent(context: android.content.Context, eventId: Long?) {
        if (eventId == null) return
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            val deleteUri = android.content.ContentUris.withAppendedId(android.provider.CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(deleteUri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MinNotesScreen(MinBg: Color, MinCardBg: Color, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, MinAccent: Color, selectedGroup: String = "310101", isDarkTheme: Boolean = true) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE) }
    val gson = remember { com.google.gson.Gson() }

    var predefinedSubjects by remember { mutableStateOf<List<String>>(emptyList()) }
    
    androidx.compose.runtime.LaunchedEffect(selectedGroup) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val appPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val targetGroup = if (selectedGroup.isNotBlank() && selectedGroup != "114001") selectedGroup else appPrefs.getString("login_group", "310101") ?: "310101"
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

                        val actualSchedules = (if (!scheduleResp?.schedules.isNullOrEmpty()) scheduleResp?.schedules else scheduleResp?.nextSchedules) ?: emptyMap()
                    actualSchedules.forEach { (_, dayLessons) ->
                            dayLessons.forEach { lesson ->
                                val sName = lesson.subject?.takeIf { it.isNotBlank() } ?: lesson.subjectFullName
                                if (!sName.isNullOrBlank()) sessionSubjects.add(sName.trim())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Current semester schedule subjects only
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
    var selectedLessonTypeFilter by remember { mutableStateOf("Все") }
    var noteText by remember { mutableStateOf("") }
    var isEventNote by remember { mutableStateOf(false) }
    var showNewNoteTimePicker by remember { mutableStateOf(false) }
    var showNewNoteDatePicker by remember { mutableStateOf(false) }

    val todayCal = remember { java.util.Calendar.getInstance() }
    var selectedDay by remember { mutableStateOf(todayCal.get(java.util.Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableStateOf(todayCal.get(java.util.Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(todayCal.get(java.util.Calendar.YEAR)) }
    var selectedHour by remember { mutableStateOf(todayCal.get(java.util.Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(todayCal.get(java.util.Calendar.MINUTE)) }
    var viewMonth by remember { mutableStateOf(todayCal.get(java.util.Calendar.MONTH)) }
    var viewYear by remember { mutableStateOf(todayCal.get(java.util.Calendar.YEAR)) }

    var pendingAttachments by remember { mutableStateOf<List<NoteAttachment>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val newFilePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val copiedList = uris.mapNotNull { uri ->
                    copyAttachmentToStorage(context, uri)
                }
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    pendingAttachments = pendingAttachments + copiedList
                }
            }
        }
    }

    var scaleLevel by remember { mutableFloatStateOf(1.0f) }
    var isGridMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewNoteDialog by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            calendarPermissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR))
        }
    }

    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }

    // Reload notes every time this composable becomes active
    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
            notes = com.google.gson.Gson().fromJson<List<Note>>(prefs.getString("notes_data", "[]"), type) ?: emptyList()
        } catch (e: Exception) { notes = emptyList() }
    }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }

    var isCalendarExpanded by remember { mutableStateOf(false) }
    var radialExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = noteToDelete != null || noteToEdit != null || showNewNoteDialog || radialExpanded || isCalendarExpanded || subjectDropdownExpanded) {
        if (noteToDelete != null) {
            noteToDelete = null
        } else if (noteToEdit != null) {
            noteToEdit = null
        } else if (showNewNoteDialog) {
            showNewNoteDialog = false
        } else if (radialExpanded) {
            radialExpanded = false
        } else if (subjectDropdownExpanded) {
            subjectDropdownExpanded = false
        } else if (isCalendarExpanded) {
            isCalendarExpanded = false
        }
    }

    fun formatDate(d: Int, m: Int, y: Int) = "%02d.%02d.%04d".format(d, m + 1, y)

    fun scheduleAlarm(context: android.content.Context, note: Note, d: Int, m: Int, y: Int, hour: Int, minute: Int) {
        val appPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val timing = appPrefs.getString("event_reminder_timing", "15_min_before") ?: "15_min_before"
        val customHours = appPrefs.getInt("custom_reminder_hours", 3)
        val triggerTime = calculateReminderTime(timing, d, m, y, hour, minute, customHours)

        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(context, NoteReminderReceiver::class.java).apply {
            putExtra("SUBJECT", note.subject)
            putExtra("TEXT", note.text)
            putExtra("IS_EVENT", note.isEvent)
        }
        if (triggerTime > System.currentTimeMillis()) {
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, note.id.hashCode(), intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (e: SecurityException) { }
        }
    }

    fun saveNotes(list: List<Note>) { notes = list; prefs.edit().putString("notes_data", gson.toJson(list)).apply() }

    val allSubjects = remember(predefinedSubjects) { predefinedSubjects.ifEmpty { listOf("Общее") } }
    val subjects = remember(notes, allSubjects) {
        val list = mutableListOf("Все")
        if (notes.any { it.isPinned }) list.add("Закрепленные")
        if (notes.any { it.isEvent }) list.add("События")
        list.addAll(allSubjects)
        list.distinct()
    }
    val highlightedDates = remember(notes, selectedSubject) {
        val list = when (selectedSubject) {
            "Все" -> notes
            "Закрепленные" -> notes.filter { it.isPinned }
            "События" -> notes.filter { it.isEvent }
            else -> notes.filter { it.subject == selectedSubject }
        }
        list.map { it.date }.toSet()
    }
    
    // Helper to extract or infer lesson type from a note
    fun inferNoteLessonType(note: Note): String {
        val explicit = note.lessonType.trim().uppercase()
        if (explicit.contains("ЛР") || explicit.contains("ЛАБ")) return "ЛР"
        if (explicit.contains("ЛК") || explicit.contains("ЛЕК")) return "ЛК"
        if (explicit.contains("ПЗ") || explicit.contains("ПРАК")) return "ПЗ"
        
        // Infer from note text if not explicitly set
        val txt = note.text.uppercase()
        return when {
            txt.contains("ЛР") || txt.contains("ЛАБ") || txt.contains("ЛАБОРАТОР") -> "ЛР"
            txt.contains("ПЗ") || txt.contains("ПРАКТ") || txt.contains("СЕМИНАР") -> "ПЗ"
            txt.contains("ЛК") || txt.contains("ЛЕКЦ") -> "ЛК"
            else -> ""
        }
    }

    // Filter and sort notes
    val filteredNotes = remember(notes, selectedSubject, selectedLessonTypeFilter, searchQuery) {
        var list = when (selectedSubject) {
            "Все" -> notes
            "Закрепленные" -> notes.filter { it.isPinned }
            "События" -> notes.filter { it.isEvent }
            else -> notes.filter { it.subject == selectedSubject }
        }
        if (selectedLessonTypeFilter != "Все") {
            list = list.filter { note ->
                inferNoteLessonType(note) == selectedLessonTypeFilter
            }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { note ->
                note.text.lowercase().contains(q) ||
                note.subject.lowercase().contains(q) ||
                note.date.contains(q) ||
                inferNoteLessonType(note).lowercase().contains(q)
            }
        }
        list.sortedBy { parseNoteDateMillis(it.date) }
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val regularNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    if (noteToDelete != null) {
        val deleteDialogBg = MinCardBg
        val deleteBorder = MinBorder
        val deleteTitleColor = MinTextPrimary
        val deleteSubColor = MinTextSecondary

        Dialog(onDismissRequest = { noteToDelete = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = if (isDarkTheme) 24.dp else 12.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(deleteDialogBg)
                    .border(1.dp, deleteBorder, RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Удалить заметку?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = deleteTitleColor
                        )
                        Text(
                            "Вы уверены, что хотите удалить эту заметку? Это действие также удалит синхронизированное событие из Google Календаря.",
                            fontSize = 14.sp,
                            color = deleteSubColor,
                            lineHeight = 19.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                AppHapticManager.playClick()
                                noteToDelete = null
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MinTextPrimary.copy(alpha = 0.08f),
                                contentColor = deleteTitleColor
                            )
                        ) {
                            Text("Отмена", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                AppHapticManager.playClick()
                                GoogleCalendarSync.deleteEvent(context, noteToDelete!!.calendarEventId)
                                saveNotes(notes.filter { it.id != noteToDelete!!.id })
                                noteToDelete = null
                            },
                            modifier = Modifier.weight(1.1f).height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF3B30),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Удалить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Notion Note Edit Dialog (Adaptive Light / Dark Theme)
    if (noteToEdit != null) {
        var editText by remember(noteToEdit) { mutableStateOf(noteToEdit!!.text) }
        var editSubject by remember(noteToEdit) { mutableStateOf(noteToEdit!!.subject) }
        var editLessonType by remember(noteToEdit) { mutableStateOf(inferNoteLessonType(noteToEdit!!)) }
        var editIsEvent by remember(noteToEdit) { mutableStateOf(noteToEdit!!.isEvent) }
        var editIsPinned by remember(noteToEdit) { mutableStateOf(noteToEdit!!.isPinned) }
        var editAttachments by remember(noteToEdit) { mutableStateOf(noteToEdit!!.attachments ?: emptyList()) }
        var editSubjectExpanded by remember { mutableStateOf(false) }
        val initDateParts = remember(noteToEdit) { noteToEdit!!.date.split(".") }
        var editDay by remember(noteToEdit) { mutableStateOf(initDateParts.getOrNull(0)?.toIntOrNull() ?: selectedDay) }
        var editMonth by remember(noteToEdit) { mutableStateOf((initDateParts.getOrNull(1)?.toIntOrNull()?.minus(1)) ?: selectedMonth) }
        var editYear by remember(noteToEdit) { mutableStateOf(initDateParts.getOrNull(2)?.toIntOrNull() ?: selectedYear) }
        var showEditDatePicker by remember { mutableStateOf(false) }
        var editHour by remember(noteToEdit) { mutableStateOf(noteToEdit!!.time?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 12) }
        var editMinute by remember(noteToEdit) { mutableStateOf(noteToEdit!!.time?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0) }
        var showEditTimePicker by remember { mutableStateOf(false) }

        val editFilePickerLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isNotEmpty()) {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val copiedList = uris.mapNotNull { uri ->
                        copyAttachmentToStorage(context, uri)
                    }
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        editAttachments = editAttachments + copiedList
                    }
                }
            }
        }

        val editDialogBg = MinCardBg
        val editSubBg = MinTextPrimary.copy(alpha = 0.08f)
        val editBorder = Color.Transparent
        val editFieldBg = editSubBg
        val editTextColor = MinTextPrimary
        val editSecTextColor = MinTextSecondary

        androidx.compose.ui.window.Dialog(onDismissRequest = { noteToEdit = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(editDialogBg)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Редактировать", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = editTextColor)
                        IconButton(onClick = { noteToEdit = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Закрыть", tint = editSecTextColor)
                        }
                    }

                    val nextEditTarget = remember(editSubject, selectedGroup) {
                        findNextLessonForSubject(context, selectedGroup, editSubject)
                    }
                    // Oval badges summary in editor
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subject Selector Pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MinAccent.copy(alpha = if (isDarkTheme) 0.16f else 0.12f))
                                .border(1.dp, MinAccent.copy(alpha = 0.3f), CircleShape)
                                .clickable { 
                                    AppHapticManager.playClick()
                                    editSubjectExpanded = !editSubjectExpanded 
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(editSubject, color = MinAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MinAccent, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Date Pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(editSubBg)
                                .border(1.dp, editBorder, CircleShape)
                                .clickable {
                                    AppHapticManager.playClick()
                                    showEditDatePicker = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(formatDate(editDay, editMonth, editYear), color = editSecTextColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        // Time Pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(editSubBg)
                                .border(1.dp, editBorder, CircleShape)
                                .clickable {
                                    showEditTimePicker = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "%02d:%02d".format(editHour, editMinute),
                                color = editSecTextColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        if (nextEditTarget != null) {
                            val isNextApplied = editDay == nextEditTarget.day && editMonth == nextEditTarget.month0Based && editYear == nextEditTarget.year
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isNextApplied) MinAccent.copy(alpha = 0.16f) else editSubBg)
                                    .border(1.dp, if (isNextApplied) MinAccent.copy(alpha = 0.4f) else editBorder, CircleShape)
                                    .clickable {
                                        AppHapticManager.playClick()
                                        editDay = nextEditTarget.day
                                        editMonth = nextEditTarget.month0Based
                                        editYear = nextEditTarget.year
                                        editHour = nextEditTarget.hour
                                        editMinute = nextEditTarget.minute
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "К след. паре (${nextEditTarget.dateFormatted.take(5)})",
                                    color = if (isNextApplied) MinAccent else editSecTextColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Type of Lesson Selector (Все, ЛР, ЛК, ПЗ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Тип:", color = editSecTextColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        listOf("" to "Обычная", "ЛР" to "ЛР", "ЛК" to "ЛК", "ПЗ" to "ПЗ").forEach { (typeVal, typeLabel) ->
                            val isSel = editLessonType == typeVal
                            val badgeColor = when (typeVal) {
                                "ЛР" -> Color(0xFF34C759)
                                "ЛК" -> Color(0xFF007AFF)
                                "ПЗ" -> Color(0xFFFF9500)
                                else -> MinAccent
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) badgeColor.copy(alpha = 0.18f) else editSubBg)
                                    .border(1.dp, if (isSel) badgeColor.copy(alpha = 0.6f) else editBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        AppHapticManager.playClick()
                                        editLessonType = typeVal
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    color = if (isSel) badgeColor else editSecTextColor,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = editSubjectExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Transparent)
                                .border(1.dp, editBorder, RoundedCornerShape(14.dp))
                                .padding(6.dp)
                        ) {
                            allSubjects.forEach { s ->
                                val isSel = s == editSubject
                                Text(
                                    s,
                                    color = if (isSel) MinAccent else editTextColor,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) MinAccent.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            AppHapticManager.playClick()
                                            editSubject = s
                                            editSubjectExpanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Spacious Notion Text Editor Field
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        placeholder = { Text("Введите заметку...", color = editSecTextColor) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 320.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = editTextColor,
                            unfocusedTextColor = editTextColor,
                            cursorColor = MinAccent,
                            focusedBorderColor = MinAccent,
                            unfocusedBorderColor = editBorder,
                            focusedContainerColor = editFieldBg,
                            unfocusedContainerColor = editFieldBg
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Attachments Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Файлы и вложения (${editAttachments.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = editSecTextColor
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(editSubBg)
                                    .border(1.dp, editBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        AppHapticManager.playClick()
                                        editFilePickerLauncher.launch(arrayOf("*/*"))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MinAccent, modifier = Modifier.size(14.dp))
                                Text("+ Прикрепить", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MinAccent)
                            }
                        }

                        if (editAttachments.isNotEmpty()) {
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                editAttachments.forEach { att ->
                                    NotionAttachmentChip(
                                        attachment = att,
                                        isDarkTheme = isDarkTheme,
                                        onDelete = {
                                            AppHapticManager.playClick()
                                            editAttachments = editAttachments.filter { it.id != att.id }
                                        },
                                        onClick = { openAttachmentFile(context, att) }
                                    )
                                }
                            }
                        }
                    }

                    // Toggles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pin Toggle Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (editIsPinned) Color(0xFFFF9500).copy(alpha = if (isDarkTheme) 0.18f else 0.14f) else editSubBg)
                                .border(1.dp, if (editIsPinned) Color(0xFFFF9500).copy(alpha = 0.45f) else editBorder, RoundedCornerShape(12.dp))
                                .clickable { 
                                    AppHapticManager.playClick()
                                    editIsPinned = !editIsPinned 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    tint = if (editIsPinned) Color(0xFFFF9500) else editSecTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Закрепить",
                                    color = if (editIsPinned) Color(0xFFFF9500) else editTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Event Toggle Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (editIsEvent) Color(0xFFFFB800).copy(alpha = if (isDarkTheme) 0.18f else 0.14f) else editSubBg)
                                .border(1.dp, if (editIsEvent) Color(0xFFFFB800).copy(alpha = 0.45f) else editBorder, RoundedCornerShape(12.dp))
                                .clickable { 
                                    AppHapticManager.playClick()
                                    editIsEvent = !editIsEvent 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (editIsEvent) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = if (editIsEvent) Color(0xFFFFB800) else editSecTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Событие",
                                    color = if (editIsEvent) Color(0xFFFFB800) else editTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Action Buttons Row (Delete & Save styled as matching pill)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                AppHapticManager.playClick()
                                GoogleCalendarSync.deleteEvent(context, noteToEdit!!.calendarEventId)
                                saveNotes(notes.filter { it.id != noteToEdit!!.id })
                                noteToEdit = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) Color(0xFFFF3B30).copy(alpha = 0.15f) else Color(0xFFFF3B30).copy(alpha = 0.10f),
                                contentColor = Color(0xFFFF3B30)
                            )
                        ) {
                            Text("Удалить", fontWeight = FontWeight.Bold)
                        }

                        // Save button matching Event button aesthetic
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MinAccent.copy(alpha = if (isDarkTheme) 0.20f else 0.15f))
                                .border(1.dp, MinAccent.copy(alpha = if (isDarkTheme) 0.50f else 0.40f), RoundedCornerShape(14.dp))
                                .clickable {
                                    if (editText.isNotBlank() || editAttachments.isNotEmpty()) {
                                        AppHapticManager.playClick()
                                        
                                        var newCalEventId = noteToEdit!!.calendarEventId
                                        val day = editDay
                                        val month = editMonth
                                        val year = editYear
                                        val newDateStr = formatDate(day, month, year)

                                        if (editIsEvent) {
                                            val syncedId = GoogleCalendarSync.syncEvent(
                                                context,
                                                noteToEdit!!.copy(text = editText, subject = editSubject, date = newDateStr, isEvent = true),
                                                day, month, year
                                            )
                                            if (syncedId != null) newCalEventId = syncedId
                                        } else if (noteToEdit!!.calendarEventId != null) {
                                            GoogleCalendarSync.deleteEvent(context, noteToEdit!!.calendarEventId)
                                            newCalEventId = null
                                        }

                                        val updated = noteToEdit!!.copy(
                                            text = editText,
                                            subject = editSubject,
                                            date = newDateStr,
                                            isEvent = editIsEvent,
                                            isPinned = editIsPinned,
                                            lessonType = editLessonType,
                                            time = "%02d:%02d".format(editHour, editMinute),
                                            attachments = editAttachments,
                                            calendarEventId = newCalEventId
                                        )
                                        saveNotes(notes.map { if (it.id == updated.id) updated else it })
                                        if (editIsEvent) {
                                            scheduleAlarm(context, updated, day, month, year, editHour, editMinute)
                                        }
                                        noteToEdit = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = "Сохранить", tint = MinAccent, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }

        if (showEditDatePicker) {
            IosDatePickerDialog(
                initialDay = editDay,
                initialMonth = editMonth,
                initialYear = editYear,
                highlightedDates = highlightedDates,
                onDismiss = { showEditDatePicker = false },
                onDateSelected = { d, m, y ->
                    editDay = d
                    editMonth = m
                    editYear = y
                },
                MinTextPrimary = MinTextPrimary,
                MinTextSecondary = MinTextSecondary,
                MinCardBg = MinCardBg,
                MinAccent = MinAccent,
                MinBg = MinBg
            )
        }

        if (showEditTimePicker) {
            IosTimePickerDialog(
                initialHour = editHour,
                initialMinute = editMinute,
                onDismiss = { showEditTimePicker = false },
                onTimeSelected = { h, m ->
                    editHour = h
                    editMinute = m
                },
                MinTextPrimary = MinTextPrimary,
                MinTextSecondary = MinTextSecondary,
                MinCardBg = MinCardBg,
                MinAccent = MinAccent
            )
        }
    }

    val styleType = LocalStyleType.current
    var newNoteText by remember { mutableStateOf("") }
    var newNoteSubject by remember { mutableStateOf("Общее") }
    var newNoteLessonType by remember { mutableStateOf("") }
    var newNoteIsEvent by remember { mutableStateOf(false) }
    var newNoteIsPinned by remember { mutableStateOf(false) }
    var newSubjectExpanded by remember { mutableStateOf(false) }
    var newCalendarExpanded by remember { mutableStateOf(false) }

    // Helper lambda for rendering a single modern Apple Note Card
    val renderNoteCard = @Composable { note: Note, isCompact: Boolean ->
        val lines = note.text.lines()
        val noteTitle = lines.firstOrNull()?.trim().orEmpty().ifBlank { "Без названия" }
        val noteBody = lines.drop(1).joinToString("\n").trim()
        val noteAttachments = note.attachments ?: emptyList()
        val labelColor = MinTextPrimary
        val dotColor = when {
            note.isEvent -> Color(0xFFFFB800)
            note.isPinned -> Color(0xFFFF9500)
            else -> MinAccent
        }
        val gridBg = if (isGridMode) MinCardBg else Color.Transparent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(gridBg)
                .combinedClickable(
                    onClick = { AppHapticManager.playClick(); noteToEdit = note },
                    onLongClick = { AppHapticManager.playClick(); noteToDelete = note }
                )
                .padding(horizontal = if (isGridMode) 14.dp else 4.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = note.subject,
                    color = MinTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val inferredType = inferNoteLessonType(note)
                if (inferredType.isNotBlank()) {
                    val badgeColor = when (inferredType) {
                        "ЛР" -> Color(0xFF34C759)
                        "ЛК" -> Color(0xFF007AFF)
                        "ПЗ" -> Color(0xFFFF9500)
                        else -> MinAccent
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.16f))
                            .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = inferredType,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "  ·  " + (if (note.time != null) "${note.date} ${note.time}" else note.date),
                    color = MinTextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                if (note.isPinned) Icon(Icons.Outlined.PushPin, null, tint = Color(0xFFFF9500), modifier = Modifier.size(14.dp))
                if (note.isEvent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Outlined.NotificationsActive, null, tint = Color(0xFFFFB800), modifier = Modifier.size(14.dp))
                }
                if (noteAttachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Outlined.AttachFile, null, tint = MinTextSecondary, modifier = Modifier.size(14.dp))
                    Text("${noteAttachments.size}", color = MinTextSecondary, fontSize = 12.sp)
                }
            }
            Text(
                text = noteTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                maxLines = if (isCompact) 2 else 3,
                lineHeight = 22.sp
            )
            if (noteBody.isNotBlank()) {
                Text(
                    text = noteBody,
                    fontSize = 14.sp,
                    color = MinTextSecondary,
                    maxLines = if (isCompact) 3 else 2,
                    lineHeight = 20.sp
                )
            }
            if (!isGridMode) {
                androidx.compose.material3.HorizontalDivider(
                    color = MinBorder.copy(alpha = 0.3f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Title & View Mode Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        AutoResizedText("Заметки", fontSize = 32.sp, minFontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                        val notesCountStr = "${filteredNotes.size} ${if (filteredNotes.size % 10 == 1 && filteredNotes.size % 100 != 11) "заметка" else if (filteredNotes.size % 10 in 2..4 && filteredNotes.size % 100 !in 12..14) "заметки" else "заметок"}"
                        val eventsCount = notes.count { it.isEvent }
                        Text(
                            if (eventsCount > 0) "$notesCountStr • $eventsCount ${if (eventsCount == 1) "событие" else if (eventsCount in 2..4) "события" else "событий"}" else notesCountStr,
                            fontSize = 14.sp,
                            color = MinTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Grid / List View Toggle Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MinCardBg)
                            .border(1.dp, MinBorder, CircleShape)
                            .clickable { 
                                AppHapticManager.playClick()
                                isGridMode = !isGridMode 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isGridMode) Icons.Outlined.GridView else Icons.Outlined.ViewAgenda,
                            contentDescription = "Вид",
                            tint = if (isGridMode) MinAccent else MinTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Apple Search Bar
            item {
                AppleSearchInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Поиск",
                    isDarkTheme = isDarkTheme,
                    inputBg = MinCardBg,
                    inputBorder = MinBorder,
                    inputTextColor = MinTextPrimary,
                    inputIconColor = MinTextSecondary,
                    cursorColor = MinAccent
                )
            }

            // Subject Dropdown Selector (replaces horizontal carousel)
            item {
                val selectedLabel = when (selectedSubject) {
                    "Все" -> "Все предметы (${notes.size})"
                    "Закрепленные" -> "Закрепленные (${notes.count { it.isPinned }})"
                    "События" -> "События (${notes.count { it.isEvent }})"
                    else -> selectedSubject
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MinCardBg)
                            .border(1.dp, if (subjectDropdownExpanded) MinAccent.copy(alpha = 0.5f) else MinBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                AppHapticManager.playClick()
                                subjectDropdownExpanded = !subjectDropdownExpanded
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Предмет:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MinTextSecondary
                            )
                            Text(
                                selectedLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedSubject == "Все") MinTextPrimary else MinAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (subjectDropdownExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Выбрать предмет",
                            tint = if (subjectDropdownExpanded) MinAccent else MinTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = subjectDropdownExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MinCardBg)
                                .border(1.dp, MinBorder, RoundedCornerShape(14.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            subjects.forEach { subject ->
                                val itemLabel = when (subject) {
                                    "Все" -> "Все предметы (${notes.size})"
                                    "Закрепленные" -> "Закрепленные (${notes.count { it.isPinned }})"
                                    "События" -> "События (${notes.count { it.isEvent }})"
                                    else -> subject
                                }
                                val isSelected = selectedSubject == subject
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MinAccent.copy(alpha = 0.14f) else Color.Transparent)
                                        .clickable {
                                            AppHapticManager.playClick()
                                            selectedSubject = subject
                                            subjectDropdownExpanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        itemLabel,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MinAccent else MinTextPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MinAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lesson Type Filter Segment (Все, ЛР, ЛК, ПЗ)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lessonTypes = listOf("Все", "ЛР", "ЛК", "ПЗ")
                    lessonTypes.forEach { typeOption ->
                        val isSel = selectedLessonTypeFilter == typeOption
                        val activeColor = when (typeOption) {
                            "ЛР" -> Color(0xFF34C759)
                            "ЛК" -> Color(0xFF007AFF)
                            "ПЗ" -> Color(0xFFFF9500)
                            else -> MinAccent
                        }
                        val count = if (typeOption == "Все") {
                            notes.size
                        } else {
                            notes.count { inferNoteLessonType(it) == typeOption }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) activeColor.copy(alpha = if (isDarkTheme) 0.22f else 0.16f) else MinCardBg)
                                .border(1.dp, if (isSel) activeColor.copy(alpha = 0.6f) else MinBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    AppHapticManager.playClick()
                                    selectedLessonTypeFilter = typeOption
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (count > 0 && typeOption != "Все") "$typeOption ($count)" else typeOption,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) activeColor else MinTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Notes Content / Empty State
            if (filteredNotes.isEmpty()) {
                item {
                    AppleNotesEmptyState(
                        isSearch = searchQuery.isNotBlank(),
                        isDarkTheme = isDarkTheme,
                        MinTextPrimary = MinTextPrimary,
                        MinTextSecondary = MinTextSecondary
                    )
                }
            } else {
                // 1. PINNED NOTES SECTION
                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Text(
                            "Закреплённые",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MinTextSecondary
                        )
                    }

                    if (isGridMode) {
                        val chunks = pinnedNotes.chunked(2)
                        items(chunks.size) { chunkIdx ->
                            val pair = chunks[chunkIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    renderNoteCard(pair[0], true)
                                }
                                if (pair.size > 1) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        renderNoteCard(pair[1], true)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(pinnedNotes, key = { it.id }) { note ->
                            renderNoteCard(note, false)
                        }
                    }
                }

                // 2. REGULAR NOTES SECTION
                if (regularNotes.isNotEmpty()) {
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Text(
                                "Заметки",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MinTextSecondary
                            )
                        }
                    }

                    if (isGridMode) {
                        val chunks = regularNotes.chunked(2)
                        items(chunks.size) { chunkIdx ->
                            val pair = chunks[chunkIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    renderNoteCard(pair[0], true)
                                }
                                if (pair.size > 1) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        renderNoteCard(pair[1], true)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(regularNotes, key = { it.id }) { note ->
                            renderNoteCard(note, false)
                        }
                    }
                }
            }
        }

        // Floating Action Button in Bottom-Right Corner
        FloatingActionButton(
            onClick = {
                AppHapticManager.playClick()
                newNoteText = ""
                pendingAttachments = emptyList()
                newNoteIsEvent = false
                newNoteIsPinned = false
                newNoteLessonType = if (selectedLessonTypeFilter != "Все") selectedLessonTypeFilter else ""
                newNoteSubject = if (selectedSubject != "Все" && selectedSubject != "Закрепленные" && selectedSubject != "События") selectedSubject else allSubjects.firstOrNull() ?: "Общее"
                val nextTarget = findNextLessonForSubject(context, selectedGroup, newNoteSubject)
                if (nextTarget != null) {
                    selectedDay = nextTarget.day
                    selectedMonth = nextTarget.month0Based
                    selectedYear = nextTarget.year
                    selectedHour = nextTarget.hour
                    selectedMinute = nextTarget.minute
                } else {
                    val nowCal = java.util.Calendar.getInstance()
                    selectedDay = nowCal.get(java.util.Calendar.DAY_OF_MONTH)
                    selectedMonth = nowCal.get(java.util.Calendar.MONTH)
                    selectedYear = nowCal.get(java.util.Calendar.YEAR)
                    selectedHour = nowCal.get(java.util.Calendar.HOUR_OF_DAY)
                    selectedMinute = nowCal.get(java.util.Calendar.MINUTE)
                }
                showNewNoteDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 28.dp, end = 20.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = MinAccent,
            contentColor = MinCardBg,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Создать заметку",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Bottom sheet for creating a new note
    if (showNewNoteDialog) {
        val sheetBg = MinCardBg
        val chipBg = MinTextPrimary.copy(alpha = 0.08f)
        val textC = MinTextPrimary
        val secC = MinTextSecondary
        val pinColor = Color(0xFFFF9500)
        val eventColor = Color(0xFFFFB800)

        val saveNewNote: () -> Unit = {
            if (newNoteText.isNotBlank() || pendingAttachments.isNotEmpty()) {
                AppHapticManager.playClick()
                var calId: Long? = null
                if (newNoteIsEvent) {
                    calId = GoogleCalendarSync.syncEvent(
                        context,
                        Note(
                            subject = newNoteSubject,
                            text = newNoteText,
                            date = formatDate(selectedDay, selectedMonth, selectedYear),
                            time = "%02d:%02d".format(selectedHour, selectedMinute),
                            isEvent = true,
                            isPinned = newNoteIsPinned
                        ),
                        selectedDay, selectedMonth, selectedYear
                    )
                }
                val resolvedType = if (newNoteLessonType.isNotBlank()) {
                    newNoteLessonType
                } else {
                    val txt = newNoteText.uppercase()
                    when {
                        txt.contains("ЛР") || txt.contains("ЛАБ") || txt.contains("ЛАБОРАТОР") -> "ЛР"
                        txt.contains("ПЗ") || txt.contains("ПРАКТ") || txt.contains("СЕМИНАР") -> "ПЗ"
                        txt.contains("ЛК") || txt.contains("ЛЕКЦ") -> "ЛК"
                        else -> ""
                    }
                }
                val note = Note(
                    subject = newNoteSubject,
                    text = newNoteText,
                    date = formatDate(selectedDay, selectedMonth, selectedYear),
                    time = "%02d:%02d".format(selectedHour, selectedMinute),
                    isEvent = newNoteIsEvent,
                    isPinned = newNoteIsPinned,
                    lessonType = resolvedType,
                    attachments = pendingAttachments,
                    calendarEventId = calId
                )
                saveNotes(notes + note)
                if (newNoteIsEvent) {
                    scheduleAlarm(context, note, selectedDay, selectedMonth, selectedYear, selectedHour, selectedMinute)
                }
                newNoteText = ""
                pendingAttachments = emptyList()
                showNewNoteDialog = false
            }
        }

        val sheetChip = @Composable { label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, active: Boolean, tint: Color, onClick: () -> Unit ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) tint.copy(alpha = 0.16f) else chipBg)
                    .clickable { AppHapticManager.playClick(); onClick() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) Icon(icon, null, tint = if (active) tint else secC, modifier = Modifier.size(16.dp))
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (active) tint else textC, maxLines = 1)
            }
        }

        Dialog(
            onDismissRequest = { showNewNoteDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showNewNoteDialog = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(sheetBg)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { }
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(secC.copy(alpha = 0.35f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Отмена",
                            fontSize = 16.sp,
                            color = secC,
                            modifier = Modifier.clickable { showNewNoteDialog = false }.padding(vertical = 6.dp)
                        )
                        Text("Новая заметка", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textC)
                        Text(
                            "Готово",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (newNoteText.isNotBlank() || pendingAttachments.isNotEmpty()) MinAccent else secC.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { saveNewNote() }.padding(vertical = 6.dp)
                        )
                    }

                    androidx.compose.foundation.text.BasicTextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = textC, fontSize = 18.sp, lineHeight = 26.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MinAccent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp, max = 240.dp),
                        decorationBox = { inner ->
                            Box {
                                if (newNoteText.isEmpty()) {
                                    Text("Заметка", fontSize = 18.sp, color = secC.copy(alpha = 0.7f))
                                }
                                inner()
                            }
                        }
                    )

                    val nextNewTarget = remember(newNoteSubject, selectedGroup) {
                        findNextLessonForSubject(context, selectedGroup, newNoteSubject)
                    }
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sheetChip(
                            newNoteSubject,
                            if (newSubjectExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            true,
                            MinAccent
                        ) {
                            newSubjectExpanded = !newSubjectExpanded
                        }
                        sheetChip("%02d.%02d.%04d".format(selectedDay, selectedMonth + 1, selectedYear), Icons.Outlined.CalendarToday, false, MinAccent) {
                            showNewNoteDatePicker = true
                        }
                        sheetChip("%02d:%02d".format(selectedHour, selectedMinute), Icons.Outlined.AccessTime, false, MinAccent) {
                            showNewNoteTimePicker = true
                        }
                        if (nextNewTarget != null) {
                            val isNextApplied = selectedDay == nextNewTarget.day && selectedMonth == nextNewTarget.month0Based && selectedYear == nextNewTarget.year
                            sheetChip(
                                "К след. паре (${nextNewTarget.dateFormatted.take(5)})",
                                Icons.Outlined.EventAvailable,
                                isNextApplied,
                                MinAccent
                            ) {
                                selectedDay = nextNewTarget.day
                                selectedMonth = nextNewTarget.month0Based
                                selectedYear = nextNewTarget.year
                                selectedHour = nextNewTarget.hour
                                selectedMinute = nextNewTarget.minute
                                viewMonth = nextNewTarget.month0Based
                                viewYear = nextNewTarget.year
                            }
                        }
                    }

                    // Lesson type chips (ЛР, ЛК, ПЗ) for new note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Тип:", color = secC, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        listOf("" to "Обычная", "ЛР" to "ЛР", "ЛК" to "ЛК", "ПЗ" to "ПЗ").forEach { (typeVal, typeLabel) ->
                            val isSel = newNoteLessonType == typeVal
                            val badgeColor = when (typeVal) {
                                "ЛР" -> Color(0xFF34C759)
                                "ЛК" -> Color(0xFF007AFF)
                                "ПЗ" -> Color(0xFFFF9500)
                                else -> MinAccent
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) badgeColor.copy(alpha = 0.18f) else chipBg)
                                    .border(1.dp, if (isSel) badgeColor.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable {
                                        AppHapticManager.playClick()
                                        newNoteLessonType = typeVal
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    color = if (isSel) badgeColor else secC,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = newSubjectExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            allSubjects.forEach { s ->
                                val isSel = s == newNoteSubject
                                Text(
                                    s,
                                    color = if (isSel) MinAccent else textC,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) MinAccent.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable {
                                            AppHapticManager.playClick()
                                            newNoteSubject = s
                                            newSubjectExpanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sheetChip("Закрепить", Icons.Outlined.PushPin, newNoteIsPinned, pinColor) { newNoteIsPinned = !newNoteIsPinned }
                        sheetChip("Напомнить", if (newNoteIsEvent) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications, newNoteIsEvent, eventColor) { newNoteIsEvent = !newNoteIsEvent }
                        sheetChip(if (pendingAttachments.isEmpty()) "Файл" else "Файлы (${pendingAttachments.size})", Icons.Outlined.AttachFile, pendingAttachments.isNotEmpty(), MinAccent) {
                            newFilePickerLauncher.launch(arrayOf("*/*"))
                        }
                    }

                    if (pendingAttachments.isNotEmpty()) {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pendingAttachments.forEach { att ->
                                NotionAttachmentChip(
                                    attachment = att,
                                    isDarkTheme = isDarkTheme,
                                    onDelete = {
                                        AppHapticManager.playClick()
                                        pendingAttachments = pendingAttachments.filter { it.id != att.id }
                                    },
                                    onClick = { openAttachmentFile(context, att) }
                                )
                            }
                        }
                    }

                    val canSave = newNoteText.isNotBlank() || pendingAttachments.isNotEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (canSave) MinAccent else chipBg)
                            .clickable(enabled = canSave) { saveNewNote() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Сохранить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (canSave) MinCardBg else secC)
                    }
                }
            }
        }
    }

    if (showNewNoteDatePicker) {
        IosDatePickerDialog(
            initialDay = selectedDay,
            initialMonth = selectedMonth,
            initialYear = selectedYear,
            highlightedDates = highlightedDates,
            onDismiss = { showNewNoteDatePicker = false },
            onDateSelected = { d, m, y ->
                selectedDay = d
                selectedMonth = m
                selectedYear = y
                viewMonth = m
                viewYear = y
            },
            MinTextPrimary = MinTextPrimary,
            MinTextSecondary = MinTextSecondary,
            MinCardBg = MinCardBg,
            MinAccent = MinAccent,
            MinBg = MinBg
        )
    }

    if (showNewNoteTimePicker) {
        IosTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showNewNoteTimePicker = false },
            onTimeSelected = { h, m ->
                selectedHour = h
                selectedMinute = m
            },
            MinTextPrimary = MinTextPrimary,
            MinTextSecondary = MinTextSecondary,
            MinCardBg = MinCardBg,
            MinAccent = MinAccent
        )
    }
}

@Composable
fun AppleNotesEmptyState(
    isSearch: Boolean,
    isDarkTheme: Boolean,
    MinTextPrimary: Color,
    MinTextSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MinTextPrimary.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Outlined.SearchOff else Icons.Outlined.EditNote,
                contentDescription = null,
                tint = MinTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearch) "Ничего не найдено" else "Нет заметок",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MinTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isSearch) "Попробуйте изменить поисковый запрос" else "Нажмите «+», чтобы создать первую заметку",
            fontSize = 14.sp,
            color = MinTextSecondary,
            textAlign = TextAlign.Center
        )
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val monthNames = listOf("Январь","Февраль","Март","Апрель","Май","Июнь","Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
    val dayNames = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
    val cal = remember(viewYear, viewMonth) { java.util.Calendar.getInstance().apply { set(viewYear, viewMonth, 1) } }
    val firstDayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, MinBorder, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                AppHapticManager.playClick()
                onPrevMonth()
            }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = null, tint = MinTextPrimary, modifier = Modifier.size(20.dp))
            }
            Text("${monthNames[viewMonth]} $viewYear", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple))
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                AppHapticManager.playClick()
                onNextMonth()
            }, contentAlignment = Alignment.Center) {
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
        val todayCal = remember { java.util.Calendar.getInstance() }
        val curDay = todayCal.get(java.util.Calendar.DAY_OF_MONTH)
        val curMonth = todayCal.get(java.util.Calendar.MONTH)
        val curYear = todayCal.get(java.util.Calendar.YEAR)

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - firstDayOfWeek + 1
                    val isValid = dayNum in 1..daysInMonth
                    val dateStr = if (isValid) "%02d.%02d.%04d".format(dayNum, viewMonth + 1, viewYear) else ""
                    val isHighlighted = isValid && highlightedDates.contains(dateStr)
                    val isSelected = isValid && dayNum == selectedDay && viewMonth == selectedMonth && viewYear == selectedYear
                    val isToday = isValid && dayNum == curDay && viewMonth == curMonth && viewYear == curYear

                    val cellBg = when {
                        isSelected -> MinAccent
                        isHighlighted -> MinAccent.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape)
                            .background(cellBg)
                            .then(if (isValid) Modifier.clickable { onDaySelected(dayNum, viewMonth, viewYear) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isValid) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(
                                    dayNum.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected || isHighlighted || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MinBg else MinTextPrimary
                                )
                                if (isToday) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MinBg else MinAccent)
                                    )
                                } else if (isHighlighted && !isSelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
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
    var showNotifications by remember { mutableStateOf(false) }
    var showAnnouncements by remember { mutableStateOf(false) }
    var showDormitory by remember { mutableStateOf(false) }
    var showBenefits by remember { mutableStateOf(false) }
    var showPenalties by remember { mutableStateOf(false) }
    var showAbsences by remember { mutableStateOf(false) }
    var showHeadmanScreen by remember { mutableStateOf(false) }
    var showIisResumeScreen by remember { mutableStateOf(false) }
    var showReminderSettings by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var userFio by remember { mutableStateOf("Загрузка...") }
    var userDesc by remember { mutableStateOf("Загрузка...") }
    var userPhoto by remember { mutableStateOf("") }
    var userGroup by remember { mutableStateOf("Загрузка...") }
    var userBirthDate by remember { mutableStateOf("Не указана") }
    var showGroupDescToggle by remember { mutableStateOf(false) }

    val profilePrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var unreadNotifCount by remember { mutableIntStateOf(profilePrefs.getInt("cached_unread_notif_count", 0)) }
    var unexcusedHoursBadge by remember {
        val raw = profilePrefs.getInt("cached_unexcused_hours", 0)
        // Reset stale 6-hour default from previous build
        if (raw == 6 && !profilePrefs.getBoolean("absences_month_fixed_v2", false)) {
            profilePrefs.edit().putInt("cached_unexcused_hours", 0).putBoolean("absences_month_fixed_v2", true).apply()
            mutableIntStateOf(0)
        } else {
            mutableIntStateOf(raw)
        }
    }
    var isUserHeadman by remember { mutableStateOf(profilePrefs.getBoolean("cached_is_headman", false)) }
    var memoryAvatarBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Load saved avatar immediately
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val avatarFile = java.io.File(context.filesDir, "custom_avatar.png")
                if (avatarFile.exists()) {
                    val bmp = android.graphics.BitmapFactory.decodeFile(avatarFile.absolutePath)
                    if (bmp != null) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            memoryAvatarBitmap = bmp.asImageBitmap()
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val originalBmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                        if (originalBmp != null) {
                            val maxDim = 800
                            val width = originalBmp.width
                            val height = originalBmp.height
                            val scaledBmp = if (width > maxDim || height > maxDim) {
                                val ratio = width.toFloat() / height.toFloat()
                                val newW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                                val newH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                                android.graphics.Bitmap.createScaledBitmap(originalBmp, newW, newH, true)
                            } else {
                                originalBmp
                            }
                            val avatarFile = java.io.File(context.filesDir, "custom_avatar.png")
                            avatarFile.outputStream().use { out ->
                                scaledBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                            }
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                memoryAvatarBitmap = scaledBmp.asImageBitmap()
                                profilePrefs.edit().putLong("custom_avatar_timestamp", System.currentTimeMillis()).apply()
                                AppHapticManager.playClick()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(4000)
            showGroupDescToggle = !showGroupDescToggle
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val animatedPullOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isRefreshing) 72f else pullDistance.coerceIn(0f, 130f),
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    )

    suspend fun fetchProfileData(forceRefresh: Boolean = false) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""
        val cachedFio = prefs.getString("login_fio", null)
        val cachedGroup = prefs.getString("login_group", null)
        val cachedPhoto = prefs.getString("login_photo", null)
        val cachedDesc = prefs.getString("login_desc", null)

        if (token.isNotEmpty()) {
            if (!forceRefresh && cachedFio != null) {
                userFio = cachedFio
                userDesc = cachedDesc ?: cachedGroup ?: "БГУИР"
                userPhoto = cachedPhoto ?: ""
                userGroup = cachedGroup ?: "Неизвестно"
            }
        } else {
            userFio = "Гость"
            userDesc = "Вход не выполнен"
            userPhoto = ""
            userGroup = prefs.getString("selectedGroup", "БГУИР") ?: "БГУИР"
        }

        if (token.isNotEmpty()) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                fetchAndStoreStudentProfile(context)
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
                                val currentGroup = student.optString("group", cachedGroup ?: "")
                                
                                val newDesc = if (faculty.isNotEmpty()) "$faculty, $course курс" else if (currentGroup.isNotEmpty()) "Группа $currentGroup" else "Студент БГУИР"
                                prefs.edit().putString("login_fio", fio).putString("login_desc", newDesc).apply()
                                
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    userFio = fio
                                    userDesc = newDesc
                                    if (currentGroup.isNotEmpty()) userGroup = currentGroup
                                }
                            }
                        }
                    }
                } catch (e: Exception) {}

                // Also refresh group data from student-groups
                try {
                    val targetGrp = userGroup.ifBlank { cachedGroup ?: "" }
                    if (targetGrp.isNotBlank()) {
                        val client = com.example.schedule.NetworkClient.client
                        val request = NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/student-groups")
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val jsonArray = org.json.JSONArray(body)
                            for (i in 0 until jsonArray.length()) {
                                val groupObj = jsonArray.optJSONObject(i)
                                if (groupObj?.optString("name") == targetGrp) {
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
                    }
                } catch (e: Exception) {}
            }
        } else {
            userFio = "Гость"
            userDesc = "Войдите в аккаунт"
        }
    }

    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < 0 && pullDistance > 0f) {
                    val consumed = available.y
                    pullDistance = (pullDistance + available.y * 0.5f).coerceAtLeast(0f)
                    return androidx.compose.ui.geometry.Offset(0f, consumed)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    val newDistance = pullDistance + available.y * 0.45f
                    if (pullDistance < 70f && newDistance >= 70f) {
                        AppHapticManager.playClick()
                    }
                    pullDistance = newDistance
                    return available
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullDistance >= 70f && !isRefreshing) {
                    isRefreshing = true
                    AppHapticManager.playClick()
                    scope.launch {
                        fetchProfileData(forceRefresh = true)
                        kotlinx.coroutines.delay(500)
                        isRefreshing = false
                        pullDistance = 0f
                    }
                } else {
                    pullDistance = 0f
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchProfileData(forceRefresh = false)
        unreadNotifCount = fetchUnreadNotificationsCount(context)
        unexcusedHoursBadge = profilePrefs.getInt("cached_unexcused_hours", 0)
        ScheduleWidgetProvider.updateAllWidgets(context)
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = profilePrefs.getString("auth_token", "") ?: ""
                val myFio = profilePrefs.getString("login_fio", "") ?: ""
                var detectedHeadman = false

                fun checkHeadmanInGroupJson(jsonStr: String): Boolean {
                    try {
                        val root = org.json.JSONObject(jsonStr.trim())
                        val arr = root.optJSONArray("groupInfoStudentDto") ?: return false
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val fio = item.optString("fio", "").trim()
                            val pos = item.optString("position", "").trim()
                            val isMe = myFio.isNotBlank() && (
                                fio.equals(myFio, ignoreCase = true) ||
                                fio.contains(myFio, ignoreCase = true) ||
                                myFio.contains(fio, ignoreCase = true)
                            )
                            if (isMe) {
                                return pos.contains("Староста", ignoreCase = true) && !pos.contains("Зам", ignoreCase = true)
                            }
                        }
                    } catch (_: Exception) {}
                    return false
                }

                val cachedGrp = profilePrefs.getString("cached_user_group_info", null)
                if (!cachedGrp.isNullOrBlank()) {
                    detectedHeadman = checkHeadmanInGroupJson(cachedGrp)
                }

                if (token.isNotBlank()) {
                    try {
                        NetworkClient.executeWithAuth(
                            NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/student-groups/user-group-info", token),
                            context
                        ).use { resp ->
                            val b = resp.body?.string()
                            if (resp.isSuccessful && !b.isNullOrBlank()) {
                                profilePrefs.edit().putString("cached_user_group_info", b).apply()
                                detectedHeadman = checkHeadmanInGroupJson(b)
                            }
                        }
                    } catch (_: Exception) {}
                }

                profilePrefs.edit().putBoolean("cached_is_headman", detectedHeadman).apply()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isUserHeadman = detectedHeadman
                }
            } catch (_: Exception) {}
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
        androidx.activity.compose.BackHandler(onBack = {
            unexcusedHoursBadge = profilePrefs.getInt("cached_unexcused_hours", 0)
            showAbsences = false
        })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinAbsencesScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
                unexcusedHoursBadge = profilePrefs.getInt("cached_unexcused_hours", 0)
                showAbsences = false
            }
        }
    } else if (showGroupScreen) {
        androidx.activity.compose.BackHandler(onBack = { showGroupScreen = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinGroupScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
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
    } else if (showHeadmanScreen) {
        androidx.activity.compose.BackHandler(onBack = { showHeadmanScreen = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinHeadmanJournalScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
                showHeadmanScreen = false
            }
        }
    } else if (showIisResumeScreen) {
        androidx.activity.compose.BackHandler(onBack = { showIisResumeScreen = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinIisResumeScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
                showIisResumeScreen = false
            }
        }
    } else if (showNotifications) {
        androidx.activity.compose.BackHandler(onBack = {
            unreadNotifCount = 0
            showNotifications = false
        })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinNotificationsScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) {
                unreadNotifCount = 0
                showNotifications = false
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
            MinPenaltiesAndIncentivesScreen(MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) { showPenalties = false }
        }
    } else if (showDormitory) {
        androidx.activity.compose.BackHandler(onBack = { showDormitory = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinDormitoryAndPrivilegesScreen(0, MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) { showDormitory = false }
        }
    } else if (showBenefits) {
        androidx.activity.compose.BackHandler(onBack = { showBenefits = false })
        Box(modifier = Modifier.fillMaxSize().background(MinBg).zIndex(100f)) {
            MinDormitoryAndPrivilegesScreen(1, MinBg, MinCardBg, MinBorder, MinTextPrimary, MinTextSecondary, currentAccent, isDarkTheme) { showBenefits = false }
        }
    } else {
        val isDialogOpen = false
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .let { if (isDialogOpen) it.blur(16.dp) else it }
            ) {
                // Pull to refresh indicator at the top
                if (animatedPullOffset > 0f || isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(animatedPullOffset.dp)
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = MinTextPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            val rotation = (animatedPullOffset / 70f * 180f).coerceIn(0f, 180f)
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Потяните для обновления",
                                tint = if (animatedPullOffset >= 70f) MinTextPrimary else MinTextSecondary,
                                modifier = Modifier
                                    .size(26.dp)
                                    .rotate(rotation)
                            )
                        }
                    }
                }

    val styleType = LocalStyleType.current
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = animatedPullOffset.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 40.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    var isVibratingActive by remember { mutableStateOf(MaxVibrationController.isVibrating) }
                    var holdProgress by remember { mutableFloatStateOf(0f) }

                    val finalAvatar = memoryAvatarBitmap ?: remember(userPhoto) {
                        if (userPhoto.isNotEmpty()) {
                            try {
                                val base64String = if (userPhoto.contains(",")) userPhoto.substringAfter(",") else userPhoto
                                val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (e: Exception) { null }
                        } else null
                    }

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .border(2.dp, MinTextPrimary.copy(alpha = 0.35f), CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            val startTime = System.currentTimeMillis()
                                            var completed = false
                                            val holdJob = scope.launch {
                                                while (true) {
                                                    val elapsed = System.currentTimeMillis() - startTime
                                                    holdProgress = (elapsed / 5000f).coerceIn(0f, 1f)
                                                    if (elapsed >= 5000L) {
                                                        completed = true
                                                        val active = MaxVibrationController.toggle(context)
                                                        isVibratingActive = active
                                                        AppHapticManager.playClick()
                                                        break
                                                    }
                                                    kotlinx.coroutines.delay(30)
                                                }
                                                holdProgress = 0f
                                            }
                                            tryAwaitRelease()
                                            holdJob.cancel()
                                            holdProgress = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (finalAvatar != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = finalAvatar,
                                    contentDescription = "Аватар",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.avatar),
                                    contentDescription = "Аватар",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }

                            // Circular progress or glowing ring while holding or active
                            if (holdProgress > 0f) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                    drawArc(
                                        color = Color(0xFFFF3366),
                                        startAngle = -90f,
                                        sweepAngle = 360f * holdProgress,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEEEEE))
                                .clickable {
                                AppHapticManager.playClick()
                                launcher.launch("image/*")
                            },
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
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(userFio, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 27.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.animation.Crossfade(
                            targetState = showGroupDescToggle,
                            modifier = Modifier.fillMaxWidth(),
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
                        ) { showGroup ->
                            Text(
                                text = if (showGroup) "Группа $userGroup" else userDesc,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MinTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

        item {
            val acc = MinTextPrimary
            val lc = MinTextPrimary
            ProfileSection("", MinCardBg, MinTextSecondary) {
                ProfileRow(
                    "Уведомления",
                    Icons.Outlined.Notifications,
                    acc,
                    MinBorder,
                    lc,
                    MinTextSecondary,
                    value = if (unreadNotifCount > 0) "$unreadNotifCount новых" else null
                ) { showNotifications = true }
                ProfileRow(
                    "Учёба и справки",
                    Icons.Outlined.Description,
                    acc,
                    MinBorder,
                    lc,
                    MinTextSecondary
                ) { showStudyScreen = true }
                ProfileRow("Зачётка", Icons.Outlined.List, acc, MinBorder, lc, MinTextSecondary) { showGradebook = true }
                ProfileRow("Группа", Icons.Outlined.Person, acc, MinBorder, lc, MinTextSecondary, value = userGroup) { showGroupScreen = true }
                if (isUserHeadman) {
                    ProfileRow("Журнал старосты", Icons.Outlined.FactCheck, acc, MinBorder, lc, MinTextSecondary) { showHeadmanScreen = true }
                }
                ProfileRow("Пропуски", Icons.Outlined.DateRange, acc, MinBorder, lc, MinTextSecondary, value = if (unexcusedHoursBadge > 0) "$unexcusedHoursBadge ч" else null, isLast = true) { showAbsences = true }
            }
            Spacer(modifier = Modifier.height(14.dp))
            ProfileSection("", MinCardBg, MinTextSecondary) {
                ProfileRow("Резюме и виджет ИИС", Icons.Outlined.Badge, acc, MinBorder, lc, MinTextSecondary) { showIisResumeScreen = true }
                ProfileRow("Общежитие", Icons.Outlined.Home, acc, MinBorder, lc, MinTextSecondary) { showDormitory = true }
                ProfileRow("Льготы", Icons.Outlined.StarOutline, acc, MinBorder, lc, MinTextSecondary) { showBenefits = true }
                ProfileRow("Взыскания и поощрения", Icons.Outlined.WarningAmber, acc, MinBorder, lc, MinTextSecondary) { showPenalties = true }
                ProfileRow("Объявления", Icons.Outlined.Info, acc, MinBorder, lc, MinTextSecondary, isLast = true) { showAnnouncements = true }
            }
            Spacer(modifier = Modifier.height(14.dp))
            ProfileSection("", MinCardBg, MinTextSecondary) {
                ProfileRow("Кастомизация", Icons.Outlined.Settings, acc, MinBorder, lc, MinTextSecondary, isLast = true) { showCustomization = true }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            val localContext = androidx.compose.ui.platform.LocalContext.current
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Выйти",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF3B30),
                    modifier = Modifier
                        .clickable {
                            AppHapticManager.playClick()
                            val prefs = localContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().clear().apply()
                            val activity = localContext as? android.app.Activity
                            activity?.finish()
                            localContext.startActivity(android.content.Intent(localContext, MainActivity::class.java))
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                )
                Text(
                    "GitHub",
                    fontSize = 13.sp,
                    color = MinTextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        AppHapticManager.playClick()
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Elate11/IIS_BSUIR_Shedule_app"))
                        localContext.startActivity(intent)
                    }.padding(8.dp)
                )
            }
        }
        }
            }

            if (showReminderSettings) {
                var selectedTiming by remember {
                    mutableStateOf(profilePrefs.getString("event_reminder_timing", "1_day_before_18") ?: "1_day_before_18")
                }
                val timingOptions = listOf(
                    "1_day_before_18" to "За 1 день до события (в 18:00)",
                    "1_day_before_21" to "За 1 день до события (в 21:00)",
                    "same_day_08" to "В день события (в 08:00)",
                    "same_day_09" to "В день события (в 09:00)",
                    "2_hours_before" to "За 2 часа до события",
                    "1_hour_before" to "За 1 час до события",
                    "15_min_before" to "За 15 минут до события"
                )

                val dialogBg = MinCardBg
                val dialogBorder = MinBorder

                Dialog(onDismissRequest = { showReminderSettings = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = if (isDarkTheme) 24.dp else 12.dp, shape = RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(dialogBg)
                            .border(1.dp, dialogBorder, RoundedCornerShape(24.dp))
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(currentAccent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.NotificationsActive,
                                            contentDescription = null,
                                            tint = currentAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        "Напоминания",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MinTextPrimary
                                    )
                                }
                                IconButton(onClick = { showReminderSettings = false }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Закрыть", tint = MinTextSecondary)
                                }
                            }

                            Text(
                                "Выберите, за какое время до события отправлять push-уведомление:",
                                fontSize = 14.sp,
                                color = MinTextSecondary,
                                lineHeight = 19.sp
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                timingOptions.forEach { (key, label) ->
                                    val isSelected = key == selectedTiming
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.Transparent)
                                            .border(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) MinTextPrimary else dialogBorder,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                AppHapticManager.playClick()
                                                selectedTiming = key
                                                profilePrefs.edit().putString("event_reminder_timing", key).apply()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MinTextPrimary else MinTextSecondary
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MinTextPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(currentAccent.copy(alpha = if (isDarkTheme) 0.20f else 0.15f))
                                    .border(1.dp, currentAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        AppHapticManager.playClick()
                                        showReminderSettings = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Готово", color = currentAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


data class DeanNotificationItem(
    val id: Long,
    val message: String,
    val date: String,
    val type: String, // INFO, SUCCESS, FAILURE, etc.
    val isViewed: Boolean = true
)

@Composable
fun MinNotificationsScreen(
    MinBg: androidx.compose.ui.graphics.Color,
    MinCardBg: androidx.compose.ui.graphics.Color,
    MinBorder: androidx.compose.ui.graphics.Color,
    MinTextPrimary: androidx.compose.ui.graphics.Color,
    MinTextSecondary: androidx.compose.ui.graphics.Color,
    currentAccent: androidx.compose.ui.graphics.Color,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    var notifications by remember { mutableStateOf<List<DeanNotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Все") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    fun parseNotifications(jsonStr: String): List<DeanNotificationItem> {
        val list = mutableListOf<DeanNotificationItem>()
        try {
            val root = org.json.JSONObject(jsonStr.trim())
            val arr = root.optJSONArray("notifications") ?: root.optJSONArray("items") ?: root.optJSONArray("content")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optLong("id", i.toLong())
                    val msg = obj.optString("message", "").trim()
                    val date = obj.optString("date", "").trim()
                    val type = obj.optString("type", "INFO").trim()
                    val isViewed = obj.optBoolean("isViewed", true)
                    if (msg.isNotBlank()) {
                        list.add(DeanNotificationItem(id, msg, date, type, isViewed))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    suspend fun fetchNotifications() {
        val token = prefs.getString("auth_token", "") ?: ""
        val url = "https://iis.bsuir.by/api/v1/notifications?pageNumber=0&pageSize=50"

        // 1. Cached
        val cached = prefs.getString("cached_dean_notifications", null)
        if (!cached.isNullOrBlank() && notifications.isEmpty()) {
            val parsed = parseNotifications(cached)
            if (parsed.isNotEmpty()) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    notifications = parsed
                    isLoading = false
                }
            }
        }

        // 2. Network
        if (token.isNotBlank()) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = com.example.schedule.NetworkClient.client
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .addHeader("Cookie", token)
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .get()
                        .build()
                    client.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: ""
                            if (body.isNotBlank()) {
                                prefs.edit().putString("cached_dean_notifications", body).apply()
                                val parsed = parseNotifications(body)
                                val unreadIds = parsed.filter { !it.isViewed }.map { it.id }
                                if (unreadIds.isNotEmpty()) {
                                    markDeanNotificationsAsRead(context, unreadIds)
                                } else {
                                    prefs.edit().putInt("cached_unread_notif_count", 0).apply()
                                }
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    notifications = parsed
                                    isLoading = false
                                    isRefreshing = false
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isLoading = false
                        isRefreshing = false
                    }
                }
            }
        } else {
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        fetchNotifications()
    }

    val filters = listOf("Все", "Инфо", "Успешно", "Важное")

    val filteredList = remember(notifications, selectedFilter, searchQuery) {
        var list = when (selectedFilter) {
            "Инфо" -> notifications.filter { it.type.equals("INFO", ignoreCase = true) }
            "Успешно" -> notifications.filter { it.type.equals("SUCCESS", ignoreCase = true) }
            "Важное" -> notifications.filter { it.type.equals("FAILURE", ignoreCase = true) || it.type.equals("WARNING", ignoreCase = true) }
            else -> notifications
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { it.message.lowercase().contains(q) || it.date.contains(q) }
        }
        list
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MinBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 36.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = androidx.compose.ui.Modifier.size(32.dp).clickable { onBack() }
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    androidx.compose.material3.Text(
                        "Уведомления",
                        fontSize = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MinTextPrimary
                    )
                    androidx.compose.material3.Text(
                        "Деканат и ректорат • ${notifications.size}",
                        fontSize = 13.sp,
                        color = MinTextSecondary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Outlined.Refresh,
                    contentDescription = "Обновить",
                    tint = if (isRefreshing) currentAccent else MinTextSecondary,
                    modifier = androidx.compose.ui.Modifier.size(24.dp).clickable {
                        if (!isRefreshing) {
                            isRefreshing = true
                            isLoading = true
                            scope.launch { fetchNotifications() }
                        }
                    }
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(14.dp))

            // Search Bar
            if (notifications.isNotEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MinCardBg)
                        .border(1.dp, MinBorder, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MinTextSecondary,
                            modifier = androidx.compose.ui.Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(10.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MinTextPrimary,
                                fontSize = 15.sp
                            ),
                            singleLine = true,
                            modifier = androidx.compose.ui.Modifier.weight(1f).padding(vertical = 8.dp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    androidx.compose.material3.Text(
                                        "Поиск",
                                        color = MinTextSecondary.copy(alpha = 0.6f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Outlined.Close,
                                contentDescription = "Очистить",
                                tint = MinTextSecondary,
                                modifier = androidx.compose.ui.Modifier.size(18.dp).clickable { searchQuery = "" }
                            )
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(10.dp))

                // Filter chips
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(filters.size) { idx ->
                        val f = filters[idx]
                        AppleOptionChip(
                            label = f,
                            isSelected = selectedFilter == f,
                            onClick = {
                                AppHapticManager.playClick()
                                selectedFilter = f
                            },
                            isDarkTheme = isDarkTheme,
                            accentColor = currentAccent,
                            unselectedBg = MinCardBg,
                            unselectedTextColor = MinTextPrimary,
                            selectedTextColor = MinCardBg,
                            borderColorOverride = MinBorder
                        )
                    }
                }
            }
        }

        if (isLoading && notifications.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = currentAccent)
                }
            }
        } else if (filteredList.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        if (searchQuery.isNotEmpty()) "Ничего не найдено" else "Уведомлений нет",
                        color = MinTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        } else {
            items(filteredList.size) { index ->
                val notif = filteredList[index]
                val (typeLabel, typeColor) = when {
                    notif.type.equals("SUCCESS", ignoreCase = true) -> Pair("Успешно", if (isDarkTheme) Color(0xFF4CC38A) else Color(0xFF16A34A))
                    notif.type.equals("FAILURE", ignoreCase = true) -> Pair("Важное", if (isDarkTheme) Color(0xFFE5484D) else Color(0xFFDC2626))
                    notif.type.equals("WARNING", ignoreCase = true) -> Pair("Внимание", Color(0xFFFF9500))
                    else -> Pair("Инфо", currentAccent)
                }

                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    // Top Row: Type Pill & Date
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            typeLabel,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = typeColor
                        )

                        if (notif.date.isNotBlank()) {
                            androidx.compose.material3.Text(
                                notif.date,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = MinTextSecondary
                            )
                        }
                    }

                    // Notification Message Body
                    androidx.compose.material3.Text(
                        notif.message,
                        fontSize = 14.5.sp,
                        color = MinTextPrimary,
                        lineHeight = 21.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                    )

                    if (index < filteredList.size - 1) {
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                        androidx.compose.material3.HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
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
                androidx.compose.material3.Text(title, fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = MinTextPrimary)
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
                                androidx.compose.material3.Text(date, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = MinTextPrimary)
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
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
                androidx.compose.material3.Text("Кастомизация", fontSize = 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = MinTextPrimary)
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }

        item {
            androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    AutoResizedText("Тема", fontSize = 19.sp, minFontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary, maxLines = 1)
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(48.dp).border(1.dp, MinTextSecondary.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(if (isDarkTheme) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White).clickable {
                        AppHapticManager.playClick()
                        AppHapticManager.playClick()
                        onThemeToggle()
                    })
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    AutoResizedText(if (isDarkTheme) "Темная" else "Светлая", fontSize = 17.sp, minFontSize = 10.sp, color = MinTextSecondary, maxLines = 1)
                }

                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    AutoResizedText("Основной", fontSize = 19.sp, minFontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary, maxLines = 1)
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(48.dp).border(1.dp, MinTextSecondary.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(MinTextPrimary).clickable {
                        AppHapticManager.playClick()
                        showPrimaryStrip = true
                    })
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    AutoResizedText("Сброс", fontSize = 17.sp, minFontSize = 10.sp, color = MinTextSecondary, maxLines = 1, modifier = androidx.compose.ui.Modifier.clickable {
                        AppHapticManager.playClick()
                        onPrimaryColorChange(null)
                    })
                }
    
                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    AutoResizedText("Фон", fontSize = 19.sp, minFontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MinTextPrimary, maxLines = 1)
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(48.dp).border(1.dp, MinTextSecondary.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(MinBg).clickable {
                        AppHapticManager.playClick()
                        showBackgroundStrip = true
                    })
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    AutoResizedText("Сброс", fontSize = 17.sp, minFontSize = 10.sp, color = MinTextSecondary, maxLines = 1, modifier = androidx.compose.ui.Modifier.clickable {
                        AppHapticManager.playClick()
                        onBackgroundColorChange(null)
                    })
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
            androidx.compose.material3.HorizontalDivider(color = MinBorder, thickness = 1.dp)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }


        item {
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
                                AppHapticManager.playClick()
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
                                AppHapticManager.playClick()
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
                        onClick = {
                            AppHapticManager.playClick()
                            onBgModeChange(mode)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = if (bgMode == mode) MinTextPrimary else MinTextSecondary
                        )
                    ) {
                        Text(name, fontWeight = if (bgMode == mode) FontWeight.SemiBold else FontWeight.Bold)
                    }
                }
            }
            
            if (bgMode == "Gallery" || bgMode == "Gradient") {
                Spacer(modifier = Modifier.height(16.dp))
                if (bgMode == "Gallery") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        if (uri != null) {
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val bgFile = java.io.File(context.filesDir, "custom_background.png")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        bgFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        onBgImageUriChange(bgFile.absolutePath)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        onBgImageUriChange(uri.toString())
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = {
                        AppHapticManager.playClick()
                        launcher.launch("image/*")
                    }, border = androidx.compose.foundation.BorderStroke(2.dp, MinTextPrimary), colors = ButtonDefaults.outlinedButtonColors(contentColor = MinTextPrimary)) { Text("Выбрать фото", color = MinTextPrimary, fontWeight = FontWeight.Bold) }
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
            val reminderContext = androidx.compose.ui.platform.LocalContext.current
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            val reminderPrefs = remember { reminderContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
            var selectedTiming by remember {
                mutableStateOf(reminderPrefs.getString("event_reminder_timing", "15_min_before") ?: "15_min_before")
            }
            var customHours by remember {
                mutableIntStateOf(reminderPrefs.getInt("custom_reminder_hours", 3))
            }
            val timingOptions = listOf(
                "15_min_before" to "За 15 мин",
                "1_hour_before" to "За 1 час",
                "2_hours_before" to "За 2 часа",
                "1_day_before_18" to "За 1 день",
                "custom_hours" to "Свой вариант"
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Напоминания о событиях", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Carousel of options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timingOptions.forEach { (key, label) ->
                    val isSelected = key == selectedTiming
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Transparent)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MinTextPrimary else MinBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                AppHapticManager.playClick()
                                selectedTiming = key
                                reminderPrefs.edit().putString("event_reminder_timing", key).apply()
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AutoResizedText(
                            label,
                            fontSize = 15.sp,
                            minFontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MinTextPrimary else MinTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            if (selectedTiming == "custom_hours") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Уведомить за (часов):", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MinBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (customHours > 1) {
                                        AppHapticManager.playClick()
                                        customHours--
                                        reminderPrefs.edit().putInt("custom_reminder_hours", customHours).apply()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = MinTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .padding(horizontal = 12.dp)
                                .border(1.dp, MinTextPrimary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$customHours ч.", color = MinTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MinBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (customHours < 72) {
                                        AppHapticManager.playClick()
                                        customHours++
                                        reminderPrefs.edit().putInt("custom_reminder_hours", customHours).apply()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = MinTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Test notification button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Transparent)
                    .border(1.dp, MinTextPrimary, RoundedCornerShape(14.dp))
                    .clickable {
                        AppHapticManager.playClick()
                        val textMsg = if (selectedTiming == "custom_hours") "Напоминание сработает за $customHours ч. до события!" else "Напоминание сработает за выбранное время!"
                        val testIntent = android.content.Intent(reminderContext, NoteReminderReceiver::class.java).apply {
                            putExtra("SUBJECT", "Тестовое событие")
                            putExtra("TEXT", textMsg)
                            putExtra("IS_EVENT", true)
                        }
                        reminderContext.sendBroadcast(testIntent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MinTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Проверить уведомление",
                        color = MinTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MinBorder, thickness = 1.dp)
        }

                item {
            val customPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
            var vibEnabled by remember { mutableStateOf(customPrefs.getBoolean("vibration_enabled", true)) }
            var vibStrength by remember { mutableStateOf(customPrefs.getFloat("vibration_strength", 0.6f)) }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MinBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Виброотклик", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Включить вибрацию", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary)
                Switch(
                    checked = vibEnabled,
                    onCheckedChange = { active ->
                        vibEnabled = active
                        AppHapticManager.isEnabled = active
                        customPrefs.edit().putBoolean("vibration_enabled", active).apply()
                        if (active) AppHapticManager.playClick()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinBg,
                        checkedTrackColor = MinTextPrimary,
                        uncheckedThumbColor = MinTextPrimary,
                        uncheckedTrackColor = MinTextSecondary.copy(alpha = 0.3f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            if (vibEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                val strengthLabel = when {
                    vibStrength < 0.35f -> "Слабая"
                    vibStrength < 0.7f -> "Средняя"
                    else -> "Сильная"
                }
                Text("Сила вибрации: $strengthLabel (${(vibStrength * 100).toInt()}%)", fontSize = 19.sp, color = MinTextSecondary)
                Slider(
                    value = vibStrength,
                    onValueChange = { valValue ->
                        vibStrength = valValue
                        AppHapticManager.strength = valValue
                        customPrefs.edit().putFloat("vibration_strength", valValue).apply()
                    },
                    onValueChangeFinished = {
                        AppHapticManager.playClick()
                    },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MinTextPrimary,
                        activeTrackColor = MinTextPrimary,
                        inactiveTrackColor = MinBorder
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = { AppHapticManager.playClick() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MinBorder)
                ) {
                    Text("Проверить вибрацию", color = MinTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
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
            val introPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
            var introVideoEnabled by remember { mutableStateOf(introPrefs.getBoolean("intro_video_enabled", true)) }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Заставка", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Интро при входе",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MinTextPrimary,
                    style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple)
                )
                Switch(
                    checked = introVideoEnabled,
                    onCheckedChange = { enabled ->
                        AppHapticManager.playClick()
                        introVideoEnabled = enabled
                        introPrefs.edit().putBoolean("intro_video_enabled", enabled).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinBg,
                        checkedTrackColor = MinTextPrimary,
                        uncheckedThumbColor = MinTextPrimary,
                        uncheckedTrackColor = MinTextSecondary.copy(alpha = 0.3f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    AppHapticManager.playClick()
                    IntroVideoPreviewTrigger.requestReplay = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MinBorder)
            ) {
                Text("Посмотреть интро", color = MinTextPrimary, fontWeight = FontWeight.Bold)
            }

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
                    onCheckedChange = {
                    AppHapticManager.playClick()
                    onTransitionsToggle(it)
                },
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
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val allTypes = TransitionType.values().toList()
                    val chunkedTypes = allTypes.chunked(3)
                    chunkedTypes.forEach { rowTypes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            rowTypes.forEach { type ->
                                TransitionDemoBox(type, transitionSpeedMultiplier, isSelected = (transitionType == type), onClick = { onTransitionTypeChange(type) }, MinTextPrimary, MinBorder)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

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
             .clickable {
             AppHapticManager.playClick()
             onClick()
         }
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
        AutoResizedText(type.title, fontSize = 17.sp, minFontSize = 10.sp, color = if(isSelected) MinTextPrimary else MinBorder, fontWeight = FontWeight.Bold, maxLines = 1)
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
fun ProfileSection(title: String, cardBg: Color, textSecondary: Color, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        content = content
    )
}

@Composable
fun ProfileRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, divider: Color, textPrimary: Color, textSecondary: Color, isLast: Boolean = false, value: String? = null, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable { AppHapticManager.playClick(); onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textPrimary, modifier = Modifier.weight(1f))
            if (value != null) {
                Text(value, fontSize = 15.sp, color = textSecondary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
        if (!isLast) {
            androidx.compose.material3.HorizontalDivider(
                color = divider.copy(alpha = 0.3f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 64.dp)
            )
        }
    }
}

@Composable
fun MinStat(label: String, value: String, MinTextPrimary: Color, MinTextSecondary: Color, onClick: (() -> Unit)? = null) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable {
             AppHapticManager.playClick()
             onClick()
         }.padding(horizontal=16.dp, vertical=8.dp) else Modifier.padding(horizontal=16.dp, vertical=8.dp)) {
        AutoResizedText(label, fontSize = 17.sp, minFontSize = 11.sp, fontWeight = FontWeight.Bold, color = MinTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        AutoResizedText(value, fontSize = 29.sp, minFontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
    }
}

@Composable
fun MinListAction(label: String, MinBorder: Color, MinTextPrimary: Color, MinTextSecondary: Color, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, isLast: Boolean = false, onClick: () -> Unit = {}) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val padding = Modifier.padding(vertical = 16.dp)
    Column(modifier = Modifier.fillMaxWidth().clickable { 
        AppHapticManager.playClick()
        onClick() 
    }.then(padding)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MinTextPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                }
                AutoResizedText(label, fontSize = 24.sp, minFontSize = 14.sp, fontWeight = FontWeight.Bold, color = MinTextPrimary, style = androidx.compose.material3.LocalTextStyle.current.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Simple))
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Canvas(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).pointerInput(Unit) {
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
                    
                    val x = (hue / 360f) * size.width
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 4.dp.toPx()
                    )
                    drawLine(
                        color = Color.Black,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).pointerInput(Unit) {
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
                    
                    val fraction = if (saturation >= 0.99f) value / 2f else 0.5f + (1f - saturation) / 2f
                    val x = fraction * size.width
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 4.dp.toPx()
                    )
                    drawLine(
                        color = Color.Black,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("ОТМЕНА", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onDismiss() }.padding(12.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ВЫБРАТЬ", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).clickable {
                        onColorSelected(currentColor)
                        onDismiss()
                    }.padding(horizontal = 16.dp, vertical = 12.dp))
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
                fontWeight = FontWeight.SemiBold,
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

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
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) AppleTokens.Tile1 else AppleTokens.Parchment)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "ИИС БГУИР",
                style = AppleTypography.captionStrong(),
                color = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Личный кабинет",
                style = AppleTypography.displayLg(),
                color = if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))

            OutlinedTextField(
                value = gradebookNumber,
                onValueChange = { gradebookNumber = it },
                label = { Text("Номер зачетки", style = AppleTypography.caption()) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                textStyle = AppleTypography.bodyStrong().copy(fontSize = 18.sp, color = if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink,
                    unfocusedTextColor = if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink,
                    focusedBorderColor = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue,
                    unfocusedBorderColor = if (isDarkTheme) AppleTokens.HairlineDark else AppleTokens.HairlineLight,
                    focusedContainerColor = if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.CanvasWhite,
                    unfocusedContainerColor = if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.CanvasWhite,
                    focusedLabelColor = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue,
                    unfocusedLabelColor = if (isDarkTheme) AppleTokens.InkSecondary else AppleTokens.InkMuted48
                ),
                singleLine = true,
                shape = RoundedCornerShape(AppleTokens.RadiusLg)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль", style = AppleTypography.caption()) },
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                trailingIcon = {
                    val icon = if (passwordVisible) androidx.compose.material.icons.Icons.Outlined.Visibility else androidx.compose.material.icons.Icons.Outlined.VisibilityOff
                    androidx.compose.material3.IconButton(onClick = {
                        AppHapticManager.playClick()
                        passwordVisible = !passwordVisible
                    }) {
                        androidx.compose.material3.Icon(icon, contentDescription = null, tint = if (isDarkTheme) AppleTokens.InkSecondary else AppleTokens.InkMuted48)
                    }
                },
                textStyle = AppleTypography.bodyStrong().copy(fontSize = 18.sp, color = if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink,
                    unfocusedTextColor = if (isDarkTheme) AppleTokens.BodyOnDark else AppleTokens.Ink,
                    focusedBorderColor = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue,
                    unfocusedBorderColor = if (isDarkTheme) AppleTokens.HairlineDark else AppleTokens.HairlineLight,
                    focusedContainerColor = if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.CanvasWhite,
                    unfocusedContainerColor = if (isDarkTheme) AppleTokens.Tile2 else AppleTokens.CanvasWhite,
                    focusedLabelColor = if (isDarkTheme) AppleTokens.SkyLinkBlue else AppleTokens.ActionBlue,
                    unfocusedLabelColor = if (isDarkTheme) AppleTokens.InkSecondary else AppleTokens.InkMuted48
                ),
                singleLine = true,
                shape = RoundedCornerShape(AppleTokens.RadiusLg)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color(0xFFFF453A), style = AppleTypography.caption(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
            }

            AppleButtonPrimary(
                text = if (isLoading) "Вход..." else "Войти",
                onClick = {
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    if (gradebookNumber.isBlank() || password.isBlank()) {
                        errorMessage = "Пожалуйста, заполните все поля"
                        return@AppleButtonPrimary
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
                                        var extractedToken = ""
                                        if (responseBody != null) {
                                            try {
                                                val json = org.json.JSONObject(responseBody)
                                                val lgGroup = json.optString("group", "").ifBlank {
                                                    json.optJSONObject("studentGroup")?.optString("name", "") ?: ""
                                                }
                                                prefs.edit().apply {
                                                    putString("login_fio", json.optString("fio"))
                                                    if (lgGroup.isNotBlank()) {
                                                        putString("login_group", lgGroup)
                                                        putString("selectedGroup", lgGroup)
                                                    }
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
                                        prefs.edit()
                                            .putString("saved_password", password)
                                            .putString("gradebook_number", gradebookNumber)
                                            .apply()
                                        fetchAndStoreStudentProfile(context)
                                        onLoginSuccess(gradebookNumber, extractedToken)
                                    } else {
                                        when (response.code) {
                                            400, 401, 403, 404 -> errorMessage = "Неверный логин или пароль"
                                            500, 502, 503 -> errorMessage = "Ошибка сервера (${response.code})"
                                            else -> errorMessage = "Ошибка: ${response.code}"
                                        }
                                    }
                                }
                            }
                        } catch (e: java.io.IOException) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                errorMessage = "Отсутствует подключение к сети"
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                errorMessage = "Ошибка: ${e.localizedMessage}"
                            }
                        }
                    }
                },
                enabled = !isLoading,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            AppleButtonSecondaryPill(
                text = "Продолжить без входа",
                onClick = {
                    onLoginSuccess("310101", "")
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
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

fun Modifier.liquidGlassEffect(
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    tint: Color = Color.Unspecified,
    accentColor: Color = Color(0xFF6C63FF),
    isDarkTheme: Boolean = true,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    isActive: Boolean = true
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
                topLeft = androidx.compose.ui.geometry.Offset(-1.5.dp.toPx() + tiltX * 4.dp.toPx(), -1.5.dp.toPx() + tiltY * 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(w + 3.dp.toPx(), h + 3.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r + 1.5.dp.toPx(), r + 1.5.dp.toPx())
            )

            // 2. Optical Liquid Glass Body
            val glassBodyBrush = androidx.compose.ui.graphics.Brush.linearGradient(
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
                start = androidx.compose.ui.geometry.Offset(tiltX * 15f, tiltY * 15f),
                end = androidx.compose.ui.geometry.Offset(w - tiltX * 15f, h - tiltY * 15f)
            )
            drawRoundRect(
                brush = glassBodyBrush,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
            )

            // 3. Draw Inner Content
            drawContent()

            // 4. Specular Highlight & Caustic Lens Reflection
            val highlightAngle = kotlin.math.atan2(tiltY, tiltX)
            val normalX = kotlin.math.cos(highlightAngle)
            val normalY = kotlin.math.sin(highlightAngle)
            
            val causticBrush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDarkTheme) 0.65f else 0.85f),
                    Color.White.copy(alpha = if (isDarkTheme) 0.20f else 0.35f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(w / 2f + normalX * (w * 0.25f), h / 2f + normalY * (h * 0.25f)),
                radius = maxOf(w, h) * 0.75f
            )
            drawRoundRect(
                brush = causticBrush,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                blendMode = androidx.compose.ui.graphics.BlendMode.Plus
            )

            // 5. Prismatic Diamond-Cut Bevel Rim
            val hueShift = ((highlightAngle * 180 / Math.PI + 360) % 360).toFloat()
            val prismColor = Color.hsl(hueShift, 0.70f, if (isDarkTheme) 0.75f else 0.55f)
            val rimBrush = androidx.compose.ui.graphics.Brush.sweepGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.90f),
                    prismColor.copy(alpha = 0.60f),
                    Color.White.copy(alpha = 0.35f),
                    prismColor.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.90f)
                ),
                center = androidx.compose.ui.geometry.Offset(w / 2f + tiltX * 10f, h / 2f + tiltY * 10f)
            )
            drawRoundRect(
                brush = rimBrush,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }
)

object MaxVibrationController {
    var isVibrating = false
        private set

    fun toggle(context: android.content.Context): Boolean {
        return if (isVibrating) {
            stop(context)
            false
        } else {
            start(context)
            true
        }
    }

    fun start(context: android.content.Context) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 1000, 10)
                val amplitudes = intArrayOf(0, 255, 0)
                val effect = android.os.VibrationEffect.createWaveform(pattern, amplitudes, 1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 1000, 10)
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 1)
            }
            isVibrating = true
        }
    }

    fun stop(context: android.content.Context) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
        vibrator?.cancel()
        isVibrating = false
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IosWheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    MinTextPrimary: androidx.compose.ui.graphics.Color,
    MinTextSecondary: androidx.compose.ui.graphics.Color
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)
    val view = androidx.compose.ui.platform.LocalView.current
    
    var lastVibratedIndex by remember { mutableStateOf(initialIndex) }
    
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { offset ->
                val layoutInfo = listState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height
                val center = viewportHeight / 2
                
                var closestItem: androidx.compose.foundation.lazy.LazyListItemInfo? = null
                var minDistance = Int.MAX_VALUE
                
                for (item in layoutInfo.visibleItemsInfo) {
                    val itemCenter = item.offset + item.size / 2
                    val distance = kotlin.math.abs(itemCenter - center)
                    if (distance < minDistance) {
                        minDistance = distance
                        closestItem = item
                    }
                }
                
                closestItem?.let {
                    val realIndex = it.index - 2
                    if (realIndex >= 0 && realIndex < items.size && realIndex != lastVibratedIndex) {
                        AppHapticManager.playClick()
                        lastVibratedIndex = realIndex
                        onItemSelected(realIndex)
                    }
                }
            }
    }
    
    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = Modifier.height(150.dp).width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items.size + 4) { index ->
            val itemIndex = index - 2
            val text = if (itemIndex in items.indices) items[itemIndex] else ""
            val isSelected = itemIndex == lastVibratedIndex
            Box(
                modifier = Modifier.height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text,
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MinTextPrimary else MinTextSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun IosTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    MinTextPrimary: androidx.compose.ui.graphics.Color,
    MinTextSecondary: androidx.compose.ui.graphics.Color,
    MinCardBg: androidx.compose.ui.graphics.Color,
    MinAccent: androidx.compose.ui.graphics.Color
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val pickerText = MinTextPrimary

    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val dialogWindowProvider = androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
        LaunchedEffect(dialogWindowProvider) {
            dialogWindowProvider?.window?.let { window ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 64
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MinCardBg)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Выберите время",
                    color = pickerText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosWheelPicker(
                        items = (0..23).map { "%02d".format(it) },
                        initialIndex = initialHour,
                        onItemSelected = { selectedHour = it },
                        MinTextPrimary = MinTextPrimary,
                        MinTextSecondary = MinTextSecondary
                    )
                    Text(" : ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pickerText, modifier = Modifier.padding(horizontal = 8.dp))
                    IosWheelPicker(
                        items = (0..59).map { "%02d".format(it) },
                        initialIndex = initialMinute,
                        onItemSelected = { selectedMinute = it },
                        MinTextPrimary = MinTextPrimary,
                        MinTextSecondary = MinTextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            AppHapticManager.playClick()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pickerText.copy(alpha = 0.08f),
                            contentColor = pickerText
                        )
                    ) {
                        AutoResizedText("Отмена", fontWeight = FontWeight.Bold, minFontSize = 10.sp)
                    }
                    
                    Button(
                        onClick = { 
                            onTimeSelected(selectedHour, selectedMinute)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MinAccent,
                            contentColor = MinCardBg
                        )
                    ) {
                        AutoResizedText("Готово", fontWeight = FontWeight.Bold, minFontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun IosDatePickerDialog(
    initialDay: Int,
    initialMonth: Int,
    initialYear: Int,
    highlightedDates: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit,
    MinTextPrimary: androidx.compose.ui.graphics.Color,
    MinTextSecondary: androidx.compose.ui.graphics.Color,
    MinCardBg: androidx.compose.ui.graphics.Color,
    MinAccent: androidx.compose.ui.graphics.Color,
    MinBg: androidx.compose.ui.graphics.Color
) {
    val pickerText = MinTextPrimary
    var tempDay by remember { mutableStateOf(initialDay) }
    var tempMonth by remember { mutableStateOf(initialMonth) }
    var tempYear by remember { mutableStateOf(initialYear) }
    var viewMonth by remember { mutableStateOf(initialMonth) }
    var viewYear by remember { mutableStateOf(initialYear) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val dialogWindowProvider = androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
        LaunchedEffect(dialogWindowProvider) {
            dialogWindowProvider?.window?.let { window ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 64
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MinCardBg)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Выберите дату",
                    color = pickerText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                MinNotesCalendar(
                    viewMonth = viewMonth,
                    viewYear = viewYear,
                    selectedDay = tempDay,
                    selectedMonth = tempMonth,
                    selectedYear = tempYear,
                    highlightedDates = highlightedDates,
                    MinTextPrimary = MinTextPrimary,
                    MinTextSecondary = MinTextSecondary,
                    MinBorder = Color.Transparent,
                    MinAccent = MinAccent,
                    MinBg = MinBg,
                    onDaySelected = { d, m, y ->
                        AppHapticManager.playClick()
                        tempDay = d
                        tempMonth = m
                        tempYear = y
                    },
                    onPrevMonth = { if (viewMonth == 0) { viewMonth = 11; viewYear-- } else viewMonth-- },
                    onNextMonth = { if (viewMonth == 11) { viewMonth = 0; viewYear++ } else viewMonth++ }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            AppHapticManager.playClick()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pickerText.copy(alpha = 0.08f),
                            contentColor = pickerText
                        )
                    ) {
                        AutoResizedText("Отмена", fontWeight = FontWeight.Bold, minFontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            AppHapticManager.playClick()
                            onDateSelected(tempDay, tempMonth, tempYear)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MinAccent,
                            contentColor = MinCardBg
                        )
                    ) {
                        AutoResizedText("Готово", fontWeight = FontWeight.Bold, minFontSize = 10.sp)
                    }
                }
            }
        }
    }
}

