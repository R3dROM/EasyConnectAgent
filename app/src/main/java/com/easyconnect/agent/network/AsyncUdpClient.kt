package com.easyconnect.agent.network

import android.util.Log
import com.easyconnect.agent.interfaces.IUdpBroadcast
import com.easyconnect.agent.model.ServerUdpMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException

class AsyncUdpClient : IUdpBroadcast {
    override suspend fun startClient () : ServerUdpMessage?
    {
        return withContext(Dispatchers.IO)
        {
            val socket = DatagramSocket(11000)
            socket.soTimeout = 30000
            val buffer = ByteArray(100)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.use { socket ->
                while (isActive) {
                    try {
                        socket.receive(receivePacket)
                        val validData = receivePacket.data.copyOfRange(0, receivePacket.length)
                        Log.i(
                            "UDP_CLIENT",
                            "Packet receive: ${validData.decodeToString()} - from: ${receivePacket.address}")
                        val message = validData.decodeToString().let {
                            Json.decodeFromString<ServerUdpMessage>(it)
                        }
                        return@use message
                    } catch (e: SocketTimeoutException) {
                        // receive() superó el SO_TIMEOUT
                        Log.e("UDP_CLIENT","SocketTimeoutException: $e")
                    } catch (e: SocketException) {
                        // socket cerrado, dirección/puerto inválido,
                        // problema de red a nivel de socket, etc.
                        Log.e("UDP_CLIENT","SocketException: $e")
                    } catch (e: SecurityException) {
                        Log.e("UDP_CLIENT","SecurityException: $e")
                    } catch (e: IOException) {
                        // error de I/O
                        Log.e("UDP_CLIENT","IOException: $e")
                    }
                }
            } as ServerUdpMessage?
        }
    }
}