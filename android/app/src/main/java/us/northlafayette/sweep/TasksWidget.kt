package us.northlafayette.sweep

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors

class TasksWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_ITEM = "us.northlafayette.sweep.TASK_ITEM"
        const val ACTION_REFRESH = "us.northlafayette.sweep.TASKS_REFRESH"
        const val ACTION_MIDNIGHT = "us.northlafayette.sweep.TASKS_MIDNIGHT"
        const val EXTRA_PAGE_ID = "pageId"
        const val EXTRA_TASK_NAME = "taskName"

        private val executor = Executors.newSingleThreadExecutor()

        fun pushUpdate(context: Context, awm: AppWidgetManager, ids: IntArray) {
            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_tasks)

                val adapterIntent = Intent(context, TasksWidgetService::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                adapterIntent.data = Uri.parse(adapterIntent.toUri(Intent.URI_INTENT_SCHEME))
                views.setRemoteAdapter(R.id.task_list, adapterIntent)
                views.setEmptyView(R.id.task_list, R.id.empty_view)

                // Item taps (check-off) arrive as broadcasts filled in by the factory.
                val template = Intent(context, TasksWidget::class.java).setAction(ACTION_ITEM)
                views.setPendingIntentTemplate(
                    R.id.task_list,
                    PendingIntent.getBroadcast(
                        context, 0, template,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                )

                views.setOnClickPendingIntent(
                    R.id.btn_refresh,
                    PendingIntent.getBroadcast(
                        context, 1,
                        Intent(context, TasksWidget::class.java).setAction(ACTION_REFRESH),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                views.setOnClickPendingIntent(
                    R.id.btn_add,
                    PendingIntent.getActivity(
                        context, 2,
                        Intent(context, CaptureActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                val allTasks = PendingIntent.getActivity(
                    context, 3,
                    Intent(context, AllTasksActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_title, allTasks)
                views.setOnClickPendingIntent(R.id.all_link, allTasks)

                awm.updateAppWidget(id, views)
            }
        }

        private fun refreshData(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            val ids = awm.getAppWidgetIds(ComponentName(context, TasksWidget::class.java))
            if (ids.isNotEmpty()) awm.notifyAppWidgetViewDataChanged(ids, R.id.task_list)
        }

        private fun midnightIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 4,
                Intent(context, TasksWidget::class.java).setAction(ACTION_MIDNIGHT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        /**
         * "Due today" depends on the local calendar day, and Android's
         * updatePeriodMillis timer is best-effort (skipped in Doze, throttled
         * by launchers), so the day boundary gets its own alarm. Inexact +
         * allow-while-idle needs no special permission, and the PendingIntent
         * is reused, so calling this repeatedly just re-arms the same alarm.
         */
        fun scheduleMidnightRefresh(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val next = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .plusMinutes(1)
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                midnightIntent(context)
            )
        }

        private fun cancelMidnightRefresh(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(midnightIntent(context))
        }
    }

    override fun onUpdate(context: Context, awm: AppWidgetManager, ids: IntArray) {
        pushUpdate(context, awm, ids)
        awm.notifyAppWidgetViewDataChanged(ids, R.id.task_list)
        // Alarms don't survive a reboot; onUpdate does run after one.
        scheduleMidnightRefresh(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMidnightRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMidnightRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refreshData(context)
            ACTION_MIDNIGHT,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                refreshData(context)
                scheduleMidnightRefresh(context)
            }
            ACTION_ITEM -> {
                val pageId = intent.getStringExtra(EXTRA_PAGE_ID) ?: return
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""
                val token = Prefs.token(context)
                val doneProp = Prefs.doneProp(context)
                val result = goAsync()
                executor.execute {
                    val handler = Handler(Looper.getMainLooper())
                    try {
                        NotionApi.completeTask(token, pageId, doneProp)
                        handler.post {
                            Toast.makeText(
                                context,
                                context.getString(R.string.task_done, taskName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        refreshData(context)
                    } catch (e: Exception) {
                        handler.post {
                            Toast.makeText(
                                context,
                                context.getString(R.string.task_done_failed, e.message ?: ""),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        result.finish()
                    }
                }
            }
        }
    }
}
