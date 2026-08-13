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
    val weeks: List<Int> = listOf(1, 2, 3, 4), val dayOfWeek: Int = 0
)

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLessons(lessons: List<Lesson>) {
        val json = gson.toJson(lessons)
        prefs.edit().putString("lessons", json).apply()
    }

    fun getLessons(): List<Lesson> {
        val json = prefs.getString("lessons", null)
        return if (json != null) {
            val type = object : TypeToken<List<Lesson>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Default dummy data if empty
            listOf(
                Lesson("08:00", "09:35", "Матан", "Лекция, ауд. 414-2", true, 0.65f, "Математический анализ", "ЛК", "Иванов И.И.", "Иванов Иван Иванович", "", "", "414-2", 0, listOf(1, 2, 3, 4)),
                Lesson("09:50", "11:25", "Физика", "Лаб. работа, ауд. 101-1", false, null, "Физика", "ЛР", "Петров П.П.", "Петров Петр Петрович", "", "", "101-1", 1, listOf(1, 3)),
                Lesson("11:40", "13:15", "ОАиП", "Практика, ауд. 210-4", false, null, "Основы алгоритмизации и программирования", "ПЗ", "Сидоров С.С.", "Сидоров Сергей Сергеевич", "", "", "210-4", 2, listOf(2, 4))
            )
        }
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

