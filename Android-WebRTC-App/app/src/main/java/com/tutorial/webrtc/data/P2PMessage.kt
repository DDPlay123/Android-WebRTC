package com.tutorial.webrtc.data

sealed class P2PMessage(val messageType: MessageType) {
    data class OfferAnswerMessage(val description: String) : P2PMessage(MessageType.OfferAnswer)

    data class CandidateMessage(val label: Int, val id: String, val candidate: String) : P2PMessage(MessageType.Candidate)

    data class JoinRoomMessage(val remoteId: String, val offer: Boolean) : P2PMessage(MessageType.JoinRoom)

    data class LeaveRoomMessage(val msg: String) : P2PMessage(MessageType.LeaveRoom)
}