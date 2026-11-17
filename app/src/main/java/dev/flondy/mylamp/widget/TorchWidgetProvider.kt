package dev.flondy.mylamp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.flondy.mylamp.MyLampApp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.torch.LightMode

/**
 * Startbildschirm-Widget: ein Tipp schaltet die LED an oder aus (laufende Muster werden beendet).
 * Nutzt denselben [dev.flondy.mylamp.torch.TorchController] wie App und Schnelleinstellungs-Kachel.
 */
class TorchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        (context.applicationContext as MyLampApp).ensureWidgetSync()
        val mode = torch(context).mode.value
        appWidgetIds.forEach { manager.updateAppWidget(it, buildViews(context, mode)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val torch = torch(context)
            if (!torch.hasFlash) return
            if (torch.mode.value == LightMode.OFF) torch.startTorch() else torch.stop()
            // Aktualisierung kommt über MyLampApp (beobachtet den Modus)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "dev.flondy.mylamp.widget.TOGGLE"

        private fun torch(context: Context) = (context.applicationContext as MyLampApp).container.torchController

        fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, TorchWidgetProvider::class.java)).isNotEmpty()

        fun updateAll(context: Context, mode: LightMode) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TorchWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context, mode)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context, mode: LightMode): RemoteViews {
            val on = mode != LightMode.OFF
            val intent = Intent(context, TorchWidgetProvider::class.java).setAction(ACTION_TOGGLE)
            val pending = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return RemoteViews(context.packageName, R.layout.widget_torch).apply {
                setInt(R.id.widget_button, "setBackgroundResource", if (on) R.drawable.widget_bg_on else R.drawable.widget_bg_off)
                setInt(R.id.widget_icon, "setColorFilter", if (on) 0xFF050711.toInt() else 0xFFFDEE51.toInt())
                setContentDescription(
                    R.id.widget_button,
                    context.getString(R.string.cd_power) + " – " + context.getString(if (on) R.string.status_on else R.string.status_off),
                )
                setOnClickPendingIntent(R.id.widget_root, pending)
            }
        }
    }
}
