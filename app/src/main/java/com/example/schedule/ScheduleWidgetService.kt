package com.example.schedule

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleWidgetListFactory(applicationContext, intent)
    }
}

class ScheduleWidgetListFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private data class LessonItem(
        val startTime: String,
        val endTime: String,
        val typeAbbrev: String,
        val subject: String,
        val teacher: String,
        val auditory: String
    )

    private val items = mutableListOf<LessonItem>()
    private var isDarkTheme = true
    private var primaryColor = Color.WHITE
    private var secondaryColor = Color.GRAY

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        items.clear()
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val groupPrefs = context.getSharedPreferences("group_prefs", Context.MODE_PRIVATE)
        val targetGroup = prefs.getString("selectedGroup", null)
            ?.takeIf { it.isNotBlank() && it != "114001" }
            ?: prefs.getString("login_group", null)?.takeIf { it.isNotBlank() }
            ?: "510902"
        val subgroup = groupPrefs.getInt("subgroup", 0)
        val dayOffset = prefs.getInt("widget_day_offset", 0)

        isDarkTheme = prefs.getBoolean("isDarkTheme", true)
        val customPrimary = prefs.getInt("customPrimaryColor", -1)
        val isTechno = prefs.getString("styleType", "Minimal") == "Techno"

        primaryColor = when {
            isTechno -> Color.parseColor("#00FF41")
            customPrimary != -1 -> customPrimary
            isDarkTheme -> Color.parseColor("#F5F5F7")
            else -> Color.parseColor("#1D1D1F")
        }
        secondaryColor = when {
            isTechno -> Color.parseColor("#00AA22")
            isDarkTheme -> Color.parseColor("#98989F")
            else -> Color.parseColor("#6E6E73")
        }

        try {
            BsuirApi.initCache(context)
            val cachedResp = if (targetGroup.any { it.isLetter() }) {
                BsuirApi.getCachedEmployeeSchedule(targetGroup)
            } else {
                BsuirApi.getCachedGroupSchedule(targetGroup)
            }
            val resp = cachedResp ?: if (targetGroup.any { it.isLetter() }) {
                runBlocking { BsuirApi.getEmployeeSchedule(targetGroup) }
            } else {
                runBlocking { BsuirApi.getGroupSchedule(targetGroup) }
            }
            val schedules = (if (!resp?.schedules.isNullOrEmpty()) resp?.schedules else resp?.nextSchedules) ?: emptyMap()
            val dayNamesRu = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

            val targetIndex = 7 + dayOffset
            val (targetDow, targetWeek) = getBsuirWeekForCalendarDay(targetIndex)
            val targetKey = dayNamesRu.getOrElse(targetDow - 1) { "Понедельник" }

            val targetLessons = (schedules[targetKey] ?: emptyList()).filter { l ->
                (l.weekNumber == null || l.weekNumber.contains(targetWeek)) &&
                    (subgroup == 0 || l.numSubgroup == null || l.numSubgroup == 0 || l.numSubgroup == subgroup)
            }

            fun mapLesson(l: BsuirLesson): LessonItem {
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
                return LessonItem(
                    startTime = l.startLessonTime ?: "",
                    endTime = l.endLessonTime ?: "",
                    typeAbbrev = l.lessonTypeAbbrev ?: "ЗАН",
                    subject = "${l.subject ?: "Пара"}$subBit",
                    teacher = teacherShort,
                    auditory = cleanAud
                )
            }

            if (targetLessons.isNotEmpty()) {
                targetLessons.forEach { items.add(mapLesson(it)) }
            } else if (schedules.isEmpty()) {
                val dm = DataManager(context)
                val localLessons = dm.getLessonsForTarget(targetGroup).ifEmpty { dm.getLessons() }
                val localForDay = localLessons.filter { l ->
                    l.dayOfWeek == targetDow &&
                        (l.weeks.isEmpty() || l.weeks.contains(targetWeek)) &&
                        (subgroup == 0 || l.subgroup == 0 || l.subgroup == subgroup)
                }
                localForDay.forEach { l ->
                    val subBit = if (l.subgroup != 0) " (${l.subgroup} п.)" else ""
                    items.add(
                        LessonItem(
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
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_lesson_item)
        if (position < 0 || position >= items.size) return views
        val r = items[position]

        views.setTextViewText(R.id.w_item_start, r.startTime)
        views.setTextColor(R.id.w_item_start, primaryColor)
        views.setTextViewText(R.id.w_item_end, r.endTime)
        views.setTextColor(R.id.w_item_end, secondaryColor)

        val typeColor = when (r.typeAbbrev.uppercase()) {
            "ЛК" -> if (isDarkTheme) Color.parseColor("#4ADE80") else Color.parseColor("#15803D")
            "ПЗ" -> if (isDarkTheme) Color.parseColor("#C084FC") else Color.parseColor("#7E22CE")
            "ЛР" -> if (isDarkTheme) Color.parseColor("#FB923C") else Color.parseColor("#C2410C")
            else -> primaryColor
        }
        views.setTextViewText(R.id.w_item_type, r.typeAbbrev)
        views.setTextColor(R.id.w_item_type, typeColor)

        views.setTextViewText(R.id.w_item_subj, r.subject)
        views.setTextColor(R.id.w_item_subj, primaryColor)

        if (r.teacher.isNotBlank() && r.teacher != "Не указан") {
            views.setViewVisibility(R.id.w_item_teacher, View.VISIBLE)
            views.setTextViewText(R.id.w_item_teacher, r.teacher)
            views.setTextColor(R.id.w_item_teacher, secondaryColor)
        } else {
            views.setViewVisibility(R.id.w_item_teacher, View.GONE)
        }

        views.setTextViewText(R.id.w_item_aud, r.auditory)
        views.setTextColor(R.id.w_item_aud, primaryColor)

        val fillInIntent = Intent().apply {
            putExtra("selected_subject", r.subject)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
