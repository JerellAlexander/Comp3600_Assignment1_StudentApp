package dev.jerell.studentapp.chatlist



import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.jerell.studentapp.R
import dev.jerell.studentapp.models.ContentModel

class ChatListAdapter : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    private val chatList: MutableList<ContentModel> = mutableListOf()

    // ViewHolder class to hold the message view
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val messageLayout: LinearLayout = itemView.findViewById(R.id.messageLayout)
    }

    // Inflate the layout for each chat message
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return ViewHolder(view)
    }

    // Bind the message data to the view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]

        // Align the message based on whether the sender is the lecturer or student
        if (chat.senderIp == "192.168.49.1") {  // Lecturer's IP
            holder.messageLayout.gravity = Gravity.START  // Align lecturer messages to the left
        } else {
            holder.messageLayout.gravity = Gravity.END  // Align student messages to the right
        }

        // Set the message text in the TextView
        holder.messageTextView.text = chat.message
    }

    // Return the number of messages in the list
    override fun getItemCount(): Int {
        return chatList.size
    }

    // Add a new message to the end of the list and refresh the RecyclerView
    fun addItemToEnd(content: ContentModel) {
        chatList.add(content)
        notifyItemInserted(chatList.size - 1)
    }

    // Clear the chat list (useful for resetting the chat)
    fun clearChat() {
        chatList.clear()
        notifyDataSetChanged()
    }
}
