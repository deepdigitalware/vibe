package com.vibe.rtc

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.vibe.signaling.FirestoreSignaling
import com.vibe.signaling.SignalingService
import com.vibe.util.createAnswer
import com.vibe.util.createOffer
import com.vibe.util.setLocalDescription
import com.vibe.util.setRemoteDescription
import org.webrtc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Minimal local preview using Google WebRTC to demonstrate camera/mic on emulator/device.
 * Replace signaling/peer connection for real 1:1.
 */
class RtcClient(private val context: Context) {

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var signaling: SignalingService? = null

    // Reconnection state
    private var currentRole: SignalingService.Role? = null
    private var currentRoomId: String? = null
    private var reconnectAttempts: Int = 0
    private val maxReconnectAttempts: Int = 3

    fun startPreview(localContainerId: Int, remoteContainerId: Int) {
        // Init WebRTC components
        if (peerConnectionFactory == null) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )

            eglBase = EglBase.create()

            val encoderFactory = DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()
        }

        val activity = context as? Activity ?: return
        val localContainer: FrameLayout = activity.findViewById(localContainerId)
        val remoteContainer: FrameLayout = activity.findViewById(remoteContainerId)

        // Create and attach local renderer view
        localRenderer = SurfaceViewRenderer(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            init(eglBase!!.eglBaseContext, null)
            setMirror(true)
        }
        localContainer.removeAllViews()
        localContainer.addView(localRenderer)

        // Setup camera capturer
        val cameraEnumerator = Camera2Enumerator(activity)
        val deviceNames = cameraEnumerator.deviceNames
        var capturer: VideoCapturer? = null
        // Prefer front-facing camera
        for (name in deviceNames) {
            if (cameraEnumerator.isFrontFacing(name)) {
                capturer = cameraEnumerator.createCapturer(name, null)
                if (capturer != null) break
            }
        }
        if (capturer == null) {
            // Fallback to any camera
            for (name in deviceNames) {
                if (!cameraEnumerator.isFrontFacing(name)) {
                    capturer = cameraEnumerator.createCapturer(name, null)
                    if (capturer != null) break
                }
            }
        }
        videoCapturer = capturer

        val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase!!.eglBaseContext)
        videoSource = peerConnectionFactory!!.createVideoSource(false)
        videoCapturer?.initialize(surfaceHelper, activity, videoSource!!.capturerObserver)

        // Start capture (choose sensible defaults for emulator/device)
        try {
            videoCapturer?.startCapture(1280, 720, 30)
        } catch (e: Exception) {
            // Lower resolution fallback
            videoCapturer?.startCapture(640, 480, 24)
        }

        videoTrack = peerConnectionFactory!!.createVideoTrack("LOCAL_VIDEO", videoSource)
        videoTrack?.addSink(localRenderer)

        // Create audio
        audioSource = peerConnectionFactory!!.createAudioSource(MediaConstraints())
        audioTrack = peerConnectionFactory!!.createAudioTrack("LOCAL_AUDIO", audioSource)

        // Remote container placeholder (in real implementation, attach remote renderer here)
        remoteContainer.removeAllViews()
        remoteRenderer = SurfaceViewRenderer(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            init(eglBase!!.eglBaseContext, null)
            setMirror(false)
        }
        remoteContainer.addView(remoteRenderer)
        // No remote track yet — to be wired with signaling/peer connection.
    }

    private fun ensurePeerConnection(role: SignalingService.Role, roomId: String) {
        if (peerConnection != null) return
        val activity = context as? Activity ?: return
        val iceServers = mutableListOf<PeerConnection.IceServer>()
        // Add your TURN servers here
        // iceServers.add(PeerConnection.IceServer.builder("turn:your-turn-server.com")
        //     .setUsername("user")
        //     .setPassword("password")
        //     .createIceServer())

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        signaling = FirestoreSignaling()
        peerConnection = peerConnectionFactory!!.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                signaling?.sendIceCandidate(roomId, role, candidate)
            }
            override fun onTrack(transceiver: RtpTransceiver) {
                (transceiver.receiver.track() as? VideoTrack)?.addSink(remoteRenderer)
            }
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                when (newState) {
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> attemptReconnect()
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                if (newState == PeerConnection.PeerConnectionState.FAILED || newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    attemptReconnect()
                }
            }
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        })

        // Add local media to peer
        val localVideo = videoTrack ?: return
        peerConnection!!.addTransceiver(localVideo, RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV))
        peerConnection!!.addTrack(audioTrack)

        // Track session
        currentRole = role
        currentRoomId = roomId
    }

    fun startAsCaller(localContainerId: Int, remoteContainerId: Int, roomId: String) {
        startPreview(localContainerId, remoteContainerId)
        ensurePeerConnection(SignalingService.Role.Caller, roomId)
        val pc = peerConnection ?: return

        GlobalScope.launch(Dispatchers.Main) {
            val offer = pc.createOffer(MediaConstraints())
            pc.setLocalDescription(offer)

            // Save offer and listen for answer
            signaling?.createRoom(roomId, offer.description)
            signaling?.observeAnswer(roomId) { sdp ->
                val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                GlobalScope.launch(Dispatchers.Main) {
                    pc.setRemoteDescription(answer)
                }
            }

            // Listen for callee ICE
            signaling?.observeRemoteIce(roomId, SignalingService.Role.Caller) { cand ->
                pc.addIceCandidate(cand)
            }
        }
    }

    fun startAsCallee(localContainerId: Int, remoteContainerId: Int, roomId: String) {
        startPreview(localContainerId, remoteContainerId)
        ensurePeerConnection(SignalingService.Role.Callee, roomId)
        val pc = peerConnection ?: return

        GlobalScope.launch(Dispatchers.Main) {
            val offerSdp = signaling?.getOffer(roomId)
            if (offerSdp.isNullOrEmpty()) return@launch
            pc.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offerSdp))
            val answer = pc.createAnswer(MediaConstraints())
            pc.setLocalDescription(answer)
            signaling?.setAnswer(roomId, answer.description)

            // Listen for caller ICE
            signaling?.observeRemoteIce(roomId, SignalingService.Role.Callee) { cand ->
                pc.addIceCandidate(cand)
            }
        }
    }

    private fun attemptReconnect() {
        val pc = peerConnection ?: return
        val role = currentRole ?: return
        val roomId = currentRoomId ?: return
        if (reconnectAttempts >= maxReconnectAttempts) return
        reconnectAttempts += 1

        // Restart ICE and renegotiate offer/answer
        pc.restartIce()
        GlobalScope.launch(Dispatchers.Main) {
            try {
                when (role) {
                    SignalingService.Role.Caller -> {
                        val offer = pc.createOffer(MediaConstraints())
                        pc.setLocalDescription(offer)
                        signaling?.createRoom(roomId, offer.description)
                    }
                    SignalingService.Role.Callee -> {
                        val offerSdp = signaling?.getOffer(roomId)
                        if (!offerSdp.isNullOrEmpty()) {
                            pc.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offerSdp))
                            val answer = pc.createAnswer(MediaConstraints())
                            pc.setLocalDescription(answer)
                            signaling?.setAnswer(roomId, answer.description)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun endCall() {
        reconnectAttempts = 0
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) { }
        videoCapturer?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        localRenderer?.release()
        remoteRenderer?.release()
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory?.dispose()
        eglBase?.release()

        videoCapturer = null
        videoSource = null
        videoTrack = null
        audioSource = null
        audioTrack = null
        localRenderer = null
        remoteRenderer = null
        peerConnectionFactory = null
        eglBase = null
    }
}