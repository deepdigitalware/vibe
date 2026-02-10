package com.vibe.util

import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.PeerConnection
import org.webrtc.MediaConstraints
import kotlin.coroutines.resume

// Helpers to await WebRTC operations using coroutines
suspend fun PeerConnection.createOffer(constraints: org.webrtc.MediaConstraints) =
    suspendCancellableCoroutine<SessionDescription> { cont ->
        this.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) { cont.resume(sdp!!) }
            override fun onCreateFailure(p0: String?) { cont.cancel(RuntimeException(p0)) }
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

suspend fun PeerConnection.createAnswer(constraints: org.webrtc.MediaConstraints) =
    suspendCancellableCoroutine<SessionDescription> { cont ->
        this.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) { cont.resume(sdp!!) }
            override fun onCreateFailure(p0: String?) { cont.cancel(RuntimeException(p0)) }
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

suspend fun PeerConnection.setLocalDescription(sdp: SessionDescription) =
    suspendCancellableCoroutine<Unit> { cont ->
        this.setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() { cont.resume(Unit) }
            override fun onSetFailure(p0: String?) { cont.cancel(RuntimeException(p0)) }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

suspend fun PeerConnection.setRemoteDescription(sdp: SessionDescription) =
    suspendCancellableCoroutine<Unit> { cont ->
        this.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { cont.resume(Unit) }
            override fun onSetFailure(p0: String?) { cont.cancel(RuntimeException(p0)) }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }