package com.tutorial.webrtc.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.tutorial.webrtc.R
import com.tutorial.webrtc.utils.Contracts.rtc_permission

fun Activity.requestRTCPermission(): Boolean {
    if (!Method.requestPermission(this, *rtc_permission)) {
        displayShortToast(getString(R.string.toast_ask_rtc_permission))
        return false
    }
    return true
}

fun Context.displayShortToast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()