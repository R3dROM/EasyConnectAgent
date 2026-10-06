package com.easyconnect.agent.model

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
    Receive,
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
    Hardware,
    Heartbeat,
    Acknowledge,
    Update
}
enum class JobType
{
    NoJob,
    StartExperience,
    UninstallExperience,
    Deployment,
    Connection,
    Activity,
    Cancellation
}