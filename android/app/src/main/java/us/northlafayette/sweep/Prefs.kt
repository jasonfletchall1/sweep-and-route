package us.northlafayette.sweep

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "sweep_prefs"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun token(ctx: Context): String = sp(ctx).getString("token", "") ?: ""
    fun inboxDbId(ctx: Context): String = sp(ctx).getString("inboxDbId", "") ?: ""
    fun inboxTitleProp(ctx: Context): String = sp(ctx).getString("inboxTitleProp", "Name") ?: "Name"
    fun tasksDbId(ctx: Context): String = sp(ctx).getString("tasksDbId", "") ?: ""
    fun doneProp(ctx: Context): String = sp(ctx).getString("doneProp", "Done") ?: "Done"
    fun dueProp(ctx: Context): String = sp(ctx).getString("dueProp", "Due") ?: "Due"

    fun save(
        ctx: Context,
        token: String,
        inboxDbId: String,
        inboxTitleProp: String,
        tasksDbId: String,
        doneProp: String,
        dueProp: String
    ) {
        sp(ctx).edit()
            .putString("token", token)
            .putString("inboxDbId", inboxDbId)
            .putString("inboxTitleProp", inboxTitleProp)
            .putString("tasksDbId", tasksDbId)
            .putString("doneProp", doneProp)
            .putString("dueProp", dueProp)
            .apply()
    }

    /**
     * How the mic button dictates: null = not chosen yet, "ime" = the user's
     * own keyboard (Wispr Flow etc., via the input-method picker), otherwise
     * a flattened ComponentName of a speech-recognizer activity.
     */
    fun voiceMode(ctx: Context): String? = sp(ctx).getString("voiceMode", null)

    fun setVoiceMode(ctx: Context, mode: String?) {
        sp(ctx).edit().putString("voiceMode", mode).apply()
    }

    fun captureReady(ctx: Context): Boolean =
        token(ctx).isNotBlank() && inboxDbId(ctx).isNotBlank()

    fun tasksReady(ctx: Context): Boolean =
        token(ctx).isNotBlank() && tasksDbId(ctx).isNotBlank()
}
