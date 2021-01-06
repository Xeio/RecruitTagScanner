package com.xeio.recruittagscanner

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.xeio.recruittagscanner.managers.RecruitPrefsManager
import com.xeio.recruittagscanner.services.ScreenshotWatcherService

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment())
                    .commit()
        }

        if(RecruitPrefsManager.getEnableSetting(this)) {
            Intent(this, ScreenshotWatcherService::class.java).also { intent -> startService(intent) }
        }

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == 987){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent(this, ScreenshotWatcherService::class.java).also { intent -> startService(intent) }
            }
            else{
                PreferenceManager.getDefaultSharedPreferences(this).edit {
                    putBoolean(RecruitPrefsManager.enable, false)
                    apply()

                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.settings, SettingsFragment())
                        .commit()
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if(key == RecruitPrefsManager.enable) {
            if (sharedPreferences.getBoolean(RecruitPrefsManager.enable, false)) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Intent(this, ScreenshotWatcherService::class.java).also { intent -> startService(intent) }
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 987)
                }
            } else {
                Intent(this, ScreenshotWatcherService::class.java).also { intent -> stopService(intent) }
            }
        }

        if(key == RecruitPrefsManager.hideNotification && sharedPreferences.getBoolean(RecruitPrefsManager.hideNotification, false)) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) == PackageManager.PERMISSION_DENIED) {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        }
    }
}