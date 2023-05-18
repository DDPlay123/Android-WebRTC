package com.tutorial.webrtc.data

import org.webrtc.SessionDescription

// 表示不同的 P2P SDP（Session Description Protocol）結果。
sealed class P2PSDPResult {
    data class P2PSDPCreateSuccess(val descriptor: SessionDescription) : P2PSDPResult()

    data class P2PSDPCreateFailure(val reason: String?) : P2PSDPResult()

    data class P2PSDPSetSuccess(val description: String) : P2PSDPResult()

    data class P2PSDPSetFailure(val reason: String?) : P2PSDPResult()
}
