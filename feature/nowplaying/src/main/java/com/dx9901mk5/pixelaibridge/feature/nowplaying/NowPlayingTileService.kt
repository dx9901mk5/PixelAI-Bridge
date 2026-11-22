package com.dx9901mk5.pixelaibridge.feature.nowplaying

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile for one-tap access to PixelAI Now Playing recognition.
 */
class NowPlayingTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Now Playing 識曲"
            updateTile()
        }
    }

    override fun onClick() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            putExtra(EXTRA_START_TAB, 2)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                3021,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent)
        }
    }

    companion object {
        const val EXTRA_START_TAB = "extra_start_tab"
    }
}
