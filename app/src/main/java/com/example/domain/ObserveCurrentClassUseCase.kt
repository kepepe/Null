package com.example.domain

import com.example.data.ScheduleRepository
import com.example.model.ClassSlot
import com.example.model.CurrentClassStatus
import com.example.model.WeekParity
import com.example.model.WeekParityMode
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

class ObserveCurrentClassUseCase(
    private val repository: ScheduleRepository
) {
    /**
     * Ticks every 10 seconds to update live countdowns and detect class transitions.
     */
    private fun tickerFlow(periodMillis: Long = 10_000L): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(periodMillis)
        }
    }

    operator fun invoke(): Flow<CurrentClassStatus> {
        return combine(
            repository.observeAllClasses(),
            repository.userProfileFlow,
            tickerFlow()
        ) { classes, profile, _ ->
            evaluateCurrentStatus(classes, profile.parityMode, LocalDate.now(), LocalTime.now())
        }
    }

    fun evaluateCurrentStatus(
        classes: List<ClassSlot>,
        parityMode: WeekParityMode,
        currentDate: LocalDate,
        currentTime: LocalTime
    ): CurrentClassStatus {
        val currentDay = currentDate.dayOfWeek
        val weekNumber = currentDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val currentParity = when (parityMode) {
            WeekParityMode.ODD -> WeekParity.ODD
            WeekParityMode.EVEN -> WeekParity.EVEN
            WeekParityMode.AUTO -> if (weekNumber % 2 == 0) WeekParity.EVEN else WeekParity.ODD
        }

        val todayClasses = classes
            .filter { it.dayOfWeek == currentDay }
            .filter { it.weekParity == WeekParity.ALL || it.weekParity == currentParity }
            .sortedBy { it.startTime }

        if (todayClasses.isEmpty()) {
            return CurrentClassStatus.NoClassesToday
        }

        // 1. Is there an active ongoing class right now?
        val activeIndex = todayClasses.indexOfFirst { slot ->
            !currentTime.isBefore(slot.startTime) && currentTime.isBefore(slot.endTime)
        }

        if (activeIndex != -1) {
            val ongoing = todayClasses[activeIndex]
            val next = todayClasses.getOrNull(activeIndex + 1)
            val remainingMinutes = ChronoUnit.MINUTES.between(currentTime, ongoing.endTime).coerceAtLeast(1)
            return CurrentClassStatus.ActiveClass(
                currentSlot = ongoing,
                remainingMinutes = remainingMinutes,
                nextSlot = next
            )
        }

        // 2. Is there an upcoming class later today?
        val upcomingClass = todayClasses.firstOrNull { slot -> currentTime.isBefore(slot.startTime) }
        if (upcomingClass != null) {
            val startsInMinutes = ChronoUnit.MINUTES.between(currentTime, upcomingClass.startTime).coerceAtLeast(1)
            return CurrentClassStatus.FreePeriod(
                nextSlot = upcomingClass,
                startsInMinutes = startsInMinutes
            )
        }

        // 3. Finished for the day
        return CurrentClassStatus.DoneForToday
    }

    companion object {
        fun getCurrentWeekParityText(
            date: LocalDate = LocalDate.now(),
            parityMode: WeekParityMode = WeekParityMode.AUTO
        ): String {
            return when (parityMode) {
                WeekParityMode.ODD -> "Нечётная неделя (I)"
                WeekParityMode.EVEN -> "Чётная неделя (II)"
                WeekParityMode.AUTO -> {
                    val weekNumber = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                    val parity = if (weekNumber % 2 == 0) "Чётная" else "Нечётная"
                    "Неделя $weekNumber ($parity)"
                }
            }
        }
    }
}
