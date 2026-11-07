package com.example.schedule

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Lesson(
    val startTime: String,
    val endTime: String,
    val title: String,
    val details: String,
    val isActive: Boolean = false,
    val progress: Float? = null,
    val subjectFullName: String = "",
    val lessonType: String = "",
    val teacherName: String = "",
    val teacherFullName: String = "",
    val teacherPhoto: String = "", val teacherUrlId: String = "",
    val auditory: String = "",
    val subgroup: Int = 0,
    val weeks: List<Int> = listOf(1, 2, 3, 4), val dayOfWeek: Int = 0,
    val dateLesson: String? = null,
    val note: String? = null,
    val studentGroups: List<String> = emptyList()
)

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLessons(lessons: List<Lesson>) {
        val json = gson.toJson(lessons)
        prefs.edit().putString("lessons", json).apply()
    }

    fun saveLessonsForTarget(target: String, lessons: List<Lesson>) {
        val json = gson.toJson(lessons)
        prefs.edit()
            .putString("lessons_target_${target.trim()}", json)
            .putString("lessons", json)
            .putLong("lessons_updated_at_${target.trim()}", System.currentTimeMillis())
            .apply()
    }

    fun getLessonsForTarget(target: String): List<Lesson> {
        val key = "lessons_target_${target.trim()}"
        val json = prefs.getString(key, null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Lesson>>() {}.type
                val parsed = gson.fromJson<List<Lesson>>(json, type)
                if (!parsed.isNullOrEmpty()) return parsed
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    fun getLessons(): List<Lesson> {
        val json = prefs.getString("lessons", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<Lesson>>() {}.type
                val list = gson.fromJson<List<Lesson>>(json, type) ?: emptyList()
                if (list.size == 3 && list[0].title == "Матан") emptyList() else list
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveExams(group: String, exams: List<Lesson>) {
        val json = gson.toJson(exams)
        prefs.edit().putString("exams_list_$group", json).apply()
    }

    fun getExams(group: String): List<Lesson> {
        val json = prefs.getString("exams_list_$group", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Lesson>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSessionPeriod(group: String, start: String?, end: String?) {
        prefs.edit()
            .putString("start_exams_date_$group", start ?: "")
            .putString("end_exams_date_$group", end ?: "")
            .apply()
    }

    fun getSessionPeriod(group: String): Pair<String, String> {
        val s = prefs.getString("start_exams_date_$group", "") ?: ""
        val e = prefs.getString("end_exams_date_$group", "") ?: ""
        return s to e
    }

    fun saveMarks(marks: List<SubjectMarks>) {
        val json = gson.toJson(marks)
        prefs.edit().putString("marks", json).apply()
    }

    fun getMarks(): List<SubjectMarks> {
        val json = prefs.getString("marks", null)
        return if (json != null) {
            val type = object : TypeToken<List<SubjectMarks>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Default dummy data if empty
            listOf(
                SubjectMarks("Матан",  listOf(8, 7, 9, 8, 10)),
                SubjectMarks("Физика",  listOf(7, 6, 8, 7)),
                SubjectMarks("ОАиП",  listOf(9, 10, 9, 10, 9))
            )
        }
    }
}

