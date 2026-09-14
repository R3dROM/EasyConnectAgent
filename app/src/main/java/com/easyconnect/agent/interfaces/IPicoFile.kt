package com.easyconnect.agent.interfaces

interface IPicoFile {
    suspend fun copy(from: String, to: String): Int
}