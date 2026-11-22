package com.aliskazem.livetv

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

enum class CategoryId {
    BEIN, ALWAN, ALKASS, ABUDHABI, THMANYAH
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

data class MatchScheduleItem(
    val id: String,
    val teams: String,
    val tournament: String,
    val timeText: String,
    val isLive: Boolean = false
)

private val ScreenBg = Color(0xFF060B08)
private val PanelBg = Color(0xFF0A140F)
private val TileBg = Color(0xFF07120C)
private val TileInnerBg = Color(0xFF040B07)
private val BorderGreen = Color(0xFF143322)
private val ActiveGreen = Color(0xFF10B981)
private val TextWhite = Color(0xFFF3F6F4)
private val TextMuted = Color(0xFF8FA398)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // شعار التطبيق المرجعي: /assets/images/1793667103213.jpg -> drawable: img_1793667103213
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

        val matches = listOf(
            MatchScheduleItem(
                id = "m1",
                teams = "المنتخب العراقي × خصم دولي",
                tournament = "مباراة دولية",
                timeText = "21:00",
                isLive = true
            ),
            MatchScheduleItem(
                id = "m2",
                teams = "ريال مدريد × برشلونة",
                tournament = "الدوري الإسباني",
                timeText = "23:00"
            ),
            MatchScheduleItem(
                id = "m3",
                teams = "مانشستر سيتي × أرسنال",
                tournament = "الدوري الإنجليزي",
                timeText = "19:30"
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
                    matches = matches
                )
            }
        }
    }
}

@Composable
fun AliKazemSportApp(
    appLogoAsset: String,
    categories: List<CategoryItem>,
    matches: List<MatchScheduleItem>
) {
    // 0: حساباتي | 1: جدول المباريات | 2: قنوات البث
    var selectedTab by remember { mutableIntStateOf(2) }
    var isArabic by remember { mutableStateOf(true) }
    var activeCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedChannel by remember { mutableStateOf<ChannelItem?>(null) }
    var useBackupStream by remember { mutableStateOf(false) }

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
                // البطاقة العلوية: زر اللغة + العنوان + الشعار الدائري
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

                // شريط التبويب الثلاثي
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
                        label = if (isArabic) "حساباتي" else "Accounts",
                        iconGlyph = "⑂",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
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
                        label = if (isArabic) "قنوات البث" else "Live TV",
                        iconGlyph = "▣",
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
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
                                selectedChannel = selectedChannel,
                                useBackupStream = useBackupStream,
                                onToggleBackup = { useBackupStream = !useBackupStream },
                                onClearChannel = {
                                    selectedChannel = null
                                    useBackupStream = false
                                },
                                onSelectCategory = { cat ->
                                    activeCategory = cat
                                }
                            )
                        } else {
                            CategoryChannelsListView(
                                category = currentCategory,
                                selectedChannel = selectedChannel,
                                useBackupStream = useBackupStream,
                                onToggleBackup = { useBackupStream = !useBackupStream },
                                onBackToCategories = { activeCategory = null },
                                onSelectChannel = { ch ->
                                    selectedChannel = ch
                                    useBackupStream = false
                                },
                                onClearChannel = {
                                    selectedChannel = null
                                    useBackupStream = false
                                }
                            )
                        }
                    }
                    1 -> MatchesScheduleView(matches = matches)
                    else -> AccountsTabView()
                }
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
private fun SelectedStreamBanner(
    channel: ChannelItem,
    useBackupStream: Boolean,
    onToggleBackup: () -> Unit,
    onClose: () -> Unit
) {
    val activeUrl = if (useBackupStream && !channel.backupUrl.isNullOrBlank()) {
        channel.backupUrl
    } else {
        channel.url
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelBg)
            .border(1.dp, ActiveGreen, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "القناة المحددة: ${channel.name}",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "إغلاق",
                color = ActiveGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ActiveGreen.copy(alpha = 0.15f))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Text(
            text = activeUrl,
            color = ActiveGreen,
            fontSize = 11.sp,
            maxLines = 1
        )

        if (activeUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            StreamVideoPlayer(streamUrl = activeUrl)
        }

        if (!channel.backupUrl.isNullOrBlank()) {
            Text(
                text = if (useBackupStream) "السيرفر الاحتياطي مفعّل (اضغط للأساسي)" else "تبديل إلى السيرفر الاحتياطي",
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TileInnerBg)
                    .border(1.dp, BorderGreen, RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleBackup)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
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

    var isLandscape by remember { mutableStateOf(false) }

    fun setLandscapeMode(enable: Boolean) {
        isLandscape = enable
        activity?.requestedOrientation = if (enable) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    setLandscapeMode(false)
                    onBackToChannels()
                }) {
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

            Row(verticalAlignment = Alignment.CenterVertically) {
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

                IconButton(onClick = { setLandscapeMode(!isLandscape) }) {
                    Text(
                        text = if (isLandscape) "↙" else "⛶",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamVideoPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun CategoriesGridView(
    categories: List<CategoryItem>,
    selectedChannel: ChannelItem?,
    useBackupStream: Boolean,
    onToggleBackup: () -> Unit,
    onClearChannel: () -> Unit,
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
    useBackupStream: Boolean,
    onToggleBackup: () -> Unit,
    onBackToCategories: () -> Unit,
    onSelectChannel: (ChannelItem) -> Unit,
    onClearChannel: () -> Unit
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

        items(category.channels, key = { it.id }) { channel ->
            val isSelected = selectedChannel?.id == channel.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFF0E2419) else PanelBg)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) ActiveGreen else BorderGreen,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectChannel(channel) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ChannelOrCategoryArtwork(
                    assetName = channel.logoAssetName,
                    contentDescription = channel.name,
                    badgeText = channel.name.take(2).uppercase(),
                    badgeBg = TileInnerBg,
                    badgeTextColor = ActiveGreen,
                    modifier = Modifier.width(74.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "الشعار: ${channel.logoAssetName} • ${if (!channel.backupUrl.isNullOrBlank()) "يدعم رابط احتياطي" else "رابط أساسي"}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (isSelected) "محدد" else "اختيار",
                    color = if (isSelected) Color.White else ActiveGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ActiveGreen else ActiveGreen.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MatchesScheduleView(matches: List<MatchScheduleItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(matches, key = { it.id }) { match ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelBg)
                    .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.teams,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = match.tournament,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = if (match.isLive) "مباشر • ${match.timeText}" else match.timeText,
                    color = if (match.isLive) Color.White else ActiveGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (match.isLive) ActiveGreen else ActiveGreen.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountsTabView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PanelBg)
            .border(1.dp, BorderGreen, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "حسابات القناة والمنصات",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "TikTok: علي كاظم 🇮🇶🇮🇶",
            color = ActiveGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "YouTube & Live Streams: Ali Kazem SPORTS",
            color = TextMuted,
            fontSize = 13.sp
        )
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
