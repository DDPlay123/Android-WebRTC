package com.tutorial.webrtc.utils

import android.Manifest

object Contracts {
    /**
     * Variable
     */
    const val PERMISSION_CODE = 1001

    /**
     * Permission
     */
    val rtc_permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
}