package com.easyconnect.agent
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