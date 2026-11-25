package com.aliskazem.livetv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val FOOTBALL_DATA_API_KEY = "b11ae4a8257e485a83389f1ef51b8df8"
private val REGIONAL_API_FOOTBALL_KEYS = listOf(
    "34b4b3a4b2ca1509fbc1a4fc3915298"
)
private const val CURRENT_SEASON = 2026

private const val REMOTE_CHANNELS_JSON_URL =
    "https://raw.githubusercontent.com/lylykazm667-ai/ali-sport/refs/heads/main/channels.json"
private const val CHANNELS_CACHE_PREFS = "alikazem_channels_cache"
private const val CHANNELS_CACHE_KEY = "channels_json_v1"
private const val MATCHES_CACHE_KEY = "matches_feed_cache_v1"
private const val STANDINGS_CACHE_PREFIX = "standings_cache_v3_"
private const val SCORERS_CACHE_PREFIX = "scorers_cache_v2_"
private const val ASSISTS_CACHE_PREFIX = "assists_cache_v1_"

enum class CategoryId {
    BEIN, ALWAN, ALKASS, ABUDHABI, THMANYAH
}

enum class MatchFeedStatus {
    SCHEDULED, LIVE, HALFTIME, FINISHED
}

enum class PlayerScaleMode(val labelAr: String) {
    STRETCH("تمديد"),
    FIT_16_9("16:9")
}

data class StreamServerOption(
    val url: String,
    val active: Boolean = true
)

data class ChannelItem(
    val id: String,
    val name: String,
    val url: String,
    val backupUrl: String? = null,
    val servers: List<StreamServerOption> = emptyList(),
    val logoAssetName: String,
    val logoUrl: String? = null,
    val badgeText: String,
    val badgeBg: Color,
    val badgeTextColor: Color = Color.White
) {
    fun effectiveServers(): List<StreamServerOption> {
        if (servers.isNotEmpty()) return servers
        val list = mutableListOf(StreamServerOption(url = url, active = true))
        if (!backupUrl.isNullOrBlank()) {
            list.add(StreamServerOption(url = backupUrl, active = true))
        }
        return list
    }
}

data class CategoryItem(
    val id: CategoryId,
    val name: String,
    val logoAssetName: String,
    val logoUrl: String? = null,
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
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalDiff: Int,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val points: Int
)

data class ScorerRowItem(
    val rank: Int,
    val playerName: String,
    val teamName: String,
    val teamCrestUrl: String? = null,
    val goals: Int,
    val assists: Int = 0
)

data class LeagueOption(
    val code: String,
    val shortLabel: String
)

data class ExtraLeagueConfig(
    val code: String,
    val shortLabel: String,
    val name: String,
    val logoUrl: String? = null,
    val priority: Int = 50
)

data class RemoteConfigData(
    val categories: List<CategoryItem>,
    val teamOverrides: Map<String, String> = emptyMap(),
    val playerOverrides: Map<String, String> = emptyMap(),
    val extraLeagues: List<ExtraLeagueConfig> = emptyList(),
    val customStandings: Map<String, List<StandingRowItem>> = emptyMap()
)

data class SocialAccountItem(
    val id: String,
    val title: String,
    val url: String,
    val logoAssetName: String,
    val logoUrl: String? = null,
    val fallbackGlyph: String,
    val badgeBg: Color,
    val badgeTextColor: Color = Color.White
)

private data class DayOption(
    val key: String,
    val dayTitle: String,
    val dateLabel: String
)

private sealed class MatchFeedListEntry {
    data class LeagueHeader(
        val leagueKey: String,
        val leagueName: String,
        val leagueCode: String?,
        val leagueLogoUrl: String?,
        val matchesCount: Int
    ) : MatchFeedListEntry()

    data class MatchCard(
        val match: LiveMatchItem
    ) : MatchFeedListEntry()
}

private val ScreenBg = Color(0xFF060B08)
private val PanelBg = Color(0xFF0A140F)
private val TileBg = Color(0xFF07120C)
private val TileInnerBg = Color(0xFF040B07)
private val BorderGreen = Color(0xFF143322)
private val ActiveGreen = Color(0xFF10B981)
private val LiveRed = Color(0xFFEF4444)
private val TextWhite = Color(0xFFF3F6F4)
private val TextMuted = Color(0xFF8FA398)

private val STANDING_LEAGUES = listOf(
    LeagueOption("PL", "الدوري الإنجليزي الممتاز"),
    LeagueOption("PD", "الدوري الإسباني"),
    LeagueOption("SA", "الدوري الإيطالي"),
    LeagueOption("BL1", "الدوري الألماني"),
    LeagueOption("FL1", "الدوري الفرنسي"),
    LeagueOption("CL", "دوري الأبطال")
)

private val SCORER_LEAGUES = listOf(
    LeagueOption("PL", "الدوري الإنجليزي"),
    LeagueOption("PD", "الدوري الإسباني"),
    LeagueOption("SA", "الدوري الإيطالي"),
    LeagueOption("BL1", "الدوري الألماني"),
    LeagueOption("FL1", "الدوري الفرنسي"),
    LeagueOption("CL", "دوري الأبطال")
)

private val LEAGUE_ORDER_PRIORITY: Map<String, Int> = mapOf(
    "CL" to 1,
    "PL" to 2,
    "PD" to 3,
    "SA" to 4,
    "BL1" to 5,
    "FL1" to 6,
    "SAU" to 7,
    "ACL" to 8,
    "CAFCL" to 9
)

private val LEAGUE_LOGO_URLS: Map<String, String> = mapOf(
    "PL" to "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/47.png",
    "PD" to "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/87.png",
    "SA" to "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/55.png",
    "BL1" to "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/54.png",
    "FL1" to "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/53.png",
    "CL" to "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/42.png",
    "SAU" to "https://media.api-sports.io/football/leagues/307.png",
    "ACL" to "https://media.api-sports.io/football/leagues/17.png",
    "CAFCL" to "https://media.api-sports.io/football/leagues/12.png"
)

private val REGIONAL_API_FOOTBALL_LEAGUE_IDS: Map<String, List<Int>> = mapOf(
    "SAU" to listOf(307),
    "ACL" to listOf(17, 946),
    "CAFCL" to listOf(12)
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

private val PLAYER_ARABIC_NAMES: Map<String, String> = mapOf(
    "lamine yamal" to "لامين يمال",
    "lamine yamal nasraoui ebana" to "لامين يمال",
    "yamal" to "لامين يمال",
    "لاميني يامال" to "لامين يمال",
    "ousmane dembele" to "عثمان دمبيلي",
    "dembele" to "عثمان دمبيلي",
    "ووسماني ديمبيلي" to "عثمان دمبيلي",
    "عثمان ديمبيلي" to "عثمان دمبيلي",
    "harry kane" to "هاري كاين",
    "kane" to "هاري كاين",
    "هارري كاني" to "هاري كاين",
    "هاري كين" to "هاري كاين"
)

private val TEAM_ARABIC_NAMES: Map<String, String> = mapOf(
    "arsenal" to "آرسنال",
    "brighton hove albion" to "برايتون",
    "brighton hove" to "برايتون",
    "brighton" to "برايتون",
    "manchester city" to "مانشستر سيتي",
    "man city" to "مانشستر سيتي",
    "liverpool" to "ليفربول",
    "bournemouth" to "بورنموث",
    "leeds united" to "ليدز يونايتد",
    "leeds" to "ليدز يونايتد",
    "everton" to "إيفرتون",
    "chelsea" to "تشيلسي",
    "tottenham hotspur" to "توتنهام",
    "tottenham" to "توتنهام",
    "brentford" to "برينتفورد",
    "aston villa" to "أستون فيلا",
    "newcastle united" to "نيوكاسل",
    "newcastle" to "نيوكاسل",
    "fulham" to "فولهام",
    "nottingham forest" to "نوتينغهام فورست",
    "nottingham" to "نوتينغهام فورست",
    "manchester united" to "مانشستر يونايتد",
    "man united" to "مانشستر يونايتد",
    "crystal palace" to "كريستال بالاس",
    "hull city" to "هال سيتي",
    "hull" to "هال سيتي",
    "coventry city" to "كوفنتري سيتي",
    "coventry" to "كوفنتري سيتي",
    "ipswich town" to "إيبسويتش تاون",
    "ipswich" to "إيبسويتش تاون",
    "sunderland" to "سندرلاند",
    "leicester city" to "ليستر سيتي",
    "southampton" to "ساوثهامبتون",
    "west ham united" to "وست هام",
    "west ham" to "وست هام",
    "wolverhampton wanderers" to "وولفرهامبتون",
    "wolves" to "وولفرهامبتون",
    "barcelona" to "برشلونة",
    "barca" to "برشلونة",
    "real madrid" to "ريال مدريد",
    "atletico madrid" to "أتلتيكو مدريد",
    "atleti" to "أتلتيكو مدريد",
    "real betis balompie" to "ريال بيتيس",
    "real betis" to "ريال بيتيس",
    "betis" to "ريال بيتيس",
    "real sociedad de futbol" to "ريال سوسيداد",
    "real sociedad" to "ريال سوسيداد",
    "sociedad" to "ريال سوسيداد",
    "sevilla" to "إشبيلية",
    "athletic bilbao" to "أتلتيك بيلباو",
    "athletic" to "أتلتيك بيلباو",
    "deportivo alaves" to "ديبورتيفو ألافيس",
    "alaves" to "ديبورتيفو ألافيس",
    "villarreal" to "فياريال",
    "espanyol" to "إسبانيول",
    "rcd espanyol" to "إسبانيول",
    "osasuna" to "أوساسونا",
    "ca osasuna" to "أوساسونا",
    "celta vigo" to "سيلتا فيغو",
    "celta" to "سيلتا فيغو",
    "rayo vallecano" to "رايو فاليكانو",
    "rayo" to "رايو فاليكانو",
    "valencia" to "فالنسيا",
    "getafe" to "خيتافي",
    "levante" to "ليفانتي",
    "elche" to "إلتشي",
    "real racing club" to "راسينغ سانتاندير",
    "racing santander" to "راسينغ سانتاندير",
    "santander" to "راسينغ سانتاندير",
    "deportivo la coruna" to "ديبورتيفو لاكورونيا",
    "deportivo" to "ديبورتيفو لاكورونيا",
    "malaga" to "ملقا",
    "girona" to "جيرونا",
    "mallorca" to "مايوركا",
    "lazio" to "لاتسيو",
    "ss lazio" to "لاتسيو",
    "roma" to "روما",
    "as roma" to "روما",
    "internazionale milano" to "إنتر ميلان",
    "inter milan" to "إنتر ميلان",
    "inter" to "إنتر ميلان",
    "milan" to "ميلان",
    "ac milan" to "ميلان",
    "como 1907" to "كومو",
    "como" to "كومو",
    "juventus" to "يوفنتوس",
    "ssc napoli" to "نابولي",
    "napoli" to "نابولي",
    "bologna 1909" to "بولونيا",
    "bologna" to "بولونيا",
    "genoa cfc" to "جنوى",
    "genoa" to "جنوى",
    "cagliari calcio" to "كالياري",
    "cagliari" to "كالياري",
    "atalanta" to "أتالانتا",
    "torino" to "تورينو",
    "lecce" to "ليتشي",
    "acf fiorentina" to "فيورنتينا",
    "fiorentina" to "فيورنتينا",
    "parma calcio 1913" to "بارما",
    "parma" to "بارما",
    "sassuolo calcio" to "ساسولو",
    "sassuolo" to "ساسولو",
    "monza" to "مونزا",
    "udinese calcio" to "أودينيزي",
    "udinese" to "أودينيزي",
    "frosinone calcio" to "فروسينوني",
    "frosinone" to "فروسينوني",
    "venezia" to "فينيزيا",
    "hellas verona" to "هيلاس فيرونا",
    "verona" to "هيلاس فيرونا",
    "borussia dortmund" to "بوروسيا دورتموند",
    "dortmund" to "بوروسيا دورتموند",
    "bayern munchen" to "بايرن ميونخ",
    "bayern munich" to "بايرن ميونخ",
    "bayern" to "بايرن ميونخ",
    "freiburg" to "فرايبورغ",
    "rb leipzig" to "لايبزيغ",
    "leipzig" to "لايبزيغ",
    "bayer 04 leverkusen" to "باير ليفركوزن",
    "bayer leverkusen" to "باير ليفركوزن",
    "leverkusen" to "باير ليفركوزن",
    "werder bremen" to "فيردر بريمن",
    "bremen" to "فيردر بريمن",
    "eintracht frankfurt" to "آينتراخت فرانكفورت",
    "frankfurt" to "آينتراخت فرانكفورت",
    "vfb stuttgart" to "شتوتغارت",
    "stuttgart" to "شتوتغارت",
    "augsburg" to "أوغسبورغ",
    "tsg 1899 hoffenheim" to "هوفنهايم",
    "hoffenheim" to "هوفنهايم",
    "hamburger sv" to "هامبورغ",
    "hamburger" to "هامبورغ",
    "hsv" to "هامبورغ",
    "1 fsv mainz 05" to "ماينتس",
    "mainz" to "ماينتس",
    "1 union berlin" to "يونيون برلين",
    "union berlin" to "يونيون برلين",
    "borussia monchengladbach" to "بوروسيا مونشنغلادباخ",
    "monchengladbach" to "بوروسيا مونشنغلادباخ",
    "m gladbach" to "بوروسيا مونشنغلادباخ",
    "gladbach" to "بوروسيا مونشنغلادباخ",
    "elversberg" to "إلفرسبيرغ",
    "1 fc koln" to "كولن",
    "koln" to "كولن",
    "paderborn" to "بادربورن",
    "sc paderborn" to "بادربورن",
    "schalke 04" to "شالكه",
    "schalke" to "شالكه",
    "paris saint germain" to "باريس سان جيرمان",
    "psg" to "باريس سان جيرمان",
    "paris fc" to "باريس إف سي",
    "lille osc" to "ليل",
    "lille" to "ليل",
    "olympique lyonnais" to "أولمبيك ليون",
    "olympique lyon" to "أولمبيك ليون",
    "lyon" to "أولمبيك ليون",
    "olympique marseille" to "مارسيليا",
    "marseille" to "مارسيليا",
    "stade brestois 29" to "بريست",
    "brest" to "بريست",
    "ogc nice" to "نيس",
    "nice" to "نيس",
    "as monaco" to "موناكو",
    "monaco" to "موناكو",
    "stade rennais fc 1901" to "رين",
    "stade rennais" to "رين",
    "rennes" to "رين",
    "lorient" to "لوريان",
    "racing club de lens" to "لانس",
    "rc lens" to "لانس",
    "lens" to "لانس",
    "toulouse" to "تولوز",
    "aj auxerre" to "أوكسير",
    "auxerre" to "أوكسير",
    "rc strasbourg alsace" to "ستراسبورغ",
    "strasbourg" to "ستراسبورغ",
    "angers sco" to "أنجيه",
    "angers" to "أنجيه",
    "le havre ac" to "لو هافر",
    "le havre" to "لو هافر",
    "le mans fc" to "لومان",
    "le mans" to "لومان",
    "es troyes ac" to "تروا",
    "troyes" to "تروا",
    "hilal" to "الهلال",
    "al hilal" to "الهلال",
    "nassr" to "النصر",
    "al nassr" to "النصر",
    "ittihad" to "الاتحاد",
    "al ittihad" to "الاتحاد",
    "ahli" to "الأهلي السعودي",
    "al ahli" to "الأهلي السعودي",
    "club brugge" to "كلوب بروج",
    "sporting cp" to "سبورتينغ لشبونة",
    "sporting clube de portugal" to "سبورتينغ لشبونة",
    "lask" to "لاسك",
    "galatasaray" to "غلطة سراي",
    "fenerbahce" to "فنربخشة",
    "shakhtar donetsk" to "شاختار دونيتسك",
    "viking" to "فايكنغ",
    "psv eindhoven" to "بي إس في آيندهوفن",
    "psv" to "بي إس في آيندهوفن",
    "bodo glimt" to "بودو غليمت",
    "porto" to "بورتو",
    "aek athens" to "أيك أثينا",
    "aek" to "أيك أثينا",
    "sabah" to "صباح",
    "feyenoord" to "فينورد",
    "slavia praha" to "سلافيا براغ",
    "slovan bratislava" to "سلوفان براتيسلافا",
    "sk slovan bratislava" to "سلوفان براتيسلافا",
    "bratislava" to "سلوفان براتيسلافا"
)

private fun resolveCategoryId(raw: String, index: Int): CategoryId {
    return when (raw.trim().uppercase(Locale.ROOT)) {
        "BEIN" -> CategoryId.BEIN
        "ALWAN" -> CategoryId.ALWAN
        "ALKASS" -> CategoryId.ALKASS
        "ABUDHABI" -> CategoryId.ABUDHABI
        "THMANYAH" -> CategoryId.THMANYAH
        else -> CategoryId.entries.getOrElse(index) { CategoryId.BEIN }
    }
}

private fun foldSpecialLatinChars(raw: String): String {
    return raw.lowercase(Locale.ROOT)
        .replace('á', 'a').replace('à', 'a').replace('â', 'a').replace('ä', 'a').replace('ã', 'a').replace('å', 'a')
        .replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ë', 'e')
        .replace('í', 'i').replace('ì', 'i').replace('î', 'i').replace('ï', 'i')
        .replace('ó', 'o').replace('ò', 'o').replace('ô', 'o').replace('ö', 'o').replace('õ', 'o').replace('ø', 'o')
        .replace('ú', 'u').replace('ù', 'u').replace('û', 'u').replace('ü', 'u')
        .replace('ñ', 'n').replace('ç', 'c').replace('ý', 'y').replace('ÿ', 'y')
        .replace('ß', 's')
        .replace('’', '\'')
}

private fun normalizeTeamLookupKey(raw: String): String {
    val s = foldSpecialLatinChars(raw)
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('-', ' ')
        .replace('/', ' ')
        .replace('.', ' ')
        .replace('&', ' ')

    if (s.trim() == "paris fc") return "paris fc"

    val stopWords = setOf(
        "fc", "cf", "sc", "ac", "afc", "cd", "ud", "sd", "rc", "osc", "ogc",
        "as", "ss", "us", "sv", "bv", "kv", "fk", "sk", "bc", "pae", "sl", "ca", "rcd", "ssc", "acf",
        "club", "de", "del", "la", "las", "los", "al"
    )
    val tokens = s.split(Regex("\\s+")).filter { it.isNotBlank() && it !in stopWords }
    return tokens.joinToString(" ").trim()
}

private fun transliterateToArabic(raw: String): String {
    val cleaned = foldSpecialLatinChars(raw)
        .replace(Regex("\\b(FC|CF|SC|AC|AFC|CD|UD|SD|RC|OSC|OGC|AS|SS|US|SV|BV|KV|FK|SK|BC|PAE|SL|CA|RCD|SSC|ACF)\\b", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (cleaned.isBlank()) return "فريق"

    val lower = cleaned
        .replace("sh", "ش")
        .replace("ch", "تش")
        .replace("th", "ث")
        .replace("ph", "ف")
        .replace("gh", "غ")
        .replace("kh", "خ")
        .replace("oo", "و")
        .replace("ee", "ي")

    val sb = StringBuilder()
    for (ch in lower) {
        when (ch) {
            'a' -> sb.append('ا')
            'b' -> sb.append('ب')
            'c' -> sb.append('ك')
            'd' -> sb.append('د')
            'e' -> sb.append('ي')
            'f' -> sb.append('ف')
            'g' -> sb.append('غ')
            'h' -> sb.append('ه')
            'i' -> sb.append('ي')
            'j' -> sb.append('ج')
            'k' -> sb.append('ك')
            'l' -> sb.append('ل')
            'm' -> sb.append('م')
            'n' -> sb.append('ن')
            'o' -> sb.append('و')
            'p' -> sb.append('ب')
            'q' -> sb.append('ك')
            'r' -> sb.append('ر')
            's' -> sb.append('س')
            't' -> sb.append('ت')
            'u' -> sb.append('و')
            'v' -> sb.append('ف')
            'w' -> sb.append('و')
            'x' -> sb.append("كس")
            'y' -> sb.append('ي')
            'z' -> sb.append('ز')
            else -> sb.append(ch)
        }
    }
    return sb.toString().replace(Regex("\\s+"), " ").trim()
}

private fun parseStringMap(obj: JSONObject?): Map<String, String> {
    if (obj == null) return emptyMap()
    val map = mutableMapOf<String, String>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        val v = obj.optString(k, "").trim()
        if (k.isNotBlank() && v.isNotBlank()) {
            map[foldSpecialLatinChars(k.trim())] = v
            map[normalizeTeamLookupKey(k)] = v
        }
    }
    return map
}

private fun parseExtraLeagues(arr: JSONArray?): List<ExtraLeagueConfig> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ExtraLeagueConfig>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val code = obj.optString("code", "").trim().uppercase(Locale.ROOT)
        val name = obj.optString("name", "").trim()
        if (code.isBlank() || name.isBlank() || code == "IRQ") continue
        val shortLabel = obj.optString("shortLabel", "").trim().ifBlank { name }
        val logoUrl = obj.optString("logoUrl", "").trim().takeIf { it.isNotBlank() && it != "null" }
        val priority = obj.optInt("priority", 50)
        list.add(
            ExtraLeagueConfig(
                code = code,
                shortLabel = shortLabel,
                name = name,
                logoUrl = logoUrl,
                priority = priority
            )
        )
    }
    return list
}

private fun parseCustomStandings(obj: JSONObject?): Map<String, List<StandingRowItem>> {
    if (obj == null) return emptyMap()
    val map = mutableMapOf<String, List<StandingRowItem>>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val rawCode = keys.next()
        val code = rawCode.trim().uppercase(Locale.ROOT)
        if (code == "IRQ") continue
        val rowsArr = obj.optJSONArray(rawCode) ?: continue
        val rows = mutableListOf<StandingRowItem>()
        for (i in 0 until rowsArr.length()) {
            val rObj = rowsArr.optJSONObject(i) ?: continue
            val teamName = rObj.optString("teamName", "").trim()
            if (teamName.isBlank()) continue
            val crestUrl = rObj.optString("crestUrl", "").trim().takeIf { it.isNotBlank() && it != "null" }
            val played = rObj.optInt("played", 0)
            val won = rObj.optInt("won", 0)
            val draw = rObj.optInt("draw", 0)
            val lost = rObj.optInt("lost", 0)
            val goalDiff = rObj.optInt("goalDiff", 0)
            val goalsFor = if (rObj.has("goalsFor")) {
                rObj.optInt("goalsFor", 0)
            } else {
                val against = rObj.optInt("goalsAgainst", 0)
                if (against > 0 || goalDiff != 0) (against + goalDiff).coerceAtLeast(0) else 0
            }
            val goalsAgainst = if (rObj.has("goalsAgainst")) {
                rObj.optInt("goalsAgainst", 0)
            } else {
                (goalsFor - goalDiff).coerceAtLeast(0)
            }
            val yellowCards = rObj.optInt("yellowCards", 0)
            val redCards = rObj.optInt("redCards", 0)
            rows.add(
                StandingRowItem(
                    position = rObj.optInt("position", i + 1),
                    teamName = teamName,
                    crestUrl = crestUrl,
                    played = played,
                    won = won,
                    draw = draw,
                    lost = lost,
                    goalsFor = goalsFor,
                    goalsAgainst = goalsAgainst,
                    goalDiff = goalDiff,
                    yellowCards = yellowCards,
                    redCards = redCards,
                    points = rObj.optInt("points", 0)
                )
            )
        }
        if (rows.isNotEmpty()) {
            map[code] = rows.sortedBy { it.position }
        }
    }
    return map
}

private fun parseChannelServers(
    item: JSONObject,
    primaryUrl: String,
    backupUrl: String?,
    fallbackServers: List<StreamServerOption>
): List<StreamServerOption> {
    val srvArr = item.optJSONArray("servers")
    if (srvArr != null && srvArr.length() > 0) {
        val list = mutableListOf<StreamServerOption>()
        for (k in 0 until srvArr.length()) {
            val sObj = srvArr.optJSONObject(k)
            if (sObj != null) {
                val sUrl = sObj.optString("url", "").trim()
                if (sUrl.isBlank()) continue
                val isActive = sObj.optBoolean("active", true) && !sObj.optBoolean("hiddenRandom", false)
                list.add(StreamServerOption(url = sUrl, active = isActive))
            } else {
                val sUrl = srvArr.optString(k, "").trim()
                if (sUrl.isNotBlank()) {
                    list.add(StreamServerOption(url = sUrl, active = true))
                }
            }
        }
        if (list.isNotEmpty()) return list
    }

    if (fallbackServers.isNotEmpty()) return fallbackServers

    val built = mutableListOf(StreamServerOption(url = primaryUrl, active = true))
    if (!backupUrl.isNullOrBlank()) {
        built.add(StreamServerOption(url = backupUrl, active = true))
    }
    return built
}

private fun parseRemoteConfigJson(jsonText: String, fallbackCategories: List<CategoryItem>): RemoteConfigData {
    return try {
        val root = JSONObject(jsonText)
        val teamOverrides = parseStringMap(root.optJSONObject("teamOverrides"))
        val playerOverrides = parseStringMap(root.optJSONObject("playerOverrides"))
        val extraLeagues = parseExtraLeagues(root.optJSONArray("extraLeagues"))
        val customStandings = parseCustomStandings(root.optJSONObject("customStandings"))

        val catsArr = root.optJSONArray("categories")
        if (catsArr == null) {
            return RemoteConfigData(
                categories = fallbackCategories,
                teamOverrides = teamOverrides,
                playerOverrides = playerOverrides,
                extraLeagues = extraLeagues,
                customStandings = customStandings
            )
        }

        val fallbackById = fallbackCategories.associateBy { it.id }
        val result = mutableListOf<CategoryItem>()

        for (i in 0 until catsArr.length()) {
            val cObj = catsArr.optJSONObject(i) ?: continue
            val catId = resolveCategoryId(cObj.optString("id", ""), i)
            val fallbackCat = fallbackById[catId]
            val catLogoUrl = cObj.optString("logoUrl", "").trim()
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: fallbackCat?.logoUrl
            val chArr = cObj.optJSONArray("channels") ?: JSONArray()
            val channels = mutableListOf<ChannelItem>()

            for (j in 0 until chArr.length()) {
                val item = chArr.optJSONObject(j) ?: continue
                val fallbackCh = fallbackCat?.channels?.getOrNull(j)
                val url = item.optString("url", "").trim().ifBlank { fallbackCh?.url.orEmpty() }
                if (url.isBlank()) continue
                val backupRaw = item.optString("backupUrl", "").trim()
                val backup = when {
                    backupRaw.equals("null", ignoreCase = true) -> null
                    backupRaw.isNotBlank() -> backupRaw
                    else -> fallbackCh?.backupUrl
                }
                val chLogoUrl = item.optString("logoUrl", "").trim()
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val parsedServers = parseChannelServers(
                    item = item,
                    primaryUrl = url,
                    backupUrl = backup,
                    fallbackServers = fallbackCh?.servers.orEmpty()
                )

                channels.add(
                    ChannelItem(
                        id = item.optString("id", "").trim().ifBlank { "${catId.name.lowercase(Locale.ROOT)}_${j + 1}" },
                        name = item.optString("name", "").trim().ifBlank { fallbackCh?.name ?: "قناة ${j + 1}" },
                        url = url,
                        backupUrl = backup,
                        servers = parsedServers,
                        logoAssetName = item.optString("logoAssetName", "").trim().ifBlank {
                            fallbackCh?.logoAssetName ?: (fallbackCat?.logoAssetName ?: "bein")
                        },
                        logoUrl = chLogoUrl ?: fallbackCh?.logoUrl,
                        badgeText = item.optString("badgeText", "").trim().ifBlank {
                            fallbackCh?.badgeText ?: item.optString("name", "LIVE")
                        },
                        badgeBg = fallbackCh?.badgeBg ?: (fallbackCat?.badgeBg ?: PanelBg),
                        badgeTextColor = fallbackCh?.badgeTextColor ?: (fallbackCat?.badgeTextColor ?: Color.White)
                    )
                )
            }

            if (channels.isNotEmpty()) {
                result.add(
                    CategoryItem(
                        id = catId,
                        name = cObj.optString("name", "").trim().ifBlank { fallbackCat?.name ?: catId.name },
                        logoAssetName = cObj.optString("logoAssetName", "").trim().ifBlank {
                            fallbackCat?.logoAssetName ?: "bein"
                        },
                        logoUrl = catLogoUrl,
                        badgeText = cObj.optString("badgeText", "").trim().ifBlank {
                            fallbackCat?.badgeText ?: catId.name
                        },
                        badgeBg = fallbackCat?.badgeBg ?: PanelBg,
                        badgeTextColor = fallbackCat?.badgeTextColor ?: Color.White,
                        channels = channels
                    )
                )
            }
        }

        RemoteConfigData(
            categories = if (result.isNotEmpty()) result else fallbackCategories,
            teamOverrides = teamOverrides,
            playerOverrides = playerOverrides,
            extraLeagues = extraLeagues,
            customStandings = customStandings
        )
    } catch (_: Exception) {
        RemoteConfigData(categories = fallbackCategories)
    }
}

private fun saveCachedString(context: Context, key: String, value: String) {
    val prefs = context.getSharedPreferences(CHANNELS_CACHE_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putString(key, value).apply()
}

private fun loadCachedString(context: Context, key: String): String? {
    val prefs = context.getSharedPreferences(CHANNELS_CACHE_PREFS, Context.MODE_PRIVATE)
    return prefs.getString(key, null)
}

private fun saveCachedChannelsJson(context: Context, jsonText: String) {
    saveCachedString(context, CHANNELS_CACHE_KEY, jsonText)
}

private fun loadCachedChannelsJson(context: Context): String? {
    return loadCachedString(context, CHANNELS_CACHE_KEY)
}

private fun saveMatchesCache(context: Context, list: List<LiveMatchItem>) {
    try {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("utcIso", m.utcIso)
                    put("baghdadDate", m.baghdadDate)
                    put("timeText", m.timeText)
                    put("homeTeam", m.homeTeam)
                    put("awayTeam", m.awayTeam)
                    put("homeCrestUrl", m.homeCrestUrl ?: JSONObject.NULL)
                    put("awayCrestUrl", m.awayCrestUrl ?: JSONObject.NULL)
                    put("league", m.league)
                    put("leagueCode", m.leagueCode ?: JSONObject.NULL)
                    put("leagueLogoUrl", m.leagueLogoUrl ?: JSONObject.NULL)
                    put("status", m.status.name)
                    put("minute", m.minute ?: JSONObject.NULL)
                    put("homeScore", m.homeScore ?: JSONObject.NULL)
                    put("awayScore", m.awayScore ?: JSONObject.NULL)
                }
            )
        }
        saveCachedString(context, MATCHES_CACHE_KEY, arr.toString())
    } catch (_: Exception) {
    }
}

private fun loadMatchesCache(context: Context): List<LiveMatchItem> {
    val raw = loadCachedString(context, MATCHES_CACHE_KEY) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val out = mutableListOf<LiveMatchItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val statusStr = obj.optString("status", MatchFeedStatus.SCHEDULED.name)
            val status = try {
                MatchFeedStatus.valueOf(statusStr)
            } catch (_: Exception) {
                MatchFeedStatus.SCHEDULED
            }
            val code = obj.optString("leagueCode", "").takeIf { it.isNotBlank() && it != "null" }
            val league = obj.optString("league", "")
            if (isExcludedCompetition(league, code)) continue
            out.add(
                LiveMatchItem(
                    id = obj.optString("id", "cached_$i"),
                    utcIso = obj.optString("utcIso", ""),
                    baghdadDate = obj.optString("baghdadDate", ""),
                    timeText = obj.optString("timeText", ""),
                    homeTeam = obj.optString("homeTeam", ""),
                    awayTeam = obj.optString("awayTeam", ""),
                    homeCrestUrl = obj.optString("homeCrestUrl", "").takeIf { it.isNotBlank() && it != "null" },
                    awayCrestUrl = obj.optString("awayCrestUrl", "").takeIf { it.isNotBlank() && it != "null" },
                    league = league,
                    leagueCode = code,
                    leagueLogoUrl = obj.optString("leagueLogoUrl", "").takeIf { it.isNotBlank() && it != "null" },
                    status = status,
                    minute = obj.optString("minute", "").takeIf { it.isNotBlank() && it != "null" },
                    homeScore = if (obj.isNull("homeScore")) null else obj.optInt("homeScore"),
                    awayScore = if (obj.isNull("awayScore")) null else obj.optInt("awayScore")
                )
            )
        }
        out
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveStandingsCache(context: Context, leagueCode: String, rows: List<StandingRowItem>) {
    try {
        val arr = JSONArray()
        rows.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("position", r.position)
                    put("teamName", r.teamName)
                    put("crestUrl", r.crestUrl ?: JSONObject.NULL)
                    put("played", r.played)
                    put("won", r.won)
                    put("draw", r.draw)
                    put("lost", r.lost)
                    put("goalsFor", r.goalsFor)
                    put("goalsAgainst", r.goalsAgainst)
                    put("goalDiff", r.goalDiff)
                    put("yellowCards", r.yellowCards)
                    put("redCards", r.redCards)
                    put("points", r.points)
                }
            )
        }
        saveCachedString(context, "$STANDINGS_CACHE_PREFIX${leagueCode.uppercase(Locale.ROOT)}", arr.toString())
    } catch (_: Exception) {
    }
}

private fun loadStandingsCache(context: Context, leagueCode: String): List<StandingRowItem> {
    val raw = loadCachedString(context, "$STANDINGS_CACHE_PREFIX${leagueCode.uppercase(Locale.ROOT)}")
        ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val out = mutableListOf<StandingRowItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val goalsFor = obj.optInt("goalsFor", 0)
            val goalDiff = obj.optInt("goalDiff", 0)
            val goalsAgainst = if (obj.has("goalsAgainst")) {
                obj.optInt("goalsAgainst", 0)
            } else {
                (goalsFor - goalDiff).coerceAtLeast(0)
            }
            out.add(
                StandingRowItem(
                    position = obj.optInt("position", i + 1),
                    teamName = obj.optString("teamName", ""),
                    crestUrl = obj.optString("crestUrl", "").takeIf { it.isNotBlank() && it != "null" },
                    played = obj.optInt("played", 0),
                    won = obj.optInt("won", 0),
                    draw = obj.optInt("draw", 0),
                    lost = obj.optInt("lost", 0),
                    goalsFor = goalsFor,
                    goalsAgainst = goalsAgainst,
                    goalDiff = goalDiff,
                    yellowCards = obj.optInt("yellowCards", 0),
                    redCards = obj.optInt("redCards", 0),
                    points = obj.optInt("points", 0)
                )
            )
        }
        out
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveScorersCache(context: Context, leagueCode: String, rows: List<ScorerRowItem>) {
    try {
        val arr = JSONArray()
        rows.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("rank", r.rank)
                    put("playerName", r.playerName)
                    put("teamName", r.teamName)
                    put("teamCrestUrl", r.teamCrestUrl ?: JSONObject.NULL)
                    put("goals", r.goals)
                    put("assists", r.assists)
                }
            )
        }
        saveCachedString(context, "$SCORERS_CACHE_PREFIX${leagueCode.uppercase(Locale.ROOT)}", arr.toString())
    } catch (_: Exception) {
    }
}

private fun loadScorersCache(context: Context, leagueCode: String): List<ScorerRowItem> {
    val raw = loadCachedString(context, "$SCORERS_CACHE_PREFIX${leagueCode.uppercase(Locale.ROOT)}")
        ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val out = mutableListOf<ScorerRowItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            out.add(
                ScorerRowItem(
                    rank = obj.optInt("rank", i + 1),
                    playerName = obj.optString("playerName", ""),
                    teamName = obj.optString("teamName", ""),
                    teamCrestUrl = obj.optString("teamCrestUrl", "").takeIf { it.isNotBlank() && it != "null" },
                    goals = obj.optInt("goals", 0),
                    assists = obj.optInt("assists", 0)
                )
            )
        }
        out
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveAssistsCache(context: Context, leagueCode: String, rows: List<ScorerRowItem>) {
    try {
        val arr = JSONArray()
        rows.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("rank", r.rank)
                    put("playerName", r.playerName)
                    put("teamName", r.teamName)
                    put("teamCrestUrl", r.teamCrestUrl ?: JSONObject.NULL)
                    put("goals", r.goals)
                    put("assists", r.assists)
                }
            )
        }
        saveCachedString(context, "$ASSISTS_CACHE_PREFIX${leagueCode.uppercase(Locale.ROOT)}", arr.toString())
    } catch (_: Exception) {
    }
}

private fun loadAssistsCache(context: Context, leagueCode: String): List<ScorerRowItem> {
    val raw = loadCachedString(context, "$ASSISTS_CACHE_PREFIX${leagueCode.uppercase(Locale.ROOT)}")
        ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val out = mutableListOf<ScorerRowItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            out.add(
                ScorerRowItem(
                    rank = obj.optInt("rank", i + 1),
                    playerName = obj.optString("playerName", ""),
                    teamName = obj.optString("teamName", ""),
                    teamCrestUrl = obj.optString("teamCrestUrl", "").takeIf { it.isNotBlank() && it != "null" },
                    goals = obj.optInt("goals", 0),
                    assists = obj.optInt("assists", 0)
                )
            )
        }
        out
    } catch (_: Exception) {
        emptyList()
    }
}

private suspend fun fetchRemoteConfig(
    context: Context,
    remoteUrl: String,
    fallbackCategories: List<CategoryItem>
): RemoteConfigData {
    if (remoteUrl.isBlank()) {
        val cached = loadCachedChannelsJson(context)
        return if (!cached.isNullOrBlank()) {
            parseRemoteConfigJson(cached, fallbackCategories)
        } else {
            RemoteConfigData(categories = fallbackCategories)
        }
    }
    return withContext(Dispatchers.IO) {
        val urlWithCacheBust = if (remoteUrl.contains("?")) {
            "$remoteUrl&_t=${System.currentTimeMillis()}"
        } else {
            "$remoteUrl?_t=${System.currentTimeMillis()}"
        }
        val body = httpGetJson(urlWithCacheBust, mapOf("User-Agent" to "Mozilla/5.0"))
        if (!body.isNullOrBlank()) {
            val parsed = parseRemoteConfigJson(body, fallbackCategories)
            saveCachedChannelsJson(context, body)
            parsed
        } else {
            val cached = loadCachedChannelsJson(context)
            if (!cached.isNullOrBlank()) {
                parseRemoteConfigJson(cached, fallbackCategories)
            } else {
                RemoteConfigData(categories = fallbackCategories)
            }
        }
    }
}

private object CrestMemoryCache {
    private val cache = mutableMapOf<String, Bitmap>()

    @Synchronized
    fun get(url: String): Bitmap? = cache[url]

    @Synchronized
    fun put(url: String, bitmap: Bitmap) {
        if (cache.size > 300) {
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
                connectTimeout = 7000
                readTimeout = 7000
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

private fun isExcludedCompetition(text: String, code: String? = null): Boolean {
    val cleanCode = code?.trim()?.uppercase(Locale.ROOT)
    if (cleanCode == "ELC" || cleanCode == "BSA" || cleanCode == "IRQ") return true

    val normalized = foldSpecialLatinChars(text).trim()
    val blockedKeywords = listOf(
        "brazil",
        "brazilian",
        "brasileirao",
        "campeonato brasileiro",
        "serie a brazil",
        "الدوري البرازيلي",
        "البرازيلي",
        "برازيل",
        "championship",
        "efl championship",
        "league one",
        "league two",
        "درجة ثانية",
        "الدرجة الثانية",
        "iraq",
        "iraqi",
        "stars league",
        "الدوري العراقي",
        "دوري النجوم",
        "العراق"
    )
    return blockedKeywords.any { normalized.contains(it) }
}

private fun detectLeagueCode(
    rawName: String,
    code: String? = null,
    extraLeagues: List<ExtraLeagueConfig> = emptyList()
): String? {
    val cleanCode = code?.trim()?.uppercase(Locale.ROOT)
    if (!cleanCode.isNullOrBlank()) {
        if (cleanCode == "IRQ") return null
        if (extraLeagues.any { it.code.equals(cleanCode, ignoreCase = true) }) {
            return cleanCode
        }
        if (LEAGUE_LOGO_URLS.containsKey(cleanCode)) {
            return cleanCode
        }
    }
    val lower = foldSpecialLatinChars(rawName)
    extraLeagues.firstOrNull { ex ->
        ex.name.isNotBlank() && (rawName.contains(ex.name) || lower.contains(foldSpecialLatinChars(ex.name))) ||
            ex.shortLabel.isNotBlank() && (rawName.contains(ex.shortLabel) || lower.contains(foldSpecialLatinChars(ex.shortLabel)))
    }?.let { return it.code }

    return when {
        lower.contains("premier league") || rawName.contains("الإنجليزي") -> "PL"
        lower.contains("la liga") || lower.contains("primera") || rawName.contains("الإسباني") -> "PD"
        lower.contains("serie a") || rawName.contains("الإيطالي") -> "SA"
        lower.contains("bundesliga") || rawName.contains("الألماني") -> "BL1"
        lower.contains("ligue 1") || rawName.contains("الفرنسي") -> "FL1"
        lower.contains("champions league") || rawName.contains("أبطال أوروبا") || rawName.contains("الأبطال") -> "CL"
        lower.contains("saudi") || lower.contains("pro league") || rawName.contains("السعودي") -> "SAU"
        lower.contains("afc champions") || rawName.contains("أبطال آسيا") || rawName.contains("الآسيوي") -> "ACL"
        lower.contains("caf champions") || rawName.contains("أبطال أفريقيا") || rawName.contains("الأفريقي") -> "CAFCL"
        else -> null
    }
}

private fun resolveLeagueLogoUrl(
    code: String?,
    extraLeagues: List<ExtraLeagueConfig> = emptyList()
): String? {
    val cleanCode = code?.trim()?.uppercase(Locale.ROOT) ?: return null
    extraLeagues.firstOrNull { it.code.equals(cleanCode, ignoreCase = true) && !it.logoUrl.isNullOrBlank() }
        ?.let { return it.logoUrl }
    return LEAGUE_LOGO_URLS[cleanCode]
}

private fun translateCompetitionName(
    rawName: String,
    code: String? = null,
    extraLeagues: List<ExtraLeagueConfig> = emptyList()
): String {
    val resolvedCode = detectLeagueCode(rawName, code, extraLeagues)
    extraLeagues.firstOrNull { it.code.equals(resolvedCode, ignoreCase = true) }?.let { return it.name }

    val clean = rawName.trim()
    val lower = foldSpecialLatinChars(clean)
    return when (resolvedCode) {
        "PL" -> "الدوري الإنجليزي الممتاز"
        "PD" -> "الدوري الإسباني"
        "SA" -> "الدوري الإيطالي"
        "BL1" -> "الدوري الألماني"
        "FL1" -> "الدوري الفرنسي"
        "CL" -> "دوري أبطال أوروبا"
        "SAU" -> "الدوري السعودي"
        "ACL" -> "دوري أبطال آسيا"
        "CAFCL" -> "دوري أبطال أفريقيا"
        else -> when {
            lower.contains("egypt") || clean.contains("المصري") -> "الدوري المصري"
            else -> clean.ifBlank { "بطولة رياضية" }
        }
    }
}

private fun translateTeamName(
    rawTeam: String,
    teamOverrides: Map<String, String> = emptyMap()
): String {
    val clean = rawTeam.trim()
    if (clean.isBlank()) return "فريق"
    val folded = foldSpecialLatinChars(clean)
    teamOverrides[folded]?.let { return it }
    TEAM_ARABIC_NAMES[folded]?.let { return it }

    val normalized = normalizeTeamLookupKey(clean)
    teamOverrides[normalized]?.let { return it }
    TEAM_ARABIC_NAMES[normalized]?.let { return it }

    val hasArabic = clean.any { it in '\u0600'..'\u06FF' }
    return if (hasArabic) clean else transliterateToArabic(clean)
}

private fun translatePlayerName(
    rawPlayer: String,
    playerOverrides: Map<String, String> = emptyMap(),
    teamOverrides: Map<String, String> = emptyMap()
): String {
    val clean = rawPlayer.trim()
    if (clean.isBlank()) return "لاعب"
    val folded = foldSpecialLatinChars(clean)
    playerOverrides[folded]?.let { return it }
    PLAYER_ARABIC_NAMES[folded]?.let { return it }

    val normalized = normalizeTeamLookupKey(clean)
    playerOverrides[normalized]?.let { return it }
    PLAYER_ARABIC_NAMES[normalized]?.let { return it }

    return translateTeamName(clean, teamOverrides)
}

private fun currentBaghdadTodayKey(): String {
    return try {
        LocalDate.now(ZoneId.of("Asia/Baghdad")).format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    }
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

    return (-1L..6L).map { offset ->
        val d = today.plusDays(offset)
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
            connectTimeout = 8000
            readTimeout = 8000
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

private suspend fun httpGetWithRegionalApiFootballKeys(endpointPathWithQuery: String): String? {
    for (apiKey in REGIONAL_API_FOOTBALL_KEYS) {
        val url = "https://v3.football.api-sports.io/$endpointPathWithQuery"
        val body = httpGetJson(url, mapOf("x-apisports-key" to apiKey))
        if (!body.isNullOrBlank()) {
            val root = try {
                JSONObject(body)
            } catch (_: Exception) {
                null
            }
            val errorsObj = root?.optJSONObject("errors")
            val hasErrors = errorsObj != null && errorsObj.length() > 0
            val resultsCount = root?.optInt("results", 0) ?: 0
            if (!hasErrors && resultsCount > 0) {
                return body
            }
        }
    }
    return null
}

private fun parseFootballDataStatus(statusRaw: String): MatchFeedStatus {
    return when (statusRaw.uppercase(Locale.ROOT)) {
        "FINISHED", "AWARDED", "FT", "AET", "PEN" -> MatchFeedStatus.FINISHED
        "PAUSED", "HT" -> MatchFeedStatus.HALFTIME
        "IN_PLAY", "LIVE", "1H", "2H", "ET", "P" -> MatchFeedStatus.LIVE
        else -> MatchFeedStatus.SCHEDULED
    }
}

private suspend fun fetchRegionalMatchesForLeague(
    code: String,
    leagueId: Int,
    dateFrom: String,
    dateTo: String,
    teamOverrides: Map<String, String>,
    extraLeagues: List<ExtraLeagueConfig>
): List<LiveMatchItem> {
    return withContext(Dispatchers.IO) {
        val body = httpGetWithRegionalApiFootballKeys(
            "fixtures?league=$leagueId&season=$CURRENT_SEASON&from=$dateFrom&to=$dateTo&timezone=Asia/Baghdad"
        ) ?: return@withContext emptyList()

        val list = mutableListOf<LiveMatchItem>()
        val root = JSONObject(body)
        val respArr = root.optJSONArray("response") ?: JSONArray()
        val baghdadZone = ZoneId.of("Asia/Baghdad")
        val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

        for (i in 0 until respArr.length()) {
            val item = respArr.optJSONObject(i) ?: continue
            val fixture = item.optJSONObject("fixture") ?: continue
            val teams = item.optJSONObject("teams") ?: continue
            val goals = item.optJSONObject("goals")
            val statusObj = fixture.optJSONObject("status")

            val dateStr = fixture.optString("date", "")
            if (dateStr.isBlank()) continue
            val instant = try {
                Instant.parse(dateStr)
            } catch (_: Exception) {
                continue
            }

            val zdt = instant.atZone(baghdadZone)
            val bDate = zdt.toLocalDate().format(dateFmt)
            val bTime = zdt.toLocalTime().format(timeFmt)

            val homeObj = teams.optJSONObject("home")
            val awayObj = teams.optJSONObject("away")
            val homeName = translateTeamName(homeObj?.optString("name", "الفريق الأول") ?: "الفريق الأول", teamOverrides)
            val awayName = translateTeamName(awayObj?.optString("name", "الفريق الثاني") ?: "الفريق الثاني", teamOverrides)
            val homeCrest = homeObj?.optString("logo")?.takeIf { it.isNotBlank() && it != "null" }
            val awayCrest = awayObj?.optString("logo")?.takeIf { it.isNotBlank() && it != "null" }

            val shortStatus = statusObj?.optString("short", "NS") ?: "NS"
            val status = parseFootballDataStatus(shortStatus)
            val elapsed = statusObj?.opt("elapsed")?.takeIf { it != JSONObject.NULL }?.toString()
            val homeScore = goals?.opt("home")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()
            val awayScore = goals?.opt("away")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()

            list.add(
                LiveMatchItem(
                    id = "${code}_${leagueId}_${CURRENT_SEASON}_${fixture.optInt("id", i)}",
                    utcIso = dateStr,
                    baghdadDate = bDate,
                    timeText = bTime,
                    homeTeam = homeName,
                    awayTeam = awayName,
                    homeCrestUrl = homeCrest,
                    awayCrestUrl = awayCrest,
                    league = translateCompetitionName("", code, extraLeagues),
                    leagueCode = code,
                    leagueLogoUrl = resolveLeagueLogoUrl(code, extraLeagues),
                    status = status,
                    minute = elapsed,
                    homeScore = homeScore,
                    awayScore = awayScore
                )
            )
        }
        list
    }
}

private suspend fun fetchMatchesFiltered(
    dateFrom: String,
    dateTo: String,
    teamOverrides: Map<String, String> = emptyMap(),
    extraLeagues: List<ExtraLeagueConfig> = emptyList()
): List<LiveMatchItem> {
    return withContext(Dispatchers.IO) {
        val fdDeferred = async {
            val url = "https://api.football-data.org/v4/matches?dateFrom=$dateFrom&dateTo=$dateTo"
            val body = httpGetJson(url, mapOf("X-Auth-Token" to FOOTBALL_DATA_API_KEY)) ?: return@async emptyList()

            val list = mutableListOf<LiveMatchItem>()
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
                    isExcludedCompetition(compNameRaw, compCode) ||
                    isExcludedCompetition(areaNameRaw, compCode)
                ) {
                    continue
                }

                val resolvedCode = detectLeagueCode(compNameRaw, compCode, extraLeagues)
                if (resolvedCode == null || resolvedCode in setOf("IRQ", "SAU", "ACL", "CAFCL")) {
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
                    homeObj?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: homeObj?.optString("shortName", "الفريق الأول")
                        ?: "الفريق الأول",
                    teamOverrides
                )
                val awayName = translateTeamName(
                    awayObj?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: awayObj?.optString("shortName", "الفريق الثاني")
                        ?: "الفريق الثاني",
                    teamOverrides
                )
                val homeCrest = homeObj?.optString("crest")?.takeIf {
                    it.isNotBlank() && it != "null" && !it.endsWith(".svg", ignoreCase = true)
                }
                val awayCrest = awayObj?.optString("crest")?.takeIf {
                    it.isNotBlank() && it != "null" && !it.endsWith(".svg", ignoreCase = true)
                }

                val scoreObj = item.optJSONObject("score")
                val fullTimeObj = scoreObj?.optJSONObject("fullTime")
                val halfTimeObj = scoreObj?.optJSONObject("halfTime")
                val homeScore = fullTimeObj?.opt("home")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()
                    ?: halfTimeObj?.opt("home")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()
                val awayScore = fullTimeObj?.opt("away")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()
                    ?: halfTimeObj?.opt("away")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull()

                val status = parseFootballDataStatus(item.optString("status", "SCHEDULED"))
                val minuteVal = item.opt("minute")?.takeIf { it != JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }

                list.add(
                    LiveMatchItem(
                        id = item.optString("id", "fd_$i"),
                        utcIso = utcDateStr,
                        baghdadDate = bDate,
                        timeText = bTime,
                        homeTeam = homeName,
                        awayTeam = awayName,
                        homeCrestUrl = homeCrest,
                        awayCrestUrl = awayCrest,
                        league = translateCompetitionName(compNameRaw, resolvedCode, extraLeagues),
                        leagueCode = resolvedCode,
                        leagueLogoUrl = resolveLeagueLogoUrl(resolvedCode, extraLeagues),
                        status = status,
                        minute = minuteVal,
                        homeScore = homeScore,
                        awayScore = awayScore
                    )
                )
            }
            list
        }

        val regionalDeferreds = REGIONAL_API_FOOTBALL_LEAGUE_IDS.flatMap { (code, ids) ->
            ids.map { leagueId ->
                async {
                    fetchRegionalMatchesForLeague(code, leagueId, dateFrom, dateTo, teamOverrides, extraLeagues)
                }
            }
        }

        val combined = mutableListOf<LiveMatchItem>()
        combined.addAll(fdDeferred.await())
        regionalDeferreds.awaitAll().forEach { combined.addAll(it) }

        combined
            .distinctBy { "${it.baghdadDate}_${it.leagueCode}_${it.homeTeam}_${it.awayTeam}" }
            .sortedBy { it.utcIso }
    }
}

private suspend fun fetchCardsByTeamForLeague(
    leagueCode: String,
    teamOverrides: Map<String, String>
): Map<String, Pair<Int, Int>> {
    val leagueId = when (leagueCode.trim().uppercase(Locale.ROOT)) {
        "PL" -> 39
        "PD" -> 140
        "SA" -> 135
        "BL1" -> 78
        "FL1" -> 61
        "CL" -> 2
        else -> return emptyMap()
    }
    return withContext(Dispatchers.IO) {
        val body = httpGetWithRegionalApiFootballKeys(
            "players?league=$leagueId&season=$CURRENT_SEASON&page=1"
        ) ?: return@withContext emptyMap()

        val yellowMap = mutableMapOf<String, Int>()
        val redMap = mutableMapOf<String, Int>()
        try {
            val root = JSONObject(body)
            val respArr = root.optJSONArray("response") ?: JSONArray()
            for (i in 0 until respArr.length()) {
                val entry = respArr.optJSONObject(i) ?: continue
                val statsArr = entry.optJSONArray("statistics") ?: JSONArray()
                for (s in 0 until statsArr.length()) {
                    val stat = statsArr.optJSONObject(s) ?: continue
                    val teamNameRaw = stat.optJSONObject("team")?.optString("name", "") ?: ""
                    val teamKey = normalizeTeamLookupKey(translateTeamName(teamNameRaw, teamOverrides))
                    if (teamKey.isBlank()) continue
                    val cardsObj = stat.optJSONObject("cards")
                    val y = cardsObj?.optInt("yellow", 0) ?: 0
                    val r = (cardsObj?.optInt("red", 0) ?: 0) + (cardsObj?.optInt("yellowred", 0) ?: 0)
                    yellowMap[teamKey] = (yellowMap[teamKey] ?: 0) + y
                    redMap[teamKey] = (redMap[teamKey] ?: 0) + r
                }
            }
        } catch (_: Exception) {
        }

        val allKeys = yellowMap.keys + redMap.keys
        allKeys.associateWith { k -> (yellowMap[k] ?: 0) to (redMap[k] ?: 0) }
    }
}

private suspend fun fetchStandingsForLeague(
    leagueCode: String,
    teamOverrides: Map<String, String> = emptyMap(),
    customStandings: Map<String, List<StandingRowItem>> = emptyMap()
): List<StandingRowItem> {
    val cleanCode = leagueCode.trim().uppercase(Locale.ROOT)
    val customRows = customStandings[cleanCode]?.map { row ->
        row.copy(teamName = translateTeamName(row.teamName, teamOverrides))
    }

    if (
        isExcludedCompetition(cleanCode, cleanCode) ||
        cleanCode in setOf("IRQ", "SAU", "ACL", "CAFCL")
    ) {
        return customRows.orEmpty()
    }

    return withContext(Dispatchers.IO) {
        val cardsDeferred = async { fetchCardsByTeamForLeague(cleanCode, teamOverrides) }
        val url = "https://api.football-data.org/v4/competitions/$cleanCode/standings"
        val body = httpGetJson(url, mapOf("X-Auth-Token" to FOOTBALL_DATA_API_KEY))
        val cardsByTeam = cardsDeferred.await()

        if (body.isNullOrBlank()) {
            return@withContext customRows.orEmpty()
        }

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
                teamObj?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: teamObj?.optString("shortName", "")
                    ?: "",
                teamOverrides
            )
            if (teamName.isBlank()) continue
            val crest = teamObj?.optString("crest")?.takeIf {
                it.isNotBlank() && it != "null" && !it.endsWith(".svg", ignoreCase = true)
            }
            val goalsFor = row.optInt("goalsFor", 0)
            val goalDiff = row.optInt("goalDifference", 0)
            val goalsAgainst = if (row.has("goalsAgainst")) {
                row.optInt("goalsAgainst", 0)
            } else {
                (goalsFor - goalDiff).coerceAtLeast(0)
            }
            val teamKey = normalizeTeamLookupKey(teamName)
            val teamCards = cardsByTeam[teamKey] ?: (0 to 0)

            rows.add(
                StandingRowItem(
                    position = row.optInt("position", i + 1),
                    teamName = teamName,
                    crestUrl = crest,
                    played = row.optInt("playedGames", 0),
                    won = row.optInt("won", 0),
                    draw = row.optInt("draw", 0),
                    lost = row.optInt("lost", 0),
                    goalsFor = goalsFor,
                    goalsAgainst = goalsAgainst,
                    goalDiff = goalDiff,
                    yellowCards = teamCards.first,
                    redCards = teamCards.second,
                    points = row.optInt("points", 0)
                )
            )
        }

        if (rows.isEmpty() && !customRows.isNullOrEmpty()) {
            return@withContext customRows
        }

        if (!customRows.isNullOrEmpty()) {
            val overrideByTeam = customRows.associateBy { normalizeTeamLookupKey(it.teamName) }
            val merged = rows.map { base ->
                val ov = overrideByTeam[normalizeTeamLookupKey(base.teamName)]
                if (ov != null) {
                    base.copy(
                        position = ov.position,
                        teamName = ov.teamName,
                        crestUrl = ov.crestUrl ?: base.crestUrl,
                        played = ov.played,
                        won = ov.won,
                        draw = ov.draw,
                        lost = ov.lost,
                        goalsFor = ov.goalsFor,
                        goalsAgainst = ov.goalsAgainst,
                        goalDiff = ov.goalDiff,
                        yellowCards = if (ov.yellowCards > 0) ov.yellowCards else base.yellowCards,
                        redCards = if (ov.redCards > 0) ov.redCards else base.redCards,
                        points = ov.points
                    )
                } else {
                    base
                }
            }.sortedBy { it.position }
            return@withContext merged
        }

        rows
    }
}

private suspend fun fetchTopScorersForLeague(
    leagueCode: String,
    playerOverrides: Map<String, String> = emptyMap(),
    teamOverrides: Map<String, String> = emptyMap()
): List<ScorerRowItem> {
    if (isExcludedCompetition(leagueCode, leagueCode)) {
        return emptyList()
    }
    return withContext(Dispatchers.IO) {
        val regionalIds = REGIONAL_API_FOOTBALL_LEAGUE_IDS[leagueCode]
        if (regionalIds != null) {
            for (leagueId in regionalIds) {
                val body = httpGetWithRegionalApiFootballKeys(
                    "players/topscorers?league=$leagueId&season=$CURRENT_SEASON"
                ) ?: continue
                val root = JSONObject(body)
                val respArr = root.optJSONArray("response") ?: JSONArray()
                val rows = mutableListOf<ScorerRowItem>()
                for (i in 0 until respArr.length()) {
                    val entry = respArr.optJSONObject(i) ?: continue
                    val playerObj = entry.optJSONObject("player") ?: continue
                    val statsArr = entry.optJSONArray("statistics") ?: JSONArray()
                    val firstStat = statsArr.optJSONObject(0)
                    val teamObj = firstStat?.optJSONObject("team")
                    val goalsObj = firstStat?.optJSONObject("goals")

                    val rawPlayerName = playerObj.optString("name", "")
                        .ifBlank {
                            "${playerObj.optString("firstname", "")} ${playerObj.optString("lastname", "")}".trim()
                        }
                    if (rawPlayerName.isBlank()) continue

                    val rawTeamName = teamObj?.optString("name", "") ?: ""
                    val teamCrest = teamObj?.optString("logo")?.takeIf { it.isNotBlank() && it != "null" }
                    val goalsCount = goalsObj?.optInt("total", 0) ?: 0
                    val assistsCount = goalsObj?.optInt("assists", 0) ?: 0

                    rows.add(
                        ScorerRowItem(
                            rank = i + 1,
                            playerName = translatePlayerName(rawPlayerName, playerOverrides, teamOverrides),
                            teamName = translateTeamName(rawTeamName, teamOverrides),
                            teamCrestUrl = teamCrest,
                            goals = goalsCount,
                            assists = assistsCount
                        )
                    )
                }
                if (rows.isNotEmpty()) {
                    return@withContext rows
                }
            }
            return@withContext emptyList()
        }

        val url = "https://api.football-data.org/v4/competitions/$leagueCode/scorers?limit=25"
        val body = httpGetJson(url, mapOf("X-Auth-Token" to FOOTBALL_DATA_API_KEY)) ?: return@withContext emptyList()

        val root = JSONObject(body)
        val scorersArr = root.optJSONArray("scorers") ?: JSONArray()
        val rows = mutableListOf<ScorerRowItem>()

        for (i in 0 until scorersArr.length()) {
            val entry = scorersArr.optJSONObject(i) ?: continue
            val playerObj = entry.optJSONObject("player")
            val teamObj = entry.optJSONObject("team")

            val rawPlayerName = playerObj?.optString("name", "") ?: ""
            if (rawPlayerName.isBlank()) continue

            val rawTeamName = teamObj?.optString("name", "")
                ?.takeIf { it.isNotBlank() }
                ?: teamObj?.optString("shortName", "")
                ?: ""
            val teamCrest = teamObj?.optString("crest")?.takeIf {
                it.isNotBlank() && it != "null" && !it.endsWith(".svg", ignoreCase = true)
            }
            val goalsCount = entry.optInt("goals", 0)
            val assistsCount = if (entry.isNull("assists")) 0 else entry.optInt("assists", 0)

            rows.add(
                ScorerRowItem(
                    rank = i + 1,
                    playerName = translatePlayerName(rawPlayerName, playerOverrides, teamOverrides),
                    teamName = translateTeamName(rawTeamName, teamOverrides),
                    teamCrestUrl = teamCrest,
                    goals = goalsCount,
                    assists = assistsCount
                )
            )
        }
        rows
    }
}

private suspend fun fetchTopAssistersForLeague(
    leagueCode: String,
    playerOverrides: Map<String, String> = emptyMap(),
    teamOverrides: Map<String, String> = emptyMap()
): List<ScorerRowItem> {
    if (isExcludedCompetition(leagueCode, leagueCode)) {
        return emptyList()
    }
    return withContext(Dispatchers.IO) {
        val regionalIds = REGIONAL_API_FOOTBALL_LEAGUE_IDS[leagueCode]
        if (regionalIds != null) {
            for (leagueId in regionalIds) {
                val body = httpGetWithRegionalApiFootballKeys(
                    "players/topassists?league=$leagueId&season=$CURRENT_SEASON"
                ) ?: continue
                val root = JSONObject(body)
                val respArr = root.optJSONArray("response") ?: JSONArray()
                val rows = mutableListOf<ScorerRowItem>()
                for (i in 0 until respArr.length()) {
                    val entry = respArr.optJSONObject(i) ?: continue
                    val playerObj = entry.optJSONObject("player") ?: continue
                    val statsArr = entry.optJSONArray("statistics") ?: JSONArray()
                    val firstStat = statsArr.optJSONObject(0)
                    val teamObj = firstStat?.optJSONObject("team")
                    val goalsObj = firstStat?.optJSONObject("goals")

                    val rawPlayerName = playerObj.optString("name", "")
                        .ifBlank {
                            "${playerObj.optString("firstname", "")} ${playerObj.optString("lastname", "")}".trim()
                        }
                    if (rawPlayerName.isBlank()) continue

                    val rawTeamName = teamObj?.optString("name", "") ?: ""
                    val teamCrest = teamObj?.optString("logo")?.takeIf { it.isNotBlank() && it != "null" }
                    val goalsCount = goalsObj?.optInt("total", 0) ?: 0
                    val assistsCount = goalsObj?.optInt("assists", 0) ?: 0

                    rows.add(
                        ScorerRowItem(
                            rank = i + 1,
                            playerName = translatePlayerName(rawPlayerName, playerOverrides, teamOverrides),
                            teamName = translateTeamName(rawTeamName, teamOverrides),
                            teamCrestUrl = teamCrest,
                            goals = goalsCount,
                            assists = assistsCount
                        )
                    )
                }
                if (rows.isNotEmpty()) {
                    return@withContext rows
                }
            }
        }

        val scorers = fetchTopScorersForLeague(leagueCode, playerOverrides, teamOverrides)
        scorers
            .filter { it.assists > 0 }
            .sortedByDescending { it.assists }
            .mapIndexed { idx, row -> row.copy(rank = idx + 1) }
    }
}

private fun groupMatchesByLeagueOrdered(
    dayMatches: List<LiveMatchItem>,
    extraLeagues: List<ExtraLeagueConfig> = emptyList()
): List<MatchFeedListEntry> {
    if (dayMatches.isEmpty()) return emptyList()

    val customPriorities = extraLeagues.associate { it.code.uppercase(Locale.ROOT) to it.priority }

    val grouped = dayMatches.groupBy { it.leagueCode ?: it.league }
    val sortedKeys = grouped.keys.sortedWith { k1, k2 ->
        val p1 = customPriorities[k1?.uppercase(Locale.ROOT)] ?: LEAGUE_ORDER_PRIORITY[k1] ?: 99
        val p2 = customPriorities[k2?.uppercase(Locale.ROOT)] ?: LEAGUE_ORDER_PRIORITY[k2] ?: 99
        if (p1 != p2) p1.compareTo(p2) else k1.compareTo(k2)
    }

    val entries = mutableListOf<MatchFeedListEntry>()
    for (key in sortedKeys) {
        val listForLeague = grouped[key].orEmpty().sortedBy { it.utcIso }
        if (listForLeague.isEmpty()) continue
        val sample = listForLeague.first()
        entries.add(
            MatchFeedListEntry.LeagueHeader(
                leagueKey = key,
                leagueName = sample.league,
                leagueCode = sample.leagueCode,
                leagueLogoUrl = sample.leagueLogoUrl ?: resolveLeagueLogoUrl(sample.leagueCode, extraLeagues),
                matchesCount = listForLeague.size
            )
        )
        listForLeague.forEach { m ->
            entries.add(MatchFeedListEntry.MatchCard(m))
        }
    }
    return entries
}

@androidx.annotation.OptIn(UnstableApi::class)
private class SystemOverlayPipManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val onExpandToApp: (ChannelItem) -> Unit,
    private val onCloseOverlay: () -> Unit
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var currentChannel by mutableStateOf<ChannelItem?>(null)

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity?.startActivity(intent) ?: context.startActivity(intent)
        }
    }

    fun show(channel: ChannelItem) {
        currentChannel = channel
        if (overlayView != null) {
            return
        }
        val typeFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            typeFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@SystemOverlayPipManager)
            setViewTreeViewModelStoreOwner(this@SystemOverlayPipManager)
            setViewTreeSavedStateRegistryOwner(this@SystemOverlayPipManager)
            setContent {
                val activeCh = currentChannel ?: return@setContent
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(138.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.5.dp, ActiveGreen, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                params.x = (params.x + dragAmount.x.roundToInt()).coerceAtLeast(0)
                                params.y = (params.y + dragAmount.y.roundToInt()).coerceAtLeast(0)
                                try {
                                    windowManager.updateViewLayout(this@apply, params)
                                } catch (_: Exception) {
                                }
                            }
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            }
                        },
                        update = { pv ->
                            pv.player = exoPlayer
                            pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeCh.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ActiveGreen)
                                    .clickable {
                                        val chToOpen = currentChannel
                                        dismiss()
                                        if (chToOpen != null) {
                                            onExpandToApp(chToOpen)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⤢",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                                    .clickable {
                                        dismiss()
                                        onCloseOverlay()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        try {
            windowManager.addView(view, params)
            overlayView = view
            layoutParams = params
        } catch (_: Exception) {
        }
    }

    fun dismiss() {
        val v = overlayView
        if (v != null) {
            try {
                windowManager.removeView(v)
            } catch (_: Exception) {
            }
            overlayView = null
            layoutParams = null
        }
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun destroy() {
        dismiss()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
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

        val todayStr = currentBaghdadTodayKey()

        val fallbackMatches = listOf(
            LiveMatchItem(
                id = "m3",
                utcIso = "2026-11-25T20:00:00Z",
                baghdadDate = todayStr,
                timeText = "23:00",
                homeTeam = "ريال مدريد",
                awayTeam = "برشلونة",
                league = "الدوري الإسباني",
                leagueCode = "PD",
                leagueLogoUrl = LEAGUE_LOGO_URLS["PD"],
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AliKazemSportApp(
    appLogoAsset: String,
    categories: List<CategoryItem>,
    fallbackMatches: List<LiveMatchItem>,
    accounts: List<SocialAccountItem>
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var selectedTab by remember { mutableIntStateOf(2) }
    var isArabic by remember { mutableStateOf(true) }
    var activeCategoryId by remember { mutableStateOf<CategoryId?>(null) }
    var selectedChannelId by remember { mutableStateOf<String?>(null) }
    var selectedServerIndex by remember { mutableIntStateOf(0) }
    var isMiniWindow by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var scaleMode by remember { mutableStateOf(PlayerScaleMode.STRETCH) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    var remoteConfig by remember {
        mutableStateOf(
            loadCachedChannelsJson(context)?.let { parseRemoteConfigJson(it, categories) }
                ?: RemoteConfigData(categories = categories)
        )
    }

    LaunchedEffect(Unit) {
        remoteConfig = fetchRemoteConfig(context, REMOTE_CHANNELS_JSON_URL, remoteConfig.categories)
    }

    val liveCategories = remoteConfig.categories

    val activeCategory = remember(liveCategories, activeCategoryId) {
        activeCategoryId?.let { id -> liveCategories.find { it.id == id } }
    }

    val selectedChannel = remember(liveCategories, activeCategory, selectedChannelId) {
        val chId = selectedChannelId ?: return@remember null
        activeCategory?.channels?.find { it.id == chId }
            ?: liveCategories.asSequence().flatMap { it.channels.asSequence() }.find { it.id == chId }
    }

    val serverOptions = remember(selectedChannel) {
        selectedChannel?.effectiveServers().orEmpty()
    }
    val safeServerIndex = selectedServerIndex.coerceIn(0, (serverOptions.size - 1).coerceAtLeast(0))
    val activeStreamUrl = selectedChannel?.let { ch ->
        serverOptions.getOrNull(safeServerIndex)?.url ?: ch.url
    }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000,
                50000,
                2000,
                4000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val sharedExoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    val overlayManager = remember(sharedExoPlayer) {
        SystemOverlayPipManager(
            context = context.applicationContext,
            exoPlayer = sharedExoPlayer,
            onExpandToApp = { ch ->
                selectedChannelId = ch.id
                isMiniWindow = false
                val launchIntent = Intent(context.applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.applicationContext.startActivity(launchIntent)
            },
            onCloseOverlay = {
                isMiniWindow = false
                selectedChannelId = null
                selectedServerIndex = 0
                sharedExoPlayer.stop()
            }
        )
    }

    LaunchedEffect(isMiniWindow, selectedChannel) {
        if (isMiniWindow && selectedChannel != null) {
            if (overlayManager.hasOverlayPermission()) {
                overlayManager.show(selectedChannel)
            } else {
                isMiniWindow = false
                overlayManager.requestOverlayPermission(activity)
            }
        } else {
            overlayManager.dismiss()
        }
    }

    LaunchedEffect(sharedExoPlayer, isMuted) {
        sharedExoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(activeStreamUrl, reloadNonce) {
        if (!activeStreamUrl.isNullOrBlank()) {
            sharedExoPlayer.setMediaItem(MediaItem.fromUri(activeStreamUrl))
            sharedExoPlayer.volume = if (isMuted) 0f else 1f
            sharedExoPlayer.prepare()
            sharedExoPlayer.playWhenReady = true
        } else {
            sharedExoPlayer.stop()
        }
    }

    DisposableEffect(sharedExoPlayer, safeServerIndex, serverOptions, activeStreamUrl) {
        var sameServerRetryCount = 0
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val url = activeStreamUrl ?: return
                if (sameServerRetryCount < 1) {
                    sameServerRetryCount++
                    sharedExoPlayer.stop()
                    sharedExoPlayer.setMediaItem(MediaItem.fromUri(url))
                    sharedExoPlayer.prepare()
                    sharedExoPlayer.playWhenReady = true
                } else {
                    val activeCandidates = serverOptions.indices.filter { idx ->
                        idx != safeServerIndex && serverOptions[idx].active
                    }
                    if (activeCandidates.isNotEmpty()) {
                        val nextIdx = activeCandidates.firstOrNull { it > safeServerIndex }
                            ?: activeCandidates.first()
                        selectedServerIndex = nextIdx
                    } else {
                        sameServerRetryCount = 0
                        sharedExoPlayer.stop()
                        sharedExoPlayer.setMediaItem(MediaItem.fromUri(url))
                        sharedExoPlayer.prepare()
                        sharedExoPlayer.playWhenReady = true
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    sameServerRetryCount = 0
                } else if (playbackState == Player.STATE_ENDED && !activeStreamUrl.isNullOrBlank()) {
                    sharedExoPlayer.seekToDefaultPosition()
                    sharedExoPlayer.prepare()
                    sharedExoPlayer.playWhenReady = true
                }
            }
        }
        sharedExoPlayer.addListener(listener)
        onDispose {
            sharedExoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(sharedExoPlayer, overlayManager) {
        onDispose {
            overlayManager.destroy()
            sharedExoPlayer.release()
        }
    }

    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val showFullScreenPlayer = selectedChannel != null && !isMiniWindow

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .padding(horizontal = if (showFullScreenPlayer) 0.dp else 14.dp, vertical = if (showFullScreenPlayer) 0.dp else 12.dp)
        ) {
            if (showFullScreenPlayer && selectedChannel != null) {
                FullScreenStreamPlayer(
                    channel = selectedChannel,
                    exoPlayer = sharedExoPlayer,
                    selectedServerIndex = safeServerIndex,
                    isMuted = isMuted,
                    scaleMode = scaleMode,
                    onToggleMute = { isMuted = !isMuted },
                    onCycleScaleMode = {
                        scaleMode = when (scaleMode) {
                            PlayerScaleMode.STRETCH -> PlayerScaleMode.FIT_16_9
                            PlayerScaleMode.FIT_16_9 -> PlayerScaleMode.STRETCH
                        }
                    },
                    onReload = { reloadNonce++ },
                    onEnterMiniWindow = {
                        if (overlayManager.hasOverlayPermission()) {
                            isMiniWindow = true
                        } else {
                            overlayManager.requestOverlayPermission(activity)
                        }
                    },
                    onSelectServer = { idx -> selectedServerIndex = idx },
                    onBackToChannels = {
                        selectedChannelId = null
                        selectedServerIndex = 0
                        isMiniWindow = false
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
                            if (activeCategory == null) {
                                CategoriesGridView(
                                    categories = liveCategories,
                                    onSelectCategory = { cat -> activeCategoryId = cat.id }
                                )
                            } else {
                                CategoryChannelsListView(
                                    category = activeCategory,
                                    selectedChannel = selectedChannel,
                                    onBackToCategories = { activeCategoryId = null },
                                    onSelectChannel = { ch ->
                                        val availableServers = ch.effectiveServers()
                                        val activeIndices = availableServers.indices.filter { availableServers[it].active }
                                        selectedServerIndex = if (activeIndices.isNotEmpty()) {
                                            activeIndices[Random.nextInt(activeIndices.size)]
                                        } else {
                                            0
                                        }
                                        selectedChannelId = ch.id
                                        isMiniWindow = false
                                    }
                                )
                            }
                        }
                        1 -> MatchesAndStandingsView(
                            fallbackMatches = fallbackMatches,
                            remoteConfig = remoteConfig
                        )
                        else -> AccountsTabView(accounts = accounts)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueFallbackMark(
    leagueCode: String?,
    fallbackLabel: String,
    modifier: Modifier = Modifier
) {
    val code = leagueCode?.trim()?.uppercase(Locale.ROOT)
        ?: detectLeagueCode(fallbackLabel, null)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (code != null && LEAGUE_LOGO_URLS.containsKey(code)) {
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
                        val starR = r * 0.34f
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
                        val wave = Path().apply {
                            moveTo(cx - r * 0.46f, cy + r * 0.05f)
                            quadraticBezierTo(cx - r * 0.15f, cy - r * 0.36f, cx, cy)
                            quadraticBezierTo(cx + r * 0.15f, cy + r * 0.36f, cx + r * 0.46f, cy - r * 0.05f)
                        }
                        drawPath(
                            path = wave,
                            color = Color(0xFF00E676),
                            style = Stroke(width = r * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    else -> {
                        drawCircle(color = ActiveGreen, radius = r * 0.72f)
                    }
                }
            }
        } else {
            Text(
                text = fallbackLabel.take(2),
                color = ActiveGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RemoteCrestBadge(
    crestUrl: String?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
    leagueCode: String? = null
) {
    val resolvedCode = leagueCode ?: detectLeagueCode(fallbackLabel, null)
    val effectiveUrl = crestUrl ?: resolvedCode?.let { LEAGUE_LOGO_URLS[it] }

    var bitmap by remember(effectiveUrl) {
        mutableStateOf(effectiveUrl?.let { CrestMemoryCache.get(it) })
    }

    LaunchedEffect(effectiveUrl) {
        if (bitmap == null && !effectiveUrl.isNullOrBlank()) {
            bitmap = fetchBitmapFromUrl(effectiveUrl)
        }
    }

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(TileInnerBg)
            .border(1.dp, BorderGreen, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = fallbackLabel,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            )
        } else {
            LeagueFallbackMark(
                leagueCode = resolvedCode,
                fallbackLabel = fallbackLabel
            )
        }
    }
}

@Composable
private fun MatchesAndStandingsView(
    fallbackMatches: List<LiveMatchItem>,
    remoteConfig: RemoteConfigData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dayTick by remember { mutableIntStateOf(0) }
    val dayOptions = remember(dayTick) { buildBaghdadDays() }
    val todayKey = remember(dayTick) { currentBaghdadTodayKey() }

    val standingLeagues = remember(remoteConfig.extraLeagues, remoteConfig.customStandings) {
        val merged = STANDING_LEAGUES.toMutableList()
        val existingCodes = merged.map { it.code.uppercase(Locale.ROOT) }.toMutableSet()
        remoteConfig.extraLeagues.forEach { ex ->
            val codeUpper = ex.code.uppercase(Locale.ROOT)
            if (codeUpper != "IRQ" && codeUpper !in existingCodes && (remoteConfig.customStandings.containsKey(codeUpper) || codeUpper in LEAGUE_LOGO_URLS)) {
                merged.add(LeagueOption(code = codeUpper, shortLabel = ex.shortLabel))
                existingCodes.add(codeUpper)
            }
        }
        remoteConfig.customStandings.keys.forEach { rawKey ->
            val codeUpper = rawKey.trim().uppercase(Locale.ROOT)
            if (codeUpper != "IRQ" && codeUpper !in existingCodes) {
                merged.add(LeagueOption(code = codeUpper, shortLabel = codeUpper))
                existingCodes.add(codeUpper)
            }
        }
        merged
    }

    val scorerLeagues = remember(remoteConfig.extraLeagues) {
        val merged = SCORER_LEAGUES.toMutableList()
        val existingCodes = merged.map { it.code.uppercase(Locale.ROOT) }.toMutableSet()
        remoteConfig.extraLeagues.forEach { ex ->
            val codeUpper = ex.code.uppercase(Locale.ROOT)
            if (codeUpper != "IRQ" && codeUpper !in existingCodes && codeUpper in REGIONAL_API_FOOTBALL_LEAGUE_IDS) {
                merged.add(LeagueOption(code = codeUpper, shortLabel = ex.shortLabel))
                existingCodes.add(codeUpper)
            }
        }
        merged
    }

    val cachedInitialMatches = remember { loadMatchesCache(context) }

    // 0: المباريات | 1: الترتيب | 2: الهدافون (ويحتوي بداخله على تبويب الهدافون واسيست)
    var subMode by remember { mutableIntStateOf(0) }
    // false = الهدافون | true = اسيست
    var showAssistsMode by remember { mutableStateOf(false) }

    var selectedDayKey by remember { mutableStateOf(todayKey) }
    var lastObservedTodayKey by remember { mutableStateOf(todayKey) }
    var matches by remember { mutableStateOf(cachedInitialMatches) }
    var isLoadingMatches by remember { mutableStateOf(cachedInitialMatches.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }

    var selectedStandingLeague by remember { mutableStateOf(STANDING_LEAGUES.first()) }
    var standingsRows by remember {
        mutableStateOf(loadStandingsCache(context, STANDING_LEAGUES.first().code))
    }
    var isLoadingStandings by remember { mutableStateOf(standingsRows.isEmpty()) }

    var selectedScorerLeague by remember { mutableStateOf(SCORER_LEAGUES.first()) }
    var scorersRows by remember {
        mutableStateOf(loadScorersCache(context, SCORER_LEAGUES.first().code))
    }
    var isLoadingScorers by remember { mutableStateOf(scorersRows.isEmpty()) }

    var assistersRows by remember {
        mutableStateOf(loadAssistsCache(context, SCORER_LEAGUES.first().code))
    }
    var isLoadingAssisters by remember { mutableStateOf(assistersRows.isEmpty()) }

    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            tick++
            val latestToday = currentBaghdadTodayKey()
            if (latestToday != lastObservedTodayKey) {
                lastObservedTodayKey = latestToday
                dayTick++
                selectedDayKey = latestToday
            }
        }
    }

    fun loadMatches(manual: Boolean = false) {
        scope.launch {
            if (manual) {
                isRefreshing = true
            } else if (matches.isEmpty()) {
                isLoadingMatches = true
            }
            val daysRange = buildBaghdadDays()
            val fromDate = daysRange.first().key
            val toDate = daysRange.last().key
            val fetched = fetchMatchesFiltered(
                dateFrom = fromDate,
                dateTo = toDate,
                teamOverrides = remoteConfig.teamOverrides,
                extraLeagues = remoteConfig.extraLeagues
            )
            if (fetched.isNotEmpty()) {
                matches = fetched
                saveMatchesCache(context, fetched)
            } else if (matches.isEmpty()) {
                matches = fallbackMatches
                    .filter { !isExcludedCompetition(it.league, it.leagueCode) }
                    .map { m ->
                        m.copy(
                            homeTeam = translateTeamName(m.homeTeam, remoteConfig.teamOverrides),
                            awayTeam = translateTeamName(m.awayTeam, remoteConfig.teamOverrides)
                        )
                    }
            }
            isLoadingMatches = false
            isRefreshing = false
        }
    }

    fun loadStandings(code: String) {
        scope.launch {
            val cachedRows = loadStandingsCache(context, code)
            if (cachedRows.isNotEmpty()) {
                standingsRows = cachedRows
                isLoadingStandings = false
            } else {
                standingsRows = emptyList()
                isLoadingStandings = true
            }
            val fetched = fetchStandingsForLeague(
                leagueCode = code,
                teamOverrides = remoteConfig.teamOverrides,
                customStandings = remoteConfig.customStandings
            )
            if (fetched.isNotEmpty()) {
                standingsRows = fetched
                saveStandingsCache(context, code, fetched)
            }
            isLoadingStandings = false
        }
    }

    fun loadScorers(code: String) {
        scope.launch {
            val cachedRows = loadScorersCache(context, code)
            if (cachedRows.isNotEmpty()) {
                scorersRows = cachedRows
                isLoadingScorers = false
            } else {
                scorersRows = emptyList()
                isLoadingScorers = true
            }
            val fetched = fetchTopScorersForLeague(
                leagueCode = code,
                playerOverrides = remoteConfig.playerOverrides,
                teamOverrides = remoteConfig.teamOverrides
            )
            if (fetched.isNotEmpty()) {
                scorersRows = fetched
                saveScorersCache(context, code, fetched)
            }
            isLoadingScorers = false
        }
    }

    fun loadAssisters(code: String) {
        scope.launch {
            val cachedRows = loadAssistsCache(context, code)
            if (cachedRows.isNotEmpty()) {
                assistersRows = cachedRows
                isLoadingAssisters = false
            } else {
                assistersRows = emptyList()
                isLoadingAssisters = true
            }
            val fetched = fetchTopAssistersForLeague(
                leagueCode = code,
                playerOverrides = remoteConfig.playerOverrides,
                teamOverrides = remoteConfig.teamOverrides
            )
            if (fetched.isNotEmpty()) {
                assistersRows = fetched
                saveAssistsCache(context, code, fetched)
            }
            isLoadingAssisters = false
        }
    }

    LaunchedEffect(remoteConfig) {
        loadMatches(manual = false)
        when (subMode) {
            1 -> loadStandings(selectedStandingLeague.code)
            2 -> {
                if (showAssistsMode) loadAssisters(selectedScorerLeague.code)
                else loadScorers(selectedScorerLeague.code)
            }
        }
    }

    LaunchedEffect(subMode, selectedStandingLeague) {
        if (subMode == 1) {
            loadStandings(selectedStandingLeague.code)
        }
    }

    LaunchedEffect(subMode, selectedScorerLeague, showAssistsMode) {
        if (subMode == 2) {
            if (showAssistsMode) loadAssisters(selectedScorerLeague.code)
            else loadScorers(selectedScorerLeague.code)
        }
    }

    val dayMatches = remember(matches, selectedDayKey) {
        matches.filter {
            it.baghdadDate == selectedDayKey &&
                !isExcludedCompetition(it.league, it.leagueCode) &&
                !isExcludedCompetition(it.homeTeam) &&
                !isExcludedCompetition(it.awayTeam)
        }
    }

    val orderedEntries = remember(dayMatches, remoteConfig.extraLeagues) {
        groupMatchesByLeagueOrdered(dayMatches, remoteConfig.extraLeagues)
    }

    val selectedDayInfo = dayOptions.find { it.key == selectedDayKey }
        ?: dayOptions.find { it.key == todayKey }
        ?: dayOptions.first()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBg)
                .border(1.dp, BorderGreen, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf(
                0 to "المباريات",
                1 to "الترتيب",
                2 to "الهدافون"
            )
            subTabs.forEach { (mode, title) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (subMode == mode) ActiveGreen else Color.Transparent)
                        .clickable { subMode = mode }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (subMode == mode) Color.White else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (subMode) {
            0 -> {
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
                } else if (orderedEntries.isEmpty()) {
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
                        items(
                            items = orderedEntries,
                            key = { entry ->
                                when (entry) {
                                    is MatchFeedListEntry.LeagueHeader -> "hdr_${entry.leagueKey}"
                                    is MatchFeedListEntry.MatchCard -> "m_${entry.match.id}"
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is MatchFeedListEntry.LeagueHeader -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(TileBg)
                                            .border(1.dp, BorderGreen, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            RemoteCrestBadge(
                                                crestUrl = entry.leagueLogoUrl,
                                                fallbackLabel = entry.leagueName,
                                                modifier = Modifier.size(22.dp),
                                                leagueCode = entry.leagueCode
                                            )
                                            Text(
                                                text = entry.leagueName,
                                                color = ActiveGreen,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Text(
                                            text = "${entry.matchesCount}",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PanelBg)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                is MatchFeedListEntry.MatchCard -> {
                                    val match = entry.match
                                    val statusBadge = remember(match, tick) {
                                        buildMatchStatusBadge(match)
                                    }
                                    val isStartedOrFinished = match.status == MatchFeedStatus.LIVE ||
                                        match.status == MatchFeedStatus.HALFTIME ||
                                        match.status == MatchFeedStatus.FINISHED ||
                                        statusBadge.isLive ||
                                        statusBadge.label == "انتهت"

                                    val homeNum = match.homeScore ?: 0
                                    val awayNum = match.awayScore ?: 0
                                    val centerDisplay = if (isStartedOrFinished) {
                                        "$homeNum - $awayNum"
                                    } else {
                                        match.timeText
                                    }

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
                                            Text(
                                                text = match.league,
                                                color = TextMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

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
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Text(
                                                text = centerDisplay,
                                                color = if (isStartedOrFinished) ActiveGreen else TextWhite,
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
                                                    maxLines = 2,
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
                    }
                }
            }

            1 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standingLeagues.forEach { league ->
                        val selected = league.code == selectedStandingLeague.code
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) ActiveGreen else PanelBg)
                                .border(1.dp, if (selected) ActiveGreen else BorderGreen, RoundedCornerShape(12.dp))
                                .clickable { selectedStandingLeague = league }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            RemoteCrestBadge(
                                crestUrl = resolveLeagueLogoUrl(league.code, remoteConfig.extraLeagues),
                                fallbackLabel = league.shortLabel,
                                modifier = Modifier.size(18.dp),
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

                if (isLoadingStandings && standingsRows.isEmpty()) {
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
                            text = "لا توجد بيانات ترتيب متاحة لهذه البطولة حالياً",
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
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                        Text("الفريق", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("لعب", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                        Text("ف", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                        Text("ت", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                        Text("خ", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                        Text("له", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                        Text("عليه", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        Text("فارق", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        Text("🟨", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                        Text("🟥", color = LiveRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                        Text("نقاط", color = ActiveGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(standingsRows, key = { "${it.position}_${it.teamName}" }) { row ->
                            val gdLabel = if (row.goalDiff > 0) "+${row.goalDiff}" else "${row.goalDiff}"
                            val gdColor = when {
                                row.goalDiff > 0 -> ActiveGreen
                                row.goalDiff < 0 -> LiveRed
                                else -> TextMuted
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PanelBg)
                                    .border(1.dp, BorderGreen, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${row.position}",
                                    color = if (row.position <= 4) ActiveGreen else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(16.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RemoteCrestBadge(
                                        crestUrl = row.crestUrl,
                                        fallbackLabel = row.teamName,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = row.teamName,
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text("${row.played}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                                Text("${row.won}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                                Text("${row.draw}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                                Text("${row.lost}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                                Text("${row.goalsFor}", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                                Text("${row.goalsAgainst}", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                Text(gdLabel, color = gdColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                Text("${row.yellowCards}", color = Color(0xFFFACC15), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                                Text("${row.redCards}", color = LiveRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                                Text(
                                    text = "${row.points}",
                                    color = ActiveGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(26.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    scorerLeagues.forEach { league ->
                        val selected = league.code == selectedScorerLeague.code
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) ActiveGreen else PanelBg)
                                .border(1.dp, if (selected) ActiveGreen else BorderGreen, RoundedCornerShape(12.dp))
                                .clickable { selectedScorerLeague = league }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            RemoteCrestBadge(
                                crestUrl = resolveLeagueLogoUrl(league.code, remoteConfig.extraLeagues),
                                fallbackLabel = league.shortLabel,
                                modifier = Modifier.size(18.dp),
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TileBg)
                        .border(1.dp, BorderGreen, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!showAssistsMode) ActiveGreen else Color.Transparent)
                            .clickable { showAssistsMode = false }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "الهدافون",
                            color = if (!showAssistsMode) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (showAssistsMode) ActiveGreen else Color.Transparent)
                            .clickable { showAssistsMode = true }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اسيست",
                            color = if (showAssistsMode) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!showAssistsMode) {
                    if (isLoadingScorers && scorersRows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ActiveGreen)
                        }
                    } else if (scorersRows.isEmpty()) {
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
                                text = "لا توجد بيانات هدافين متاحة لهذه البطولة حالياً",
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
                            Text("اللاعب / النادي", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("اسيست", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                            Text("الأهداف", color = ActiveGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(scorersRows, key = { "${it.rank}_${it.playerName}_${it.teamName}" }) { row ->
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
                                        text = "${row.rank}",
                                        color = if (row.rank <= 3) ActiveGreen else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    RemoteCrestBadge(
                                        crestUrl = row.teamCrestUrl,
                                        fallbackLabel = row.teamName
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.playerName,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = row.teamName,
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${row.assists}",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(44.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "${row.goals}",
                                        color = ActiveGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .width(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TileInnerBg)
                                            .border(1.dp, BorderGreen, RoundedCornerShape(8.dp))
                                            .padding(vertical = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (isLoadingAssisters && assistersRows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ActiveGreen)
                        }
                    } else if (assistersRows.isEmpty()) {
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
                                text = "لا توجد بيانات اسيست متاحة لهذه البطولة حالياً",
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
                            Text("اللاعب / النادي", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("اسيست", color = ActiveGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(assistersRows, key = { "${it.rank}_${it.playerName}_${it.teamName}" }) { row ->
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
                                        text = "${row.rank}",
                                        color = if (row.rank <= 3) ActiveGreen else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    RemoteCrestBadge(
                                        crestUrl = row.teamCrestUrl,
                                        fallbackLabel = row.teamName
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.playerName,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = row.teamName,
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${row.assists}",
                                        color = ActiveGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .width(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TileInnerBg)
                                            .border(1.dp, BorderGreen, RoundedCornerShape(8.dp))
                                            .padding(vertical = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
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

private fun formatMinuteLabel(rawMinute: String?): String? {
    val clean = rawMinute?.trim().orEmpty()
    if (clean.isBlank()) return null
    return if (clean.endsWith("'")) clean else "$clean'"
}

private fun buildMatchStatusBadge(match: LiveMatchItem): StatusBadgeUi {
    val kickMs = try {
        Instant.parse(match.utcIso).toEpochMilli()
    } catch (_: Exception) {
        0L
    }
    val elapsedMin = if (kickMs > 0L) ((System.currentTimeMillis() - kickMs) / 60000L) else -1L

    return when (match.status) {
        MatchFeedStatus.FINISHED -> StatusBadgeUi("انتهت", isLive = false)
        MatchFeedStatus.HALFTIME -> StatusBadgeUi("استراحة", isLive = true)
        MatchFeedStatus.LIVE -> {
            val formattedMin = formatMinuteLabel(match.minute)
                ?: if (elapsedMin in 0..130) "$elapsedMin'" else "مباشر"
            StatusBadgeUi(formattedMin, isLive = true)
        }
        MatchFeedStatus.SCHEDULED -> {
            if (elapsedMin in 0..120) {
                val formattedMin = formatMinuteLabel(match.minute) ?: "$elapsedMin'"
                StatusBadgeUi(formattedMin, isLive = true)
            } else if (elapsedMin > 120) {
                StatusBadgeUi("انتهت", isLive = false)
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullScreenStreamPlayer(
    channel: ChannelItem,
    exoPlayer: ExoPlayer,
    selectedServerIndex: Int,
    isMuted: Boolean,
    scaleMode: PlayerScaleMode,
    onToggleMute: () -> Unit,
    onCycleScaleMode: () -> Unit,
    onReload: () -> Unit,
    onEnterMiniWindow: () -> Unit,
    onSelectServer: (Int) -> Unit,
    onBackToChannels: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val serverOptions = remember(channel.id, channel.url, channel.backupUrl, channel.servers) {
        channel.effectiveServers()
    }
    val safeIndex = selectedServerIndex.coerceIn(0, (serverOptions.size - 1).coerceAtLeast(0))

    var showControls by remember(channel.id) { mutableStateOf(true) }

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

    val resizeModeValue = when (scaleMode) {
        PlayerScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        PlayerScaleMode.FIT_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls },
        contentAlignment = Alignment.Center
    ) {
        val playerModifier = if (scaleMode == PlayerScaleMode.FIT_16_9) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(16f / 9f)
        } else {
            Modifier.fillMaxSize()
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = resizeModeValue
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeModeValue
            },
            modifier = playerModifier
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (serverOptions.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            serverOptions.forEachIndexed { idx, srv ->
                                val isCurrent = safeIndex == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isCurrent -> ActiveGreen
                                                !srv.active -> Color.DarkGray.copy(alpha = 0.45f)
                                                else -> Color.DarkGray.copy(alpha = 0.85f)
                                            }
                                        )
                                        .clickable { onSelectServer(idx) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = if (srv.active || isCurrent) Color.White else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMuted) LiveRed.copy(alpha = 0.85f) else Color.DarkGray.copy(alpha = 0.85f))
                            .clickable(onClick = onToggleMute),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMuted) "🔇" else "🔊",
                            fontSize = 15.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ActiveGreen.copy(alpha = 0.85f))
                            .clickable(onClick = onEnterMiniWindow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "❐",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray.copy(alpha = 0.85f))
                            .clickable(onClick = onCycleScaleMode)
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scaleMode.labelAr,
                            color = ActiveGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray.copy(alpha = 0.85f))
                            .clickable(onClick = onReload),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "إعادة تحميل",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    IconButton(onClick = onBackToChannels) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
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
                imageUrl = category.logoUrl,
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
                                imageUrl = channel.logoUrl,
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
                                imageUrl = acc.logoUrl,
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
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    var remoteBitmap by remember(cleanUrl) {
        mutableStateOf(cleanUrl?.let { CrestMemoryCache.get(it) })
    }

    LaunchedEffect(cleanUrl) {
        if (remoteBitmap == null && !cleanUrl.isNullOrBlank()) {
            remoteBitmap = fetchBitmapFromUrl(cleanUrl)
        }
    }

    val drawableId = rememberDrawableId(assetName)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TileInnerBg),
        contentAlignment = Alignment.Center
    ) {
        val loadedRemote = remoteBitmap
        if (loadedRemote != null) {
            Image(
                bitmap = loadedRemote.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        } else if (drawableId != 0) {
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
