package com.vibe.signaling

import org.webrtc.IceCandidate

interface SignalingService {
    enum class Role { Caller, Callee }

    suspend fun createRoom(roomId: String, offerSdp: String)
    suspend fun getOffer(roomId: String): String?
    suspend fun setAnswer(roomId: String, answerSdp: String)

    fun observeAnswer(roomId: String, onAnswer: (String) -> Unit)
    fun sendIceCandidate(roomId: String, role: Role, candidate: IceCandidate)
    fun observeRemoteIce(roomId: String, role: Role, onCandidate: (IceCandidate) -> Unit)
    suspend fun clearRoom(roomId: String)
}