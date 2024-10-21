package dev.jerell.studentapp.models

data class ContentModel(
    val messageType: MessageType,
    val studentId: String? = null,
    val studentName: String? = null,
    val attendanceStatus: Boolean? = null,
    val question: String? = null,
    val answer: String? = null,
    val authChallenge: String? = null,
    val authResponse: String? = null,
    val message: String? = null,
    val senderIp: String,
    val timestamp: Long
)
