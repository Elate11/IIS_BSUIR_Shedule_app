package com.example.schedule

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class BsuirScheduleResponse(
    val schedules: Map<String, List<BsuirLesson>>?,
    val exams: List<BsuirLesson>?,
    val currentWeekNumber: Int?,
    val studentGroupDto: BsuirStudentGroup?,
    val employeeDto: BsuirEmployee?
)

data class BsuirLesson(
    val subject: String?,
    val subjectFullName: String?,
    val lessonTypeAbbrev: String?,
    val startLessonTime: String?,
    val endLessonTime: String?,
    val numSubgroup: Int?,
    val weekNumber: List<Int>?,
    val auditories: List<String>?,
    val employees: List<BsuirEmployee>?,
    val studentGroups: List<BsuirStudentGroup>?
)

data class BsuirEmployee(
    val firstName: String?,
    val lastName: String?,
    val middleName: String?,
    val photoLink: String?,
    val degree: String?,
    val rank: String?,
    val id: Int?,
    val urlId: String?
) {
    val fullName: String get() = "${lastName ?: ""} ${firstName ?: ""} ${middleName ?: ""}".trim()
}

data class BsuirStudentGroup(
    val name: String?,
    val facultyAbbrev: String?,
    val specialityAbbrev: String?,
    val course: Int?,
    val id: Int?
)

object BsuirApi {
    private val gson = Gson()
    private const val BASE_URL = "https://iis.bsuir.by/api/v1"

    private fun fetchJson(urlString: String): String {
        val request = NetworkClient.buildGetRequest(urlString)
        val response = NetworkClient.client.newCall(request).execute()
        return response.body?.string() ?: throw java.io.IOException("Empty response body")
    }

    suspend fun getGroupSchedule(groupNumber: String): BsuirScheduleResponse? = withContext(Dispatchers.IO) {
        val trimmed = groupNumber.trim()
        if (trimmed.isEmpty()) return@withContext null
        try {
            val json = fetchJson("$BASE_URL/schedule?studentGroup=$trimmed")
            gson.fromJson(json, BsuirScheduleResponse::class.java)
        } catch (e: Exception) {
            try {
                val json = fetchJson("$BASE_URL/schedules?studentGroup=$trimmed")
                gson.fromJson(json, BsuirScheduleResponse::class.java)
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }

    suspend fun getEmployeeSchedule(urlId: String): BsuirScheduleResponse? = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson("$BASE_URL/employees/schedule/$urlId")
            gson.fromJson(json, BsuirScheduleResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCurrentWeek(): Int? = withContext(Dispatchers.IO) {
        try {
            val cal = java.util.Calendar.getInstance()
            val month = cal.get(java.util.Calendar.MONTH)
            if (month == java.util.Calendar.JULY || month == java.util.Calendar.AUGUST) {
                return@withContext 0
            }

            val json = fetchJson("$BASE_URL/schedule/current-week")
            json.toIntOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
