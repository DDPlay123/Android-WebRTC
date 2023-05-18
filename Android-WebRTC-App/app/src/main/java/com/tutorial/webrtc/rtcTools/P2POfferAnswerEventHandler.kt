package com.tutorial.webrtc.rtcTools

import com.tutorial.webrtc.data.P2PSDPResult
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

// SdpObserver 事件監聽複寫
class P2POfferAnswerEventHandler(val callback: (P2PSDPResult) -> Unit) : SdpObserver {

    override fun onSetFailure(reason: String?) = callback(P2PSDPResult.P2PSDPSetFailure(reason))

    override fun onSetSuccess() = callback(P2PSDPResult.P2PSDPSetSuccess(""))

    override fun onCreateSuccess(descriptor: SessionDescription) = callback(P2PSDPResult.P2PSDPCreateSuccess(descriptor))

    override fun onCreateFailure(reason: String?) = callback(P2PSDPResult.P2PSDPCreateFailure(reason))
}