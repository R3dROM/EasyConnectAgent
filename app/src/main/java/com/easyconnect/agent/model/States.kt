package com.easyconnect.agent.model

import kotlinx.serialization.Serializable

enum class ActivityType
{
    StartExperience,
    UninstallExperience,
    StartUpdate
}
enum class DeploymentState
{
    Download,
    Move,
    Install,
    Uninstall,
    Complete,
    Cancel,
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
    Offline,
    Updating
}
enum class MessageType
{
    Register,
    Deployment,
    Battery,
    Heartbeat,
    Acknowledge,
    Update
}
@Serializable
enum class CommandType
{
    Deployment,
    Connection,
    Activity,
    Cancellation
}