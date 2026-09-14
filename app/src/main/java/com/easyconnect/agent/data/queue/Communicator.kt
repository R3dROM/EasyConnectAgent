package com.easyconnect.agent.data.queue

import com.easyconnect.agent.interfaces.ICommunicator
import com.easyconnect.agent.commandManager.Command
import com.easyconnect.agent.network.report.Report
import kotlinx.coroutines.channels.Channel

object Communicator : ICommunicator {
    val reportQueue = Channel<Report>(
        Channel.UNLIMITED
    )
    val commandQueue = Channel<Command>(
        Channel.UNLIMITED
    )
    override suspend fun publishReport(report: Report)
    {
        reportQueue.send(report)
    }
    override suspend fun publishCommand(command: Command)
    {
        commandQueue.send(command)
    }
    override suspend fun receiveReport() : Report {
        return reportQueue.receive()
    }
    override suspend fun receiveCommand() : Command {
        return commandQueue.receive()
    }
}