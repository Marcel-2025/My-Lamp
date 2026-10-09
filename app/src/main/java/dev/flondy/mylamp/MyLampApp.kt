package dev.flondy.mylamp

import android.app.Application
import dev.flondy.mylamp.core.AppContainer

class MyLampApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
