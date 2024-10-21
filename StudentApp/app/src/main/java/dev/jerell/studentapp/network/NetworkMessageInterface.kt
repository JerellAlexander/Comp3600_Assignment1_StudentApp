package dev.jerell.studentapp.network



import dev.jerell.studentapp.models.ContentModel

interface NetworkMessageInterface {
    // Called when a message is received from the server (lecturer)
    fun onContentReceived(content: ContentModel)
}
