package com.tutorial.webrtc.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.tutorial.webrtc.utils.Contracts.PERMISSION_CODE

object Method {
    /**
     * Logcat
     */
    fun logE(tag: String, message: String) {
        Log.e(tag, message)
    }

    /**
     * Permissions
     */
    fun requestPermission(activity: Activity, vararg permissions: String): Boolean {
        return if (!hasPermissions(activity, *permissions)) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_CODE)
            false
        } else
            true
    }

    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        for (permission in permissions)
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false
        return true
    }
}