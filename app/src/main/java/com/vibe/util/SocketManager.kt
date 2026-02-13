package com.vibe.util

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object SocketManager {
    private var socket: Socket? = null
    private const val TAG = "SocketManager"
    private var currentRoomId: String? = null

    fun init(context: Context) {
        if (socket != null) return

        try {
            val baseUrl = com.vibe.BuildConfig.CONFIG_BASE_URL
            val opts = IO.Options()
            opts.reconnection = true
            socket = IO.socket(baseUrl, opts)
            
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to Socket")
            }

            socket?.on("receive_message") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val msg = data.optString("message")
                    val senderName = data.optString("senderName", "User")
                    val roomId = data.optString("roomId")
                    
                    // Basic duplicate check or simple notification logic
                    if (roomId != currentRoomId) {
                        NotificationHelper.showNotification(context, senderName, msg)
                    }
                }
            }
            
            socket?.connect()

        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun getSocket(): Socket? {
        return socket
    }
    
    fun setCurrentRoom(roomId: String?) {
        currentRoomId = roomId
        if (roomId != null) {
            socket?.emit("join_room", roomId)
        }
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
}