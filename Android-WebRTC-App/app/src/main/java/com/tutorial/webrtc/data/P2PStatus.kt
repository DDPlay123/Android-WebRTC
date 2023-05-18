package com.tutorial.webrtc.data

import com.tutorial.webrtc.R

// 狀態呼叫
enum class P2PStatus(val label: Int, val color: Int) {
    CONNECTING(R.string.status_connecting, R.color.colorConnecting),
    WAITING(R.string.status_waiting, R.color.colorJoing),
    CONNECTED(R.string.status_connected, R.color.colorConnected),
    FINISHED(R.string.status_finished, R.color.colorConnected);
}