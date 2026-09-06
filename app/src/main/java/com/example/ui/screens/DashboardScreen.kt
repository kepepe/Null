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
import com.example.ui.components.BentoLiveClassCard
import com.example.ui.components.BentoNextUpCard
import com.example.ui.components.BentoSkipsSummaryCard
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    currentStatus: CurrentClassStatus,
    windows: List<ScheduleWindow>,
    allClasses: List<ClassSlot>,
    userProfile: UserProfile,
    onOpenSchedule: () -> Unit,
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

        // Bento Header
        val greetingText = if (userProfile.name.isNotBlank()) {
            val firstName = userProfile.name.trim().split(" ").firstOrNull() ?: userProfile.name
            "Привет, $firstName"
        } else {
            "Моё Расписание"
        }

        val (statusText, statusFg, statusBg) = when (currentStatus) {
            is CurrentClassStatus.ActiveClass -> Triple(
                "На паре (${currentStatus.currentSlot.formattedTimeSpan})",
                BentoSuccessGreen,
                BentoGreenContainer
            )
            is CurrentClassStatus.FreePeriod -> if (currentStatus.isBeforeFirstClass) {
                Triple(
                    "Отдых перед парами ☕",
                    BentoSuccessGreen,
                    BentoGreenContainer
                )
            } else {
                Triple(
                    "Перемена",
                    BentoCoral,
                    BentoCoralContainer
                )
            }
            is CurrentClassStatus.DoneForToday -> Triple(
                "Закончил учиться 🎉",
                BentoPrimary,
                BentoPrimaryContainer
            )
            is CurrentClassStatus.NoClassesToday -> Triple(
                "Пар нет ☕",
                BentoOnSurfaceVariant,
                BentoSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: App Title / Greeting and User Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = BentoPrimary,
                            letterSpacing = (-0.5).sp
                        ),
                        modifier = Modifier.testTag("app_header_title"),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dayFormatted,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = BentoOnSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // User Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (userProfile.isRegistered) BentoPrimary else BentoSurfaceVariant)
                        .border(2.dp, BentoBorderLight, CircleShape)
                        .shadow(elevation = 2.dp, shape = CircleShape)
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Row 2: Status & Parity Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Parity Mode Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BentoPrimaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleParityMode)
                        .testTag("toggle_parity_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = parityText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BentoOnPrimaryContainer
                            ),
                            maxLines = 1
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

                // Live Class Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(statusFg.copy(alpha = 0.4f)),
                        width = 1.dp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenProfile)
                        .testTag("profile_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusFg)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = statusFg
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Hero Card: Current/Live Class Status (with progress bar and timer to end of class)
        BentoLiveClassCard(
            status = currentStatus,
            onClick = onOpenSchedule
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Next Up Card
        BentoNextUpCard(
            nextSlot = nextSlot,
            onClick = onOpenSchedule,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Skip Counter Summary Card
        BentoSkipsSummaryCard(
            allClasses = allClasses,
            onClick = onOpenSchedule
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Add Class Quick Action
        BentoActionCard(
            onAddClassClick = onAddNewClass
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}
