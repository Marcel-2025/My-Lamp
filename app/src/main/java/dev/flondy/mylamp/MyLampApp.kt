package dev.flondy.mylamp

import android.app.Application
import dev.flondy.mylamp.core.AppContainer
import dev.flondy.mylamp.widget.TorchWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MyLampApp : Application() {
    lateinit var container: AppContainer
        private set

    private var widgetSync: Job? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (TorchWidgetProvider.hasWidgets(this)) ensureWidgetSync()
    }

    /** Hält das Startbildschirm-Widget mit dem Lichtzustand synchron (nur wenn ein Widget existiert). */
    fun ensureWidgetSync() {
        if (widgetSync?.isActive == true) return
        widgetSync = container.appScope.launch(Dispatchers.Main) {
            container.torchController.mode.collect { TorchWidgetProvider.updateAll(this@MyLampApp, it) }
        }
    }
}
