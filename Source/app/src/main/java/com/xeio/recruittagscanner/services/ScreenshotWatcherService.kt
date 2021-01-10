package com.xeio.recruittagscanner.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.*
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.xeio.recruittagscanner.Globals
import com.xeio.recruittagscanner.managers.DataManager
import com.xeio.recruittagscanner.managers.RecruitmentManager
import java.io.File
import java.io.IOException

class ScreenshotWatcherService : Service() {
    private var contentObserver: ContentObserver? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(Globals.TAG, "ScreenshotWatcherService create")
        super.onCreate()

        createNotificationChannel()
        enableHttpCache()

        //Trigger static constructor for data, starts the async data fetches
        //Maybe find a better way to do this (while avoiding network on main thread?)
        DataManager.allTags
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(Globals.TAG, "ScreenshotWatcherService start")
        super.onStartCommand(intent, flags, startId)

        if(contentObserver == null) {
            contentObserver = createObserver()

            contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    contentObserver!!
            )
            contentResolver.registerContentObserver(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    true,
                    contentObserver!!
            )
        }

        return START_STICKY
    }

    private fun createObserver(): ContentObserver{
        return object : ContentObserver(Handler(Looper.getMainLooper())) {
            var lastScanned: Uri? = null

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.i(Globals.TAG, "ContentObserver: $uri")
                if(uri == null) return
                if(!uri.query.isNullOrBlank()) return //Downloads app on Android 7 fires events with query strings on old screenshots sometimes, ignore them
                if(uri == lastScanned) return //Get duplicate notifications for files, ignore the last file we scanned

                val projection = mutableListOf(MediaStore.MediaColumns.DISPLAY_NAME)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    projection.addAll(listOf(MediaStore.MediaColumns.IS_PENDING, MediaStore.MediaColumns.RELATIVE_PATH))
                }

                val query = this@ScreenshotWatcherService.contentResolver.runCatching {
                    query(uri, projection.toTypedArray(), null, null, MediaStore.Audio.Media.DATE_ADDED + " DESC")
                }.onFailure {
                    Log.i(Globals.TAG, "Media query failed $it")
                }.getOrNull()

                query?.use{ cursor ->
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)

                    if(cursor.moveToFirst()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val pendingIndex = cursor.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
                            val pending = cursor.getInt(pendingIndex)
                            if (pending == 1) return //Skip pending
                        }

                        val name = cursor.getString(nameIndex)
                        val path = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                            cursor.getString(pathIndex)
                        } else { "" }

                        if(shouldScanFile(name, path)){
                            Log.i(Globals.TAG, "Scanning: $name, $path")
                            lastScanned = uri
                            kotlin.runCatching {
                                contentResolver.openFileDescriptor(uri, "r")?.use {
                                    val bitmap = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                                    val ocrTask = TextRecognition.getClient().process(inputImage)
                                    ocrTask.addOnSuccessListener { result ->
                                        RecruitmentManager.checkRecruitment(this@ScreenshotWatcherService, result.text, uri)
                                    }.addOnFailureListener { ex ->
                                        Log.i(Globals.TAG, "OCR Failed: $ex")
                                    }
                                } ?: {
                                    Log.i(Globals.TAG, "Failed to open file descriptor.")
                                }
                            }.onFailure { e ->
                                Log.i(Globals.TAG, "Unable to open file due to error $e")
                                lastScanned = null //Maybe we can try again later
                            }
                        }
                    }
                }
            }
        }
    }

    fun shouldScanFile(name: String, path: String) : Boolean{
        return path.contains("Screenshot", true) || name.contains("Screenshot", true) ||
        path.contains("Arknights", true) || name.contains("Arknights", true)
    }

    override fun onDestroy() {
        Log.i(Globals.TAG, "ScreenshotWatcherService destroy")
        super.onDestroy()

        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun enableHttpCache(){
        try {
            val httpCacheDir = File(cacheDir, "http")
            val httpCacheSize = 50 * 1024 * 1024.toLong()
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.i(Globals.TAG, "HTTP response cache installation failed: $e")
        }
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(Globals.RECRUIT_CHANNEL_ID, "Recruit Alerts", NotificationManager.IMPORTANCE_HIGH)
            mChannel.description =  "Alerts for recruitment"

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }
}