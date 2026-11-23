package com.aliskazem.livetv

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val FOOTBALL_DATA_API_KEY = "b11ae4a8257e485a83389f1ef51b8df8"
private const val API_FOOTBALL_KEY = "cff0d26710ee2ab9ac363f5c81cf74b1"

enum class CategoryId {
    BEIN, ALWAN, ALKASS, ABUDHABI, THMANYAH
}

enum class MatchFeedStatus {
    SCHEDULED, LIVE, HALFTIME, FINISHED
}

data class ChannelItem(
    val id: String,
    val name: String,
    val url: String,
    val backupUrl: String? = null,
    val logoAssetName: String,
    val badgeText: String,
    val badgeBg: Color,
    val badgeTextColor: Color = Color.White
)

data class CategoryItem(
    val id: CategoryId,
    val name: String,
    val logoAssetName: String,
    val badgeText: String,
    val badgeBg: Color,
    val badgeTextColor: Color = Color.White,
    val channels: List<ChannelItem>
)

data class LiveMatchItem(
    val id: String,
    val utcIso: String,
    val baghdadDate: String,
    val timeText: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeCrestUrl: String? = null,
    val awayCrestUrl: String? = null,
    val league: String,
    val leagueCode: String? = null,
    val leagueLogoUrl: String? = null,
    val status: MatchFeedStatus,
    val minute: String? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null
)

data class StandingRowItem(
    val position: Int,
    val teamName: String,
    val crestUrl: String? = null,
    val played: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val goalDiff: Int,
    val points: Int
)

data class LeagueOption(
    val code: String,
    val shortLabel: String
)

data class SocialAccountItem(
    val id: String,
    val title: String,
    val url: String,
    val logoAssetName: String,
    val fallbackGlyph: String,
    val badgeBg: Color,
    val badgeTextColor: Color = Color.White
)

private data class DayOption(
    val key: String,
    val dayTitle: String,
    val dateLabel: String
)

private val ScreenBg = Color(0xFF060B08)
private val PanelBg = Color(0xFF0A140F)
private val TileBg = Color(0xFF07120C)
private val TileInnerBg = Color(0xFF040B07)
private val BorderGreen = Color(0xFF143322)
private val ActiveGreen = Color(0xFF10B981)
private val LiveRed = Color(0xFFEF4444)
private val TextWhite = Color(0xFFF3F6F4)
private val TextMuted = Color(0xFF8FA398)
private val LightBadgeBg = Color(0xFFF3F6F4)

private val STANDING_LEAGUES = listOf(
    LeagueOption("PL", "الدوري الإنجليزي الممتاز"),
    LeagueOption("PD", "الدوري الإسباني"),
    LeagueOption("SA", "الدوري الإيطالي"),
    LeagueOption("BL1", "الدوري الألماني"),
    LeagueOption("FL1", "الدوري الفرنسي"),
    LeagueOption("CL", "دوري الأبطال")
)

private val SOCIAL_ACCOUNTS = listOf(
    SocialAccountItem(
        id = "tiktok",
        title = "TikTok",
        url = "https://www.tiktok.com/@ali.kazem70",
        logoAssetName = "tiktok",
        fallbackGlyph = "♪",
        badgeBg = Color(0xFF0A140F),
        badgeTextColor = Color.White
    ),
    SocialAccountItem(
        id = "telegram",
        title = "Telegram",
        url = "https://t.me/Alikazem200",
        logoAssetName = "telegram",
        fallbackGlyph = "✈",
        badgeBg = Color(0xFF1D9BF0),
        badgeTextColor = Color.White
    )
)

private val TEAM_ARABIC_NAMES: Map<String, String> = mapOf(
    "arsenal fc" to "آرسنال",
    "arsenal" to "آرسنال",
    "brighton & hove albion fc" to "برايتون",
    "brighton hove" to "برايتون",
    "brighton" to "برايتون",
    "manchester city fc" to "مانشستر سيتي",
    "man city" to "مانشستر سيتي",
    "manchester city" to "مانشستر سيتي",
    "liverpool fc" to "ليفربول",
    "liverpool" to "ليفربول",
    "afc bournemouth" to "بورنموث",
    "bournemouth" to "بورنموث",
    "leeds united fc" to "ليدز يونايتد",
    "leeds united" to "ليدز يونايتد",
    "everton fc" to "إيفرتون",
    "everton" to "إيفرتون",
    "chelsea fc" to "تشيلسي",
    "chelsea" to "تشيلسي",
    "tottenham hotspur fc" to "توتنهام",
    "tottenham" to "توتنهام",
    "brentford fc" to "برينتفورد",
    "brentford" to "برينتفورد",
    "hull city afc" to "هال سيتي",
    "hull city" to "هال سيتي",
    "aston villa fc" to "أستون فيلا",
    "aston villa" to "أستون فيلا",
    "newcastle united fc" to "نيوكاسل",
    "newcastle" to "نيوكاسل",
    "coventry city fc" to "كوفنتري سيتي",
    "coventry city" to "كوفنتري سيتي",
    "fulham fc" to "فولهام",
    "fulham" to "فولهام",
    "ipswich town fc" to "إيبسويتش تاون",
    "ipswich town" to "إيبسويتش تاون",
    "nottingham forest fc" to "نوتينغهام فورست",
    "nottingham" to "نوتينغهام فورست",
    "sunderland afc" to "سندرلاند",
    "sunderland" to "سندرلاند",
    "manchester united fc" to "مانشستر يونايتد",
    "man united" to "مانشستر يونايتد",
    "manchester united" to "مانشستر يونايتد",
    "crystal palace fc" to "كريستال بالاس",
    "crystal palace" to "كريستال بالاس",

    "fc barcelona" to "برشلونة",
    "barcelona" to "برشلونة",
    "barça" to "برشلونة",
    "barca" to "برشلونة",
    "real madrid cf" to "ريال مدريد",
    "real madrid" to "ريال مدريد",
    "club atlético de madrid" to "أتلتيكو مدريد",
    "atletico madrid" to "أتلتيكو مدريد",
    "atleti" to "أتلتيكو مدريد",
    "real betis balompié" to "ريال بيتيس",
    "real betis" to "ريال بيتيس",
    "real sociedad de fútbol" to "ريال سوسيداد",
    "real sociedad" to "ريال سوسيداد",
    "sevilla fc" to "إشبيلية",
    "sevilla" to "إشبيلية",
    "santander" to "راسينغ سانتاندير",
    "racing santander" to "راسينغ سانتاندير",
    "athletic club" to "أتلتيك بيلباو",
    "athletic" to "أتلتيك بيلباو",
    "deportivo alavés" to "ديبورتيفو ألافيس",
    "alavés" to "ديبورتيفو ألافيس",
    "alaves" to "ديبورتيفو ألافيس",
    "villarreal cf" to "فياريال",
    "villarreal" to "فياريال",
    "rcd espanyol de barcelona" to "إسبانيول",
    "espanyol" to "إسبانيول",
    "deportivo" to "ديبورتيفو لاكورونيا",
    "ca osasuna" to "أوساسونا",
    "osasuna" to "أوساسونا",
    "rc celta de vigo" to "سيلتا فيغو",
    "celta" to "سيلتا فيغو",
    "rayo vallecano de madrid" to "رايو فاليكانو",
    "rayo vallecano" to "رايو فاليكانو",
    "valencia cf" to "فالنسيا",
    "valencia" to "فالنسيا",
    "getafe cf" to "خيتافي",
    "getafe" to "خيتافي",
    "levante ud" to "ليفانتي",
    "levante" to "ليفانتي",
    "elche cf" to "إلتشي",
    "elche" to "إلتشي",
    "málaga cf" to "ملقا",
    "málaga" to "ملقا",
    "malaga" to "ملقا",

    "ss lazio" to "لاتسيو",
    "lazio" to "لاتسيو",
    "as roma" to "روما",
    "roma" to "روما",
    "fc internazionale milano" to "إنتر ميلان",
    "inter" to "إنتر ميلان",
    "inter milan" to "إنتر ميلان",
    "ac milan" to "ميلان",
    "milan" to "ميلان",
    "como 1907" to "كومو",
    "como" to "كومو",
    "juventus fc" to "يوفنتوس",
    "juventus" to "يوفنتوس",
    "frosinone calcio" to "فروسينوني",
    "frosinone" to "فروسينوني",
    "ssc napoli" to "نابولي",
    "napoli" to "نابولي",
    "bologna fc 1909" to "بولونيا",
    "bologna" to "بولونيا",
    "genoa cfc" to "جنوى",
    "genoa" to "جنوى",
    "cagliari calcio" to "كالياري",
    "cagliari" to "كالياري",
    "atalanta bc" to "أتالانتا",
    "atalanta" to "أتالانتا",
    "torino fc" to "تورينو",
    "torino" to "تورينو",
    "us lecce" to "ليتشي",
    "lecce" to "ليتشي",
    "acf fiorentina" to "فيورنتينا",
    "fiorentina" to "فيورنتينا",
    "parma calcio 1913" to "بارما",
    "parma" to "بارما",
    "us sassuolo calcio" to "ساسولو",
    "sassuolo" to "ساسولو",
    "ac monza" to "مونزا",
    "monza" to "مونزا",
    "udinese calcio" to "أودينيزي",
    "udinese" to "أودينيزي",
    "venezia fc" to "فينيزيا",
    "venezia" to "فينيزيا",

    "borussia dortmund" to "بوروسيا دورتموند",
    "dortmund" to "بوروسيا دورتموند",
    "fc bayern münchen" to "بايرن ميونخ",
    "bayern munich" to "بايرن ميونخ",
    "bayern" to "بايرن ميونخ",
    "sc freiburg" to "فرايبورغ",
    "freiburg" to "فرايبورغ",
    "rb leipzig" to "لايبزيغ",
    "leipzig" to "لايبزيغ",
    "bayer 04 leverkusen" to "باير ليفركوزن",
    "leverkusen" to "باير ليفركوزن",
    "sv werder bremen" to "فيردر بريمن",
    "bremen" to "فيردر بريمن",
    "eintracht frankfurt" to "آينتراخت فرانكفورت",
    "frankfurt" to "آينتراخت فرانكفورت",
    "vfb stuttgart" to "شتوتغارت",
    "stuttgart" to "شتوتغارت",
    "sv elversberg" to "إلفيرسبيرغ",
    "elversberg" to "إلفيرسبيرغ",
    "fc augsburg" to "أوغسبورغ",
    "augsburg" to "أوغسبورغ",
    "tsg 1899 hoffenheim" to "هوفنهايم",
    "hoffenheim" to "هوفنهايم",
    "hamburger sv" to "هامبورغ",
    "hsv" to "هامبورغ",
    "1. fsv mainz 05" to "ماينتس",
    "mainz" to "ماينتس",
    "1. fc köln" to "كولن",
    "fc köln" to "كولن",
    "sc paderborn 07" to "بادربورن",
    "sc paderborn" to "بادربورن",
    "fc schalke 04" to "شالكه",
    "schalke" to "شالكه",
    "1. fc union berlin" to "يونيون برلين",
    "union berlin" to "يونيون برلين",
    "borussia mönchengladbach" to "بوروسيا مونشنغلادباخ",
    "m'gladbach" to "بوروسيا مونشنغلادباخ",

    "paris saint-germain fc" to "باريس سان جيرمان",
    "paris saint-germain" to "باريس سان جيرمان",
    "psg" to "باريس سان جيرمان",
    "paris fc" to "باريس إف سي",
    "lille osc" to "ليل",
    "lille" to "ليل",
    "olympique lyonnais" to "أولمبيك ليون",
    "olympique lyon" to "أولمبيك ليون",
    "olympique de marseille" to "مارسيليا",
    "marseille" to "مارسيليا",
    "stade brestois 29" to "بريست",
    "brest" to "بريست",
    "ogc nice" to "نيس",
    "nice" to "نيس",
    "as monaco fc" to "موناكو",
    "monaco" to "موناكو",
    "stade rennais fc 1901" to "رين",
    "stade rennais" to "رين",
    "fc lorient" to "لوريان",
    "lorient" to "لوريان",
    "rc lens" to "لانس",
    "toulouse fc" to "تولوز",
    "toulouse" to "تولوز",
    "le mans fc" to "لو مان",
    "le mans" to "لو مان",
    "estac troyes" to "تروا",
    "troyes" to "تروا",
    "aj auxerre" to "أوكسير",
    "auxerre" to "أوكسير",
    "rc strasbourg alsace" to "ستراسبورغ",
    "strasbourg" to "ستراسبورغ",
    "angers sco" to "أنجيه",
    "le havre ac" to "لو هافر",
    "le havre" to "لو هافر",

    "club brugge kv" to "كلوب بروج",
    "club brugge" to "كلوب بروج",
    "sporting clube de portugal" to "سبورتينغ لشبونة",
    "sporting cp" to "سبورتينغ لشبونة",
    "lask" to "لاسك",
    "galatasaray sk" to "غلطة سراي",
    "galatasaray" to "غلطة سراي",
    "fenerbahçe sk" to "فنربخشة",
    "fenerbahçe" to "فنربخشة",
    "fc shakhtar donetsk" to "شاختار دونيتسك",
    "shaktar" to "شاختار دونيتسك",
    "viking fk" to "فايكنغ",
    "viking" to "فايكنغ",
    "psv eindhoven" to "بي إس في آيندهوفن",
    "psv" to "بي إس في آيندهوفن",
    "fk bodø/glimt" to "بودو غليمت",
    "bodø/glimt" to "بودو غليمت",
    "bodo/glimt" to "بودو غليمت",
    "fc porto" to "بورتو",
    "porto" to "بورتو",
    "pae aek" to "أيك أثينا",
    "aek athens" to "أيك أثينا",
    "sabah fk" to "صباح",
    "sabah" to "صباح",
    "feyenoord rotterdam" to "فينورد",
    "feyenoord" to "فينورد",
    "sk slavia praha" to "سلافيا براغ",
    "slavia praha" to "سلافيا براغ",
    "sl. bratislava" to "سلوفان براتيسلافا",
    "slovan bratislava" to "سلوفان براتيسلافا"
)

private object CrestMemoryCache {
    private val cache = mutableMapOf<String, Bitmap>()

    @Synchronized
    fun get(url: String): Bitmap? = cache[url]

    @Synchronized
    fun put(url: String, bitmap: Bitmap) {
        if (cache.size > 220) {
            cache.clear()
        }
        cache[url] = bitmap
    }
}

private suspend fun fetchBitmapFromUrl(urlString: String): Bitmap? {
    if (urlString.isBlank()) return null
    CrestMemoryCache.get(urlString)?.let { return it }
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 9000
                readTimeout = 9000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode in 200..299) {
                val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    CrestMemoryCache.put(urlString, bmp)
                }
                bmp
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

private fun isBrazilianCompetition(text: String): Boolean {
    val normalized = text.lowercase(Locale.ROOT).trim()
    val blockedKeywords = listOf(
        "brazil",
        "brazilian",
        "brasileirao",
        "brasileirão",
        "campeonato brasileiro",
        "serie a brazil",
        "série a brazil",
        "الدوري البرازيلي",
        "البرازيلي",
        "برازيل"
    )
    return blockedKeywords.any { normalized.contains(it) }
}

private fun detectLeagueCode(rawName: String, code: String? = null): String? {
    val cleanCode = code?.trim()?.uppercase(Locale.ROOT)
    if (!cleanCode.isNullOrBlank()) {
        return cleanCode
    }
    val lower = rawName.lowercase(Locale.ROOT)
    return when {
        lower.contains("premier league") || rawName.contains("الإنجليزي") -> "PL"
        lower.contains("la liga") || lower.contains("primera") || rawName.contains("الإسباني") -> "PD"
        lower.contains("serie a") || rawName.contains("الإيطالي") -> "SA"
        lower.contains("bundesliga") || rawName.contains("الألماني") -> "BL1"
        lower.contains("ligue 1") || rawName.contains("الفرنسي") -> "FL1"
        lower.contains("champions") || rawName.contains("الأبطال") -> "CL"
        else -> null
    }
}

private fun translateCompetitionName(rawName: String, code: String? = null): String {
    val resolvedCode = detectLeagueCode(rawName, code)
    val clean = rawName.trim()
    val lower = clean.lowercase(Locale.ROOT)
    return when (resolvedCode) {
        "PL" -> "الدوري الإنجليزي الممتاز"
        "PD" -> "الدوري الإسباني"
        "SA" -> "الدوري الإيطالي"
        "BL1" -> "الدوري الألماني"
        "FL1" -> "الدوري الفرنسي"
        "CL" -> "دوري أبطال أوروبا"
        "ELC" -> "دوري البطولة الإنجليزية"
        else -> when {
            lower.contains("egypt") || clean.contains("المصري") -> "الدوري المصري"
            lower.contains("iraq") || clean.contains("العراقي") -> "الدوري العراقي"
            else -> clean.ifBlank { "بطولة رياضية" }
        }
    }
}

private fun translateTeamName(rawTeam: String): String {
    val clean = rawTeam.trim()
    return TEAM_ARABIC_NAMES[clean.lowercase(Locale.ROOT)] ?: clean
}

private fun buildBaghdadDays(): List<DayOption> {
    val baghdadZone = ZoneId.of("Asia/Baghdad")
    val today = LocalDate.now(baghdadZone)
    val arabicDays = mapOf(
        1 to "الإثنين",
        2 to "الثلاثاء",
        3 to "الأربعاء",
        4 to "الخميس",
        5 to "الجمعة",
        6 to "السبت",
        7 to "الأحد"
    )
    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    return (0..6).map { offset ->
        val d = today.plusDays(offset.toLong())
        val dayName = arabicDays[d.dayOfWeek.value] ?: ""
        val monthName = arabicMonths.getOrElse(d.monthValue - 1) { "" }
        DayOption(
            key = d.format(isoFormatter),
            dayTitle = dayName,
            dateLabel = "${d.dayOfMonth} $monthName"
        )
    }
}

private fun httpGetJson(urlString: String, headers: Map<String, String> = emptyMap()): String? {
    var connection: HttpURLConnection? = null
    return try {
        val url = URL(urlString)
        connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

private fun parseFootballDataStatus(statusRaw: String): MatchFeedStatus {
    return when (statusRaw.uppercase(Locale.ROOT)) {
        "FINISHED", "AWARDED" -> MatchFeedStatus.FINISHED
        "PAUSED", "HT" -> MatchFeedStatus.HALFTIME
        "IN_PLAY", "LIVE" -> MatchFeedStatus.LIVE
        else -> MatchFeedStatus.SCHEDULED
    }
}

private suspend fun fetchMatchesWithoutBrazil(dateFrom: String, dateTo: String): List<LiveMatchItem> {
    return withContext(Dispatchers.IO) {
        val url = "https://api.football-data.org/v4/matches?dateFrom=$dateFrom&dateTo=$dateTo"
        val body = httpGetJson(url, mapOf("X-Auth-Token" to FOOTBALL_DATA_API_KEY)) ?: return@withContext emptyList()

        val result = mutableListOf<LiveMatchItem>()
        val root = JSONObject(body)
        val matchesArr = root.optJSONArray("matches") ?: JSONArray()

        val baghdadZone = ZoneId.of("Asia/Baghdad")
        val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

        for (i in 0 until matchesArr.length()) {
            val item = matchesArr.optJSONObject(i) ?: continue
            val compObj = item.optJSONObject("competition")
            val areaObj = item.optJSONObject("area")

            val compNameRaw = compObj?.optString("name", "") ?: ""
            val compCode = compObj?.optString("code", "") ?: ""
            val areaNameRaw = areaObj?.optString("name", "") ?: ""

            if (
                isBrazilianCompetition(compNameRaw) ||
                isBrazilianCompetition(areaNameRaw) ||
                compCode.equals("BSA", ignoreCase = true)
            ) {
                continue
            }

            val utcDateStr = item.optString("utcDate", "")
            if (utcDateStr.isBlank()) continue

            val instant = try {
                Instant.parse(utcDateStr)
            } catch (_: Exception) {
                continue
            }

            val zdtBaghdad = instant.atZone(baghdadZone)
            val bDate = zdtBaghdad.toLocalDate().format(dateFmt)
            val bTime = zdtBaghdad.toLocalTime().format(timeFmt)

            val homeObj = item.optJSONObject("homeTeam")
            val awayObj = item.optJSONObject("awayTeam")
            val homeName = translateTeamName(
                homeObj?.optString("shortName")?.takeIf { it.isNotBlank() }
                    ?: homeObj?.optString("name", "الفريق الأول")
                    ?: "الفريق الأول"
            )
            val awayName = translateTeamName(
                awayObj?.optString("shortName")?.takeIf { it.isNotBlank() }
                    ?: awayObj?.optString("name", "الفريق الثاني")
                    ?: "الفريق الثاني"
            )
            val homeCrest = homeObj?.optString("crest")?.takeIf {
                it.isNotBlank() && it != "null" && !it.endsWith(".svg", ignoreCase = true)
            }
            val awayCrest = awayObj?.optString("crest")?.takeIf {
                it.isNotBlank() && it != "null" && !it.endsWith(".svg", ignoreCase = true)
            }

            val scoreObj = item.optJSONObject("score")
            val fullTimeObj = scoreObj?.optJSONObject("fullTime")
            val homeScore = fullTimeObj?.opt("home")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()
            val awayScore = fullTimeObj?.opt("away")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()

            val status = parseFootballDataStatus(item.optString("status", "SCHEDULED"))
            val minuteVal = item.opt("minute")?.takeIf { it != JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }
            val resolvedCode = detectLeagueCode(compNameRaw, compCode)

            result.add(
                LiveMatchItem(
                    id = item.optString("id", "fd_$i"),
                    utcIso = utcDateStr,
                    baghdadDate = bDate,
                    timeText = bTime,
                    homeTeam = homeName,
                    awayTeam = awayName,
                    homeCrestUrl = homeCrest,
                    awayCrestUrl = awayCrest,
                    league = translateCompetitionName(compNameRaw, resolvedCode),
                    leagueCode = resolvedCode,
                    leagueLogoUrl = null,
                    status = status,
                    minute = minuteVal,
                    homeScore = homeScore,
                    awayScore = awayScore
                )
            )
        }

        result.sortedBy { it.utcIso }
    }
}

private suspend fun fetchStandingsForLeague(leagueCode: String): List<StandingRowItem> {
    if (isBrazilianCompetition(leagueCode) || leagueCode.equals("BSA", ignoreCase = true)) {
        return emptyList()
    }
    return withContext(Dispatchers.IO) {
        val url = "https://api.football-data.org/v4/competitions/$leagueCode/standings"
        val body = httpGetJson(url, mapOf("X-Auth-Token" to FOOTBALL_DATA_API_KEY)) ?: return@withContext emptyList()

        val root = JSONObject(body)
        val standingsArr = root.optJSONArray("standings") ?: JSONArray()
        var totalStandingObj: JSONObject? = null
        for (i in 0 until standingsArr.length()) {
            val sObj = standingsArr.optJSONObject(i) ?: continue
            if (sObj.optString("type").equals("TOTAL", ignoreCase = true)) {
                totalStandingObj = sObj
                break
            }
        }
        val tableArr = (totalStandingObj ?: standingsArr.optJSONObject(0))?.optJSONArray("table") ?: JSONArray()

        val rows = mutableListOf<StandingRowItem>()
        for (i in 0 until tableArr.length()) {
            val row = tableArr.optJSONObject(i) ?: continue
            val teamObj = row.optJSONObject("team")
            val teamName = translateTeamName(
                teamObj?.optString("shortName")?.takeIf { it.isNotBlank() }
                    ?: teamObj?.optString("name", "")
                    ?: ""
            )
            if (teamName.isBlank()) continue
            val crest = teamObj?.optString("crest")?.takeIf {
                it.isNotBlank() && it != "null" && !it.endsWith(".svg", ignoreCase = true)
            }

            rows.add(
                StandingRowItem(
                    position = row.optInt("position", i + 1),
                    teamName = teamName,
                    crestUrl = crest,
                    played = row.optInt("playedGames", 0),
                    won = row.optInt("won", 0),
                    draw = row.optInt("draw", 0),
                    lost = row.optInt("lost", 0),
                    goalDiff = row.optInt("goalDifference", 0),
                    points = row.optInt("points", 0)
                )
            )
        }
        rows
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appLogoAsset = "img_1793667103213"

        val beinChannels = listOf(
            ChannelItem(
                id = "bein-1",
                name = "bein sport 1",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153582",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7256",
                logoAssetName = "bs1",
                badgeText = "beIN 1",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-2",
                name = "bein sport 2",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153583",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/55414",
                logoAssetName = "bs2",
                badgeText = "beIN 2",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-3",
                name = "bein sport 3",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153584",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/64692",
                logoAssetName = "bs3",
                badgeText = "beIN 3",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-4",
                name = "bein sport 4",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7754",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7262",
                logoAssetName = "bs4",
                badgeText = "beIN 4",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-5",
                name = "bein sport 5",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153587",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7261",
                logoAssetName = "bs5",
                badgeText = "beIN 5",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-6",
                name = "bein sport 6",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153588",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7260",
                logoAssetName = "bs6",
                badgeText = "beIN 6",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-7",
                name = "bein sport 7",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153589",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7259",
                logoAssetName = "bs7",
                badgeText = "beIN 7",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-8",
                name = "bein sport 8",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153590",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7258",
                logoAssetName = "bs8",
                badgeText = "beIN 8",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-9",
                name = "bein sport 9",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/153591",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/7257",
                logoAssetName = "bs9",
                badgeText = "beIN 9",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-10",
                name = "bein sport",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237786",
                backupUrl = null,
                logoAssetName = "bs10",
                badgeText = "beIN HD",
                badgeBg = Color(0xFF4C1D95)
            ),
            ChannelItem(
                id = "bein-news",
                name = "bein sport news",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/102546",
                backupUrl = null,
                logoAssetName = "bs11",
                badgeText = "beIN NEWS",
                badgeBg = Color(0xFF4C1D95)
            )
        )

        val alwanChannels = listOf(
            ChannelItem(
                id = "alwan-1",
                name = "Alwan Sport 1",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237015",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237016",
                logoAssetName = "alw1",
                badgeText = "ALWAN 1",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80)
            ),
            ChannelItem(
                id = "alwan-2",
                name = "Alwan Sport 2",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237018",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237019",
                logoAssetName = "alw2",
                badgeText = "ALWAN 2",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80)
            ),
            ChannelItem(
                id = "alwan-3",
                name = "Alwan Sport 3",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237021",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237022",
                logoAssetName = "alw3",
                badgeText = "ALWAN 3",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80)
            ),
            ChannelItem(
                id = "alwan-4",
                name = "Alwan Sport 4",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237024",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237025",
                logoAssetName = "alw4",
                badgeText = "ALWAN 4",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80)
            ),
            ChannelItem(
                id = "alwan-5",
                name = "Alwan Sport 5",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237027",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237028",
                logoAssetName = "alw5",
                badgeText = "ALWAN 5",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80)
            ),
            ChannelItem(
                id = "alwan-6",
                name = "Alwan Sport 6",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237029",
                backupUrl = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/237029",
                logoAssetName = "alw6",
                badgeText = "ALWAN 6",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80)
            )
        )

        val alkassChannels = listOf(
            ChannelItem(
                id = "alkass-1",
                name = "Alkass Sport 1",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/102199",
                logoAssetName = "alk1",
                badgeText = "الكأس 1",
                badgeBg = Color(0xFF881337)
            ),
            ChannelItem(
                id = "alkass-2",
                name = "Alkass Sport 2",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/102200",
                logoAssetName = "alk2",
                badgeText = "الكأس 2",
                badgeBg = Color(0xFF881337)
            ),
            ChannelItem(
                id = "alkass-3",
                name = "Alkass Sport 3",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/102201",
                logoAssetName = "alk3",
                badgeText = "الكأس 3",
                badgeBg = Color(0xFF881337)
            ),
            ChannelItem(
                id = "alkass-4",
                name = "Alkass Sport 4",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/102202",
                logoAssetName = "alk4",
                badgeText = "الكأس 4",
                badgeBg = Color(0xFF881337)
            ),
            ChannelItem(
                id = "alkass-5",
                name = "Alkass Sport 5",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/102203",
                logoAssetName = "alk5",
                badgeText = "الكأس 5",
                badgeBg = Color(0xFF881337)
            ),
            ChannelItem(
                id = "alkass-6",
                name = "Alkass Sport 6",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/164857",
                logoAssetName = "alk6",
                badgeText = "الكأس 6",
                badgeBg = Color(0xFF881337)
            ),
            ChannelItem(
                id = "alkass-7",
                name = "Alkass Sport 7",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/164857",
                logoAssetName = "alk7",
                badgeText = "الكأس 7",
                badgeBg = Color(0xFF881337)
            )
        )

        val abudhabiChannels = listOf(
            ChannelItem(
                id = "ad-1",
                name = "Abu Dhabi Sports 1",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/3341",
                logoAssetName = "ad1",
                badgeText = "أبوظبي 1",
                badgeBg = Color(0xFF0B1C14)
            ),
            ChannelItem(
                id = "ad-2",
                name = "Abu Dhabi Sports 2",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/3340",
                logoAssetName = "ad2",
                badgeText = "أبوظبي 2",
                badgeBg = Color(0xFF0B1C14)
            ),
            ChannelItem(
                id = "adb-1",
                name = "Abu Dhabi Premium 1",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/177284",
                logoAssetName = "adb1",
                badgeText = "AD Prem 1",
                badgeBg = Color(0xFF0B1C14)
            ),
            ChannelItem(
                id = "adb-2",
                name = "Abu Dhabi Premium 2",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/177286",
                logoAssetName = "adb2",
                badgeText = "AD Prem 2",
                badgeBg = Color(0xFF0B1C14)
            )
        )

        val thmanyahChannels = listOf(
            ChannelItem(
                id = "thmanyah-1",
                name = "Thmanyah 1",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/236891",
                logoAssetName = "thmanyah",
                badgeText = "▲ 1",
                badgeBg = Color(0xFF0A1710)
            ),
            ChannelItem(
                id = "thmanyah-2",
                name = "Thmanyah 2",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/236892",
                logoAssetName = "thmanyah",
                badgeText = "▲ 2",
                badgeBg = Color(0xFF0A1710)
            ),
            ChannelItem(
                id = "thmanyah-3",
                name = "Thmanyah 3",
                url = "http://legendking.net:8080/yzidalaoui/1EH0eGLDvDas1Ba/236893",
                logoAssetName = "thmanyah",
                badgeText = "▲ 3",
                badgeBg = Color(0xFF0A1710)
            )
        )

        val categories = listOf(
            CategoryItem(
                id = CategoryId.BEIN,
                name = "bein sport",
                logoAssetName = "bein",
                badgeText = "beIN\nSPORTS",
                badgeBg = Color(0xFF4C1D95),
                channels = beinChannels
            ),
            CategoryItem(
                id = CategoryId.ALWAN,
                name = "Alwan Sport",
                logoAssetName = "alwan",
                badgeText = "ALWAN\nSPORT",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80),
                channels = alwanChannels
            ),
            CategoryItem(
                id = CategoryId.ALKASS,
                name = "Alkass Sport",
                logoAssetName = "alkass",
                badgeText = "┐",
                badgeBg = Color(0xFF881337),
                channels = alkassChannels
            ),
            CategoryItem(
                id = CategoryId.ABUDHABI,
                name = "Abu Dhabi Sports",
                logoAssetName = "abudhabi",
                badgeText = "أبوظبي الرياضية",
                badgeBg = Color(0xFF0B1C14),
                channels = abudhabiChannels
            ),
            CategoryItem(
                id = CategoryId.THMANYAH,
                name = "Thmanyah Sports",
                logoAssetName = "thmanyah",
                badgeText = "▲",
                badgeBg = Color(0xFF0A1710),
                channels = thmanyahChannels
            )
        )

        val todayStr = try {
            LocalDate.now(ZoneId.of("Asia/Baghdad")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        }

        val fallbackMatches = listOf(
            LiveMatchItem(
                id = "m1",
                utcIso = "2026-11-23T18:00:00Z",
                baghdadDate = todayStr,
                timeText = "21:00",
                homeTeam = "المنتخب العراقي",
                awayTeam = "خصم دولي",
                league = "مباراة دولية",
                status = MatchFeedStatus.LIVE,
                minute = "64"
            ),
            LiveMatchItem(
                id = "m2",
                utcIso = "2026-11-23T20:00:00Z",
                baghdadDate = todayStr,
                timeText = "23:00",
                homeTeam = "ريال مدريد",
                awayTeam = "برشلونة",
                league = "الدوري الإسباني",
                leagueCode = "PD",
                status = MatchFeedStatus.SCHEDULED
            ),
            LiveMatchItem(
                id = "m3",
                utcIso = "2026-11-23T16:30:00Z",
                baghdadDate = todayStr,
                timeText = "19:30",
                homeTeam = "مانشستر سيتي",
                awayTeam = "آرسنال",
                league = "الدوري الإنجليزي الممتاز",
                leagueCode = "PL",
                status = MatchFeedStatus.SCHEDULED
            )
        )

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = ActiveGreen,
                    background = ScreenBg,
                    surface = PanelBg
                )
            ) {
                AliKazemSportApp(
                    appLogoAsset = appLogoAsset,
                    categories = categories,
                    fallbackMatches = fallbackMatches,
                    accounts = SOCIAL_ACCOUNTS
                )
            }
        }
    }
}

@Composable
fun AliKazemSportApp(
    appLogoAsset: String,
    categories: List<CategoryItem>,
    fallbackMatches: List<LiveMatchItem>,
    accounts: List<SocialAccountItem>
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var isArabic by remember { mutableStateOf(true) }
    var activeCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedChannel by remember { mutableStateOf<ChannelItem?>(null) }
    var useBackupStream by remember { mutableStateOf(false) }

    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (selectedChannel != null) {
                FullScreenStreamPlayer(
                    channel = selectedChannel!!,
                    useBackupStream = useBackupStream,
                    onToggleBackup = { useBackupStream = !useBackupStream },
                    onBackToChannels = {
                        selectedChannel = null
                        useBackupStream = false
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(PanelBg)
                            .border(1.dp, BorderGreen, RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ActiveGreen)
                                .clickable { isArabic = !isArabic }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "عربي" else "EN",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = if (isArabic) "علي كاظم سبورت" else "Ali Kazem Sport",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF1B5E20), Color(0xFF071A0E))
                                    )
                                )
                                .border(2.dp, ActiveGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val logoDrawableId = rememberDrawableId(appLogoAsset)
                            if (logoDrawableId != 0) {
                                Image(
                                    painter = painterResource(id = logoDrawableId),
                                    contentDescription = "Ali Kazem SPORTS",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = "Ali Kazem\nSPORTS",
                                    color = Color(0xFFFEF5E9),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 9.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(PanelBg)
                            .border(1.dp, BorderGreen, RoundedCornerShape(20.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopNavTab(
                            label = if (isArabic) "قنوات البث" else "Live TV",
                            iconGlyph = "▣",
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                        TopNavTab(
                            label = if (isArabic) "جدول المباريات" else "Matches",
                            iconGlyph = "▦",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        TopNavTab(
                            label = if (isArabic) "حساباتي" else "Accounts",
                            iconGlyph = "⑂",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (selectedTab) {
                        2 -> {
                            val currentCategory = activeCategory
                            if (currentCategory == null) {
                                CategoriesGridView(
                                    categories = categories,
                                    onSelectCategory = { cat -> activeCategory = cat }
                                )
                            } else {
                                CategoryChannelsListView(
                                    category = currentCategory,
                                    selectedChannel = selectedChannel,
                                    onBackToCategories = { activeCategory = null },
                                    onSelectChannel = { ch ->
                                        selectedChannel = ch
                                        useBackupStream = false
                                    }
                                )
                            }
                        }
                        1 -> MatchesAndStandingsView(fallbackMatches = fallbackMatches)
                        else -> AccountsTabView(accounts = accounts)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernLeagueArtwork(
    leagueCode: String?,
    fallbackLabel: String = "",
    modifier: Modifier = Modifier
) {
    val code = leagueCode?.trim()?.uppercase(Locale.ROOT)
        ?: detectLeagueCode(fallbackLabel, null)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) / 2f

            when (code) {
                "PL" -> {
                    drawCircle(color = Color(0xFF3D195B), radius = r * 0.94f)
                    val crownPath = Path().apply {
                        moveTo(cx - r * 0.46f, cy + r * 0.25f)
                        lineTo(cx - r * 0.50f, cy - r * 0.24f)
                        lineTo(cx - r * 0.17f, cy + r * 0.02f)
                        lineTo(cx, cy - r * 0.38f)
                        lineTo(cx + r * 0.17f, cy + r * 0.02f)
                        lineTo(cx + r * 0.50f, cy - r * 0.24f)
                        lineTo(cx + r * 0.46f, cy + r * 0.25f)
                        close()
                    }
                    drawPath(path = crownPath, color = Color(0xFF00FF85))
                }
                "CL" -> {
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0A2E8A), Color(0xFF1A73E8))
                        ),
                        radius = r * 0.94f
                    )
                    drawCircle(
                        color = Color(0xFF7DD3FC),
                        radius = r * 0.62f,
                        style = Stroke(width = r * 0.12f)
                    )
                    val starR = r * 0.30f
                    val starPath = Path()
                    for (i in 0 until 10) {
                        val rad = if (i % 2 == 0) starR else starR * 0.45f
                        val angle = (i * 36.0 - 90.0) * PI / 180.0
                        val px = cx + (rad * cos(angle)).toFloat()
                        val py = cy + (rad * sin(angle)).toFloat()
                        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                    }
                    starPath.close()
                    drawPath(path = starPath, color = Color.White)
                }
                "FL1" -> {
                    drawCircle(color = Color(0xFF071C24), radius = r * 0.94f)
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = r * 0.72f,
                        style = Stroke(width = r * 0.12f)
                    )
                    val wave = Path().apply {
                        moveTo(cx - r * 0.46f, cy + r * 0.05f)
                        quadraticBezierTo(cx - r * 0.15f, cy - r * 0.36f, cx, cy)
                        quadraticBezierTo(cx + r * 0.15f, cy + r * 0.36f, cx + r * 0.46f, cy - r * 0.05f)
                    }
                    drawPath(
                        path = wave,
                        color = Color(0xFF38BDF8),
                        style = Stroke(width = r * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "PD" -> {
                    drawCircle(color = Color(0xFF14181F), radius = r * 0.94f)
                    drawArc(
                        color = Color(0xFFFF4B4B),
                        startAngle = -35f,
                        sweepAngle = 250f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.65f, cy - r * 0.65f),
                        size = Size(r * 1.3f, r * 1.3f),
                        style = Stroke(width = r * 0.18f, cap = StrokeCap.Round)
                    )
                }
                "SA" -> {
                    drawCircle(color = Color(0xFF0B2C5D), radius = r * 0.94f)
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = r * 0.56f,
                        style = Stroke(width = r * 0.14f)
                    )
                }
                "BL1" -> {
                    drawCircle(color = Color(0xFFD3010C), radius = r * 0.94f)
                    drawCircle(color = Color.White, radius = r * 0.25f)
                }
                else -> {
                    drawCircle(color = ActiveGreen, radius = r * 0.72f)
                }
            }
        }
    }
}

@Composable
private fun RemoteCrestBadge(
    crestUrl: String?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
    lightBackground: Boolean = false,
    leagueCode: String? = null
) {
    val resolvedLeague = leagueCode ?: detectLeagueCode(fallbackLabel, null)
    var bitmap by remember(crestUrl, resolvedLeague) {
        mutableStateOf(
            if (resolvedLeague != null) null else crestUrl?.let { CrestMemoryCache.get(it) }
        )
    }

    LaunchedEffect(crestUrl, resolvedLeague) {
        if (resolvedLeague == null && bitmap == null && !crestUrl.isNullOrBlank()) {
            bitmap = fetchBitmapFromUrl(crestUrl)
        }
    }

    val containerBg = if (lightBackground) LightBadgeBg else TileInnerBg

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(containerBg)
            .border(1.dp, BorderGreen, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedLeague != null) {
            ModernLeagueArtwork(
                leagueCode = resolvedLeague,
                fallbackLabel = fallbackLabel
            )
        } else {
            val loaded = bitmap
            if (loaded != null) {
                Image(
                    bitmap = loaded.asImageBitmap(),
                    contentDescription = fallbackLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                )
            } else {
                Text(
                    text = fallbackLabel.take(2),
                    color = if (lightBackground) ScreenBg else ActiveGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MatchesAndStandingsView(fallbackMatches: List<LiveMatchItem>) {
    val scope = rememberCoroutineScope()
    val dayOptions = remember { buildBaghdadDays() }

    var subMode by remember { mutableIntStateOf(0) }
    var selectedDayKey by remember { mutableStateOf(dayOptions.first().key) }
    var matches by remember { mutableStateOf<List<LiveMatchItem>>(emptyList()) }
    var isLoadingMatches by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    var selectedLeague by remember { mutableStateOf(STANDING_LEAGUES.first()) }
    var standingsRows by remember { mutableStateOf<List<StandingRowItem>>(emptyList()) }
    var isLoadingStandings by remember { mutableStateOf(false) }

    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            tick++
        }
    }

    fun loadMatches(manual: Boolean = false) {
        scope.launch {
            if (manual) isRefreshing = true else if (matches.isEmpty()) isLoadingMatches = true
            val fromDate = dayOptions.first().key
            val toDate = dayOptions.last().key
            val fetched = fetchMatchesWithoutBrazil(fromDate, toDate)
            if (fetched.isNotEmpty()) {
                matches = fetched
            } else if (matches.isEmpty()) {
                matches = fallbackMatches.filter { !isBrazilianCompetition(it.league) }
            }
            isLoadingMatches = false
            isRefreshing = false
        }
    }

    fun loadStandings(code: String) {
        scope.launch {
            isLoadingStandings = true
            standingsRows = fetchStandingsForLeague(code)
            isLoadingStandings = false
        }
    }

    LaunchedEffect(Unit) {
        loadMatches(manual = false)
    }

    LaunchedEffect(subMode, selectedLeague) {
        if (subMode == 1) {
            loadStandings(selectedLeague.code)
        }
    }

    val dayMatches = remember(matches, selectedDayKey) {
        matches.filter {
            it.baghdadDate == selectedDayKey &&
                !isBrazilianCompetition(it.league) &&
                !isBrazilianCompetition(it.homeTeam) &&
                !isBrazilianCompetition(it.awayTeam)
        }
    }

    val selectedDayInfo = dayOptions.find { it.key == selectedDayKey } ?: dayOptions.first()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBg)
                .border(1.dp, BorderGreen, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subMode == 0) ActiveGreen else Color.Transparent)
                    .clickable { subMode = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "جدول المباريات",
                    color = if (subMode == 0) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subMode == 1) ActiveGreen else Color.Transparent)
                    .clickable { subMode = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "الترتيب",
                    color = if (subMode == 1) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (subMode == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelBg)
                    .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayOptions.forEach { day ->
                    val active = day.key == selectedDayKey
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) ActiveGreen else Color.Transparent)
                            .clickable { selectedDayKey = day.key }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayTitle,
                            color = if (active) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = day.dateLabel,
                            color = if (active) Color.White.copy(alpha = 0.85f) else TextMuted.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "مباريات ${selectedDayInfo.dayTitle}",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${dayMatches.size} مباراة",
                        color = ActiveGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ActiveGreen.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PanelBg)
                            .border(1.dp, BorderGreen, RoundedCornerShape(8.dp))
                            .clickable { loadMatches(manual = true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = ActiveGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoadingMatches || isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ActiveGreen)
                }
            } else if (dayMatches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PanelBg)
                        .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مباريات لهذا اليوم ضمن البطولات المعتمدة",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(dayMatches, key = { it.id }) { match ->
                        val statusBadge = remember(match, tick) {
                            buildMatchStatusBadge(match)
                        }
                        val hasScore = match.homeScore != null && match.awayScore != null &&
                            (match.status == MatchFeedStatus.LIVE ||
                                match.status == MatchFeedStatus.HALFTIME ||
                                match.status == MatchFeedStatus.FINISHED)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PanelBg)
                                .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RemoteCrestBadge(
                                        crestUrl = match.leagueLogoUrl,
                                        fallbackLabel = match.league,
                                        modifier = Modifier.size(20.dp),
                                        lightBackground = true,
                                        leagueCode = match.leagueCode
                                    )
                                    Text(
                                        text = match.league,
                                        color = ActiveGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = statusBadge.label,
                                    color = if (statusBadge.isLive) Color.White else ActiveGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (statusBadge.isLive) LiveRed else ActiveGreen.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RemoteCrestBadge(
                                        crestUrl = match.homeCrestUrl,
                                        fallbackLabel = match.homeTeam
                                    )
                                    Text(
                                        text = match.homeTeam,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = if (hasScore) "${match.homeScore} - ${match.awayScore}" else match.timeText,
                                    color = if (hasScore) ActiveGreen else TextWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TileInnerBg)
                                        .border(1.dp, BorderGreen, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = match.awayTeam,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    RemoteCrestBadge(
                                        crestUrl = match.awayCrestUrl,
                                        fallbackLabel = match.awayTeam
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                STANDING_LEAGUES.forEach { league ->
                    val selected = league.code == selectedLeague.code
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) ActiveGreen else PanelBg)
                            .border(1.dp, if (selected) ActiveGreen else BorderGreen, RoundedCornerShape(12.dp))
                            .clickable { selectedLeague = league }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        RemoteCrestBadge(
                            crestUrl = null,
                            fallbackLabel = league.shortLabel,
                            modifier = Modifier.size(18.dp),
                            lightBackground = true,
                            leagueCode = league.code
                        )
                        Text(
                            text = league.shortLabel,
                            color = if (selected) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoadingStandings) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ActiveGreen)
                }
            } else if (standingsRows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PanelBg)
                        .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تعذر تحميل جدول الترتيب حالياً، أعد المحاولة لاحقاً",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TileBg)
                        .border(1.dp, BorderGreen, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    Text("الفريق", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("لعب", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("ف", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("ت", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("خ", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("نقاط", color = ActiveGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(standingsRows, key = { "${it.position}_${it.teamName}" }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PanelBg)
                                .border(1.dp, BorderGreen, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${row.position}",
                                color = if (row.position <= 4) ActiveGreen else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(24.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RemoteCrestBadge(
                                    crestUrl = row.crestUrl,
                                    fallbackLabel = row.teamName
                                )
                                Text(
                                    text = row.teamName,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text("${row.played}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("${row.won}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("${row.draw}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("${row.lost}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text(
                                text = "${row.points}",
                                color = ActiveGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StatusBadgeUi(
    val label: String,
    val isLive: Boolean
)

private fun buildMatchStatusBadge(match: LiveMatchItem): StatusBadgeUi {
    return when (match.status) {
        MatchFeedStatus.FINISHED -> StatusBadgeUi("انتهت", isLive = false)
        MatchFeedStatus.HALFTIME -> StatusBadgeUi("بين الشوطين", isLive = true)
        MatchFeedStatus.LIVE -> {
            val minTxt = if (!match.minute.isNullOrBlank()) "${match.minute}'" else "مباشر"
            StatusBadgeUi(minTxt, isLive = true)
        }
        MatchFeedStatus.SCHEDULED -> {
            val kickMs = try {
                Instant.parse(match.utcIso).toEpochMilli()
            } catch (_: Exception) {
                0L
            }
            val elapsedMin = if (kickMs > 0L) ((System.currentTimeMillis() - kickMs) / 60000L) else -1L
            if (elapsedMin in 0..120) {
                StatusBadgeUi("$elapsedMin'", isLive = true)
            } else {
                StatusBadgeUi(match.timeText, isLive = false)
            }
        }
    }
}

@Composable
private fun TopNavTab(
    label: String,
    iconGlyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ActiveGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = iconGlyph,
            color = if (selected) Color.White else TextMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) Color.White else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun FullScreenStreamPlayer(
    channel: ChannelItem,
    useBackupStream: Boolean,
    onToggleBackup: () -> Unit,
    onBackToChannels: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val activeUrl = if (useBackupStream && !channel.backupUrl.isNullOrBlank()) {
        channel.backupUrl
    } else {
        channel.url
    }

    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        val previousOrientation =
            activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window?.attributes?.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window?.attributes = window?.attributes
        }

        insetsController?.hide(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )

        onDispose {
            insetsController?.show(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = previousOrientation
        }
    }

    val exoPlayer = remember(activeUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(activeUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            },
            modifier = Modifier.fillMaxSize()
        )

        if (showControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToChannels) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                if (!channel.backupUrl.isNullOrBlank()) {
                    Text(
                        text = if (useBackupStream) "الأساسي" else "احتياطي",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray.copy(alpha = 0.85f))
                            .clickable { onToggleBackup() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriesGridView(
    categories: List<CategoryItem>,
    onSelectCategory: (CategoryItem) -> Unit
) {
    val rows = categories.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(rows) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryGridCard(
                        category = category,
                        onClick = { onSelectCategory(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    category: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(TileBg)
            .border(1.dp, BorderGreen, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TileInnerBg),
            contentAlignment = Alignment.Center
        ) {
            ChannelOrCategoryArtwork(
                assetName = category.logoAssetName,
                contentDescription = category.name,
                badgeText = category.badgeText,
                badgeBg = category.badgeBg,
                badgeTextColor = category.badgeTextColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = category.name,
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryChannelsListView(
    category: CategoryItem,
    selectedChannel: ChannelItem?,
    onBackToCategories: () -> Unit,
    onSelectChannel: (ChannelItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item(key = "cat_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "← رجوع للتصنيفات",
                    color = ActiveGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelBg)
                        .border(1.dp, BorderGreen, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBackToCategories)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Text(
                    text = "${category.name} (${category.channels.size})",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        val channelRows = category.channels.chunked(2)
        items(channelRows.size) { rowIndex ->
            val rowChannels = channelRows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowChannels.forEachIndexed { colIndex, channel ->
                    val isSelected = selectedChannel?.id == channel.id
                    val channelNumber = rowIndex * 2 + colIndex + 1

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF0F2A1E) else PanelBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ActiveGreen else BorderGreen,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectChannel(channel) }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ChannelOrCategoryArtwork(
                                assetName = channel.logoAssetName,
                                contentDescription = channel.name,
                                badgeText = channel.name.take(2).uppercase(),
                                badgeBg = TileInnerBg,
                                badgeTextColor = ActiveGreen,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "قناة $channelNumber",
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )

                        Text(
                            text = channel.name,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (rowChannels.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccountsTabView(accounts: List<SocialAccountItem>) {
    val context = LocalContext.current
    val rows = accounts.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(rows) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { acc ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(TileBg)
                            .border(1.dp, BorderGreen, RoundedCornerShape(22.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(acc.url))
                                context.startActivity(intent)
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(TileInnerBg),
                            contentAlignment = Alignment.Center
                        ) {
                            ChannelOrCategoryArtwork(
                                assetName = acc.logoAssetName,
                                contentDescription = acc.title,
                                badgeText = acc.fallbackGlyph,
                                badgeBg = acc.badgeBg,
                                badgeTextColor = acc.badgeTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = acc.title,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun rememberDrawableId(assetName: String): Int {
    val context = LocalContext.current
    return remember(assetName) {
        context.resources.getIdentifier(assetName, "drawable", context.packageName)
    }
}

@Composable
private fun ChannelOrCategoryArtwork(
    assetName: String,
    contentDescription: String,
    badgeText: String,
    badgeBg: Color,
    badgeTextColor: Color,
    modifier: Modifier = Modifier
) {
    val drawableId = rememberDrawableId(assetName)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TileInnerBg),
        contentAlignment = Alignment.Center
    ) {
        if (drawableId != 0) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (badgeText.length <= 2) 26.sp else 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

