package com.tutorial.webrtc.rtcTools

import com.tutorial.webrtc.utils.Method
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver

// PeerConnection 事件監聽複寫
class P2PPeerConnEventHandler(
    private val onIceCandidateCallBack: (IceCandidate) -> Unit,
    private val onAddStreamCallBack: (MediaStream) -> Unit,
    private val onRemoveStreamCallBack: (MediaStream) -> Unit
) : PeerConnection.Observer {

    override fun onIceCandidate(candidate: IceCandidate?) {
        candidate?.let(onIceCandidateCallBack)
    }

    override fun onAddStream(stream: MediaStream?) {
        stream?.let(onAddStreamCallBack)
    }

    override fun onRemoveStream(stream: MediaStream?) {
        stream?.let(onRemoveStreamCallBack)
    }

    override fun onDataChannel(chan: DataChannel?) {
        Method.logE(P2PVideoCall.TAG, "onDataChannel: $chan")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Method.logE(P2PVideoCall.TAG, "onIceConnectionReceivingChange: $p0")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Method.logE(P2PVideoCall.TAG, "onIceConnectionChange: $newState")
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        Method.logE(P2PVideoCall.TAG, "onIceGatheringChange: $newState")
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Method.logE(P2PVideoCall.TAG, "onSignalingChange: $newState")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        Method.logE(P2PVideoCall.TAG, "onIceCandidatesRemoved: $candidates")
    }

    override fun onRenegotiationNeeded() {
        Method.logE(P2PVideoCall.TAG, "onRenegotiationNeeded")
    }

    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) { }
}