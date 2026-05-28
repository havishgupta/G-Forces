package com.havish.gforces.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.havish.gforces.R

class GForceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_SERVICE) {
            val serviceIntent = Intent(context, GForceWidgetService::class.java)
            if (GForceWidgetService.isRunning) {
                context.stopService(serviceIntent)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_SERVICE = "com.havish.gforces.widget.TOGGLE_SERVICE"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_gforce)

            // Setup click intent to toggle service
            val intent = Intent(context, GForceWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_SERVICE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Draw an initial static image if service is not running
            if (!GForceWidgetService.isRunning) {
                val bitmap = GForceImageRenderer.render(300, 300, 0f, 0f)
                views.setImageViewBitmap(R.id.widget_image, bitmap)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
