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

    private var tasks: List<NotionApi.Task> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Runs on a binder thread, so a synchronous fetch is fine here.
        tasks = if (Prefs.tasksReady(context)) {
            try {
                NotionApi.openTasks(
                    Prefs.token(context),
                    Prefs.tasksDbId(context),
                    Prefs.doneProp(context),
                    Prefs.dueProp(context),
                    dueOnOrBefore = LocalDate.now().toString()
                )
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = tasks[position]
        val row = RemoteViews(context.packageName, R.layout.widget_task_item)
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
