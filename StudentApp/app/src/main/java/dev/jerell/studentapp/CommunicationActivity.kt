package dev.jerell.studentapp.communication

import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.jerell.studentapp.R
import dev.jerell.studentapp.chatlist.ChatListAdapter
import dev.jerell.studentapp.models.ContentModel
import dev.jerell.studentapp.models.MessageType
import dev.jerell.studentapp.network.Client
import dev.jerell.studentapp.network.NetworkMessageInterface
import dev.jerell.studentapp.peerList.PeerListAdapter
import dev.jerell.studentapp.peerList.PeerListAdapterInterface
import dev.jerell.studentapp.wifidirect.WifiDirectManager
import dev.jerell.studentapp.wifidirect.WifiDirectInterface
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class CommunicationActivity : AppCompatActivity(), NetworkMessageInterface, WifiDirectInterface, PeerListAdapterInterface {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var studentIdInput: EditText
    private lateinit var searchButton: Button
    private lateinit var sendButton: Button
    private lateinit var messageInput: EditText
    private lateinit var connectedLayout: LinearLayout
    private lateinit var notConnectedLayout: LinearLayout
    private lateinit var wifiAdapterOffLayout: LinearLayout
    private lateinit var classTitle: TextView
    private lateinit var peerRecyclerView: RecyclerView

    private lateinit var peerListAdapter: PeerListAdapter

    private lateinit var client: Client
    private lateinit var wifiDirectManager: WifiDirectManager

    private val lecturerIp = "192.168.49.1"  // Default IP for lecturer
    private var studentId: String = ""  // Student ID will be input by the user
    private lateinit var secretKey: SecretKey
    private var isWifiP2pEnabled = false
    private var isConnectedToLecturer = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)

        // Initialize UI components using findViewById
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        studentIdInput = findViewById(R.id.studentIdInput)
        searchButton = findViewById(R.id.searchButton)
        sendButton = findViewById(R.id.sendButton)
        messageInput = findViewById(R.id.messageInput)
        connectedLayout = findViewById(R.id.connectedLayout)
        notConnectedLayout = findViewById(R.id.notConnectedLayout)
        wifiAdapterOffLayout = findViewById(R.id.wifiAdapterOffLayout)
        classTitle = findViewById(R.id.classTitle)
        peerRecyclerView = findViewById(R.id.peerRecyclerView)

        // Setup the chat interface and adapter
        chatListAdapter = ChatListAdapter()
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatListAdapter

        // Initialize Peer List Adapter for displaying nearby peers
        peerListAdapter = PeerListAdapter(this)
        peerRecyclerView.layoutManager = LinearLayoutManager(this)
        peerRecyclerView.adapter = peerListAdapter

        // Initialize WiFi Direct manager
        val manager: WifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wifiDirectManager = WifiDirectManager(manager, channel, this)

        // Register WiFi Direct events
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(wifiDirectManager, intentFilter)

        // Handle button clicks for searching for classes (peers)
        searchButton.setOnClickListener {
            studentId = studentIdInput.text.toString()

            if (studentId.isBlank()) {
                Toast.makeText(this, "Please enter a valid student ID", Toast.LENGTH_SHORT).show()
            } else {
                // Start peer discovery process
                wifiDirectManager.discoverPeers()
            }
        }

        // Send chat message to lecturer when connected
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString()

            if (messageText.isNotEmpty()) {
                val message = ContentModel(
                    messageType = MessageType.CHAT,
                    studentId = studentId,
                    studentName = "John Doe", // Replace with actual student name if available
                    message = messageText,
                    senderIp = "192.168.49.2", // Replace with actual sender IP if needed
                    timestamp = System.currentTimeMillis()
                )
                client.sendMessage(message)

                // Add the message to the chat list and clear the input field
                chatListAdapter.addItemToEnd(message)
                messageInput.text.clear()
            }
        }

        // Initialize the client (assumes the lecturer is GO and at a fixed IP)
        client = Client(this)
    }

    // Handle authentication (Challenge-Response)
    private fun authenticateStudent() {
        // Step 1: Send "I am here" message to lecturer
        val initialMessage = ContentModel(
            messageType = MessageType.AUTH,
            studentId = studentId,
            senderIp = lecturerIp,
            timestamp = System.currentTimeMillis(),
            message = "I am here"
        )
        client.sendMessage(initialMessage)

        // Simulate receiving a random number from the lecturer
        val randomNumber = Random.nextInt(100000, 999999).toString()  // Random number generation
        onContentReceived(ContentModel(
            messageType = MessageType.AUTH,
            studentId = studentId,
            senderIp = lecturerIp,
            timestamp = System.currentTimeMillis(),
            message = randomNumber
        ))
    }

    override fun onContentReceived(content: ContentModel) {
        if (content.messageType == MessageType.AUTH) {
            // Lecturer sends the random number (R)
            val randomR = content.message?.toIntOrNull()
            if (randomR != null) {
                Log.d("CommunicationActivity", "Received random number: $randomR")

                // Step 3: Student replies with the encrypted value
                val hashStudentId = hashStudentId(studentId)
                val encryptedR = encrypt(randomR, hashStudentId)

                val authResponse = ContentModel(
                    messageType = MessageType.AUTH,
                    studentId = studentId,
                    senderIp = lecturerIp,
                    timestamp = System.currentTimeMillis(),
                    authChallenge = randomR.toString(),
                    authResponse = encryptedR
                )
                client.sendMessage(authResponse)
            }
        } else {
            // Handle normal messages (chat, feedback, etc.)
            runOnUiThread {
                chatListAdapter.addItemToEnd(content)
                chatRecyclerView.scrollToPosition(chatListAdapter.itemCount - 1)
            }
        }
    }

    // Hash the student ID using SHA-256
    private fun hashStudentId(studentId: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashedBytes = digest.digest(studentId.toByteArray())
            Base64.encodeToString(hashedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            Log.e("CommunicationActivity", "Hashing error: ${e.message}")
            studentId // Fallback to original ID in case of error
        }
    }

    // Encrypt the random number (R) with the hashed student ID
    private fun encrypt(value: Int, hash: String): String {
        return try {
            // Generate a secret key from the hashed student ID
            secretKey = SecretKeySpec(hash.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Encrypt the value
            val encryptedBytes = cipher.doFinal(value.toString().toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("CommunicationActivity", "Encryption error: ${e.message}")
            value.toString()  // Fallback to original value in case of error
        }
    }

    // Update UI based on WiFi Direct connection state
    fun updateUI() {
        wifiAdapterOffLayout.visibility = if (!isWifiP2pEnabled) View.VISIBLE else View.GONE
        notConnectedLayout.visibility = if (isWifiP2pEnabled && !isConnectedToLecturer) View.VISIBLE else View.GONE
        connectedLayout.visibility = if (isConnectedToLecturer) View.VISIBLE else View.GONE

        if (isConnectedToLecturer) {
            classTitle.text = "Connected to Class: [Class Name]"  // Replace with actual class name
        }
    }

    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        isWifiP2pEnabled = isEnabled
        updateUI()
    }

    override fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>) {
        // Update peer list
        peerListAdapter.updateList(deviceList)
        Toast.makeText(this, "Peer list updated", Toast.LENGTH_SHORT).show()
    }

    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?) {
        // Handle group status changes
        isConnectedToLecturer = groupInfo?.isGroupOwner == true
        updateUI()
    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        // Handle device status changes
        Log.d("CommunicationActivity", "Device status changed: ${thisDevice.deviceName}")
    }

    override fun onPeerClicked(peer: WifiP2pDevice) {
        // Handle peer clicked event (to connect to the lecturer)
        wifiDirectManager.connectToPeer(peer)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiDirectManager)  // Unregister the receiver when activity is destroyed
    }
}





