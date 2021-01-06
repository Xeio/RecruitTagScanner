package com.xeio.recruittagscanner.managers

import android.content.Context
import androidx.preference.PreferenceManager

class RecruitPrefsManager{
    companion object{
        val autoDelete = "autoDelete"
        val enable = "enable"
        val hideNotification = "hideNotification"

        fun getDeleteSetting(context: Context) : Boolean{
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(autoDelete, false)
        }

        fun getEnableSetting(context: Context) : Boolean{
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(enable, false)
        }

        fun getHideNotificationSetting(context: Context) : Boolean{
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(hideNotification, false)
        }
    }
}