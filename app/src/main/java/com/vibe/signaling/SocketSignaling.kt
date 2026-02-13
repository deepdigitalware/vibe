package com.vibe.signaling

import android.util.Log
import com.vibe.util.SocketManager
import org.json.JSONObject
import org.webrtc.IceCandidate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

class SocketSignaling : SignalingService {
    private val socket = SocketManager.getSocket()
    private val TAG = "SocketSignaling"

    override suspend fun createRoom(roomId: String, offerSdp: String) {
        val data = JSONObject().apply {
            put("roomId", roomId)
            put("offer", offerSdp)
        }
        socket?.emit("create_room", data)
    }

    override suspend fun getOffer(roomId: String): String? {
        val deferred = CompletableDeferred<String?>()
        socket?.emit("get_offer", roomId)
        socket?.once("receive_offer") { args ->
            val data = args[0] as? JSONObject
            deferred.complete(data?.optString("offer"))
        }
        return withTimeoutOrNull(5000) { deferred.await() }
    }

    override suspend fun setAnswer(roomId: String, answerSdp: String) {
        val data = JSONObject().apply {
            put("roomId", roomId)
            put("answer", answerSdp)
        }
        socket?.emit("set_answer", data)
    }

    override fun observeAnswer(roomId: String, onAnswer: (String) -> Unit) {
        socket?.on("receive_answer") { args ->
            val data = args[0] as? JSONObject
            val ans = data?.optString("answer")
            if (data?.optString("roomId") == roomId && !ans.isNullOrEmpty()) {
                onAnswer(ans)
            }
        }
    }

    override fun sendIceCandidate(roomId: String, role: SignalingService.Role, candidate: IceCandidate) {
        val map = JSONObject().apply {
            put("roomId", roomId)
            put("role", role.name)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
        }
        socket?.emit("send_ice_candidate", map)
    }

    override fun observeRemoteIce(roomId: String, role: SignalingService.Role, onCandidate: (IceCandidate) -> Unit) {
        socket?.on("receive_ice_candidate") { args ->
            val data = args[0] as? JSONObject
            if (data?.optString("roomId") == roomId) {
                val remoteRole = data.optString("role")
                // If I am Caller, I listen for Callee candidates. If I am Callee, I listen for Caller.
                val expectedRole = if (role == SignalingService.Role.Caller) "Callee" else "Caller"
                
                if (remoteRole == expectedRole) {
                    val cand = IceCandidate(
                        data.optString("sdpMid"),
                        data.optInt("sdpMLineIndex"),
                        data.optString("candidate")
                    )
                    onCandidate(cand)
                }
            }
        }
    }

    override suspend fun clearRoom(roomId: String) {
        socket?.emit("clear_room", roomId)
        socket?.off("receive_answer")
        socket?.off("receive_ice_candidate")
    }
}
