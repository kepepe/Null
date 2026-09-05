package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.ObserveCurrentClassUseCase
import com.example.model.*
import com.example.ui.components.BentoActionCard
import com.example.ui.components.BentoFriendsCard
import com.example.ui.components.BentoLiveClassCard
import com.example.ui.components.BentoNextUpCard
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    currentStatus: CurrentClassStatus,
    friends: List<FriendUser>,
    userProfile: UserProfile,
    allClassesCount: Int,
    onOpenSchedule: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddNewClass: () -> Unit,
    onToggleParityMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val russianLocale = Locale("ru", "RU")
    val dayFormatted = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", russianLocale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    val parityText = ObserveCurrentClassUseCase.getCurrentWeekParityText(today, userProfile.parityMode)

    val nextSlot: ClassSlot? = when (currentStatus) {
        is CurrentClassStatus.ActiveClass -> currentStatus.nextSlot
        is CurrentClassStatus.FreePeriod -> currentStatus.nextSlot
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Bento Header with updated name "null"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "null",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = BentoPrimary,
                        letterSpacing = (-0.5).sp
                    ),
                    modifier = Modifier.testTag("app_header_title")
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dayFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = BentoOnSurfaceVariant
                        )
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BentoOnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )

                    // Clickable Parity Mode Tag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BentoPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleParityMode)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = parityText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = BentoOnPrimaryContainer
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChangeCircle,
                                contentDescription = "Сменить чётность",
                                tint = BentoPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // User Profile Avatar Button (Enlarged)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (userProfile.isRegistered) BentoPrimary else BentoSurfaceVariant)
                    .border(2.dp, BentoBorderLight, CircleShape)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clickable(onClick = onOpenProfile)
                    .testTag("profile_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile.avatarUri != null) {
                    AsyncImage(
                        model = userProfile.avatarUri,
                        contentDescription = "Аватар профиля",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (userProfile.isRegistered && userProfile.initials.isNotBlank()) {
                    Text(
                        text = userProfile.initials,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoOnPrimary,
                            fontSize = 16.sp
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Профиль",
                        tint = BentoOnSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Hero Card: Current/Live Class Status (With rich large icons)
        BentoLiveClassCard(
            status = currentStatus,
            onClick = onOpenSchedule
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Bento 2-Column Split: Next Up & Friends
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BentoNextUpCard(
                nextSlot = nextSlot,
                onClick = onOpenSchedule,
                modifier = Modifier.weight(1f)
            )

            BentoFriendsCard(
                friends = friends,
                onClick = onOpenFriends,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Col-Span-2: Add Class Quick Action
        BentoActionCard(
            onAddClassClick = onAddNewClass
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}
