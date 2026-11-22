package com.dx9901mk5.pixelaibridge.feature.magiccue

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MagicCueNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val item = sbn ?: return
        val pkg = item.packageName ?: return
        if (!MagicCueBus.isPackageAllowed(this, pkg)) return

        val extras = item.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        MagicCueBus.publish(
            CapturedCueNotification(
                id = "${item.key}_${item.postTime}",
                packageName = pkg,
                title = title,
                text = text,
                timestampMs = item.postTime
            )
        )
    }
}
