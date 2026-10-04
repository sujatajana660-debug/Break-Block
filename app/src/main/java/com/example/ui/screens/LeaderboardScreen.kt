package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LeaderboardScoreEntity
import com.example.ui.components.GuestProfileDialog
import com.example.ui.components.UnityAdsBanner
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenType
import java.util.Calendar

@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val weeklyScores by viewModel.weeklyScores.collectAsStateWithLifecycle()
    val monthlyScores by viewModel.monthlyScores.collectAsStateWithLifecycle()
    val allTimeScores by viewModel.allTimeScores.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showGuestProfile by remember { mutableStateOf(false) }

    val tabTitles = listOf("WEEKLY LIVE", "MONTHLY", "ALL-TIME")
    val activeList = when (selectedTab) {
        0 -> weeklyScores
        1 -> monthlyScores
        else -> allTimeScores
    }

    // Calculate days remaining in week / month
    val cal = Calendar.getInstance()
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysLeftInWeek = (8 - dayOfWeek) % 7
    val maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDayInMonth = cal.get(Calendar.DAY_OF_MONTH)
    val daysLeftInMonth = maxDayInMonth - currentDayInMonth

    val resetText = when (selectedTab) {
        0 -> "Resets in ${maxOf(1, daysLeftInWeek)} days • Real-Time Standings"
        1 -> "Resets in ${maxOf(1, daysLeftInMonth)} days • Monthly Legends"
        else -> "All-Time Global Hall of Fame"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    BackHandler {
        viewModel.navigateTo(ScreenType.MODE_SELECT)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B0F19),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenType.MODE_SELECT) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2638))
                        .testTag("leaderboard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE PLAYERS",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = resetText,
                        color = Color(0xFFFFB74D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Live Online Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF064E3B).copy(alpha = 0.6f))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "1,429 LIVE",
                    color = Color(0xFF6EE7B7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF151D2F),
            contentColor = Color(0xFFFFD700),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFFFFD700),
                    height = 3.dp
                )
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (selectedTab == index) Color(0xFFFFD700) else Color.Gray
                        )
                    },
                    modifier = Modifier.testTag("tab_$title")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Leaderboard List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .testTag("leaderboard_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            itemsIndexed(activeList) { index, item ->
                LeaderboardRow(rank = index + 1, entry = item)
            }
        }

        // User Standing Footer with Guest ID & Edit Button
        val userRank = activeList.indexOfFirst { it.isUser }.let { if (it >= 0) it + 1 else null }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                .clickable { showGuestProfile = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (userRank != null) "#$userRank" else "#--",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = stats.avatarEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stats.playerName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "Guest: ${stats.guestId} • Tap to edit name",
                            color = Color(0xFFBFDBFE),
                            fontSize = 10.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.classicHighScore}",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Your Best",
                        color = Color(0xFF93C5FD),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Unity Ads Banner at bottom elevated
        Spacer(modifier = Modifier.height(2.dp))
        UnityAdsBanner(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }

    if (showGuestProfile) {
        GuestProfileDialog(
            currentName = stats.playerName,
            currentGuestId = stats.guestId,
            currentAvatar = stats.avatarEmoji,
            onSaveProfile = { newName, newAvatar ->
                viewModel.updateGuestProfile(newName, newAvatar)
            },
            onRegenerateGuestId = {
                viewModel.regenerateGuestId()
            },
            onDismiss = { showGuestProfile = false }
        )
    }
}

@Composable
fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardScoreEntity
) {
    val isTop3 = rank in 1..3
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color.Gray
    }
    val trophy = when (rank) {
        1 -> "👑 "
        2 -> "🥈 "
        3 -> "🥉 "
        else -> ""
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isUser) Color(0xFF1E293B) else Color(0xFF13192B)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (entry.isUser) Modifier.border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                else Modifier
            )
            .testTag("leaderboard_row_$rank")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank Number
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTop3) rankBadgeColor.copy(alpha = 0.2f) else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        color = rankBadgeColor,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isTop3) 15.sp else 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Country Flag
                Text(text = entry.countryFlag, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                // Name & Rank Details
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$trophy${entry.playerName}",
                            color = if (entry.isUser) Color(0xFF60A5FA) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (entry.isUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2563EB))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(text = "YOU", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${entry.rankTitle} • ${entry.guestIdTag}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Score
            Text(
                text = "${entry.score}",
                color = if (isTop3) Color(0xFFFFD700) else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
    }
}
