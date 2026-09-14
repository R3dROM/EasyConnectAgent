package com.easyconnect.agent.interfaces

interface IDeployProcess {
    suspend fun start() : Boolean
    fun shutdown()
    suspend fun sendReport()
}