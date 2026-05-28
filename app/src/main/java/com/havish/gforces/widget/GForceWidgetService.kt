package com.havish.gforces.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.havish.gforces.R
import com.havish.gforces.SensorManagerWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GForceWidgetService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var serviceJob: Job? = null
    private lateinit var sensorManagerWrapper: SensorManagerWrapper

    override fun onCreate() {
        super.onCreate()
        sensorManagerWrapper = SensorManagerWrapper(this)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        serviceJob?.cancel()
        serviceJob = coroutineScope.launch {
            var currentX = 0f
            var currentY = 0f

            // Launch sensor collection in background
            launch {
                sensorManagerWrapper.getGravityData().collectLatest { data ->
                    currentX = data.x
                    currentY = data.y
                }
            }

            // Update widget periodically
            while (isActive) {
                updateWidgets(currentX, currentY)
                delay(100) // Update roughly 10 times a second for widget
            }
        }
    }

    private fun updateWidgets(x: Float, y: Float) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, GForceWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        val bitmap = GForceImageRenderer.render(300, 300, x, y)
        val views = RemoteViews(packageName, R.layout.widget_gforce)
        views.setImageViewBitmap(R.id.widget_image, bitmap)

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun createNotification(): Notification {
        val channelId = "gforce_widget_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "G-Force Widget Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("G-Force Tracking Active")
            .setContentText("Widget is updating live.")
            // Use standard Android icon since we don't have custom ones right now
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob?.cancel()
        
        // Reset widget to 0 state when service dies
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, GForceWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val views = RemoteViews(packageName, R.layout.widget_gforce)
        val bitmap = GForceImageRenderer.render(300, 300, 0f, 0f)
        views.setImageViewBitmap(R.id.widget_image, bitmap)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRunning = false
            private set
        private const val NOTIFICATION_ID = 1
    }
}
