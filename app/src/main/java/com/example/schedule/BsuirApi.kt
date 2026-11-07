package com.example.schedule

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class BsuirScheduleResponse(
    val schedules: Map<String, List<BsuirLesson>>?,
    val nextSchedules: Map<String, List<BsuirLesson>>?,
    val exams: List<BsuirLesson>?,
    val currentWeekNumber: Int?,
    val studentGroupDto: BsuirStudentGroup?,
    val employeeDto: BsuirEmployee?,
    val startDate: String?,
    val endDate: String?,
    val currentTerm: String?,
    val nextTerm: String?
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
    val urlId: String?,
    val fio: String?,
    val academicDepartment: List<String>?
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

data class BsuirRatingResponse(
    val subjects: List<BsuirRatingSubject>?,
    val deadlines: List<BsuirRatingDeadlineGroup>?,
    val percentageMarks: List<BsuirRatingPercentageMark>?
)

data class BsuirRatingSubject(
    val id: Int?,
    val name: String?,
    val abbrev: String?,
    val lessonTypes: List<BsuirRatingLessonType>?
)

data class BsuirRatingLessonType(
    val id: Int?,
    val abbrev: String?,
    val termHoursId: Int?,
    val lessons: List<BsuirRatingLesson>?
)

data class BsuirRatingLesson(
    val id: Int?,
    val controlPoint: String?,
    val dateString: String?,
    val gradebookOmissions: Int?,
    val marks: List<BsuirRatingMark>?,
    val subGroup: Int?
)

data class BsuirRatingMark(
    val mark: Int?,
    val taskNumber: Int?
)

data class BsuirRatingDeadlineGroup(
    val termHoursId: Int?,
    val labCount: Int?,
    val taskCount: Int?,
    val deadlines: List<BsuirRatingDeadline>?
)

data class BsuirRatingDeadline(
    val lessonId: Int?,
    val taskNumber: Int?
)

data class BsuirRatingPercentageMark(
    val discipline: String?,
    val date: String?,
    val number: Double?
)

object BsuirApi {
    private val gson = Gson()
    private const val BASE_URL = "https://iis.bsuir.by/api/v1"

    private fun fetchJson(urlString: String): String {
        return try {
            val request = NetworkClient.buildGetRequest(urlString)
            val response = NetworkClient.client.newCall(request).execute()
            if (response.code == 404 || response.code == 403 || !response.isSuccessful) {
                ""
            } else {
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun getAllGroups(): List<BsuirStudentGroup> = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson("$BASE_URL/student-groups")
            if (json.isBlank()) return@withContext emptyList()
            val type = object : com.google.gson.reflect.TypeToken<List<BsuirStudentGroup>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllEmployees(): List<BsuirEmployee> = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson("$BASE_URL/employees/all")
            if (json.isBlank()) return@withContext emptyList()
            val type = object : com.google.gson.reflect.TypeToken<List<BsuirEmployee>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getGroupSchedule(groupNumber: String): BsuirScheduleResponse? = withContext(Dispatchers.IO) {
        var trimmed = groupNumber.trim()
        if (trimmed == "114001" || trimmed.isEmpty()) trimmed = "310101"
        try {
            val json = fetchJson("$BASE_URL/schedule?studentGroup=$trimmed")
            if (json.isNotBlank() && json != "{}") {
                gson.fromJson(json, BsuirScheduleResponse::class.java)
            } else {
                val fallbackJson = fetchJson("$BASE_URL/schedules?studentGroup=$trimmed")
                if (fallbackJson.isNotBlank() && fallbackJson != "{}") {
                    gson.fromJson(fallbackJson, BsuirScheduleResponse::class.java)
                } else null
            }
        } catch (e: Exception) {
            null
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
            val json = fetchJson("$BASE_URL/schedule/current-week")
            val num = json.trim().toIntOrNull()
            if (num != null && num in 1..4) num else 1
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }

    suspend fun getPersonalRating(token: String): BsuirRatingResponse? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        try {
            val req = NetworkClient.buildGetRequest("$BASE_URL/personal-rating", token)
            val resp = NetworkClient.client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = resp.body?.string()
                if (!json.isNullOrBlank() && json != "{}") {
                    gson.fromJson(json, BsuirRatingResponse::class.java)
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMarkBook(token: String): String? = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        try {
            val req = NetworkClient.buildGetRequest("$BASE_URL/markbook", token)
            val resp = NetworkClient.client.newCall(req).execute()
            if (resp.isSuccessful) {
                resp.body?.string()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
