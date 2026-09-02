package com.easyconnect.agent.model

enum class DeploymentState
{
    Download,
    Move,
    Install,
    Uninstall,
    Complete
}
enum class AgentStages
{
    Boot,
    Connecting,
    Retrying,
    Connected,
    Rebooting,
    ShuttingDown
}
object AgentStagesManager
{
    var state: AgentStages = AgentStages.Boot
}
enum class MessageStatus
{
    Boot,
    Waiting,
    Connected,
    Downloading,
    Moving,
    Installing,
    Uninstalling,
    Complete,
    Cancel,
    Fail
}
enum class MessageType
{
    Register,
    Download,
    Battery,
    Heartbeat,
    Acknowledge
}