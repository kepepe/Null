package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ScheduleRepository
import com.example.domain.ObserveCurrentClassUseCase
import com.example.model.CurrentClassStatus
import com.example.model.WeekParityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ScheduleWidgetProvider : AppWidgetProvider() {

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ScheduleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidgetsAsync(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        providerScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allEntities = db.classDao().getAllClassesSync()
                val allClasses = allEntities.map { it.toDomain() }

                val prefs = context.getSharedPreferences("studysync_prefs", Context.MODE_PRIVATE)
                val parityModeStr = prefs.getString("user_parity_mode", WeekParityMode.AUTO.name)
                val parityMode = runCatching { WeekParityMode.valueOf(parityModeStr!!) }.getOrDefault(WeekParityMode.AUTO)

                val today = LocalDate.now()
                val nowTime = LocalTime.now()

                val repo = ScheduleRepository(db.classDao(), db.srsTaskDao(), context)
                val useCase = ObserveCurrentClassUseCase(repo)
                val status = useCase.evaluateCurrentStatus(allClasses, parityMode, today, nowTime)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_schedule)
                    bindWidgetViews(context, views, today, status)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                // Fail gracefully without crashing
            }
        }
    }

    private fun bindWidgetViews(
        context: Context,
        views: RemoteViews,
        today: LocalDate,
        status: CurrentClassStatus
    ) {
        val russianLocale = Locale("ru", "RU")
        val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, russianLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
        val dateFormatted = today.format(DateTimeFormatter.ofPattern("d MMMM", russianLocale))
        views.setTextViewText(R.id.widget_date_text, "$dayName, $dateFormatted")

        when (status) {
            is CurrentClassStatus.ActiveClass -> {
                val slot = status.currentSlot
                views.setTextViewText(R.id.widget_status_badge, "🟢 ИДЁТ ПАРА")
                views.setTextViewText(R.id.widget_subject_title, slot.subjectTitle)
                val details = buildString {
                    append(slot.formattedTimeSpan)
                    if (slot.classroom.isNotBlank()) append(" • 📍 ${slot.classroom}")
                    if (slot.professor.isNotBlank()) append(" • 👤 ${slot.professor}")
                }
                views.setTextViewText(R.id.widget_details_text, details)
                val remainingMin = status.remainingMinutes
                views.setTextViewText(
                    R.id.widget_info_text,
                    if (remainingMin > 0) "Осталось: $remainingMin мин" else "Пара вот-вот завершится"
                )
            }
            is CurrentClassStatus.FreePeriod -> {
                val nextSlot = status.nextSlot
                val untilMinutes = status.startsInMinutes
                if (status.isBeforeFirstClass) {
                    views.setTextViewText(R.id.widget_status_badge, "🎉 ПАРЫ ЕЩЁ НЕ НАЧАЛИСЬ")
                    views.setTextViewText(R.id.widget_subject_title, "Пары закончились (или ещё не начались 😉)")
                    val details = buildString {
                        append("1-я пара: ")
                        append(nextSlot.subjectTitle)
                        append(" • ")
                        append(nextSlot.formattedTimeSpan)
                        if (nextSlot.classroom.isNotBlank()) append(" • 📍 ${nextSlot.classroom}")
                    }
                    views.setTextViewText(R.id.widget_details_text, details)
                    views.setTextViewText(R.id.widget_info_text, "☕ Отдыхай, первая пара только в ${nextSlot.startTime}!")
                } else {
                    views.setTextViewText(R.id.widget_status_badge, "☕ ПЕРЕРЫВ • ДАЛЬШЕ ЧЕРЕЗ $untilMinutes МИН")
                    views.setTextViewText(R.id.widget_subject_title, nextSlot.subjectTitle)
                    val details = buildString {
                        append(nextSlot.formattedTimeSpan)
                        if (nextSlot.classroom.isNotBlank()) append(" • 📍 ${nextSlot.classroom}")
                        if (nextSlot.professor.isNotBlank()) append(" • 👤 ${nextSlot.professor}")
                    }
                    views.setTextViewText(R.id.widget_details_text, details)
                    views.setTextViewText(R.id.widget_info_text, "⏳ Начало пары в ${nextSlot.startTime}")
                }
            }
            is CurrentClassStatus.DoneForToday -> {
                views.setTextViewText(R.id.widget_status_badge, "🎉 ВСЕ ПАРЫ ЗАВЕРШЕНЫ")
                views.setTextViewText(R.id.widget_subject_title, "На сегодня всё!")
                views.setTextViewText(R.id.widget_details_text, "Отличная работа! Отдыхайте и готовьтесь к СРС")
                views.setTextViewText(R.id.widget_info_text, "Завтра новый учебный день")
            }
            is CurrentClassStatus.NoClassesToday -> {
                views.setTextViewText(R.id.widget_status_badge, "✨ ВЫХОДНОЙ")
                views.setTextViewText(R.id.widget_subject_title, "Сегодня пар нет")
                views.setTextViewText(R.id.widget_details_text, "Свободный день для отдыха и самостоятельной работы")
                views.setTextViewText(R.id.widget_info_text, "Нажмите для перехода в приложение")
            }
        }

        // Tap to open App
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Refresh Button Tap
        val refreshIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

        fun updateAllWidgets(context: Context) {
            try {
                val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
