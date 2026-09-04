package us.northlafayette.sweep

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TasksFactory(applicationContext)
}

private class TasksFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val lock = Any()
    private var tasks: List<NotionApi.Task> = emptyList()

    /**
     * The local calendar day the current [tasks] list was fetched for. The
     * launcher keeps this factory alive across days and may rebind to it
     * without calling [onDataSetChanged] (e.g. after the periodic
     * APPWIDGET_UPDATE re-supplies the layout), so anything served from
     * [tasks] must be checked against today's date first — otherwise the
     * widget keeps showing the list from the day it was last refreshed.
     */
    private var loadedFor: LocalDate? = null

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Runs on a binder thread, so a synchronous fetch is fine here.
        load(force = true)
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        // Self-heal: if the day rolled over since the last fetch, re-query
        // before reporting anything, even though nobody asked for a refresh.
        load(force = false)
        return synchronized(lock) { tasks.size }
    }

    private fun load(force: Boolean) = synchronized(lock) {
        val today = LocalDate.now()
        if (!force && loadedFor == today) return
        if (!Prefs.tasksReady(context)) {
            tasks = emptyList()
            loadedFor = today
            return
        }
        try {
            tasks = NotionApi.openTasks(
                Prefs.token(context),
                Prefs.tasksDbId(context),
                Prefs.doneProp(context),
                Prefs.dueProp(context),
                dueOnOrBefore = today.toString()
            )
        } catch (e: Exception) {
            // A transient failure keeps the list fetched earlier today; once
            // the day has changed, stale rows are worse than an empty list.
            if (loadedFor != today) tasks = emptyList()
        }
        loadedFor = today
    }

    override fun getViewAt(position: Int): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_task_item)
        val task = synchronized(lock) { tasks.getOrNull(position) } ?: return row
        row.setTextViewText(R.id.task_name, task.name)

        val (label, overdue) = dueLabel(task.due)
        row.setTextViewText(R.id.task_due, label)
        row.setTextColor(
            R.id.task_due,
            context.getColor(if (overdue) R.color.overdue else R.color.subtext)
        )

        val fillIn = Intent()
            .putExtra(TasksWidget.EXTRA_PAGE_ID, task.id)
            .putExtra(TasksWidget.EXTRA_TASK_NAME, task.name)
        row.setOnClickFillInIntent(R.id.task_check, fillIn)
        return row
    }

    private fun dueLabel(due: String?): Pair<String, Boolean> {
        if (due == null) return "" to false
        return try {
            val datePart = LocalDate.parse(due.take(10))
            val today = LocalDate.now()
            when {
                datePart.isBefore(today) -> {
                    val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    context.getString(R.string.overdue_label, datePart.format(fmt)) to true
                }
                due.length > 10 -> {
                    val time = LocalDateTime.parse(due.take(16), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())) to false
                }
                else -> context.getString(R.string.today_label) to false
            }
        } catch (e: Exception) {
            due to false
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
