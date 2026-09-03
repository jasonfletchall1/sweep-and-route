package us.northlafayette.sweep

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class SettingsActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var tokenInput: EditText
    private lateinit var connectBtn: Button
    private lateinit var status: TextView
    private lateinit var inboxSpinner: Spinner
    private lateinit var tasksSpinner: Spinner
    private lateinit var doneSpinner: Spinner
    private lateinit var dueSpinner: Spinner
    private lateinit var saveBtn: Button

    private var databases: List<NotionApi.Database> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tokenInput = findViewById(R.id.token_input)
        connectBtn = findViewById(R.id.connect_btn)
        status = findViewById(R.id.settings_status)
        inboxSpinner = findViewById(R.id.inbox_spinner)
        tasksSpinner = findViewById(R.id.tasks_spinner)
        doneSpinner = findViewById(R.id.done_spinner)
        dueSpinner = findViewById(R.id.due_spinner)
        saveBtn = findViewById(R.id.save_btn)

        tokenInput.setText(Prefs.token(this))
        setPickersEnabled(false)

        connectBtn.setOnClickListener { loadDatabases() }
        saveBtn.setOnClickListener { save() }

        tasksSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                populatePropertyPickers()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        if (Prefs.token(this).isNotBlank()) loadDatabases()
    }

    private fun setPickersEnabled(enabled: Boolean) {
        inboxSpinner.isEnabled = enabled
        tasksSpinner.isEnabled = enabled
        doneSpinner.isEnabled = enabled
        dueSpinner.isEnabled = enabled
        saveBtn.isEnabled = enabled
    }

    private fun loadDatabases() {
        val token = tokenInput.text.toString().trim()
        if (token.isEmpty()) {
            status.setText(R.string.token_missing)
            return
        }
        connectBtn.isEnabled = false
        status.setText(R.string.connecting)
        executor.execute {
            try {
                val dbs = NotionApi.listDatabases(token)
                runOnUiThread {
                    connectBtn.isEnabled = true
                    if (dbs.isEmpty()) {
                        status.setText(R.string.no_databases)
                        return@runOnUiThread
                    }
                    databases = dbs
                    status.text = getString(R.string.found_databases, dbs.size)
                    val names = dbs.map { it.title }
                    val adapter = ArrayAdapter(
                        this, android.R.layout.simple_spinner_item, names
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    inboxSpinner.adapter = adapter
                    tasksSpinner.adapter = ArrayAdapter(
                        this, android.R.layout.simple_spinner_item, names
                    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

                    preselect(inboxSpinner, Prefs.inboxDbId(this), "inbox")
                    preselect(tasksSpinner, Prefs.tasksDbId(this), "task")
                    setPickersEnabled(true)
                    populatePropertyPickers()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    connectBtn.isEnabled = true
                    status.text = getString(R.string.connect_failed, e.message ?: "")
                }
            }
        }
    }

    private fun preselect(spinner: Spinner, savedId: String, nameHint: String) {
        var index = databases.indexOfFirst { it.id == savedId }
        if (index < 0) index = databases.indexOfFirst { it.title.contains(nameHint, ignoreCase = true) }
        if (index >= 0) spinner.setSelection(index)
    }

    private fun populatePropertyPickers() {
        val tasksDb = databases.getOrNull(tasksSpinner.selectedItemPosition) ?: return
        val checkboxes = NotionApi.propertiesOfType(tasksDb.properties, "checkbox")
        val dates = NotionApi.propertiesOfType(tasksDb.properties, "date")

        doneSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, checkboxes
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        dueSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, dates
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val doneIdx = checkboxes.indexOfFirst { it == Prefs.doneProp(this) }
            .takeIf { it >= 0 } ?: checkboxes.indexOfFirst { it.equals("Done", true) }
        if (doneIdx >= 0) doneSpinner.setSelection(doneIdx)
        val dueIdx = dates.indexOfFirst { it == Prefs.dueProp(this) }
            .takeIf { it >= 0 } ?: dates.indexOfFirst { it.equals("Due", true) }
        if (dueIdx >= 0) dueSpinner.setSelection(dueIdx)
    }

    private fun save() {
        val inboxDb = databases.getOrNull(inboxSpinner.selectedItemPosition)
        val tasksDb = databases.getOrNull(tasksSpinner.selectedItemPosition)
        val doneProp = doneSpinner.selectedItem as? String
        val dueProp = dueSpinner.selectedItem as? String
        if (inboxDb == null || tasksDb == null || doneProp == null || dueProp == null) {
            status.setText(R.string.selection_incomplete)
            return
        }
        Prefs.save(
            this,
            token = tokenInput.text.toString().trim(),
            inboxDbId = inboxDb.id,
            inboxTitleProp = NotionApi.titleProperty(inboxDb.properties),
            tasksDbId = tasksDb.id,
            doneProp = doneProp,
            dueProp = dueProp
        )
        // Nudge any placed widgets to re-render with the new config.
        val awm = AppWidgetManager.getInstance(this)
        val taskIds = awm.getAppWidgetIds(ComponentName(this, TasksWidget::class.java))
        if (taskIds.isNotEmpty()) {
            TasksWidget.pushUpdate(this, awm, taskIds)
            awm.notifyAppWidgetViewDataChanged(taskIds, R.id.task_list)
        }
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
