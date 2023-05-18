package com.tutorial.webrtc.data

// signaling message type
enum class MessageType(val value: String) {
    OfferAnswer("offerAnswer"),
    Candidate("candidate"),
    JoinRoom("joinRoom"),
    LeaveRoom("leaveRoom");
    override fun toString() = value
}