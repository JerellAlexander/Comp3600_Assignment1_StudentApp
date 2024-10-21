package dev.jerell.studentapp.peerList

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.jerell.studentapp.R

class PeerListAdapter(private val iFaceImpl: PeerListAdapterInterface) : RecyclerView.Adapter<PeerListAdapter.ViewHolder>() {

    private val peersList: MutableList<WifiP2pDevice> = mutableListOf()

    // ViewHolder class to hold peer item views
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nearbyDevicesHeader: TextView = itemView.findViewById(R.id.nearbyDevicesHeader)  // Header for "Nearby Devices"
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)  // Device Name
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)  // Device Address
    }

    // Inflate the layout for each peer item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.peer_item, parent, false)
        return ViewHolder(view)
    }

    // Bind peer data to the view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peer = peersList[position]

        // Show the header "Nearby Devices" only for the first item
        if (position == 0) {
            holder.nearbyDevicesHeader.visibility = View.VISIBLE
        } else {
            holder.nearbyDevicesHeader.visibility = View.GONE
        }

        // Set peer device name and address
        holder.titleTextView.text = peer.deviceName
        holder.descriptionTextView.text = peer.deviceAddress

        // Set click listener to handle peer selection
        holder.itemView.setOnClickListener {
            iFaceImpl.onPeerClicked(peer)
        }
    }

    override fun getItemCount(): Int {
        return peersList.size
    }

    // Update the peer list and notify the adapter of changes
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newPeersList: Collection<WifiP2pDevice>) {
        peersList.clear()
        peersList.addAll(newPeersList)
        notifyDataSetChanged()  // Notify the adapter to refresh the list
    }
}
