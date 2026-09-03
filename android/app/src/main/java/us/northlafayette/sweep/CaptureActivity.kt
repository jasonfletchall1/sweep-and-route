package us.northlafayette.sweep

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * Small dialog-style activity launched from the capture widget (or the share
 * sheet). Types straight into Notion's Inbox and closes.
 */
class CaptureActivity : Activity() {

    companion object {
        const val EXTRA_VOICE = "voice"
        private const val REQ_VOICE = 41
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var input: EditText
    private lateinit var saveBtn: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Prefs.captureReady(this)) {
            Toast.makeText(this, R.string.setup_first, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_capture)
        setTitle(R.string.capture_title)

        input = findViewById(R.id.capture_input)
        saveBtn = findViewById(R.id.capture_save)
        status = findViewById(R.id.capture_status)
        val micBtn = findViewById<ImageButton>(R.id.capture_mic)

        // Text shared from another app via the share sheet.
        if (intent?.action == Intent.ACTION_SEND) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!shared.isNullOrBlank()) input.setText(shared)
        }

        saveBtn.setOnClickListener { save() }
        micBtn.setOnClickListener { startVoice() }
        micBtn.setOnLongClickListener { chooseVoiceInput(thenRun = false); true }

        if (intent?.getBooleanExtra(EXTRA_VOICE, false) == true) startVoice()
    }

    private fun startVoice() {
        when (val mode = Prefs.voiceMode(this)) {
            null, "" -> chooseVoiceInput(thenRun = true)
            "ime" -> showKeyboardPicker()
            else -> launchRecognizer(mode)
        }
    }

    /** Speech-recognizer activities installed on this device, deduped by component. */
    private fun recognizerChoices(): List<Pair<String, String>> {
        val probe = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        return packageManager.queryIntentActivities(probe, 0).map {
            val appLabel = it.activityInfo.applicationInfo.loadLabel(packageManager).toString()
            val component =
                ComponentName(it.activityInfo.packageName, it.activityInfo.name).flattenToString()
            appLabel to component
        }.distinctBy { it.second }
    }

    private fun chooseVoiceInput(thenRun: Boolean) {
        val choices = recognizerChoices()
        val labels = choices.map { getString(R.string.voice_option_recognizer, it.first) } +
            getString(R.string.voice_option_ime)
        AlertDialog.Builder(this)
            .setTitle(R.string.voice_choose_title)
            .setItems(labels.toTypedArray()) { _, which ->
                val mode = if (which < choices.size) choices[which].second else "ime"
                Prefs.setVoiceMode(this, mode)
                Toast.makeText(this, R.string.voice_hint_longpress, Toast.LENGTH_LONG).show()
                if (thenRun) startVoice()
            }
            .show()
    }

    /** "ime" mode: the user dictates with their own keyboard (Wispr Flow, Gboard
     *  mic, …); the mic button just opens the keyboard switcher over the field. */
    private fun showKeyboardPicker() {
        input.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(input, 0)
        imm.showInputMethodPicker()
    }

    private fun launchRecognizer(component: String) {
        val voice = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
        ComponentName.unflattenFromString(component)?.let { voice.component = it }
        try {
            startActivityForResult(voice, REQ_VOICE)
        } catch (e: Exception) {
            // The chosen recognizer is gone (uninstalled/updated) — forget it and re-ask.
            Prefs.setVoiceMode(this, null)
            Toast.makeText(this, R.string.voice_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VOICE && resultCode == RESULT_OK) {
            val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!heard.isNullOrBlank()) {
                val existing = input.text.toString()
                input.setText(if (existing.isBlank()) heard else "$existing $heard")
                input.setSelection(input.text.length)
                input.requestFocus()
            }
        }
    }

    private fun save() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        saveBtn.isEnabled = false
        input.isEnabled = false
        status.visibility = View.VISIBLE
        status.setTextColor(getColor(R.color.subtext))
        status.setText(R.string.saving)

        val token = Prefs.token(this)
        val dbId = Prefs.inboxDbId(this)
        val titleProp = Prefs.inboxTitleProp(this)

        executor.execute {
            try {
                NotionApi.createItem(token, dbId, titleProp, text)
                runOnUiThread {
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    saveBtn.isEnabled = true
                    input.isEnabled = true
                    status.setTextColor(getColor(R.color.overdue))
                    status.text = getString(R.string.save_failed, e.message ?: "")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
