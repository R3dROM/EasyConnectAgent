package com.easyconnect.agent.dependency

import android.content.Context
import com.easyconnect.agent.configuration.agent.AgentConfiguration
import com.easyconnect.agent.configuration.pico.PicoConfiguration
import com.easyconnect.agent.deployment.download.DownloadClass
import com.easyconnect.agent.deployment.install.InstallApk
import com.easyconnect.agent.deployment.movefiles.MoveFilesClass
import com.easyconnect.agent.interfaces.IAgentConfigurationReader
import com.easyconnect.agent.interfaces.IAgentConfigurationWriter
import com.easyconnect.agent.interfaces.IPicoConfigurationReader
import com.easyconnect.agent.interfaces.IPicoConfigurationWriter
import com.easyconnect.agent.network.AsyncUdpClient
import com.easyconnect.agent.network.websocket.Communicator
import com.flyfishxu.kadb.Kadb

object AgentDependencies {
    fun createMoveFilesClass(
        appContext: Context,
        adbClient: Kadb?,
        bundle: String
    ) : MoveFilesClass
    {
        return MoveFilesClass(
            appContext,
            adbClient,
            bundle
        )
    }
    fun createInstallClass(
        appContext: Context,
        adbClient: Kadb?
    ) : InstallApk
    {
        return InstallApk(
            appContext,
            adbClient
        )
    }
    fun createDownloadClass(
        url: String,
        jobId: Long,
        appContext: Context) : DownloadClass {
        return DownloadClass(
            url,
            jobId,
            appContext
        )
    }
    fun createWebSocketClass(
        url: String,
        jobId: Long,
        appContext: Context
    ): Communicator {
        return Communicator(
            url,
            jobId,
            appContext
        )
    }

    fun createAsyncUdpClass() = AsyncUdpClient()
    fun getAgentConfigurationWriter() : IAgentConfigurationWriter = AgentConfiguration
    fun getAgentConfigurationReader() : IAgentConfigurationReader = AgentConfiguration
    fun getPicoConfigurationWriter() : IPicoConfigurationWriter = PicoConfiguration
    fun getPicoConfigurationReader() : IPicoConfigurationReader = PicoConfiguration
}