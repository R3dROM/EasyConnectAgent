package com.easyconnect.agent.interfaces

import com.easyconnect.agent.commandManager.Command
import com.easyconnect.agent.network.report.Report

interface ICommunicator {
    suspend fun publishReport(report: Report)
    suspend fun publishCommand(command: Command)
    suspend fun receiveReport(): Report
    suspend fun receiveCommand(): Command
}