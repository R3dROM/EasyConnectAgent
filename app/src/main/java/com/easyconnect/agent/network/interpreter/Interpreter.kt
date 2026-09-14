package com.easyconnect.agent.network.interpreter

import com.easyconnect.agent.data.queue.Communicator
import com.easyconnect.agent.interfaces.IInterpreter
import com.easyconnect.agent.commandManager.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object Interpreter: IInterpreter
{
    private val serviceScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )
    override fun decodeToCommand(input: String) {
        val inputToCommand = Json.decodeFromString<Command>(input)

        publishCommand(inputToCommand)
    }

    override fun publishCommand(command: Command) {
        serviceScope.launch {
            Communicator.publishCommand(command)
        }
    }
}