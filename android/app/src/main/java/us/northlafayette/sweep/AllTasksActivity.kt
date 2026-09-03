package us.northlafayette.sweep

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

class AllTasksActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var list: ListView
    private lateinit var status: TextView
    private val adapter = TaskAdapter()
    private var tasks: MutableList<NotionApi.Task> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_tasks)
        setTitle(R.string.all_tasks_title)
        list = findViewById(R.id.all_tasks_list)
        status = findViewById(R.id.all_tasks_status)
        list.adapter = adapter

        if (!Prefs.tasksReady(this)) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        status.visibility = View.VISIBLE
        status.setText(R.string.loading)
        executor.execute {
            try {
                val result = NotionApi.openTasks(
                    Prefs.token(this),
                    Prefs.tasksDbId(this),
                    Prefs.doneProp(this),
                    Prefs.dueProp(this),
                    dueOnOrBefore = null
                )
                runOnUiThread {
                    tasks = result.toMutableList()
                    status.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                    if (tasks.isEmpty()) status.setText(R.string.no_open_tasks)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = getString(R.string.load_failed, e.message ?: "")
                }
            }
        }
    }

    private fun complete(task: NotionApi.Task) {
        executor.execute {
            try {
                NotionApi.completeTask(Prefs.token(this), task.id, Prefs.doneProp(this))
                runOnUiThread {
                    tasks.remove(task)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, getString(R.string.task_done, task.name), Toast.LENGTH_SHORT).show()
                }
                val awm = AppWidgetManager.getInstance(this)
                val ids = awm.getAppWidgetIds(ComponentName(this, TasksWidget::class.java))
                if (ids.isNotEmpty()) awm.notifyAppWidgetViewDataChanged(ids, R.id.task_list)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.task_done_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    inner class TaskAdapter : BaseAdapter() {
        override fun getCount(): Int = tasks.size
        override fun getItem(position: Int): Any = tasks[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.row_task, parent, false)
            val task = tasks[position]
            val check = view.findViewById<CheckBox>(R.id.row_check)
            val name = view.findViewById<TextView>(R.id.row_name)
            val due = view.findViewById<TextView>(R.id.row_due)

            name.text = task.name
            due.text = formatDue(task.due)
            due.setTextColor(
                getColor(if (isOverdue(task.due)) R.color.overdue else R.color.subtext)
            )

            check.setOnCheckedChangeListener(null)
            check.isChecked = false
            check.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) complete(task)
            }
            name.setOnClickListener {
                task.url?.let {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    } catch (e: Exception) {
                        // No handler for notion links — ignore.
                    }
                }
            }
            return view
        }
    }

    private fun isOverdue(due: String?): Boolean = try {
        due != null && LocalDate.parse(due.take(10)).isBefore(LocalDate.now())
    } catch (e: Exception) {
        false
    }

    private fun formatDue(due: String?): String {
        if (due == null) return getString(R.string.no_due)
        return try {
            val date = LocalDate.parse(due.take(10))
            val fmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
            when {
                date == LocalDate.now() -> getString(R.string.today_label)
                else -> date.format(fmt)
            }
        } catch (e: Exception) {
            due
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
