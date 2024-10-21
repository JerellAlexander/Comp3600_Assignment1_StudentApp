package dev.jerell.studentapp.network



import android.util.Log
import dev.jerell.studentapp.models.ContentModel
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import kotlin.concurrent.thread

class Client(private val networkMessageInterface: NetworkMessageInterface) {

    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter

    // Lecturer's IP address (replace with dynamic value if needed)
    private val lecturerIp = "192.168.49.1"
    private val lecturerPort = 9999  // Ensure this matches the server port

    init {
        // Create a new thread to connect to the server (lecturer)
        thread {
            try {
                // Initialize the socket connection
                clientSocket = Socket(lecturerIp, lecturerPort)
                reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                writer = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))

                Log.d("Client", "Connected to the server at $lecturerIp:$lecturerPort")

                // Continuously listen for incoming messages
                while (!clientSocket.isClosed) {
                    val serverMessage = reader.readLine()
                    if (serverMessage != null) {
                        val content = Gson().fromJson(serverMessage, ContentModel::class.java)
                        networkMessageInterface.onContentReceived(content)  // Pass the received content to the interface
                    }
                }
            } catch (e: Exception) {
                Log.e("Client", "Error connecting to server", e)
            }
        }
    }

    // Method to send a message to the server
    fun sendMessage(content: ContentModel) {
        thread {
            try {
                val messageString = Gson().toJson(content)
                writer.write("$messageString\n")
                writer.flush()
            } catch (e: Exception) {
                Log.e("Client", "Error sending message to server", e)
            }
        }
    }

    // Method to close the client connection
    fun closeConnection() {
        try {
            clientSocket.close()
            Log.d("Client", "Connection closed")
        } catch (e: Exception) {
            Log.e("Client", "Error closing connection", e)
        }
    }
}
