package com.xeio.recruittagscanner.services

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xeio.recruittagscanner.Globals

class ScreenshotNotificationService : NotificationListenerService() {
    var cancelNext = false

    companion object{
        val clearScreenshotNotification = "clearLastScreenshot"

        private fun isScreenshotNotification(sbn: StatusBarNotification) : Boolean{
            return sbn.notification.extras.getString(Notification.EXTRA_TITLE)?.contains("Screenshot", true) == true
        }
    }

    private var receiver: NotificationServiceBroadcastReceiver? = null

    override fun onCreate() {
        Log.i(Globals.TAG, "ScreenshotNotificationService created")
        super.onCreate()

        receiver = NotificationServiceBroadcastReceiver(this)
        registerReceiver(receiver, IntentFilter().apply { addAction(clearScreenshotNotification) })
    }

    override fun onDestroy() {
        Log.i(Globals.TAG, "ScreenshotNotificationService destroyed")
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if(cancelNext && sbn != null && isScreenshotNotification(sbn)){
            Log.i(Globals.TAG, "Delayed cancel of notification: ${sbn.notification.extras.getString("android.title")}")
            cancelNotification(sbn.key)
            cancelNext = false
        }
    }

    private class NotificationServiceBroadcastReceiver(val notifyService: ScreenshotNotificationService) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            if (intent.action == clearScreenshotNotification) {
                Log.i(Globals.TAG, "Clearing notifications")
                notifyService.activeNotifications
                    .filter{ sbn -> isScreenshotNotification(sbn)}
                    .ifEmpty {
                        Log.i(Globals.TAG, "No notifications found, trying to clear next...")
                        notifyService.cancelNext = true
                        listOf()
                    }
                    .forEach{ sbn ->
                        Log.i(Globals.TAG, "Clearing notification: ${sbn.notification.extras.getString(Notification.EXTRA_TITLE)}")
                        notifyService.cancelNotification(sbn.key)
                    }

            }
        }
    }
}