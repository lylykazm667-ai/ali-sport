package com.aliskazem.livetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TvChannelItem(
    val id: String,
    val title: String,
    val badgeText: String,
    val badgeBg: Color,
    val badgeTextColor: Color,
    val streamUrl: String
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

        val channels = listOf(
            TvChannelItem(
                id = "alwan",
                title = "Alwan Sport",
                badgeText = "ALWAN\nSPORT",
                badgeBg = Color(0xFF0D2818),
                badgeTextColor = Color(0xFF4ADE80),
                streamUrl = "https://example.com/alwan.m3u8"
            ),
            TvChannelItem(
                id = "bein",
                title = "bein sport",
                badgeText = "beIN\nSPORTS",
                badgeBg = Color(0xFF4C1D95),
                badgeTextColor = Color.White,
                streamUrl = "https://example.com/bein.m3u8"
            ),
            TvChannelItem(
                id = "ad_sports",
                title = "Abu Dhabi Sports",
                badgeText = "أبوظبي الرياضية",
                badgeTextColor = Color.White,
                badgeBg = Color(0xFF0B1C14),
                streamUrl = "https://example.com/adsports.m3u8"
            ),
            TvChannelItem(
                id = "alkass",
                title = "Alkass Sport",
                badgeText = "┐",
                badgeBg = Color(0xFF881337),
                badgeTextColor = Color.White,
                streamUrl = "https://example.com/alkass.m3u8"
            ),
            TvChannelItem(
                id = "thmanyah",
                title = "Thmanyah Sports",
                badgeText = "▲",
                badgeBg = Color(0xFF0A1710),
                badgeTextColor = Color.White,
                streamUrl = "https://example.com/thmanyah.m3u8"
            )
        )

        val matches = listOf(
            MatchScheduleItem(
                id = "m1",
                teams = "المنتخب العراقي × خصم دولي",
                tournament = "مباراة ودية / دولية",
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
                    channels = channels,
                    matches = matches
                )
            }
        }
    }
}

@Composable
fun AliKazemSportApp(
    channels: List<TvChannelItem>,
    matches: List<MatchScheduleItem>
) {
    // 0: حساباتي | 1: جدول المباريات | 2: قنوات البث
    var selectedTab by remember { mutableIntStateOf(2) }
    var isArabic by remember { mutableStateOf(true) }
    var selectedChannel by remember { mutableStateOf<TvChannelItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
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
                    Text(
                        text = "Ali Kazem\nSPORTS",
                        color = Color(0xFFE8F5E9),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 9.sp
                    )
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
                2 -> ChannelsGridView(
                    channels = channels,
                    selectedChannel = selectedChannel,
                    onSelectChannel = { selectedChannel = it }
                )

                1 -> MatchesScheduleView(matches = matches)

                else -> AccountsTabView()
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
private fun ChannelsGridView(
    channels: List<TvChannelItem>,
    selectedChannel: TvChannelItem?,
    onSelectChannel: (TvChannelItem) -> Unit
) {
    val rows = channels.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        if (selectedChannel != null) {
            item(key = "active_banner") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PanelBg)
                        .border(1.dp, ActiveGreen, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "القناة المحددة: ${selectedChannel.title}",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = selectedChannel.streamUrl,
                            color = ActiveGreen,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إغلاق",
                        color = ActiveGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ActiveGreen.copy(alpha = 0.15f))
                            .clickable { onSelectChannel(selectedChannel) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        items(rows) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { channel ->
                    val isSelected = selectedChannel?.id == channel.id
                    ChannelGridCard(
                        channel = channel,
                        isSelected = isSelected,
                        onClick = { onSelectChannel(channel) },
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
private fun ChannelGridCard(
    channel: TvChannelItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(TileBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) ActiveGreen else BorderGreen,
                shape = RoundedCornerShape(22.dp)
            )
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(channel.badgeBg)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.badgeText,
                    color = channel.badgeTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (channel.badgeText.length <= 2) 26.sp else 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = channel.title,
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
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
