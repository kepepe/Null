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
     * Ticks every second to update live countdowns, seconds, and detect class transitions.
     */
    private fun tickerFlow(periodMillis: Long = 1_000L): Flow<Unit> = flow {
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

    fun observeTodayWindows(): Flow<List<com.example.model.ScheduleWindow>> {
        return combine(
            repository.observeAllClasses(),
            repository.userProfileFlow
        ) { classes, profile ->
            val currentDay = LocalDate.now().dayOfWeek
            val weekNumber = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val currentParity = when (profile.parityMode) {
                WeekParityMode.ODD -> WeekParity.ODD
                WeekParityMode.EVEN -> WeekParity.EVEN
                WeekParityMode.AUTO -> if (weekNumber % 2 == 0) WeekParity.EVEN else WeekParity.ODD
            }

            val todayClasses = classes
                .filter { it.dayOfWeek == currentDay }
                .filter { it.weekParity == WeekParity.ALL || it.weekParity == currentParity }
                .sortedBy { it.startTime }

            val windows = mutableListOf<com.example.model.ScheduleWindow>()
            for (i in 0 until todayClasses.size - 1) {
                val curr = todayClasses[i]
                val next = todayClasses[i + 1]
                val gapMinutes = java.time.Duration.between(curr.endTime, next.startTime).toMinutes()
                if (gapMinutes >= 15) {
                    windows.add(
                        com.example.model.ScheduleWindow(
                            previousSlot = curr,
                            nextSlot = next,
                            startTime = curr.endTime,
                            endTime = next.startTime,
                            durationMinutes = gapMinutes
                        )
                    )
                }
            }
            windows
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
            val totalSeconds = ChronoUnit.SECONDS.between(ongoing.startTime, ongoing.endTime).coerceAtLeast(1)
            val elapsedSeconds = ChronoUnit.SECONDS.between(ongoing.startTime, currentTime).coerceAtLeast(0)
            val remainingSeconds = ChronoUnit.SECONDS.between(currentTime, ongoing.endTime).coerceAtLeast(0)

            val totalMinutes = totalSeconds / 60
            val elapsedMinutes = elapsedSeconds / 60
            val remainingMinutes = (remainingSeconds + 59) / 60 // Round up to nearest whole minute for human reading
            val progressPercent = ((elapsedSeconds.toDouble() / totalSeconds.toDouble()) * 100).toInt().coerceIn(0, 100)

            return CurrentClassStatus.ActiveClass(
                currentSlot = ongoing,
                remainingMinutes = remainingMinutes,
                remainingSeconds = remainingSeconds,
                elapsedMinutes = elapsedMinutes,
                totalDurationMinutes = totalMinutes,
                progressPercent = progressPercent,
                nextSlot = next
            )
        }

        // 2. Is there an upcoming class later today?
        val upcomingClass = todayClasses.firstOrNull { slot -> currentTime.isBefore(slot.startTime) }
        if (upcomingClass != null) {
            val isBeforeFirst = (upcomingClass == todayClasses.first())
            val startsInSeconds = ChronoUnit.SECONDS.between(currentTime, upcomingClass.startTime).coerceAtLeast(0)
            val startsInMinutes = (startsInSeconds + 59) / 60
            return CurrentClassStatus.FreePeriod(
                nextSlot = upcomingClass,
                startsInMinutes = startsInMinutes,
                startsInSeconds = startsInSeconds,
                isBeforeFirstClass = isBeforeFirst
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
