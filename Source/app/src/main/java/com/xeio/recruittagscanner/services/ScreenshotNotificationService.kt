package com.xeio.recruittagscanner.services

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

        if(cancelNext && sbn?.packageName ==  "com.samsung.android.app.smartcapture"){
            Log.i(Globals.TAG, "Delayed cancel of notification: ${sbn.notification.extras.getString("android.title")}")
            cancelNotification(sbn.key)
            cancelNext = false
        }
    }

    private class NotificationServiceBroadcastReceiver(notifyService: ScreenshotNotificationService) : BroadcastReceiver() {
        val notifyService = notifyService

        override fun onReceive(context: Context?, intent: Intent) {

            if (intent.action == clearScreenshotNotification) {
                Log.i(Globals.TAG, "Clearing notifications")
                notifyService.activeNotifications
                    .filter { n -> n.packageName ==  "com.samsung.android.app.smartcapture"}
                    .ifEmpty {
                        Log.i(Globals.TAG, "No notifications found, trying to clear next...")
                        notifyService.cancelNext = true
                        listOf()
                    }
                    .forEach{ n ->
                        Log.i(Globals.TAG, "Clearing notification: ${n.notification.extras.getString("android.title")}")
                        notifyService.cancelNotification(n.key)
                    }

            }
        }
    }
}