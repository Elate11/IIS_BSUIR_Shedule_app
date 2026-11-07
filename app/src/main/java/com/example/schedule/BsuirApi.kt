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
    val startExamsDate: String?,
    val endExamsDate: String?,
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
    val studentGroups: List<BsuirStudentGroup>?,
    val dateLesson: String? = null,
    val startLessonDate: String? = null,
    val endLessonDate: String? = null,
    val note: String? = null,
    val announcement: Boolean? = null
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
    @Volatile
    private var storageDir: java.io.File? = null

    fun initCache(context: android.content.Context) {
        if (storageDir == null) {
            storageDir = java.io.File(context.applicationContext.filesDir, "bsuir_schedule_cache").apply {
                if (!exists()) mkdirs()
            }
        }
    }

    private fun sanitizeCacheKey(key: String): String {
        return key.replace(Regex("[^a-zA-Z0-9_\\-.]"), "_")
    }

    private fun readDiskCache(key: String): String? {
        val dir = storageDir ?: return null
        return try {
            val file = java.io.File(dir, "${sanitizeCacheKey(key)}.json")
            if (file.exists() && file.length() > 2L) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun writeDiskCache(key: String, json: String) {
        if (json.isBlank() || json == "{}" || json == "[]") return
        val dir = storageDir ?: return
        try {
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, "${sanitizeCacheKey(key)}.json")
            file.writeText(json)
        } catch (e: Exception) {
            // ignore disk write errors
        }
    }

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

    fun getCachedAllGroups(): List<BsuirStudentGroup> {
        val cached = readDiskCache("all_student_groups") ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<BsuirStudentGroup>>() {}.type
            gson.fromJson<List<BsuirStudentGroup>>(cached, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllGroups(): List<BsuirStudentGroup> = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson("$BASE_URL/student-groups")
            if (json.isNotBlank() && json != "[]") {
                writeDiskCache("all_student_groups", json)
                val type = object : com.google.gson.reflect.TypeToken<List<BsuirStudentGroup>>() {}.type
                gson.fromJson<List<BsuirStudentGroup>>(json, type) ?: getCachedAllGroups()
            } else {
                getCachedAllGroups()
            }
        } catch (e: Exception) {
            getCachedAllGroups()
        }
    }

    fun getCachedAllEmployees(): List<BsuirEmployee> {
        val cached = readDiskCache("all_employees") ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<BsuirEmployee>>() {}.type
            gson.fromJson<List<BsuirEmployee>>(cached, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllEmployees(): List<BsuirEmployee> = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson("$BASE_URL/employees/all")
            if (json.isNotBlank() && json != "[]") {
                writeDiskCache("all_employees", json)
                val type = object : com.google.gson.reflect.TypeToken<List<BsuirEmployee>>() {}.type
                gson.fromJson<List<BsuirEmployee>>(json, type) ?: getCachedAllEmployees()
            } else {
                getCachedAllEmployees()
            }
        } catch (e: Exception) {
            getCachedAllEmployees()
        }
    }

    fun getCachedGroupSchedule(groupNumber: String): BsuirScheduleResponse? {
        var trimmed = groupNumber.trim()
        if (trimmed == "114001" || trimmed.isEmpty()) trimmed = "310101"
        val cached = readDiskCache("sched_group_$trimmed") ?: return null
        return try {
            gson.fromJson(cached, BsuirScheduleResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getGroupSchedule(groupNumber: String): BsuirScheduleResponse? = withContext(Dispatchers.IO) {
        var trimmed = groupNumber.trim()
        if (trimmed == "114001" || trimmed.isEmpty()) trimmed = "310101"
        val cacheKey = "sched_group_$trimmed"
        try {
            val json = fetchJson("$BASE_URL/schedule?studentGroup=$trimmed")
            if (json.isNotBlank() && json != "{}") {
                val parsed = gson.fromJson(json, BsuirScheduleResponse::class.java)
                if (parsed != null) {
                    writeDiskCache(cacheKey, json)
                    return@withContext parsed
                }
            }
            val fallbackJson = fetchJson("$BASE_URL/schedules?studentGroup=$trimmed")
            if (fallbackJson.isNotBlank() && fallbackJson != "{}") {
                val parsed = gson.fromJson(fallbackJson, BsuirScheduleResponse::class.java)
                if (parsed != null) {
                    writeDiskCache(cacheKey, fallbackJson)
                    return@withContext parsed
                }
            }
            getCachedGroupSchedule(trimmed)
        } catch (e: Exception) {
            getCachedGroupSchedule(trimmed)
        }
    }

    fun getCachedEmployeeSchedule(urlId: String): BsuirScheduleResponse? {
        if (urlId.isBlank()) return null
        employeeScheduleCache[urlId]?.let { return it }
        val cached = readDiskCache("sched_emp_${urlId.trim()}") ?: return null
        return try {
            gson.fromJson(cached, BsuirScheduleResponse::class.java)?.also {
                employeeScheduleCache[urlId] = it
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEmployeeSchedule(urlId: String): BsuirScheduleResponse? = withContext(Dispatchers.IO) {
        val cleanId = urlId.trim()
        val cacheKey = "sched_emp_$cleanId"
        try {
            val json = fetchJson("$BASE_URL/employees/schedule/$cleanId")
            if (json.isNotBlank() && json != "{}") {
                val parsed = gson.fromJson(json, BsuirScheduleResponse::class.java)
                if (parsed != null) {
                    writeDiskCache(cacheKey, json)
                    employeeScheduleCache[cleanId] = parsed
                    return@withContext parsed
                }
            }
            getCachedEmployeeSchedule(cleanId)
        } catch (e: Exception) {
            getCachedEmployeeSchedule(cleanId)
        }
    }

    private val employeeScheduleCache = java.util.concurrent.ConcurrentHashMap<String, BsuirScheduleResponse>()

    suspend fun getOrFetchEmployeeSchedule(urlId: String): BsuirScheduleResponse? = withContext(Dispatchers.IO) {
        if (urlId.isBlank()) return@withContext null
        employeeScheduleCache[urlId]?.let { return@withContext it }
        getCachedEmployeeSchedule(urlId)?.let { return@withContext it }
        val sched = getEmployeeSchedule(urlId)
        if (sched != null) {
            employeeScheduleCache[urlId] = sched
        }
        sched
    }

    suspend fun resolveAllGroupsForLesson(
        teacherUrlId: String,
        dayOfWeekInt: Int,
        startTime: String,
        subjectTitle: String,
        lessonType: String,
        fallbackGroups: List<String> = emptyList(),
        currentGroup: String = "",
        dateLesson: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        if (teacherUrlId.isBlank()) return@withContext fallbackGroups
        val teacherSched = getOrFetchEmployeeSchedule(teacherUrlId) ?: return@withContext fallbackGroups

        if (dateLesson != null) {
            val exams = teacherSched.exams ?: emptyList()
            for (el in exams) {
                if (el.dateLesson == dateLesson && (startTime.isBlank() || el.startLessonTime == startTime)) {
                    val grps = el.studentGroups?.mapNotNull { it.name?.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                    if (grps.isNotEmpty()) {
                        if (currentGroup.isBlank() || grps.contains(currentGroup)) return@withContext grps
                    }
                }
            }
        }

        val dayName = when (dayOfWeekInt) {
            1 -> "Понедельник"
            2 -> "Вторник"
            3 -> "Среда"
            4 -> "Четверг"
            5 -> "Пятница"
            6 -> "Суббота"
            7 -> "Воскресенье"
            else -> ""
        }

        val schedMap = (if (!teacherSched.schedules.isNullOrEmpty()) teacherSched.schedules else teacherSched.nextSchedules) ?: emptyMap()
        val candidateLessons = mutableListOf<BsuirLesson>()

        if (dayName.isNotBlank() && schedMap.containsKey(dayName)) {
            candidateLessons.addAll(schedMap[dayName] ?: emptyList())
        } else {
            schedMap.values.forEach { candidateLessons.addAll(it) }
        }

        var bestMatch: List<String>? = null
        val cleanSubj = subjectTitle.trim()

        for (tl in candidateLessons) {
            val tTime = tl.startLessonTime ?: ""
            val tSubj = (tl.subject ?: tl.subjectFullName ?: "").trim()
            val tType = tl.lessonTypeAbbrev ?: ""

            val timeMatches = tTime == startTime
            if (!timeMatches) continue

            val subjMatches = cleanSubj.isBlank() || tSubj.contains(cleanSubj, ignoreCase = true) || cleanSubj.contains(tSubj, ignoreCase = true)
            val typeMatches = lessonType.isBlank() || tType.equals(lessonType, ignoreCase = true)

            if (subjMatches || typeMatches) {
                val grps = tl.studentGroups?.mapNotNull { it.name?.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                if (grps.isNotEmpty()) {
                    if (currentGroup.isNotBlank() && grps.contains(currentGroup)) {
                        return@withContext grps
                    }
                    if (bestMatch == null || grps.size > bestMatch.size) {
                        bestMatch = grps
                    }
                }
            }
        }

        return@withContext bestMatch?.takeIf { it.isNotEmpty() } ?: fallbackGroups
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
