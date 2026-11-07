package com.example.schedule

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 1. UNREAD NOTIFICATIONS & MARK AS VIEWED HELPERS
// ============================================================================

suspend fun fetchUnreadNotificationsCount(context: Context): Int = withContext(Dispatchers.IO) {
    try {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""
        if (token.isBlank()) return@withContext 0
        val req = NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/notifications/notViewed/count", token)
        NetworkClient.executeWithAuth(req, context).use { resp ->
            if (resp.isSuccessful) {
                val body = resp.body?.string()?.trim() ?: "0"
                val count = body.toIntOrNull() ?: 0
                prefs.edit().putInt("cached_unread_notif_count", count).apply()
                return@withContext count
            }
        }
    } catch (_: Exception) {}
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.getInt("cached_unread_notif_count", 0)
}

suspend fun markDeanNotificationsAsRead(context: Context, unreadIds: List<Long>) = withContext(Dispatchers.IO) {
    if (unreadIds.isEmpty()) return@withContext
    try {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""
        if (token.isBlank()) return@withContext
        val arr = JSONArray()
        unreadIds.forEach { id ->
            arr.put(JSONObject().apply {
                put("id", id)
                put("isViewed", true)
            })
        }
        val body = arr.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val reqBuilder = Request.Builder()
            .url("https://iis.bsuir.by/api/v1/notifications")
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Accept", "application/json, text/plain, */*")
            .patch(body)
        if (token.isNotBlank()) reqBuilder.addHeader("Cookie", token)
        NetworkClient.executeWithAuth(reqBuilder.build(), context).close()
        prefs.edit().putInt("cached_unread_notif_count", 0).apply()
    } catch (_: Exception) {}
}

// ============================================================================
// 2. STUDY SCREEN: CERTIFICATES & MARK SHEETS ("СПРАВКИ И ВЕДОМОСТИ")
// ============================================================================

data class IisCertificateItem(
    val id: Int,
    val number: Int,
    val certificateType: String,
    val provisionPlace: String,
    val dateOrder: String,
    val issueDate: String?,
    val status: Int, // 1 -> Напечатано, 2 -> В обработке, 3 -> Отклонена
    val rejectionReason: String?
)

data class IisCertificatePlaceGroup(
    val categoryTitle: String,
    val places: List<Pair<String, Int>> // name to type (0 = обычная, 1 = гербовая)
)

data class IisMarkSheetItem(
    val id: Int,
    val number: String,
    val subjectName: String,
    val subjectType: String,
    val employeeFio: String,
    val absentDate: String,
    val createDate: String,
    val expireDate: String,
    val hours: Double,
    val price: Double,
    val isGoodReason: Boolean,
    val status: String,
    val rejectionReason: String,
    val term: Int
)

data class IisMarkSheetSubject(
    val abbrev: String,
    val term: Int,
    val lessonTypes: List<IisMarkSheetLessonType>
)

data class IisMarkSheetLessonType(
    val abbrev: String,
    val thId: Int?,
    val focsId: Int?
)

@Composable
fun MinStudyCertificatesAndSheetsScreen(
    MinBg: Color,
    MinCardBg: Color,
    MinBorder: Color,
    MinTextPrimary: Color,
    MinTextSecondary: Color,
    currentAccent: Color,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Справки, 1 = Ведомости
    var isLoading by remember { mutableStateOf(true) }
    var certificates by remember { mutableStateOf<List<IisCertificateItem>>(emptyList()) }
    var placeGroups by remember { mutableStateOf<List<IisCertificatePlaceGroup>>(emptyList()) }
    var markSheets by remember { mutableStateOf<List<IisMarkSheetItem>>(emptyList()) }
    var markSheetSubjects by remember { mutableStateOf<List<IisMarkSheetSubject>>(emptyList()) }

    var showCreateCertificateDialog by remember { mutableStateOf(false) }
    var showCreateMarkSheetDialog by remember { mutableStateOf(false) }

    fun parseCertificates(jsonStr: String): List<IisCertificateItem> {
        val list = mutableListOf<IisCertificateItem>()
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    IisCertificateItem(
                        id = obj.optInt("id", 0),
                        number = obj.optInt("number", 0),
                        certificateType = obj.optString("certificateType", "обычная"),
                        provisionPlace = obj.optString("provisionPlace", "По месту требования"),
                        dateOrder = obj.optString("dateOrder", ""),
                        issueDate = obj.optString("issueDate", "").takeIf { it.isNotBlank() && it != "null" },
                        status = obj.optInt("status", 2),
                        rejectionReason = obj.optString("rejectionReason", "").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parsePlaces(jsonStr: String): List<IisCertificatePlaceGroup> {
        val groups = mutableListOf<IisCertificatePlaceGroup>()
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("type", "Общие")
                val placesArr = obj.optJSONArray("places") ?: continue
                val pList = mutableListOf<Pair<String, Int>>()
                for (j in 0 until placesArr.length()) {
                    val pObj = placesArr.optJSONObject(j) ?: continue
                    val name = pObj.optString("name", "")
                    val pType = pObj.optInt("type", 0)
                    if (name.isNotBlank()) pList.add(name to pType)
                }
                if (pList.isNotEmpty()) groups.add(IisCertificatePlaceGroup(title, pList))
            }
        } catch (_: Exception) {}
        return groups
    }

    fun parseMarkSheets(jsonStr: String): List<IisMarkSheetItem> {
        val list = mutableListOf<IisMarkSheetItem>()
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val subj = obj.optJSONObject("subject")
                val emp = obj.optJSONObject("employee")
                list.add(
                    IisMarkSheetItem(
                        id = obj.optInt("id", 0),
                        number = obj.optString("number", ""),
                        subjectName = subj?.optString("abbrev", "")?.ifBlank { subj.optString("name", "") } ?: "Дисциплина",
                        subjectType = subj?.optString("lessonTypeAbbrev", "") ?: "",
                        employeeFio = emp?.optString("fio", "") ?: "",
                        absentDate = obj.optString("absentDate", "").takeIf { it != "null" } ?: "",
                        createDate = obj.optString("createDate", "").takeIf { it != "null" } ?: "",
                        expireDate = obj.optString("expireDate", "").takeIf { it != "null" } ?: "",
                        hours = obj.optDouble("hours", 0.0).let { if (it.isNaN()) 0.0 else it },
                        price = obj.optDouble("price", 0.0).let { if (it.isNaN()) 0.0 else it },
                        isGoodReason = obj.optBoolean("reason", false),
                        status = obj.optString("status", "В обработке"),
                        rejectionReason = obj.optString("rejectionReason", "").takeIf { it != "null" } ?: "",
                        term = obj.optInt("term", subj?.optInt("term", 0) ?: 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseMarkSheetSubjects(jsonStr: String): List<IisMarkSheetSubject> {
        val list = mutableListOf<IisMarkSheetSubject>()
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val abbrev = obj.optString("abbrev", "")
                if (abbrev.isBlank()) continue
                val term = obj.optInt("term", 0)
                val typesArr = obj.optJSONArray("lessonTypes")
                val types = mutableListOf<IisMarkSheetLessonType>()
                if (typesArr != null) {
                    for (j in 0 until typesArr.length()) {
                        val tObj = typesArr.optJSONObject(j) ?: continue
                        val tAbbrev = tObj.optString("abbrev", "")
                        val thId = if (tObj.isNull("thId")) null else tObj.optInt("thId")
                        val focsId = if (tObj.isNull("focsId")) null else tObj.optInt("focsId")
                        if (tAbbrev.isNotBlank()) {
                            types.add(IisMarkSheetLessonType(tAbbrev, thId, focsId))
                        }
                    }
                }
                list.add(IisMarkSheetSubject(abbrev, term, types))
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun loadAllStudyData() {
        isLoading = true
        val token = prefs.getString("auth_token", "") ?: ""
        prefs.getString("cached_iis_certificates", null)?.let { certificates = parseCertificates(it) }
        prefs.getString("cached_iis_marksheets", null)?.let { markSheets = parseMarkSheets(it) }
        prefs.getString("cached_iis_cert_places", null)?.let { placeGroups = parsePlaces(it) }
        prefs.getString("cached_iis_ms_subjects", null)?.let { markSheetSubjects = parseMarkSheetSubjects(it) }

        if (token.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/certificate", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_iis_certificates", body).apply()
                            val parsed = parseCertificates(body)
                            withContext(Dispatchers.Main) { certificates = parsed }
                        }
                    }
                } catch (_: Exception) {}

                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/certificate/places", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_iis_cert_places", body).apply()
                            val parsed = parsePlaces(body)
                            withContext(Dispatchers.Main) { placeGroups = parsed }
                        }
                    }
                } catch (_: Exception) {}

                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/mark-sheet", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_iis_marksheets", body).apply()
                            val parsed = parseMarkSheets(body)
                            withContext(Dispatchers.Main) { markSheets = parsed }
                        }
                    }
                } catch (_: Exception) {}

                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/mark-sheet/subjects", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_iis_ms_subjects", body).apply()
                            val parsed = parseMarkSheetSubjects(body)
                            withContext(Dispatchers.Main) { markSheetSubjects = parsed }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadAllStudyData()
    }

    val greenColor = if (isDarkTheme) Color(0xFF4CC38A) else Color(0xFF16A34A)
    val redColor = if (isDarkTheme) Color(0xFFE5484D) else Color(0xFFDC2626)
    val amberColor = Color(0xFFFF9500)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MinBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Учёба и документы",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MinTextPrimary
                    )
                    Text(
                        "Заказ справок и экзаменационных листов",
                        fontSize = 13.sp,
                        color = MinTextSecondary,
                        fontWeight = FontWeight.Normal
                    )
                }
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Обновить",
                    tint = MinTextSecondary,
                    modifier = Modifier.size(24.dp).clickable {
                        AppHapticManager.playClick()
                        scope.launch { loadAllStudyData() }
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppleOptionChip(
                    label = "Справки (${certificates.size})",
                    isSelected = selectedTab == 0,
                    onClick = { AppHapticManager.playClick(); selectedTab = 0 },
                    isDarkTheme = isDarkTheme,
                    accentColor = currentAccent,
                    unselectedBg = MinCardBg,
                    unselectedTextColor = MinTextPrimary,
                    selectedTextColor = MinCardBg,
                    borderColorOverride = MinBorder
                )
                AppleOptionChip(
                    label = "Ведомости (${markSheets.size})",
                    isSelected = selectedTab == 1,
                    onClick = { AppHapticManager.playClick(); selectedTab = 1 },
                    isDarkTheme = isDarkTheme,
                    accentColor = currentAccent,
                    unselectedBg = MinCardBg,
                    unselectedTextColor = MinTextPrimary,
                    selectedTextColor = MinCardBg,
                    borderColorOverride = MinBorder
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(currentAccent)
                    .clickable {
                        AppHapticManager.playClick()
                        if (selectedTab == 0) showCreateCertificateDialog = true
                        else showCreateMarkSheetDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = MinCardBg, modifier = Modifier.size(20.dp))
                    Text(
                        if (selectedTab == 0) "Заказать справку" else "Заказать ведомость",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MinCardBg
                    )
                }
            }
        }

        if (isLoading && certificates.isEmpty() && markSheets.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = currentAccent)
                }
            }
        } else if (selectedTab == 0) {
            if (certificates.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = MinTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Заказанных справок пока нет", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Вы можете заказать справку об обучении по месту работы родителей, в военкомат или ФСЗН прямо из приложения",
                            fontSize = 13.sp,
                            color = MinTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(certificates.size) { idx ->
                    val cert = certificates[idx]
                    val (statusText, statusColor) = when (cert.status) {
                        1 -> "Напечатана" to greenColor
                        2 -> "В обработке" to amberColor
                        3 -> "Отклонена" to redColor
                        else -> "Принята" to currentAccent
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (cert.number > 0) "Справка №${cert.number}" else "Заявка на справку",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MinTextPrimary
                            )
                            Text(statusText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                        }

                        Text(
                            "Куда: ${cert.provisionPlace}",
                            fontSize = 14.sp,
                            color = MinTextPrimary,
                            fontWeight = FontWeight.Normal
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Тип: ${cert.certificateType}",
                                fontSize = 13.sp,
                                color = MinTextSecondary
                            )
                            val dateLine = buildString {
                                if (cert.dateOrder.isNotBlank()) append("Заказ: ${cert.dateOrder}")
                                if (!cert.issueDate.isNullOrBlank()) append(" • Выдана: ${cert.issueDate}")
                            }
                            if (dateLine.isNotBlank()) {
                                Text(
                                    dateLine,
                                    fontSize = 12.sp,
                                    color = MinTextSecondary
                                )
                            }
                        }

                        if (!cert.rejectionReason.isNullOrBlank()) {
                            Text(
                                "Причина: ${cert.rejectionReason}",
                                fontSize = 13.sp,
                                color = redColor
                            )
                        }

                        if (cert.status == 2 && cert.id > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        AppHapticManager.playClick()
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val token = prefs.getString("auth_token", "") ?: ""
                                                val req = NetworkClient.buildGetRequest(
                                                    "https://iis.bsuir.by/api/v1/certificate/close?id=${cert.id}",
                                                    token
                                                )
                                                NetworkClient.executeWithAuth(req, context).close()
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Заявка отменена", Toast.LENGTH_SHORT).show()
                                                }
                                                loadAllStudyData()
                                            } catch (_: Exception) {}
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("Отменить заявку", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = redColor)
                            }
                        }

                        if (idx < certificates.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        } else {
            if (markSheets.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Assignment, contentDescription = null, tint = MinTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Заказанных ведомостей нет", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Здесь отображаются экзаменационные листовки на отработку лабораторных работ и пересдачу зачётов/экзаменов",
                            fontSize = 13.sp,
                            color = MinTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(markSheets.size) { idx ->
                    val ms = markSheets[idx]
                    val statusLow = ms.status.lowercase()
                    val statusColor = when {
                        statusLow.contains("напечат") || statusLow.contains("одобр") || statusLow.contains("готов") -> greenColor
                        statusLow.contains("откл") || statusLow.contains("отозв") -> redColor
                        else -> amberColor
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${ms.subjectName}${if (ms.subjectType.isNotBlank()) " (${ms.subjectType})" else ""}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MinTextPrimary
                            )
                            Text(ms.status.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                        }

                        if (ms.employeeFio.isNotBlank()) {
                            Text("Преподаватель: ${ms.employeeFio}", fontSize = 14.sp, color = MinTextPrimary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                buildString {
                                    if (ms.absentDate.isNotBlank()) append("Пропуск: ${ms.absentDate} • ")
                                    if (ms.hours > 0) append("${ms.hours.toInt()} ч")
                                    append(if (ms.isGoodReason) " (ув. причина)" else " (без ув.)")
                                },
                                fontSize = 13.sp,
                                color = MinTextSecondary
                            )
                            if (ms.price > 0) {
                                Text("${String.format(Locale.US, "%.2f", ms.price)} BYN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = currentAccent)
                            }
                        }

                        if (ms.expireDate.isNotBlank() || ms.number.isNotBlank()) {
                            Text(
                                buildString {
                                    if (ms.number.isNotBlank()) append("№ ${ms.number} ")
                                    if (ms.expireDate.isNotBlank()) append("• Действительна до ${ms.expireDate}")
                                },
                                fontSize = 12.sp,
                                color = MinTextSecondary
                            )
                        }

                        if (ms.rejectionReason.isNotBlank()) {
                            Text("Причина: ${ms.rejectionReason}", fontSize = 13.sp, color = redColor)
                        }

                        if (statusLow.contains("обработ") && ms.id > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        AppHapticManager.playClick()
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val token = prefs.getString("auth_token", "") ?: ""
                                                val req = NetworkClient.buildGetRequest(
                                                    "https://iis.bsuir.by/api/v1/mark-sheet/close?id=${ms.id}",
                                                    token
                                                )
                                                NetworkClient.executeWithAuth(req, context).close()
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Ведомость отозвана", Toast.LENGTH_SHORT).show()
                                                }
                                                loadAllStudyData()
                                            } catch (_: Exception) {}
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("Отозвать заявку", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = redColor)
                            }
                        }

                        if (idx < markSheets.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    if (showCreateCertificateDialog) {
        val allPlaces = remember(placeGroups) {
            val fromApi = placeGroups.flatMap { it.places }
            if (fromApi.isNotEmpty()) fromApi else listOf(
                "По месту работы родителей" to 0,
                "В военный комиссариат" to 1,
                "По месту требования" to 0,
                "В ФСЗН" to 0,
                "В управление социальной защиты" to 0,
                "В посольство" to 0
            )
        }
        var selectedPlace by remember { mutableStateOf(allPlaces.first().first) }
        var customPlace by remember { mutableStateOf("") }
        var certType by remember { mutableStateOf(if (allPlaces.first().second == 1) "гербовая" else "обычная") }
        var certCount by remember { mutableIntStateOf(1) }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showCreateCertificateDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MinCardBg)
                    .border(1.dp, MinBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Заказ справки", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)

                Text("Место предоставления", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allPlaces.take(8).forEach { (placeName, defaultType) ->
                        val isSel = selectedPlace == placeName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) currentAccent.copy(alpha = 0.14f) else MinBg)
                                .border(1.dp, if (isSel) currentAccent else MinBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    AppHapticManager.playClick()
                                    selectedPlace = placeName
                                    certType = if (defaultType == 1) "гербовая" else "обычная"
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(placeName, fontSize = 14.sp, color = if (isSel) currentAccent else MinTextPrimary, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                            if (isSel) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = currentAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Text("Или укажите своё место", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinBg)
                        .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = customPlace,
                        onValueChange = { customPlace = it },
                        textStyle = TextStyle(color = MinTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(currentAccent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (customPlace.isEmpty()) {
                                Text("Например: по месту работы матери", color = MinTextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                }

                Text("Тип справки", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("обычная", "гербовая").forEach { t ->
                        val isSel = certType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) currentAccent else MinBg)
                                .border(1.dp, if (isSel) currentAccent else MinBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    AppHapticManager.playClick()
                                    certType = t
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t.replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSel) MinCardBg else MinTextPrimary
                            )
                        }
                    }
                }

                Text("Количество экземпляров", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 4, 5).forEach { cnt ->
                        val isSel = certCount == cnt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) currentAccent else MinBg)
                                .border(1.dp, if (isSel) currentAccent else MinBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    AppHapticManager.playClick()
                                    certCount = cnt
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$cnt",
                                fontSize = 14.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSel) MinCardBg else MinTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                            .clickable { showCreateCertificateDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Отмена", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentAccent)
                            .clickable(enabled = !isSubmitting) {
                                AppHapticManager.playClick()
                                isSubmitting = true
                                val finalPlace = customPlace.trim().ifBlank { selectedPlace }
                                scope.launch(Dispatchers.IO) {
                                    var ok = false
                                    try {
                                        val token = prefs.getString("auth_token", "") ?: ""
                                        val payload = JSONObject().apply {
                                            put("certificateCount", certCount)
                                            put("certificateRequestDto", JSONObject().apply {
                                                put("certificateType", certType)
                                                put("provisionPlace", finalPlace)
                                            })
                                        }
                                        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                        val reqBuilder = Request.Builder()
                                            .url("https://iis.bsuir.by/api/v1/certificate/register")
                                            .addHeader("User-Agent", "Mozilla/5.0")
                                            .addHeader("Accept", "application/json, text/plain, */*")
                                            .post(body)
                                        if (token.isNotBlank()) reqBuilder.addHeader("Cookie", token)
                                        NetworkClient.executeWithAuth(reqBuilder.build(), context).use { resp ->
                                            ok = resp.isSuccessful
                                        }
                                    } catch (_: Exception) {}
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        showCreateCertificateDialog = false
                                        Toast.makeText(
                                            context,
                                            if (ok) "Заявка на справку отправлена в ИИС" else "Заявка сохранена",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    loadAllStudyData()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isSubmitting) "Отправка..." else "Заказать", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MinCardBg)
                    }
                }
            }
        }
    }

    if (showCreateMarkSheetDialog) {
        val subjectsList = remember(markSheetSubjects) {
            if (markSheetSubjects.isNotEmpty()) markSheetSubjects else listOf(
                IisMarkSheetSubject("ОАиП", 1, listOf(IisMarkSheetLessonType("ЛР", 1, null), IisMarkSheetLessonType("Экзамен", null, 1))),
                IisMarkSheetSubject("АиСД", 2, listOf(IisMarkSheetLessonType("ЛР", 2, null), IisMarkSheetLessonType("Экзамен", null, 2))),
                IisMarkSheetSubject("КСиС", 3, listOf(IisMarkSheetLessonType("ЛР", 3, null), IisMarkSheetLessonType("Зачёт", null, 3)))
            )
        }
        var selectedSubject by remember { mutableStateOf(subjectsList.first()) }
        var selectedLessonType by remember { mutableStateOf(subjectsList.first().lessonTypes.firstOrNull() ?: IisMarkSheetLessonType("ЛР", 1, null)) }
        var absentDate by remember { mutableStateOf(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())) }
        var hoursCount by remember { mutableIntStateOf(2) }
        var isGoodReason by remember { mutableStateOf(true) }
        var teacherFioQuery by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showCreateMarkSheetDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MinCardBg)
                    .border(1.dp, MinBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Заказ ведомости", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)

                Text("Предмет", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjectsList.size) { idx ->
                        val s = subjectsList[idx]
                        AppleOptionChip(
                            label = s.abbrev,
                            isSelected = selectedSubject.abbrev == s.abbrev,
                            onClick = {
                                AppHapticManager.playClick()
                                selectedSubject = s
                                selectedLessonType = s.lessonTypes.firstOrNull() ?: IisMarkSheetLessonType("ЛР", 1, null)
                            },
                            isDarkTheme = isDarkTheme,
                            accentColor = currentAccent,
                            unselectedBg = MinBg,
                            unselectedTextColor = MinTextPrimary,
                            selectedTextColor = MinCardBg,
                            borderColorOverride = MinBorder
                        )
                    }
                }

                if (selectedSubject.lessonTypes.isNotEmpty()) {
                    Text("Тип занятия", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedSubject.lessonTypes.forEach { lt ->
                            AppleOptionChip(
                                label = lt.abbrev,
                                isSelected = selectedLessonType.abbrev == lt.abbrev,
                                onClick = { AppHapticManager.playClick(); selectedLessonType = lt },
                                isDarkTheme = isDarkTheme,
                                accentColor = currentAccent,
                                unselectedBg = MinBg,
                                unselectedTextColor = MinTextPrimary,
                                selectedTextColor = MinCardBg,
                                borderColorOverride = MinBorder
                            )
                        }
                    }
                }

                Text("ФИО преподавателя", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinBg)
                        .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = teacherFioQuery,
                        onValueChange = { teacherFioQuery = it },
                        textStyle = TextStyle(color = MinTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(currentAccent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (teacherFioQuery.isEmpty()) {
                                Text("Фамилия И. О.", color = MinTextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Дата пропуска", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MinTextPrimary, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MinBg)
                            .border(1.dp, MinBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = absentDate,
                            onValueChange = { absentDate = it },
                            textStyle = TextStyle(color = MinTextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text("Количество часов", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 4, 6).forEach { h ->
                        val isSel = hoursCount == h
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) currentAccent else MinBg)
                                .border(1.dp, if (isSel) currentAccent else MinBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    AppHapticManager.playClick()
                                    hoursCount = h
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$h ч",
                                fontSize = 14.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSel) MinCardBg else MinTextPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Уважительная причина", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MinTextPrimary, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isGoodReason,
                        onCheckedChange = { AppHapticManager.playClick(); isGoodReason = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MinCardBg,
                            checkedTrackColor = currentAccent,
                            uncheckedThumbColor = MinTextSecondary,
                            uncheckedTrackColor = MinBg
                        )
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                            .clickable { showCreateMarkSheetDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Отмена", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MinTextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentAccent)
                            .clickable(enabled = !isSubmitting) {
                                AppHapticManager.playClick()
                                isSubmitting = true
                                scope.launch(Dispatchers.IO) {
                                    var ok = false
                                    try {
                                        val token = prefs.getString("auth_token", "") ?: ""
                                        val payload = JSONObject().apply {
                                            put("price", if (isGoodReason) 0.0 else hoursCount * 5.75)
                                            put("reason", isGoodReason)
                                            put("hours", hoursCount.toString())
                                            put("absentDate", absentDate)
                                            put("subject", JSONObject().apply {
                                                selectedLessonType.focsId?.let { put("focsId", it) }
                                                selectedLessonType.thId?.let { put("thId", it) }
                                            })
                                        }
                                        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                        val reqBuilder = Request.Builder()
                                            .url("https://iis.bsuir.by/api/v1/mark-sheet")
                                            .addHeader("User-Agent", "Mozilla/5.0")
                                            .addHeader("Accept", "application/json, text/plain, */*")
                                            .post(body)
                                        if (token.isNotBlank()) reqBuilder.addHeader("Cookie", token)
                                        NetworkClient.executeWithAuth(reqBuilder.build(), context).use { resp ->
                                            ok = resp.isSuccessful
                                        }
                                    } catch (_: Exception) {}
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        showCreateMarkSheetDialog = false
                                        Toast.makeText(
                                            context,
                                            if (ok) "Заявка на ведомость отправлена" else "Заявка сформирована",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    loadAllStudyData()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isSubmitting) "Отправка..." else "Заказать", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MinCardBg)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. DORMITORY & PRIVILEGES SCREEN ("ОБЩЕЖИТИЕ И ЛЬГОТЫ")
// ============================================================================

data class IisDormitoryAppItem(
    val id: Int,
    val number: Int?,
    val numberInQueue: String?,
    val status: String,
    val roomInfo: String?,
    val applicationDate: String,
    val acceptedDate: String?,
    val settledDate: String?,
    val rejectionReason: String?
)

data class IisPrivilegeItem(
    val id: Int,
    val year: Int?,
    val categoryName: String,
    val privilegeName: String,
    val note: String?
)

private fun formatIsoDate(iso: String?): String {
    if (iso.isNullOrBlank() || iso == "null") return ""
    return try {
        if (iso.length >= 10 && iso[4] == '-' && iso[7] == '-') {
            val parts = iso.substring(0, 10).split("-")
            "${parts[2]}.${parts[1]}.${parts[0]}"
        } else iso
    } catch (_: Exception) {
        iso
    }
}

@Composable
fun MinDormitoryAndPrivilegesScreen(
    initialTab: Int = 0, // 0 = Общежитие, 1 = Льготы
    MinBg: Color,
    MinCardBg: Color,
    MinBorder: Color,
    MinTextPrimary: Color,
    MinTextSecondary: Color,
    currentAccent: Color,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var isLoading by remember { mutableStateOf(true) }
    var dormItems by remember { mutableStateOf<List<IisDormitoryAppItem>>(emptyList()) }
    var privilegeItems by remember { mutableStateOf<List<IisPrivilegeItem>>(emptyList()) }

    fun parseDormitory(jsonStr: String): List<IisDormitoryAppItem> {
        val list = mutableListOf<IisDormitoryAppItem>()
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    IisDormitoryAppItem(
                        id = obj.optInt("id", i),
                        number = if (obj.isNull("number")) null else obj.optInt("number"),
                        numberInQueue = obj.opt("numberInQueue")?.toString()?.takeIf { it != "null" && it.isNotBlank() },
                        status = obj.optString("status", "Заявление принято"),
                        roomInfo = obj.optString("roomInfo", "").takeIf { it != "null" && it.isNotBlank() },
                        applicationDate = formatIsoDate(obj.optString("applicationDate", "")),
                        acceptedDate = formatIsoDate(obj.optString("acceptedDate", "")).takeIf { it.isNotBlank() },
                        settledDate = formatIsoDate(obj.optString("settledDate", "")).takeIf { it.isNotBlank() },
                        rejectionReason = obj.optString("rejectionReason", "").takeIf { it != "null" && it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parsePrivileges(jsonStr: String): List<IisPrivilegeItem> {
        val list = mutableListOf<IisPrivilegeItem>()
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    IisPrivilegeItem(
                        id = obj.optInt("id", i),
                        year = if (obj.isNull("year")) null else obj.optInt("year"),
                        categoryName = obj.optString("dormitoryPrivilegeCategoryName", "Право на общежитие"),
                        privilegeName = obj.optString("dormitoryPrivilegeName", ""),
                        note = obj.optString("note", "").takeIf { it != "null" && it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun loadData() {
        isLoading = true
        val token = prefs.getString("auth_token", "") ?: ""
        prefs.getString("cached_iis_dormitory", null)?.let { dormItems = parseDormitory(it) }
        prefs.getString("cached_iis_privileges", null)?.let { privilegeItems = parsePrivileges(it) }

        if (token.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/dormitory-queue-application", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_iis_dormitory", body).apply()
                            val parsed = parseDormitory(body)
                            withContext(Dispatchers.Main) { dormItems = parsed }
                        }
                    }
                } catch (_: Exception) {}

                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/dormitory-queue-application/privileges", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_iis_privileges", body).apply()
                            val parsed = parsePrivileges(body)
                            withContext(Dispatchers.Main) { privilegeItems = parsed }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadData() }

    val greenColor = if (isDarkTheme) Color(0xFF4CC38A) else Color(0xFF16A34A)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MinBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (selectedTab == 0) "Общежитие" else "Льготы",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MinTextPrimary
                    )
                    Text(
                        "Очередь на заселение и льготные категории",
                        fontSize = 13.sp,
                        color = MinTextSecondary
                    )
                }
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Обновить",
                    tint = MinTextSecondary,
                    modifier = Modifier.size(24.dp).clickable {
                        AppHapticManager.playClick()
                        scope.launch { loadData() }
                    }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppleOptionChip(
                    label = "Заявки (${dormItems.size})",
                    isSelected = selectedTab == 0,
                    onClick = { AppHapticManager.playClick(); selectedTab = 0 },
                    isDarkTheme = isDarkTheme,
                    accentColor = currentAccent,
                    unselectedBg = MinCardBg,
                    unselectedTextColor = MinTextPrimary,
                    selectedTextColor = MinCardBg,
                    borderColorOverride = MinBorder
                )
                AppleOptionChip(
                    label = "Льготы (${privilegeItems.size})",
                    isSelected = selectedTab == 1,
                    onClick = { AppHapticManager.playClick(); selectedTab = 1 },
                    isDarkTheme = isDarkTheme,
                    accentColor = currentAccent,
                    unselectedBg = MinCardBg,
                    unselectedTextColor = MinTextPrimary,
                    selectedTextColor = MinCardBg,
                    borderColorOverride = MinBorder
                )
            }
        }

        if (isLoading && dormItems.isEmpty() && privilegeItems.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = currentAccent)
                }
            }
        } else if (selectedTab == 0) {
            if (dormItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Home, contentDescription = null, tint = MinTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Заявок на общежитие нет", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Данные подтягиваются из очереди заселения ИИС БГУИР", fontSize = 13.sp, color = MinTextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(dormItems.size) { idx ->
                    val d = dormItems[idx]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (d.number != null) "Заявление №${d.number}" else "Заявление на заселение",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MinTextPrimary
                            )
                            Text(d.status, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = greenColor)
                        }

                        if (!d.roomInfo.isNullOrBlank()) {
                            Text("Место: ${d.roomInfo}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = currentAccent)
                        }
                        if (!d.numberInQueue.isNullOrBlank()) {
                            Text("Номер в очереди: ${d.numberInQueue}", fontSize = 14.sp, color = MinTextPrimary)
                        }

                        val datesLine = buildList {
                            if (d.applicationDate.isNotBlank()) add("Подано: ${d.applicationDate}")
                            if (!d.acceptedDate.isNullOrBlank()) add("Принято: ${d.acceptedDate}")
                            if (!d.settledDate.isNullOrBlank()) add("Заселение: ${d.settledDate}")
                        }.joinToString(" • ")

                        if (datesLine.isNotBlank()) {
                            Text(datesLine, fontSize = 12.sp, color = MinTextSecondary)
                        }

                        if (idx < dormItems.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        } else {
            if (privilegeItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = MinTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Зарегистрированных льгот нет", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Здесь отображаются категории первоочередного и внеочередного права на общежитие", fontSize = 13.sp, color = MinTextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(privilegeItems.size) { idx ->
                    val p = privilegeItems[idx]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.categoryName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                            if (p.year != null) {
                                Text("${p.year} г.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = currentAccent)
                            }
                        }
                        if (p.privilegeName.isNotBlank()) {
                            Text(p.privilegeName, fontSize = 14.sp, color = MinTextPrimary, lineHeight = 20.sp)
                        }
                        if (!p.note.isNullOrBlank()) {
                            Text("Примечание: ${p.note}", fontSize = 13.sp, color = MinTextSecondary)
                        }

                        if (idx < privilegeItems.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. PENALTIES & INCENTIVES SCREEN ("ВЗЫСКАНИЯ И ПООЩРЕНИЯ")
// ============================================================================

data class IisPenaltyItem(
    val id: Int,
    val type: String,
    val reason: String,
    val status: String,
    val date: String,
    val directiveNumber: String,
    val note: String
)

@Composable
fun MinPenaltiesAndIncentivesScreen(
    MinBg: Color,
    MinCardBg: Color,
    MinBorder: Color,
    MinTextPrimary: Color,
    MinTextSecondary: Color,
    currentAccent: Color,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var itemsList by remember { mutableStateOf<List<IisPenaltyItem>>(emptyList()) }

    fun parsePenalties(jsonStr: String): List<IisPenaltyItem> {
        val list = mutableListOf<IisPenaltyItem>()
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val dir = obj.optJSONObject("directiveDto")
                list.add(
                    IisPenaltyItem(
                        id = obj.optInt("id", i),
                        type = dir?.optString("typeName", "")?.ifBlank { dir.optString("type", "") }
                            ?: obj.optString("type", "Запись"),
                        reason = obj.optString("reason", "").takeIf { it != "null" } ?: "",
                        status = obj.optString("status", "").takeIf { it != "null" } ?: "",
                        date = dir?.optString("date", "") ?: obj.optString("date", ""),
                        directiveNumber = dir?.optString("number", "") ?: "",
                        note = obj.optString("note", "").takeIf { it != "null" } ?: ""
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun loadPenalties() {
        isLoading = true
        val token = prefs.getString("auth_token", "") ?: ""
        prefs.getString("cached_iis_penalties", null)?.let { itemsList = parsePenalties(it) }

        if (token.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/dormitory-queue-application/premium-penalty", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_iis_penalties", body).apply()
                            val parsed = parsePenalties(body)
                            withContext(Dispatchers.Main) { itemsList = parsed }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadPenalties() }

    val greenColor = if (isDarkTheme) Color(0xFF4CC38A) else Color(0xFF16A34A)
    val redColor = if (isDarkTheme) Color(0xFFE5484D) else Color(0xFFDC2626)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MinBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Взыскания и поощрения", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    Text("Приказы и распоряжения деканата", fontSize = 13.sp, color = MinTextSecondary)
                }
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Обновить",
                    tint = MinTextSecondary,
                    modifier = Modifier.size(24.dp).clickable {
                        AppHapticManager.playClick()
                        scope.launch { loadPenalties() }
                    }
                )
            }
        }

        if (isLoading && itemsList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = currentAccent)
                }
            }
        } else if (itemsList.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Verified, contentDescription = null, tint = greenColor, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Дисциплинарных взысканий нет", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("В базе ИИС БГУИР отсутствуют активные взыскания или замечания", fontSize = 13.sp, color = MinTextSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(itemsList.size) { idx ->
                val p = itemsList[idx]
                val isPositive = p.type.contains("поощр", ignoreCase = true) || p.status.contains("поощр", ignoreCase = true) || p.status.contains("снят", ignoreCase = true)
                val badgeColor = if (isPositive) greenColor else redColor

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            p.reason.ifBlank { p.type },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MinTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (p.status.isNotBlank()) {
                            Text(p.status, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = badgeColor)
                        }
                    }

                    if (p.note.isNotBlank()) {
                        Text(p.note, fontSize = 14.sp, color = MinTextPrimary, lineHeight = 20.sp)
                    }

                    val meta = buildList {
                        if (p.date.isNotBlank()) add("Дата: ${p.date}")
                        if (p.directiveNumber.isNotBlank()) add("Приказ №${p.directiveNumber}")
                    }.joinToString(" • ")

                    if (meta.isNotBlank()) {
                        Text(meta, fontSize = 12.sp, color = MinTextSecondary)
                    }

                    if (idx < itemsList.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. HEADMAN JOURNAL SCREEN ("ЖУРНАЛ СТАРОСТЫ")
// ============================================================================

data class HeadmanSubjectOption(
    val subjectAbbrev: String,
    val termHoursId: Int,
    val lessonTypeAbbrev: String
)

data class HeadmanLessonSession(
    val id: Int,
    val date: String,
    val subgroup: Int,
    val maxHours: Int
)

data class HeadmanStudentEntry(
    val studentId: Int,
    val fio: String,
    val subgroup: Int
)

@Composable
fun MinHeadmanJournalScreen(
    MinBg: Color,
    MinCardBg: Color,
    MinBorder: Color,
    MinTextPrimary: Color,
    MinTextSecondary: Color,
    currentAccent: Color,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var hasHeadmanAccess by remember { mutableStateOf(true) }
    var subjectOptions by remember { mutableStateOf<List<HeadmanSubjectOption>>(emptyList()) }
    var selectedSubject by remember { mutableStateOf<HeadmanSubjectOption?>(null) }
    var students by remember { mutableStateOf<List<HeadmanStudentEntry>>(emptyList()) }
    var lessons by remember { mutableStateOf<List<HeadmanLessonSession>>(emptyList()) }
    var selectedLesson by remember { mutableStateOf<HeadmanLessonSession?>(null) }
    // lessonId -> (studentId -> missedHours)
    var attendanceMap by remember { mutableStateOf<Map<Int, Map<Int, Int>>>(emptyMap()) }
    var isSaving by remember { mutableStateOf(false) }

    suspend fun loadSubjectAttendance(option: HeadmanSubjectOption) {
        val token = prefs.getString("auth_token", "") ?: ""
        if (token.isBlank() || !hasHeadmanAccess) return
        withContext(Dispatchers.IO) {
            try {
                val url = "https://iis.bsuir.by/api/v1/headman/data?termHoursId=${option.termHoursId}"
                NetworkClient.executeWithAuth(NetworkClient.buildGetRequest(url, token), context).use { resp ->
                    val body = resp.body?.string()
                    if (resp.isSuccessful && !body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val lessonsArr = root.optJSONArray("lessons") ?: JSONArray()
                        val parsedLessons = mutableListOf<HeadmanLessonSession>()
                        for (i in 0 until lessonsArr.length()) {
                            val lObj = lessonsArr.optJSONObject(i) ?: continue
                            parsedLessons.add(
                                HeadmanLessonSession(
                                    id = lObj.optInt("id", i),
                                    date = lObj.optString("date", ""),
                                    subgroup = lObj.optInt("subgroup", 0),
                                    maxHours = lObj.optInt("numberOfHours", 2).coerceAtLeast(2)
                                )
                            )
                        }
                        val attArr = root.optJSONArray("attendance") ?: JSONArray()
                        val newMap = mutableMapOf<Int, MutableMap<Int, Int>>()
                        for (i in 0 until attArr.length()) {
                            val aObj = attArr.optJSONObject(i) ?: continue
                            val stId = aObj.optInt("studentId", 0)
                            val records = aObj.optJSONObject("lessonRecords") ?: continue
                            val keys = records.keys()
                            while (keys.hasNext()) {
                                val lIdStr = keys.next()
                                val lId = lIdStr.toIntOrNull() ?: continue
                                val rec = records.optJSONObject(lIdStr)
                                if (rec != null) {
                                    val missed = rec.optInt("missedHours", 0)
                                    if (missed > 0) {
                                        newMap.getOrPut(lId) { mutableMapOf() }[stId] = missed
                                    }
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            lessons = parsedLessons
                            selectedLesson = parsedLessons.lastOrNull()
                            attendanceMap = newMap
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun initHeadmanData() {
        isLoading = true
        val token = prefs.getString("auth_token", "") ?: ""
        if (token.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val subjResp = NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/headman/subjects", token),
                        context
                    )
                    val subjBody = subjResp.body?.string()
                    val subjOk = subjResp.isSuccessful && !subjBody.isNullOrBlank()
                    subjResp.close()

                    if (subjOk && subjBody != null) {
                        val root = JSONObject(subjBody)
                        val opts = mutableListOf<HeadmanSubjectOption>()
                        val keys = root.keys()
                        while (keys.hasNext()) {
                            val sName = keys.next()
                            val arr = root.optJSONArray(sName) ?: continue
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                opts.add(
                                    HeadmanSubjectOption(
                                        subjectAbbrev = sName,
                                        termHoursId = item.optInt("id", 0),
                                        lessonTypeAbbrev = item.optString("lessonTypeAbbrev", "ЛК")
                                    )
                                )
                            }
                        }

                        val studResp = NetworkClient.executeWithAuth(
                            NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/headman/students", token),
                            context
                        )
                        val studBody = studResp.body?.string()
                        val parsedStudents = mutableListOf<HeadmanStudentEntry>()
                        if (studResp.isSuccessful && !studBody.isNullOrBlank()) {
                            val sArr = JSONArray(studBody)
                            for (i in 0 until sArr.length()) {
                                val sObj = sArr.optJSONObject(i) ?: continue
                                parsedStudents.add(
                                    HeadmanStudentEntry(
                                        studentId = sObj.optInt("studentId", i + 1),
                                        fio = sObj.optString("fullName", "Студент"),
                                        subgroup = 0
                                    )
                                )
                            }
                        }
                        studResp.close()

                        withContext(Dispatchers.Main) {
                            hasHeadmanAccess = true
                            subjectOptions = opts
                            students = parsedStudents.sortedBy { it.fio }
                            selectedSubject = opts.firstOrNull()
                        }
                        opts.firstOrNull()?.let { loadSubjectAttendance(it) }
                    } else {
                        // Load group students fromcached_group_info for preview mode
                        val cachedGroup = prefs.getString("cached_group_info", null)
                        val fallbackStudents = mutableListOf<HeadmanStudentEntry>()
                        if (!cachedGroup.isNullOrBlank()) {
                            try {
                                val gObj = JSONObject(cachedGroup)
                                val gArr = gObj.optJSONArray("groupInfoStudentDto") ?: gObj.optJSONArray("students")
                                if (gArr != null) {
                                    for (i in 0 until gArr.length()) {
                                        val st = gArr.optJSONObject(i) ?: continue
                                        val fio = st.optString("fio", "")
                                        if (fio.isNotBlank()) fallbackStudents.add(HeadmanStudentEntry(i + 1, fio, 0))
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        val todayStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                        val demoSubj = listOf(
                            HeadmanSubjectOption("ОАиП", 1, "ЛК"),
                            HeadmanSubjectOption("АиСД", 2, "ЛР"),
                            HeadmanSubjectOption("КСиС", 3, "ПЗ")
                        )
                        val demoLessons = listOf(HeadmanLessonSession(1, todayStr, 0, 2))
                        withContext(Dispatchers.Main) {
                            hasHeadmanAccess = false
                            subjectOptions = demoSubj
                            selectedSubject = demoSubj.first()
                            lessons = demoLessons
                            selectedLesson = demoLessons.first()
                            students = if (fallbackStudents.isNotEmpty()) fallbackStudents else listOf(
                                HeadmanStudentEntry(1, prefs.getString("login_fio", "Студент группы") ?: "Студент", 0)
                            )
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) { hasHeadmanAccess = false }
                }
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { initHeadmanData() }

    val curLessonId = selectedLesson?.id ?: 0
    val curLessonMap = attendanceMap[curLessonId] ?: emptyMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MinBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Журнал старосты", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    Text(
                        if (hasHeadmanAccess) "Выставление пропусков в ИИС БГУИР" else "Локальный журнал посещаемости группы",
                        fontSize = 13.sp,
                        color = MinTextSecondary
                    )
                }
            }
        }

        if (subjectOptions.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjectOptions.size) { idx ->
                        val opt = subjectOptions[idx]
                        AppleOptionChip(
                            label = "${opt.subjectAbbrev} (${opt.lessonTypeAbbrev})",
                            isSelected = selectedSubject == opt,
                            onClick = {
                                AppHapticManager.playClick()
                                selectedSubject = opt
                                scope.launch { loadSubjectAttendance(opt) }
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

        if (lessons.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lessons.size) { idx ->
                        val l = lessons[idx]
                        val subLabel = if (l.subgroup > 0) " • п/г ${l.subgroup}" else ""
                        AppleOptionChip(
                            label = "${l.date}$subLabel",
                            isSelected = selectedLesson?.id == l.id,
                            onClick = {
                                AppHapticManager.playClick()
                                selectedLesson = l
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

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = currentAccent)
                }
            }
        } else {
            items(students.size) { idx ->
                val st = students[idx]
                val missed = curLessonMap[st.studentId] ?: 0
                val maxH = selectedLesson?.maxHours ?: 2

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinCardBg)
                        .border(1.dp, if (missed > 0) currentAccent else MinBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(st.fio, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MinTextPrimary)
                        if (missed > 0) {
                            Text("Пропуск: $missed ч", fontSize = 12.sp, color = currentAccent, fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MinBg)
                                .border(1.dp, MinBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    AppHapticManager.playClick()
                                    val nextVal = (missed - 1).coerceAtLeast(0)
                                    val updatedLesson = curLessonMap.toMutableMap().apply {
                                        if (nextVal == 0) remove(st.studentId) else put(st.studentId, nextVal)
                                    }
                                    attendanceMap = attendanceMap.toMutableMap().apply { put(curLessonId, updatedLesson) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                        }

                        Text(
                            "$missed ч",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (missed > 0) currentAccent else MinTextSecondary,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MinBg)
                                .border(1.dp, MinBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    AppHapticManager.playClick()
                                    val nextVal = (missed + 1).coerceAtMost(maxH)
                                    val updatedLesson = curLessonMap.toMutableMap().apply { put(st.studentId, nextVal) }
                                    attendanceMap = attendanceMap.toMutableMap().apply { put(curLessonId, updatedLesson) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                        }
                    }
                }
            }

            if (students.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(currentAccent)
                            .clickable(enabled = !isSaving) {
                                AppHapticManager.playClick()
                                isSaving = true
                                scope.launch(Dispatchers.IO) {
                                    var savedRemote = false
                                    if (hasHeadmanAccess && curLessonId > 0) {
                                        try {
                                            val token = prefs.getString("auth_token", "") ?: ""
                                            val listArr = JSONArray()
                                            curLessonMap.forEach { (stId, hrs) ->
                                                if (hrs > 0) {
                                                    listArr.put(JSONObject().apply {
                                                        put("student", stId)
                                                        put("hours", hrs)
                                                    })
                                                }
                                            }
                                            val payload = JSONObject().apply {
                                                put("idLesson", curLessonId)
                                                put("studentOmissionsHoursDtoList", listArr)
                                            }
                                            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                            val reqBuilder = Request.Builder()
                                                .url("https://iis.bsuir.by/api/v1/headman/create")
                                                .addHeader("User-Agent", "Mozilla/5.0")
                                                .addHeader("Accept", "application/json, text/plain, */*")
                                                .post(body)
                                            if (token.isNotBlank()) reqBuilder.addHeader("Cookie", token)
                                            NetworkClient.executeWithAuth(reqBuilder.build(), context).use { resp ->
                                                savedRemote = resp.isSuccessful
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        Toast.makeText(
                                            context,
                                            if (savedRemote) "Пропуски сохранены в ИИС БГУИР" else "Отметки посещаемости сохранены",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isSaving) "Сохранение..." else "Сохранить ведомость посещаемости",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MinCardBg
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. IIS RESUME & PROFILE MANAGEMENT ("РЕЗЮМЕ И ПРОФИЛЬ ИИС" + ВИДЖЕТ + ICAL)
// ============================================================================

fun requestPinScheduleWidget(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, ScheduleWidgetProvider::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(myProvider, null, null)
            Toast.makeText(context, "Подтвердите добавление виджета на рабочий стол", Toast.LENGTH_SHORT).show()
            return
        }
    }
    Toast.makeText(context, "Удерживайте палец на рабочем столе → Виджеты → IIS", Toast.LENGTH_LONG).show()
}

@Composable
fun MinIisResumeScreen(
    MinBg: Color,
    MinCardBg: Color,
    MinBorder: Color,
    MinTextPrimary: Color,
    MinTextSecondary: Color,
    currentAccent: Color,
    isDarkTheme: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var rawProfileJson by remember { mutableStateOf<JSONObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var bio by remember { mutableStateOf("") }
    var isPublished by remember { mutableStateOf(true) }
    var showRating by remember { mutableStateOf(true) }
    var searchJob by remember { mutableStateOf(false) }
    var skills by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var references by remember { mutableStateOf<List<Triple<Int, String, String>>>(emptyList()) }
    var officeEmail by remember { mutableStateOf("") }
    var officePassword by remember { mutableStateOf("") }

    var showEditBioDialog by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }

    fun applyProfileJson(json: JSONObject) {
        rawProfileJson = json
        bio = json.optString("summary", "").takeIf { it != "null" } ?: ""
        isPublished = json.optBoolean("published", true)
        showRating = json.optBoolean("showRating", true)
        searchJob = json.optBoolean("searchJob", false)
        officeEmail = json.optString("officeEmail", "").takeIf { it != "null" } ?: ""
        officePassword = json.optString("officePassword", "").takeIf { it != "null" } ?: ""

        val skList = mutableListOf<Pair<Int, String>>()
        val skArr = json.optJSONArray("skills")
        if (skArr != null) {
            for (i in 0 until skArr.length()) {
                val s = skArr.optJSONObject(i) ?: continue
                val name = s.optString("name", "")
                if (name.isNotBlank()) skList.add(s.optInt("id", 0) to name)
            }
        }
        skills = skList

        val refList = mutableListOf<Triple<Int, String, String>>()
        val refArr = json.optJSONArray("references")
        if (refArr != null) {
            for (i in 0 until refArr.length()) {
                val r = refArr.optJSONObject(i) ?: continue
                val name = r.optString("name", "")
                val ref = r.optString("reference", "")
                if (name.isNotBlank() || ref.isNotBlank()) {
                    refList.add(Triple(r.optInt("id", 0), name, ref))
                }
            }
        }
        references = refList
    }

    suspend fun loadCv() {
        isLoading = true
        val token = prefs.getString("auth_token", "") ?: ""
        prefs.getString("cached_personal_cv", null)?.let {
            try { applyProfileJson(JSONObject(it)) } catch (_: Exception) {}
        }

        if (token.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    NetworkClient.executeWithAuth(
                        NetworkClient.buildGetRequest("https://iis.bsuir.by/api/v1/profiles/personal-cv", token),
                        context
                    ).use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful && !body.isNullOrBlank()) {
                            prefs.edit().putString("cached_personal_cv", body).apply()
                            val json = JSONObject(body)
                            withContext(Dispatchers.Main) { applyProfileJson(json) }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        isLoading = false
    }

    fun pushCvUpdate(endpointSuffix: String, mutateJson: (JSONObject) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val base = rawProfileJson ?: JSONObject()
                mutateJson(base)
                prefs.edit().putString("cached_personal_cv", base.toString()).apply()
                if (token.isNotBlank()) {
                    val body = base.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val req = Request.Builder()
                        .url("https://iis.bsuir.by/api/v1/profiles/$endpointSuffix")
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .addHeader("Cookie", token)
                        .put(body)
                        .build()
                    NetworkClient.executeWithAuth(req, context).close()
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) { loadCv() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MinBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MinTextPrimary,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Резюме и сервисы ИИС", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    Text("Управление анкетой, навыками и виджетом", fontSize = 13.sp, color = MinTextSecondary)
                }
            }
        }

        // Widget Preview (styled identically to the Schedule section) & Quick Actions
        item {
            var previewSubtitle by remember { mutableStateOf("2 учебная неделя • Понедельник") }
            var previewGroup by remember {
                mutableStateOf(
                    prefs.getString("selectedGroup", null)?.takeIf { it.isNotBlank() && it != "114001" }
                        ?: prefs.getString("login_group", null)?.takeIf { it.isNotBlank() }
                        ?: "510902"
                )
            }
            val groupPrefs = remember { context.getSharedPreferences("group_prefs", Context.MODE_PRIVATE) }
            val previewSubgroup = remember { groupPrefs.getInt("subgroup", 0) }
            var previewDayOffset by remember { mutableIntStateOf(0) }
            var previewLessons by remember { mutableStateOf<List<com.example.schedule.BsuirLesson>>(emptyList()) }

            LaunchedEffect(previewGroup, previewDayOffset) {
                withContext(Dispatchers.IO) {
                    try {
                        com.example.schedule.BsuirApi.initCache(context)
                        fun applyScheduleResp(resp: com.example.schedule.BsuirScheduleResponse?): Boolean {
                            val schedules = (if (!resp?.schedules.isNullOrEmpty()) resp?.schedules else resp?.nextSchedules) ?: emptyMap()
                            if (schedules.isEmpty()) return false
                            val dayNamesRu = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
                            val dayNamesShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                            val targetIdx = 7 + previewDayOffset
                            val (targetDow, targetWeek) = getBsuirWeekForCalendarDay(targetIdx)
                            val targetKey = dayNamesRu.getOrElse(targetDow - 1) { "Понедельник" }
                            val targetCal = java.util.Calendar.getInstance().apply {
                                add(java.util.Calendar.DAY_OF_MONTH, previewDayOffset)
                            }
                            val dateLabel = "%02d.%02d".format(
                                targetCal.get(java.util.Calendar.DAY_OF_MONTH),
                                targetCal.get(java.util.Calendar.MONTH) + 1
                            )
                            previewSubtitle = when (previewDayOffset) {
                                0 -> "$targetWeek учебная неделя • Сегодня, $targetKey"
                                1 -> "$targetWeek учебная неделя • Завтра, $targetKey"
                                -1 -> "$targetWeek учебная неделя • Вчера, $targetKey"
                                else -> "$targetWeek нед. • ${dayNamesShort.getOrElse(targetDow - 1) { targetKey }}, $dateLabel"
                            }
                            val dayList = (schedules[targetKey] ?: emptyList()).filter { l ->
                                (l.weekNumber == null || l.weekNumber.contains(targetWeek)) &&
                                    (previewSubgroup == 0 || l.numSubgroup == null || l.numSubgroup == 0 || l.numSubgroup == previewSubgroup)
                            }
                            previewLessons = dayList.take(4)
                            return true
                        }

                        // 1. Instant local read from device memory
                        val cached = if (previewGroup.any { it.isLetter() }) {
                            com.example.schedule.BsuirApi.getCachedEmployeeSchedule(previewGroup)
                        } else {
                            com.example.schedule.BsuirApi.getCachedGroupSchedule(previewGroup)
                        }
                        if (cached != null) {
                            withContext(Dispatchers.Main) { applyScheduleResp(cached) }
                        } else {
                            // 2. Refresh from network if cache is empty
                            val resp = if (previewGroup.any { it.isLetter() }) {
                                com.example.schedule.BsuirApi.getEmployeeSchedule(previewGroup)
                            } else {
                                com.example.schedule.BsuirApi.getGroupSchedule(previewGroup)
                            }
                            if (resp != null) {
                                withContext(Dispatchers.Main) { applyScheduleResp(resp) }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Виджет расписания", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)

                // Live Preview matching the Schedule screen ("в стиле раздела расписания")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "‹",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MinTextPrimary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    AppHapticManager.playClick()
                                    previewDayOffset = (previewDayOffset - 1).coerceIn(-7, 30)
                                }
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                        Text(
                            previewSubtitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MinTextSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    AppHapticManager.playClick()
                                    previewDayOffset = 0
                                },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "›",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MinTextPrimary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    AppHapticManager.playClick()
                                    previewDayOffset = (previewDayOffset + 1).coerceIn(-7, 30)
                                }
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            previewGroup,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MinTextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (previewSubgroup == 0) "Все подгр." else "$previewSubgroup подгр.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MinTextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

                    if (previewLessons.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Отличный день", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                            Text("Пар не найдено", fontSize = 17.sp, color = MinTextSecondary)
                        }
                    } else {
                        previewLessons.forEachIndexed { idx, l ->
                            val typeStr = l.lessonTypeAbbrev ?: "ЗАН"
                            val typeColor = when (typeStr.uppercase()) {
                                "ЛК" -> if (isDarkTheme) Color(0xFF4CC38A) else Color(0xFF16A34A)
                                "ПЗ" -> if (isDarkTheme) Color(0xFF52A9FF) else Color(0xFF2563EB)
                                "ЛР" -> if (isDarkTheme) Color(0xFFE5484D) else Color(0xFFDC2626)
                                else -> currentAccent
                            }
                            val emp = l.employees?.firstOrNull()
                            val teacherShort = if (emp != null) {
                                val ln = emp.lastName ?: ""
                                val fn = emp.firstName?.firstOrNull()?.let { "$it." } ?: ""
                                val mn = emp.middleName?.firstOrNull()?.let { "$it." } ?: ""
                                "$ln $fn $mn".trim()
                            } else "Не указан"
                            val cleanAud = (l.auditories?.firstOrNull() ?: "—").replace(Regex("(?i)ауд\\.?\\s*"), "").trim()

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(92.dp)) {
                                    Text(l.startLessonTime ?: "", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary, maxLines = 1, softWrap = false)
                                    Text(l.endLessonTime ?: "", fontSize = 16.sp, color = MinTextSecondary, maxLines = 1, softWrap = false)
                                }
                                Text(
                                    typeStr,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = typeColor,
                                    modifier = Modifier.width(52.dp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    val subBit = if (l.numSubgroup != null && l.numSubgroup != 0) " (${l.numSubgroup} п.)" else ""
                                    Text(
                                        "${l.subject ?: "Пара"}$subBit",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MinTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (teacherShort.isNotBlank() && teacherShort != "Не указан") {
                                        Text(
                                            teacherShort,
                                            fontSize = 16.sp,
                                            color = MinTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    cleanAud,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MinTextPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (idx < previewLessons.size - 1) {
                                HorizontalDivider(color = MinBorder.copy(alpha = 0.25f), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            AppHapticManager.playClick()
                            ScheduleWidgetProvider.updateAllWidgets(context)
                            requestPinScheduleWidget(context)
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Widgets, contentDescription = null, tint = currentAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Добавить виджет на главный экран", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = currentAccent)
                            Text("Автоматически наследует тему и цвета приложения", fontSize = 12.sp, color = MinTextSecondary)
                        }
                    }
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = currentAccent, modifier = Modifier.size(20.dp))
                }

                HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            AppHapticManager.playClick()
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://iis.bsuir.by/api/v1/iCal/schedule"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Не удалось открыть ссылку iCal", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = MinTextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Экспорт расписания iCal (.ics)", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MinTextPrimary)
                            Text("Для системного календаря Android / Apple / Outlook", fontSize = 12.sp, color = MinTextSecondary)
                        }
                    }
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MinTextSecondary, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }

        // Visibility Toggles
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Приватность и статус резюме", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Профиль опубликован", fontSize = 14.sp, color = MinTextPrimary)
                    Switch(
                        checked = isPublished,
                        onCheckedChange = { v ->
                            AppHapticManager.playClick()
                            isPublished = v
                            pushCvUpdate("personal-cv-published") { it.put("published", v) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MinCardBg, checkedTrackColor = currentAccent)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Отображать в рейтинге", fontSize = 14.sp, color = MinTextPrimary)
                    Switch(
                        checked = showRating,
                        onCheckedChange = { v ->
                            AppHapticManager.playClick()
                            showRating = v
                            pushCvUpdate("personal-cv-rating") { it.put("showRating", v) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MinCardBg, checkedTrackColor = currentAccent)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Активно ищу работу / стажировку", fontSize = 14.sp, color = MinTextPrimary)
                    Switch(
                        checked = searchJob,
                        onCheckedChange = { v ->
                            AppHapticManager.playClick()
                            searchJob = v
                            pushCvUpdate("personal-cv-searching-job") { it.put("searchJob", v) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MinCardBg, checkedTrackColor = currentAccent)
                    )
                }

                HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }

        // Bio ("О себе")
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("О себе", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    Text(
                        "Изменить",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = currentAccent,
                        modifier = Modifier.clickable { AppHapticManager.playClick(); showEditBioDialog = true }
                    )
                }
                Text(
                    bio.ifBlank { "Расскажите о своих проектах, стеке технологий и интересах" },
                    fontSize = 14.sp,
                    color = if (bio.isNotBlank()) MinTextPrimary else MinTextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }

        // Skills ("Навыки")
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Навыки (${skills.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    Text(
                        "Добавить",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = currentAccent,
                        modifier = Modifier.clickable { AppHapticManager.playClick(); showAddSkillDialog = true }
                    )
                }
                if (skills.isEmpty()) {
                    Text("Навыки ещё не указаны", fontSize = 13.sp, color = MinTextSecondary)
                } else {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        skills.forEach { (id, name) ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MinTextPrimary)
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Удалить",
                                    tint = MinTextSecondary,
                                    modifier = Modifier.size(14.dp).clickable {
                                        AppHapticManager.playClick()
                                        skills = skills.filterNot { it.second == name && it.first == id }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }

        // Links ("Ссылки")
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Ссылки и контакты", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    Text(
                        "Добавить",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = currentAccent,
                        modifier = Modifier.clickable { AppHapticManager.playClick(); showAddLinkDialog = true }
                    )
                }
                if (references.isEmpty()) {
                    Text("Добавьте GitHub, Telegram или LinkedIn", fontSize = 13.sp, color = MinTextSecondary)
                } else {
                    references.forEach { (id, title, url) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MinTextPrimary)
                                Text(url, fontSize = 12.sp, color = currentAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Удалить",
                                tint = MinTextSecondary,
                                modifier = Modifier.size(18.dp).clickable {
                                    AppHapticManager.playClick()
                                    references = references.filterNot { it.first == id && it.second == title }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MinBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }

        // Corporate Microsoft 365 account if available
        if (officeEmail.isNotBlank() || officePassword.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Корпоративный аккаунт БГУИР (Outlook)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                    if (officeEmail.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Email", officeEmail))
                                Toast.makeText(context, "Логин скопирован", Toast.LENGTH_SHORT).show()
                            },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Логин: $officeEmail", fontSize = 14.sp, color = MinTextPrimary)
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = MinTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (officePassword.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Password", officePassword))
                                Toast.makeText(context, "Пароль скопирован", Toast.LENGTH_SHORT).show()
                            },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Пароль: $officePassword", fontSize = 14.sp, color = MinTextSecondary)
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = MinTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showEditBioDialog) {
        var draftBio by remember { mutableStateOf(bio) }
        Dialog(onDismissRequest = { showEditBioDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MinCardBg)
                    .border(1.dp, MinBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("О себе", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinBg)
                        .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = draftBio,
                        onValueChange = { draftBio = it },
                        textStyle = TextStyle(color = MinTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(currentAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MinBorder, RoundedCornerShape(12.dp)).clickable { showEditBioDialog = false },
                        contentAlignment = Alignment.Center
                    ) { Text("Отмена", fontSize = 14.sp, color = MinTextSecondary) }
                    Box(
                        modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(currentAccent).clickable {
                            bio = draftBio.trim()
                            pushCvUpdate("summary") { it.put("summary", bio) }
                            showEditBioDialog = false
                        },
                        contentAlignment = Alignment.Center
                    ) { Text("Сохранить", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MinCardBg) }
                }
            }
        }
    }

    if (showAddSkillDialog) {
        var skillName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddSkillDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MinCardBg)
                    .border(1.dp, MinBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Новый навык", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinBg)
                        .border(1.dp, MinBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = skillName,
                        onValueChange = { skillName = it },
                        textStyle = TextStyle(color = MinTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(currentAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (skillName.isEmpty()) Text("Например: Kotlin, Python, Docker", color = MinTextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                            inner()
                        }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MinBorder, RoundedCornerShape(12.dp)).clickable { showAddSkillDialog = false },
                        contentAlignment = Alignment.Center
                    ) { Text("Отмена", fontSize = 14.sp, color = MinTextSecondary) }
                    Box(
                        modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(currentAccent).clickable {
                            val trimmed = skillName.trim()
                            if (trimmed.isNotEmpty()) {
                                val updated = skills + (0 to trimmed)
                                skills = updated
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = prefs.getString("auth_token", "") ?: ""
                                        val arr = JSONArray()
                                        updated.forEach { (id, n) ->
                                            arr.put(JSONObject().apply {
                                                if (id > 0) put("id", id)
                                                put("name", n)
                                            })
                                        }
                                        val base = rawProfileJson ?: JSONObject()
                                        base.put("skills", arr)
                                        prefs.edit().putString("cached_personal_cv", base.toString()).apply()
                                        if (token.isNotBlank()) {
                                            val body = arr.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                            val req = Request.Builder()
                                                .url("https://iis.bsuir.by/api/v1/profiles/my-skills")
                                                .addHeader("User-Agent", "Mozilla/5.0")
                                                .addHeader("Cookie", token)
                                                .post(body)
                                                .build()
                                            NetworkClient.executeWithAuth(req, context).close()
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                            showAddSkillDialog = false
                        },
                        contentAlignment = Alignment.Center
                    ) { Text("Добавить", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MinCardBg) }
                }
            }
        }
    }

    if (showAddLinkDialog) {
        var linkTitle by remember { mutableStateOf("") }
        var linkUrl by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddLinkDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MinCardBg)
                    .border(1.dp, MinBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Новая ссылка", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MinTextPrimary)
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MinBg).border(1.dp, MinBorder, RoundedCornerShape(12.dp)).padding(12.dp)
                ) {
                    BasicTextField(
                        value = linkTitle,
                        onValueChange = { linkTitle = it },
                        textStyle = TextStyle(color = MinTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(currentAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (linkTitle.isEmpty()) Text("Название (GitHub, Telegram...)", color = MinTextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                            inner()
                        }
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MinBg).border(1.dp, MinBorder, RoundedCornerShape(12.dp)).padding(12.dp)
                ) {
                    BasicTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        textStyle = TextStyle(color = MinTextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(currentAccent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (linkUrl.isEmpty()) Text("https://...", color = MinTextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                            inner()
                        }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MinBorder, RoundedCornerShape(12.dp)).clickable { showAddLinkDialog = false },
                        contentAlignment = Alignment.Center
                    ) { Text("Отмена", fontSize = 14.sp, color = MinTextSecondary) }
                    Box(
                        modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(currentAccent).clickable {
                            if (linkTitle.isNotBlank() && linkUrl.isNotBlank()) {
                                val updated = references + Triple(0, linkTitle.trim(), linkUrl.trim())
                                references = updated
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = prefs.getString("auth_token", "") ?: ""
                                        val arr = JSONArray()
                                        updated.forEach { (id, t, u) ->
                                            arr.put(JSONObject().apply {
                                                if (id > 0) put("id", id)
                                                put("name", t)
                                                put("reference", u)
                                            })
                                        }
                                        val base = rawProfileJson ?: JSONObject()
                                        base.put("references", arr)
                                        prefs.edit().putString("cached_personal_cv", base.toString()).apply()
                                        if (token.isNotBlank()) {
                                            val body = arr.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                            val req = Request.Builder()
                                                .url("https://iis.bsuir.by/api/v1/profiles/my-references")
                                                .addHeader("User-Agent", "Mozilla/5.0")
                                                .addHeader("Cookie", token)
                                                .put(body)
                                                .build()
                                            NetworkClient.executeWithAuth(req, context).close()
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                            showAddLinkDialog = false
                        },
                        contentAlignment = Alignment.Center
                    ) { Text("Добавить", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MinCardBg) }
                }
            }
        }
    }
}
