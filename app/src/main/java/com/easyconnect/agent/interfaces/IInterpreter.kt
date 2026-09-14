package com.easyconnect.agent.interfaces

import com.easyconnect.agent.commandManager.Command

interface IInterpreter {
    fun decodeToCommand(input: String)
    fun publishCommand(command: Command)
}