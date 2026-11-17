package dev.flondy.mylamp.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.flondy.mylamp.MyLampApp
import dev.flondy.mylamp.R
import dev.flondy.mylamp.torch.LightMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Schnelleinstellungs-Kachel: schaltet die LED ohne die App zu öffnen.
 * Nutzt denselben [dev.flondy.mylamp.torch.TorchController] wie die App.
 */
class TorchTileService : TileService() {

    private var scope: CoroutineScope? = null
    private var listenJob: Job? = null

    private val torch get() = (application as MyLampApp).container.torchController

    override fun onStartListening() {
        super.onStartListening()
        val s = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope = it }
        listenJob?.cancel()
        listenJob = s.launch {
            torch.mode.collect { updateTile(it) }
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (!torch.hasFlash) return
        // Laufende Muster (SOS, Strobe, Morse, Effekte) werden durch Antippen beendet.
        if (torch.mode.value == LightMode.OFF) torch.startTorch() else torch.stop()
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    private fun updateTile(mode: LightMode) {
        val tile = qsTile ?: return
        tile.state = when {
            !torch.hasFlash -> Tile.STATE_UNAVAILABLE
            mode != LightMode.OFF -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when (mode) {
                    LightMode.OFF -> R.string.status_off
                    LightMode.TORCH -> R.string.status_on
                    LightMode.SOS -> R.string.mode_sos
                    LightMode.STROBE -> R.string.mode_strobe
                    LightMode.MORSE -> R.string.mode_morse
                    LightMode.EFFECT -> R.string.mode_effects
                }
            )
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_flashlight)
        tile.updateTile()
    }
}
