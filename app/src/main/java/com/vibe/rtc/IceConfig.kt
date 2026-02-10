package com.vibe.rtc

import com.vibe.BuildConfig

object IceConfig {
    fun iceServers(): List<org.webrtc.PeerConnection.IceServer> {
        // TURN servers help with NAT traversal; replace with production credentials
        val list = mutableListOf<org.webrtc.PeerConnection.IceServer>()
        for (url in BuildConfig.TURN_URLS.split(";")) {
            if (url.isNotBlank()) {
                list.add(org.webrtc.PeerConnection.IceServer.builder(url)
                    .setUsername(BuildConfig.TURN_USERNAME)
                    .setPassword(BuildConfig.TURN_PASSWORD)
                    .createIceServer())
            }
        }
        // Add free STUN as fallback (no auth required)
        list.add(org.webrtc.PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        return list
    }
}