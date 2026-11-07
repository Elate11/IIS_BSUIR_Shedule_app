package com.example.schedule

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        when (intent.action) {
            ACTION_REFRESH_WIDGET, ACTION_TODAY_WIDGET -> {
                prefs.edit().putInt("widget_day_offset", 0).apply()
                updateAllWidgets(context)
            }
            ACTION_PREV_DAY -> {
                val cur = prefs.getInt("widget_day_offset", 0)
                prefs.edit().putInt("widget_day_offset", (cur - 1).coerceIn(-7, 30)).apply()
                updateAllWidgets(context)
            }
            ACTION_NEXT_DAY -> {
                val cur = prefs.getInt("widget_day_offset", 0)
                prefs.edit().putInt("widget_day_offset", (cur + 1).coerceIn(-7, 30)).apply()
                updateAllWidgets(context)
            }
        }
    }

    private data class WidgetLessonRow(
        val startTime: String,
        val endTime: String,
        val typeAbbrev: String,
        val subject: String,
        val teacher: String,
        val auditory: String
    )

    private data class RowViewIds(
        val containerId: Int,
        val startId: Int,
        val endId: Int,
        val typeId: Int,
        val subjId: Int,
        val teacherId: Int,
        val audId: Int
    )

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.schedule.ACTION_REFRESH_WIDGET"
        const val ACTION_PREV_DAY = "com.example.schedule.ACTION_WIDGET_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.example.schedule.ACTION_WIDGET_NEXT_DAY"
        const val ACTION_TODAY_WIDGET = "com.example.schedule.ACTION_WIDGET_TODAY"

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            BsuirApi.initCache(appContext)
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val groupPrefs = appContext.getSharedPreferences("group_prefs", Context.MODE_PRIVATE)
                val targetGroup = prefs.getString("selectedGroup", null)
                    ?.takeIf { it.isNotBlank() && it != "114001" }
                    ?: prefs.getString("login_group", null)?.takeIf { it.isNotBlank() }
                    ?: "510902"
                val subgroup = groupPrefs.getInt("subgroup", 0)
                val dayOffset = prefs.getInt("widget_day_offset", 0)

                val isDarkTheme = prefs.getBoolean("isDarkTheme", true)
                val customPrimary = prefs.getInt("customPrimaryColor", -1)
                val customBg = prefs.getInt("customBackgroundColor", -1)
                val isTechno = prefs.getString("styleType", "Minimal") == "Techno"

                val bgColor = when {
                    isTechno -> Color.parseColor("#000000")
                    customBg != -1 -> customBg
                    isDarkTheme -> Color.parseColor("#0D0E12")
                    else -> Color.parseColor("#F5F5F7")
                }
                val primaryColor = when {
                    isTechno -> Color.parseColor("#00FF41")
                    customPrimary != -1 -> customPrimary
                    isDarkTheme -> Color.parseColor("#F5F5F7")
                    else -> Color.parseColor("#1D1D1F")
                }
                val secondaryColor = when {
                    isTechno -> Color.parseColor("#00AA22")
                    isDarkTheme -> Color.parseColor("#98989F")
                    else -> Color.parseColor("#6E6E73")
                }

                var subtitle = "Расписание"
                val rows = mutableListOf<WidgetLessonRow>()

                fun mapLesson(l: BsuirLesson): WidgetLessonRow {
                    val emp = l.employees?.firstOrNull()
                    val teacherShort = if (emp != null) {
                        val ln = emp.lastName ?: ""
                        val fn = emp.firstName?.firstOrNull()?.let { "$it." } ?: ""
                        val mn = emp.middleName?.firstOrNull()?.let { "$it." } ?: ""
                        "$ln $fn $mn".trim()
                    } else "Не указан"
                    val subBit = if (l.numSubgroup != null && l.numSubgroup != 0) " (${l.numSubgroup} п.)" else ""
                    val cleanAud = (l.auditories?.firstOrNull() ?: "—")
                        .replace(Regex("(?i)ауд\\.?\\s*"), "")
                        .trim()
                    return WidgetLessonRow(
                        startTime = l.startLessonTime ?: "",
                        endTime = l.endLessonTime ?: "",
                        typeAbbrev = l.lessonTypeAbbrev ?: "ЗАН",
                        subject = "${l.subject ?: "Пара"}$subBit",
                        teacher = teacherShort,
                        auditory = cleanAud
                    )
                }

                try {
                    val cachedResp = if (targetGroup.any { it.isLetter() }) {
                        BsuirApi.getCachedEmployeeSchedule(targetGroup)
                    } else {
                        BsuirApi.getCachedGroupSchedule(targetGroup)
                    }
                    // Use cached response first if user is flipping days, or fetch if cache is empty
                    val resp = cachedResp ?: if (targetGroup.any { it.isLetter() }) {
                        BsuirApi.getEmployeeSchedule(targetGroup)
                    } else {
                        BsuirApi.getGroupSchedule(targetGroup)
                    }
                    val schedules = (if (!resp?.schedules.isNullOrEmpty()) resp?.schedules else resp?.nextSchedules) ?: emptyMap()
                    val dayNamesRu = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
                    val dayNamesShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

                    val targetIndex = 7 + dayOffset
                    val (targetDow, targetWeek) = getBsuirWeekForCalendarDay(targetIndex)
                    val targetKey = dayNamesRu.getOrElse(targetDow - 1) { "Понедельник" }
                    val targetCal = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_MONTH, dayOffset)
                    }
                    val dateLabel = "%02d.%02d".format(
                        targetCal.get(java.util.Calendar.DAY_OF_MONTH),
                        targetCal.get(java.util.Calendar.MONTH) + 1
                    )

                    subtitle = when (dayOffset) {
                        0 -> "$targetWeek учебная неделя • Сегодня, $targetKey"
                        1 -> "$targetWeek учебная неделя • Завтра, $targetKey"
                        -1 -> "$targetWeek учебная неделя • Вчера, $targetKey"
                        else -> "$targetWeek нед. • ${dayNamesShort.getOrElse(targetDow - 1) { targetKey }}, $dateLabel"
                    }

                    val targetLessons = (schedules[targetKey] ?: emptyList()).filter { l ->
                        (l.weekNumber == null || l.weekNumber.contains(targetWeek)) &&
                            (subgroup == 0 || l.numSubgroup == null || l.numSubgroup == 0 || l.numSubgroup == subgroup)
                    }

                    if (targetLessons.isNotEmpty()) {
                        targetLessons.take(5).forEach { rows.add(mapLesson(it)) }
                    } else if (schedules.isEmpty()) {
                        // Offline fallback to DataManager cached Lesson list
                        val dm = DataManager(appContext)
                        val localLessons = dm.getLessonsForTarget(targetGroup).ifEmpty { dm.getLessons() }
                        val localForDay = localLessons.filter { l ->
                            l.dayOfWeek == targetDow &&
                                (l.weeks.isEmpty() || l.weeks.contains(targetWeek)) &&
                                (subgroup == 0 || l.subgroup == 0 || l.subgroup == subgroup)
                        }
                        localForDay.take(5).forEach { l ->
                            val subBit = if (l.subgroup != 0) " (${l.subgroup} п.)" else ""
                            rows.add(
                                WidgetLessonRow(
                                    startTime = l.startTime,
                                    endTime = l.endTime,
                                    typeAbbrev = l.lessonType.ifBlank { "ЗАН" },
                                    subject = "${l.title}$subBit",
                                    teacher = l.teacherName.ifBlank { "Не указан" },
                                    auditory = l.auditory.replace(Regex("(?i)ауд\\.?\\s*"), "").trim().ifBlank { "—" }
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                val manager = AppWidgetManager.getInstance(appContext)
                val ids = manager.getAppWidgetIds(ComponentName(appContext, ScheduleWidgetProvider::class.java))
                val rowDefs = listOf(
                    RowViewIds(R.id.widget_row_1, R.id.w_r1_start, R.id.w_r1_end, R.id.w_r1_type, R.id.w_r1_subj, R.id.w_r1_teacher, R.id.w_r1_aud),
                    RowViewIds(R.id.widget_row_2, R.id.w_r2_start, R.id.w_r2_end, R.id.w_r2_type, R.id.w_r2_subj, R.id.w_r2_teacher, R.id.w_r2_aud),
                    RowViewIds(R.id.widget_row_3, R.id.w_r3_start, R.id.w_r3_end, R.id.w_r3_type, R.id.w_r3_subj, R.id.w_r3_teacher, R.id.w_r3_aud),
                    RowViewIds(R.id.widget_row_4, R.id.w_r4_start, R.id.w_r4_end, R.id.w_r4_type, R.id.w_r4_subj, R.id.w_r4_teacher, R.id.w_r4_aud),
                    RowViewIds(R.id.widget_row_5, R.id.w_r5_start, R.id.w_r5_end, R.id.w_r5_type, R.id.w_r5_subj, R.id.w_r5_teacher, R.id.w_r5_aud)
                )

                for (widgetId in ids) {
                    val views = RemoteViews(appContext.packageName, R.layout.widget_schedule)
                    views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

                    views.setTextViewText(R.id.widget_title, targetGroup)
                    views.setTextColor(R.id.widget_title, primaryColor)

                    views.setTextViewText(R.id.widget_subtitle, subtitle)
                    views.setTextColor(R.id.widget_subtitle, secondaryColor)
                    views.setTextColor(R.id.widget_prev_day_btn, primaryColor)
                    views.setTextColor(R.id.widget_next_day_btn, primaryColor)

                    views.setTextViewText(R.id.widget_subgroup, if (subgroup == 0) "Все подгр." else "$subgroup подгр.")
                    views.setTextColor(R.id.widget_subgroup, secondaryColor)
                    views.setTextColor(R.id.widget_refresh_btn, secondaryColor)

                    views.setTextColor(R.id.widget_empty_title, primaryColor)
                    views.setTextColor(R.id.widget_empty_sub, secondaryColor)

                    val openIntent = Intent(appContext, MainActivity::class.java)
                    val openPending = PendingIntent.getActivity(
                        appContext,
                        0,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openPending)

                    val refreshIntent = Intent(appContext, ScheduleWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_WIDGET
                    }
                    val refreshPending = PendingIntent.getBroadcast(
                        appContext,
                        1,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

                    val prevIntent = Intent(appContext, ScheduleWidgetProvider::class.java).apply {
                        action = ACTION_PREV_DAY
                    }
                    val prevPending = PendingIntent.getBroadcast(
                        appContext,
                        2,
                        prevIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_prev_day_btn, prevPending)

                    val nextIntent = Intent(appContext, ScheduleWidgetProvider::class.java).apply {
                        action = ACTION_NEXT_DAY
                    }
                    val nextPending = PendingIntent.getBroadcast(
                        appContext,
                        3,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_next_day_btn, nextPending)

                    val todayIntent = Intent(appContext, ScheduleWidgetProvider::class.java).apply {
                        action = ACTION_TODAY_WIDGET
                    }
                    val todayPending = PendingIntent.getBroadcast(
                        appContext,
                        4,
                        todayIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_subtitle, todayPending)

                    if (rows.isEmpty()) {
                        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                        rowDefs.forEach { views.setViewVisibility(it.containerId, View.GONE) }
                    } else {
                        views.setViewVisibility(R.id.widget_empty, View.GONE)
                        for (i in rowDefs.indices) {
                            val def = rowDefs[i]
                            if (i < rows.size) {
                                val r = rows[i]
                                views.setViewVisibility(def.containerId, View.VISIBLE)
                                views.setTextViewText(def.startId, r.startTime)
                                views.setTextColor(def.startId, primaryColor)
                                views.setTextViewText(def.endId, r.endTime)
                                views.setTextColor(def.endId, secondaryColor)

                                val typeColor = when (r.typeAbbrev.uppercase()) {
                                    "ЛК" -> if (isDarkTheme) Color.parseColor("#4CC38A") else Color.parseColor("#16A34A")
                                    "ПЗ" -> if (isDarkTheme) Color.parseColor("#52A9FF") else Color.parseColor("#2563EB")
                                    "ЛР" -> if (isDarkTheme) Color.parseColor("#E5484D") else Color.parseColor("#DC2626")
                                    else -> primaryColor
                                }
                                views.setTextViewText(def.typeId, r.typeAbbrev)
                                views.setTextColor(def.typeId, typeColor)

                                views.setTextViewText(def.subjId, r.subject)
                                views.setTextColor(def.subjId, primaryColor)

                                if (r.teacher.isNotBlank() && r.teacher != "Не указан") {
                                    views.setViewVisibility(def.teacherId, View.VISIBLE)
                                    views.setTextViewText(def.teacherId, r.teacher)
                                    views.setTextColor(def.teacherId, secondaryColor)
                                } else {
                                    views.setViewVisibility(def.teacherId, View.GONE)
                                }

                                views.setTextViewText(def.audId, r.auditory)
                                views.setTextColor(def.audId, primaryColor)
                            } else {
                                views.setViewVisibility(def.containerId, View.GONE)
                            }
                        }
                    }
                    manager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
