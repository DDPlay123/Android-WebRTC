package com.tutorial.webrtc.rtcTools

import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoRenderer

// Local & Remote 渲染畫面
data class P2PViewRenderer(val localView: SurfaceViewRenderer?, val remoteView: SurfaceViewRenderer?) {
    // Local
    val localRenderer: (frame: VideoRenderer.I420Frame) -> Unit = {
        if(localView == null)
            VideoRenderer.renderFrameDone(it)
        else
            localView.renderFrame(it)
    }

    // Remote
    val remoteRenderer: (frame: VideoRenderer.I420Frame) -> Unit = {
        if(remoteView == null)
            VideoRenderer.renderFrameDone(it)
        else
            remoteView.renderFrame(it)
    }
}