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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SportChannel(
    val id: String,
    val name: String,
    val leagueOrTag: String,
    val quality: String,
    val streamUrl: String
)

private val DarkBg = Color(0xFF090D16)
private val CardBg = Color(0xFF121A2B)
private val CardSelectedBg = Color(0xFF162738)
private val AccentGreen = Color(0xFF00E676)
private val LiveRed = Color(0xFFFF3D57)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextSecondary = Color(0xFF9AA6BD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sampleChannels = listOf(
            SportChannel(
                id = "1",
                name = "Ali Sport HD 1",
                leagueOrTag = "بث مباشر • مباريات اليوم",
                quality = "1080p",
                streamUrl = "https://example.com/stream1.m3u8"
            ),
            SportChannel(
                id = "2",
                name = "Ali Sport Arena 2",
                leagueOrTag = "بطولات قارية ودولية",
                quality = "720p",
                streamUrl = "https://example.com/stream2.m3u8"
            ),
            SportChannel(
                id = "3",
                name = "Ali Sport Extra 3",
                leagueOrTag = "تحليل ومواقف ما بعد المباراة",
                quality = "HD",
                streamUrl = "https://example.com/stream3.m3u8"
            )
        )

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AccentGreen,
                    background = DarkBg,
                    surface = CardBg,
                    onPrimary = Color.Black,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                AliSportDarkHomeScreen(channels = sampleChannels)
            }
        }
    }
}

@Composable
fun AliSportDarkHomeScreen(channels: List<SportChannel>) {
    var selectedChannel by remember { mutableStateOf(channels.firstOrNull()) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF111B2E), DarkBg)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ALI SPORT",
                        color = AccentGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "الواجهة الرياضية الداكنة • جاهز للبث",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(LiveRed.copy(alpha = 0.16f))
                        .border(1.dp, LiveRed.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LiveRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // بطاقة معاينة المشغّل في الأعلى
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF142238), Color(0xFF0D1320))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = AccentGreen.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedChannel?.quality ?: "HD",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGreen.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "مشغّل البث المعاين",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Column {
                        Text(
                            text = selectedChannel?.name ?: "اختر قناة للبدء",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedChannel?.leagueOrTag ?: "",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "الرابط: ${selectedChannel?.streamUrl ?: ""}",
                            color = AccentGreen.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "القنوات المتاحة",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    val isSelected = selectedChannel?.id == channel.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) CardSelectedBg else CardBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AccentGreen else Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedChannel = channel }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = channel.leagueOrTag,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = if (isSelected) "محدد" else channel.quality,
                            color = if (isSelected) Color.Black else AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) AccentGreen else AccentGreen.copy(alpha = 0.14f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
