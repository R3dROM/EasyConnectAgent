package com.easyconnect.agent.model

import kotlinx.serialization.Serializable

enum class DeploymentState
{
    Download,
    Move,
    Install,
    Uninstall,
    Complete,
    Fail
}
enum class JobState
{
    Waiting,
    Executing,
    Complete,
    Cancel,
    Fail
}
enum class DeviceStatus
{
    Boot,
    Waiting,
    Online,
    Offline
}
enum class MessageType
{
    Register,
    Deployment,
    Battery,
    Heartbeat,
    StartExperience,
    Acknowledge
}
@Serializable
enum class CommandType
{
    Deployment,
    Websocket,
    Activity
}