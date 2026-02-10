package com.vibe.signaling

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate

data class ChatMessage(
    val sender: String,
    val message: String,
    val timestamp: Long
)

class FirestoreSignaling : SignalingService {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun roomRef(roomId: String): DocumentReference = firestore.collection("rooms").document(roomId)

    override suspend fun createRoom(roomId: String, offerSdp: String) {
        ensureFirebase()
        val data = hashMapOf("offer" to offerSdp)
        try {
            roomRef(roomId).set(data).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create room", e)
        }
    }

    override suspend fun getOffer(roomId: String): String? {
        ensureFirebase()
        return try {
            val snap = roomRef(roomId).get().await()
            snap.getString("offer")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get offer", e)
            null
        }
    }

    override suspend fun setAnswer(roomId: String, answerSdp: String) {
        ensureFirebase()
        try {
            roomRef(roomId).update(mapOf("answer" to answerSdp)).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set answer", e)
        }
    }

    override fun observeAnswer(roomId: String, onAnswer: (String) -> Unit) {
        ensureFirebase()
        roomRef(roomId).addSnapshotListener { snap, _ ->
            val ans = snap?.getString("answer")
            if (!ans.isNullOrEmpty()) onAnswer(ans)
        }
    }

    override fun sendIceCandidate(roomId: String, role: SignalingService.Role, candidate: IceCandidate) {
        ensureFirebase()
        val map = hashMapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp
        )
        val collName = if (role == SignalingService.Role.Caller) "callerCandidates" else "calleeCandidates"
        roomRef(roomId).collection(collName).add(map)
    }

    override fun observeRemoteIce(roomId: String, role: SignalingService.Role, onCandidate: (IceCandidate) -> Unit) {
        ensureFirebase()
        val collName = if (role == SignalingService.Role.Caller) "calleeCandidates" else "callerCandidates"
        roomRef(roomId).collection(collName).addSnapshotListener { snaps, _ ->
            if (snaps != null) {
                for (documentChange in snaps.documentChanges) {
                    val data = documentChange.document.data
                    val cand = IceCandidate(
                        data["sdpMid"] as? String,
                        (data["sdpMLineIndex"] as? Number)?.toInt() ?: 0,
                        data["candidate"] as? String ?: ""
                    )
                    onCandidate(cand)
                }
            }
        }
    }

    fun sendChatMessage(roomId: String, sender: String, message: String) {
        ensureFirebase()
        val chatMessage = ChatMessage(sender, message, System.currentTimeMillis())
        roomRef(roomId).collection("chat").add(chatMessage)
    }

    fun observeChatMessages(roomId: String, onMessage: (ChatMessage) -> Unit) {
        ensureFirebase()
        roomRef(roomId).collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, _ ->
                if (snaps != null) {
                    for (documentChange in snaps.documentChanges) {
                        val data = documentChange.document.toObject(ChatMessage::class.java)
                        onMessage(data)
                    }
                }
            }
    }

    override suspend fun clearRoom(roomId: String) {
        ensureFirebase()
        val ref = roomRef(roomId)
        try {
            val callerCandidates = ref.collection("callerCandidates").get().await()
            for (doc in callerCandidates.documents) {
                doc.reference.delete().await()
            }
            val calleeCandidates = ref.collection("calleeCandidates").get().await()
            for (doc in calleeCandidates.documents) {
                doc.reference.delete().await()
            }
            val chatMessages = ref.collection("chat").get().await()
            for (doc in chatMessages.documents) {
                doc.reference.delete().await()
            }
            ref.delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear room", e)
        }
    }

    private fun ensureFirebase() {
        try {
            FirebaseApp.getInstance()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "FirebaseApp not initialized. Add google-services.json or manual options.")
        }
    }

    companion object { private const val TAG = "FirestoreSignaling" }
}