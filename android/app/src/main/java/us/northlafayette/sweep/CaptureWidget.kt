package us.northlafayette.sweep

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CaptureWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, awm: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_capture)

            views.setOnClickPendingIntent(
                R.id.capture_area,
                PendingIntent.getActivity(
                    context, 10,
                    Intent(context, CaptureActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            views.setOnClickPendingIntent(
                R.id.capture_voice,
                PendingIntent.getActivity(
                    context, 11,
                    Intent(context, CaptureActivity::class.java)
                        .putExtra(CaptureActivity.EXTRA_VOICE, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            awm.updateAppWidget(id, views)
        }
    }
}
