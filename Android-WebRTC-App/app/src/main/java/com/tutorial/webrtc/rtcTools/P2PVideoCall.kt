package com.tutorial.webrtc.rtcTools

import android.content.Context
import android.os.Build
import com.tutorial.webrtc.data.P2PMessage
import com.tutorial.webrtc.data.P2PSDPResult
import com.tutorial.webrtc.data.P2PStatus
import com.tutorial.webrtc.utils.Method
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.webrtc.AudioSource
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer
import org.webrtc.VideoRenderer
import org.webrtc.VideoSource

class P2PVideoCall(
    private val context: Context,
    private val p2pStatusCallBack: (P2PStatus) -> Unit,
    private val p2PWebSocket: P2PWebSocket,
    private val p2pViewRenderer: P2PViewRenderer
) {
    companion object {
        const val STREAM_LABEL = "remoteStream"
        const val VIDEO_TRACK_LABEL = "remoteVideoTrack"
        const val AUDIO_TRACK_LABEL = "remoteAudioTrack"
        const val TAG = "P2PVideoCall"

        // 建立單一執行緒
        private val scope = CoroutineScope(Dispatchers.Main)

        // 連線 WebRTC 伺服器
        fun connect(
            context: Context,
            url: String,
            p2PSurfaceViewRenderer:
            P2PViewRenderer,
            p2pStatusCallBack: (P2PStatus) -> Unit
        ): P2PVideoCall {
            Method.logE(TAG, "Connect Server: $url")
            val socket = P2PWebSocket()
            val call = P2PVideoCall(context, p2pStatusCallBack, socket, p2PSurfaceViewRenderer)
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            client.newWebSocket(request, socket)
            client.dispatcher.executorService.shutdown()
            return call
        }
    }

    // PeerConnection 實例
    private var peerConnection: PeerConnection? = null

    // 生成 PeerConnectionFactory 實例
    private var factory: PeerConnectionFactory? = null

    // 是否為發送端
    private var isOffer = false

    // 視訊源
    private var videoSource: VideoSource? = null

    // 音訊源
    private var audioSource: AudioSource? = null

    // 擷取視訊
    private var videoCapturer: VideoCapturer? = null

    // 本地視訊串流
    private var localStream: MediaStream? = null

    // OpenGL EGL 顯示
    private val eglBase = EglBase.create()

    // 圖形渲染 OpenGL EGL Context ，用於給 SurfaceViewRenderer 初始化
    val renderContext: EglBase.Context
        get() = eglBase.eglBaseContext

    init {
        // WebSocket 訊息處理
        p2PWebSocket.messageHandler = this::onMessage
        // 設定初始狀態
        this.p2pStatusCallBack(P2PStatus.WAITING)
        // 初始化
        scope.launch { init() }
    }

    // 初始化
    private fun init() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
            // 加速器
            .setEnableVideoHwAcceleration(false)
            // 內部追蹤
            .setEnableInternalTracer(false)
            // 建立初始化選項
            .createInitializationOptions()
        )

        val opts = PeerConnectionFactory.Options()
        opts.networkIgnoreMask = 0

        // 建立 PeerConnectionFactory
        factory = PeerConnectionFactory.builder().setOptions(opts).createPeerConnectionFactory()
        // 設定硬體加速
        factory?.setVideoHwAccelerationOptions(eglBase.eglBaseContext, eglBase.eglBaseContext)

        // 設定多媒體
        val constraints = MediaConstraints()
        // 可接收音訊
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        // 可接收視訊
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        // ICE Server
        val iceServers = arrayListOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        // 設定 ICE Server
        val rtcCfg = PeerConnection.RTCConfiguration(iceServers)
        rtcCfg.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        val rtcEvents = P2PPeerConnEventHandler(
            this::handleLocalIceCandidate,
            this::addRemoteStream,
            this::removeRemoteStream
        )

        // 建立 PeerConnection
        peerConnection = factory?.createPeerConnection(rtcCfg, constraints, rtcEvents)
        setupMediaDevices()
    }

    // 建立 Offer
    private fun createOffer() {
        if (isOffer) {
            peerConnection?.createOffer(
                P2POfferAnswerEventHandler(this::createOrSetDescriptionCallback),
                MediaConstraints()
            )
        }
    }

    private fun handleLocalIceCandidate(candidate: IceCandidate) {
        Method.logE(TAG, "Local candidate: $candidate")
        p2PWebSocket.sendCandidate(candidate.sdpMLineIndex, candidate.sdpMid, candidate.sdp)
    }

    private fun addRemoteStream(stream: MediaStream) {
        Method.logE(TAG, "Add Remote stream: $stream")
        p2pStatusCallBack(P2PStatus.CONNECTED)
        scope.launch {
            if (stream.videoTracks.isNotEmpty()) {
                val remoteVideoTrack = stream.videoTracks.first()
                remoteVideoTrack.setEnabled(true)
                remoteVideoTrack.addRenderer(VideoRenderer(p2pViewRenderer.remoteRenderer))
            }
        }
    }

    private fun removeRemoteStream(stream: MediaStream) {
        Method.logE(TAG, "Remove Remote stream")
        p2pStatusCallBack(P2PStatus.FINISHED)
    }

    private fun handleRemoteCandidate(label: Int, id: String, strCandidate: String) {
        Method.logE(TAG, "Remote candidate: $strCandidate")
        scope.launch {
            val candidate = IceCandidate(id, label, strCandidate)
            peerConnection?.addIceCandidate(candidate)
        }
    }

    private fun setupMediaDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val camera2 = Camera2Enumerator(context)
            if (camera2.deviceNames.isNotEmpty()) {
                val selectedDevice = camera2.deviceNames.firstOrNull(camera2::isFrontFacing)
                    ?: camera2.deviceNames.first()
                videoCapturer = camera2.createCapturer(selectedDevice, null)
            }
        }

        if (videoCapturer == null) {
            val camera1 = Camera1Enumerator(true)
            val selectedDevice = camera1.deviceNames.firstOrNull(camera1::isFrontFacing)
                ?: camera1.deviceNames.first()
            videoCapturer = camera1.createCapturer(selectedDevice, null)
        }

        // 建立視訊源
        videoSource = factory?.createVideoSource(videoCapturer)
        // 開始擷取視訊
        videoCapturer?.startCapture(640, 480, 24)
        // 建立本地視訊串流
        val stream = factory?.createLocalMediaStream(STREAM_LABEL)
        // 建立視訊軌道
        val videoTrack = factory?.createVideoTrack(VIDEO_TRACK_LABEL, videoSource)
        // 設定視訊渲染器
        val videoRenderer = VideoRenderer(p2pViewRenderer.localRenderer)
        // 新增視訊渲染器
        videoTrack?.addRenderer(videoRenderer)
        // 新增視訊軌道
        stream?.addTrack(videoTrack)

        // 建立音訊源
        audioSource = factory?.createAudioSource(createAudioConstraints())
        // 建立音訊軌道
        val audioTrack = factory?.createAudioTrack(AUDIO_TRACK_LABEL, audioSource)
        // 新增音訊軌道
        stream?.addTrack(audioTrack)

        // 新增 PeerConnection
        peerConnection?.addStream(stream)
        localStream = stream
    }

    // 開關麥克風
    fun turnMicrophone(enable: Boolean) =
        localStream?.audioTracks?.get(0)?.setEnabled(enable)

    // 開關攝影機
    fun turnCamera(enable: Boolean) =
        localStream?.videoTracks?.get(0)?.setEnabled(enable)

    // 設定音訊約束
    private fun createAudioConstraints(): MediaConstraints {
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "false"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "false"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "false"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "false"))
        return audioConstraints
    }

    // 處理 Offer Answer
    private fun handleRemoteDescriptor(sdp: String) {
        Method.logE(TAG, "SDP: $sdp")
        // 如果是發送方，則設定遠端 SDP
        if (isOffer)
            // 設定遠端 Answer SDP
            peerConnection?.setRemoteDescription(
                P2POfferAnswerEventHandler(this::createOrSetDescriptionCallback),
                SessionDescription(
                    SessionDescription.Type.ANSWER, sdp
                )
            )
        else
            // 設定遠端 Offer SDP
            peerConnection?.setRemoteDescription(
                P2POfferAnswerEventHandler(this::createOrSetDescriptionCallback),
                SessionDescription(
                    SessionDescription.Type.OFFER, sdp
                )
            )
    }

    // 建立或設定 SDP Callback
    private fun createOrSetDescriptionCallback(result: P2PSDPResult) {
        when (result) {
            is P2PSDPResult.P2PSDPCreateSuccess -> {
                // 設定 Local SDP
                peerConnection?.setLocalDescription(
                    P2POfferAnswerEventHandler(this::createOrSetDescriptionCallback),
                    result.descriptor
                )
                // 發送 SDP
                p2PWebSocket.sendOfferAnswerMessage(result.descriptor.description)
            }

            is P2PSDPResult.P2PSDPCreateFailure ->
                Method.logE(TAG, "Create Offer failed: ${result.reason}")

            is P2PSDPResult.P2PSDPSetSuccess -> {
                if (!isOffer) {
                    // 建立 Answer SDP
                    peerConnection?.createAnswer(
                        P2POfferAnswerEventHandler(this::createOrSetDescriptionCallback),
                        MediaConstraints()
                    )
                }
            }

            is P2PSDPResult.P2PSDPSetFailure ->
                Method.logE(TAG, "Set Remote SDP failed: ${result.reason}")
        }
    }

    // Server 返回的訊息處理
    private fun onMessage(message: P2PMessage) {
        when (message) {
            is P2PMessage.JoinRoomMessage -> {
                p2pStatusCallBack(P2PStatus.CONNECTING)
                isOffer = message.offer
                scope.launch { createOffer() }
            }

            is P2PMessage.LeaveRoomMessage ->
                p2pStatusCallBack(P2PStatus.FINISHED)

            is P2PMessage.OfferAnswerMessage ->
                handleRemoteDescriptor(message.description)

            is P2PMessage.CandidateMessage ->
                handleRemoteCandidate(message.label, message.id, message.candidate)
        }
    }

    // 銷毀資源
    fun dispose() {
        p2PWebSocket.close()
        try {
            videoCapturer?.stopCapture()
        } catch (ignored: Exception) {
        }

        videoCapturer?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        peerConnection?.dispose()
        factory?.dispose()
        eglBase.release()
    }
}