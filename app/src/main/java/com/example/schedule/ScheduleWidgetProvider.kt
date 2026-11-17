package com.example.schedule

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
                var hasLessons = false

                try {
                    val cachedResp = if (targetGroup.any { it.isLetter() }) {
                        BsuirApi.getCachedEmployeeSchedule(targetGroup)
                    } else {
                        BsuirApi.getCachedGroupSchedule(targetGroup)
                    }
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
                        hasLessons = true
                    } else if (schedules.isEmpty()) {
                        val dm = DataManager(appContext)
                        val localLessons = dm.getLessonsForTarget(targetGroup).ifEmpty { dm.getLessons() }
                        val localForDay = localLessons.filter { l ->
                            l.dayOfWeek == targetDow &&
                                (l.weeks.isEmpty() || l.weeks.contains(targetWeek)) &&
                                (subgroup == 0 || l.subgroup == 0 || l.subgroup == subgroup)
                        }
                        hasLessons = localForDay.isNotEmpty()
                    }
                } catch (_: Exception) {}

                val manager = AppWidgetManager.getInstance(appContext)
                val ids = manager.getAppWidgetIds(ComponentName(appContext, ScheduleWidgetProvider::class.java))

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

                    // Set up RemoteViewsService for scrollable ListView
                    val serviceIntent = Intent(appContext, ScheduleWidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        // Make intent URI unique so RemoteViewsFactory re-binds on update
                        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME) + "&time=${System.currentTimeMillis()}")
                    }
                    views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)

                    val listClickIntent = Intent(appContext, MainActivity::class.java)
                    val listClickPending = PendingIntent.getActivity(
                        appContext,
                        10,
                        listClickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                    views.setPendingIntentTemplate(R.id.widget_list_view, listClickPending)

                    if (!hasLessons) {
                        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_list_view, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_empty, View.GONE)
                        views.setViewVisibility(R.id.widget_list_view, View.VISIBLE)
                    }

                    manager.updateAppWidget(widgetId, views)
                    manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view)
                }
            }
        }
    }
}
