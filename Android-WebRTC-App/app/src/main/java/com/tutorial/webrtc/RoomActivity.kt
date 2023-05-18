package com.tutorial.webrtc

import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.tutorial.webrtc.data.P2PStatus
import com.tutorial.webrtc.databinding.ActivityRoomBinding
import com.tutorial.webrtc.utils.Method
import com.tutorial.webrtc.rtcTools.P2PVideoCall
import com.tutorial.webrtc.rtcTools.P2PViewRenderer
import com.tutorial.webrtc.utils.Contracts.PERMISSION_CODE
import com.tutorial.webrtc.utils.displayShortToast
import com.tutorial.webrtc.utils.requestRTCPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.RendererCommon

class RoomActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RoomActivity"
        private const val SERVER_URL = "ws://192.168.1.108:8000/"
    }

    private val binding: ActivityRoomBinding by lazy { ActivityRoomBinding.inflate(layoutInflater) }

    private var p2PVideoCall : P2PVideoCall? = null
    // 音源控制器
    private lateinit var audioManager: AudioManager
    // 靜音開關
    private var isMicMute: Boolean = false
    // 視訊開關
    private var isCamMute: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        doInitialize()
        setListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.apply {
            // 釋放資源
            p2PVideoCall?.dispose()
            localRenderer.release()
            remoteRenderer.release()
            isMicMute = false
            isCamMute = false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                for (result in grantResults)
                    if (result != PackageManager.PERMISSION_GRANTED)
                        when {
                            permissions.any { it == android.Manifest.permission.CAMERA } ->
                                finish()

                            permissions.any { it == android.Manifest.permission.RECORD_AUDIO } ->
                                finish()
                        }
                startVideoCall()
            }
        }
    }

    private fun doInitialize() {
        binding.apply {
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.apply {
                // 預設開啟通話模式
                mode = AudioManager.MODE_IN_COMMUNICATION
                isSpeakerphoneOn = true
            }

            // 要求權限
            if (requestRTCPermission()) startVideoCall()
        }
    }

    private fun setListener() {
        binding.run {
            imgCallEnd.setOnClickListener { finish() }

            imgAudio.setOnClickListener {
                p2PVideoCall?.turnMicrophone(isMicMute)
                isMicMute = !isMicMute
                imgAudio.setImageResource(
                    if (isMicMute)
                        R.drawable.outline_mic_off_24
                    else
                        R.drawable.outline_mic_none_24
                )
            }

            imgVideo.setOnClickListener {
                p2PVideoCall?.turnCamera(isCamMute)
                isCamMute = !isCamMute
                imgVideo.setImageResource(
                    if (isCamMute)
                        R.drawable.outline_videocam_off_24
                    else
                        R.drawable.outline_videocam_24
                )
            }
        }
    }

    private fun startVideoCall() {
        binding.apply {
            p2PVideoCall = P2PVideoCall.connect(
                this@RoomActivity,
                SERVER_URL,
                P2PViewRenderer(localRenderer, remoteRenderer)
            ) { status ->
                Method.logE(TAG, "State: ${status.name}")
                CoroutineScope(Dispatchers.Main).launch {
                    when (status) {
                        P2PStatus.FINISHED -> {
                            displayShortToast(getString(R.string.disconnected))
                            finish()
                        }

                        else -> {
                            displayShortToast(getString(R.string.connected))
                            tvState.text = getString(status.label)
                            tvState.setTextColor(ContextCompat.getColor(this@RoomActivity, status.color))
                        }
                    }
                }
            }

            // 初始化 Local Renderer
            localRenderer.init(p2PVideoCall?.renderContext, null)
            // 設定顯示的縮放類型
            localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            // 設定 Z 軸順序，為了讓 Local Renderer 在 Remote Renderer 上面
            localRenderer.setZOrderMediaOverlay(true)
            // 開啟硬體加速
            localRenderer.setEnableHardwareScaler(true)

            // 初始化 Remote Renderer
            remoteRenderer.init(p2PVideoCall?.renderContext, null)
            // 設定顯示的縮放類型
            remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            // 開啟硬體加速
            remoteRenderer.setEnableHardwareScaler(true)
        }
    }
}