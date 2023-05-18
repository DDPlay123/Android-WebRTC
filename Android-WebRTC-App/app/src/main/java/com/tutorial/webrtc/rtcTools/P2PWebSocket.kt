package com.tutorial.webrtc.rtcTools

import com.tutorial.webrtc.data.P2PMessage
import com.tutorial.webrtc.utils.Method
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

// WebSocket 前后端信令交互事件監聽
class P2PWebSocket : WebSocketListener() {
    companion object {
        private const val TAG = "P2PWebSocket"
    }

    // Socket server
    private var webSocket: WebSocket? = null

    // 消息回调函数
    var messageHandler: ((P2PMessage) -> Unit) = { }

    // 開啟
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Method.logE(TAG, "WebSocket open....")
        this.webSocket = webSocket
    }

    // 關閉
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Method.logE(TAG, "WebSocket close...")
        super.onClosing(webSocket, code, reason)
        this.webSocket = null
    }

    // 強制關閉
    fun close() = webSocket?.close(1000, null)

    // 連線失敗
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        Method.logE(TAG, "WebSocket connect failed: ${t.message}")
    }

    // Server 返回的信令消息
    override fun onMessage(webSocket: WebSocket, text: String) {
        Method.logE(TAG, "Server message: $text")
        // 解析 JSON Text
        val json = JSONObject(text)
        // 取得資料
        val message =
            when (json.getString("type")) {
                "offerAnswer" ->
                    P2PMessage.OfferAnswerMessage(json.getString("description"))

                "candidate" ->
                    if (json.has("data")) {
                        val jsonData = JSONObject(json.getString("data"))
                        Method.logE(TAG, "Candidate data: $jsonData")
                        P2PMessage.CandidateMessage(
                            jsonData.getInt("sdpMLineIndex"),
                            jsonData.getString("sdpMid"),
                            jsonData.getString("candidate")
                        )
                    } else {
                        P2PMessage.CandidateMessage(
                            json.getInt("label"),
                            json.getString("id"),
                            json.getString("candidate")
                        )
                    }

                "joinRoom" ->
                    P2PMessage.JoinRoomMessage(json.getString("remoteId"), json.getBoolean("offer"))

                "leaveRoom" ->
                    P2PMessage.LeaveRoomMessage("leave")

                else -> null
            }
        Method.logE(TAG, "Receive message: $message")
        message?.let { messageHandler.invoke(it) }
    }

    // 發送 OfferAnswer 訊息
    fun sendOfferAnswerMessage(description: String) =
        sendToServer(P2PMessage.OfferAnswerMessage(description))

    // 發送 Candidate 訊息
    fun sendCandidate(label: Int, id: String, candidate: String) =
        sendToServer(P2PMessage.CandidateMessage(label, id, candidate))

    // 發送訊息至 Server
    private fun sendToServer(message: P2PMessage) {
        with (JSONObject()) {
            put("type", message.messageType)
            when (message) {
                is P2PMessage.OfferAnswerMessage -> {
                    put("description", message.description)
                }

                is P2PMessage.CandidateMessage -> {
                    put("label", message.label)
                    put("id", message.id)
                    put("candidate", message.candidate)
                }

                else -> {
                    Method.logE(TAG, "Send failed:${message.messageType.value}")
                    return
                }
            }

            Method.logE(TAG, "Send to server: ${this@with}")
            webSocket?.send(toString())
        }
    }
}