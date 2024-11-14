package com.example.applejuice

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ItemAdapter(private var items: List<Item>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var filteredItems: List<Item> = items

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView? = view.findViewById(R.id.item_title)
        val dateTextView: TextView? = view.findViewById(R.id.item_date)
        val timeTextView: TextView? = view.findViewById(R.id.item_time)
        val photoImageView: ImageView? = view.findViewById(R.id.item_photo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.titleTextView?.text = item.title
        holder.dateTextView?.text = formatDate(item.date)
        holder.timeTextView?.text = formatTime(item.date)

        // Handle the photo
        holder.photoImageView?.let { imageView ->
            item.photoUri?.let { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    imageView.setImageURI(uri)
                } catch (e: Exception) {
                    // Handle the error, e.g., show a placeholder image
                    imageView.setImageResource(R.drawable.ic_launcher_foreground)
                    Log.e("ItemAdapter", "Error loading image", e)
                }
            } ?: run {
                // No photo URI, show a placeholder
                imageView.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ItemDetailActivity::class.java).apply {
                putExtra(ItemDetailActivity.EXTRA_ITEM_ID, item.id)
            }
            context.startActivity(intent)
        }
    }
    override fun getItemCount() = filteredItems.size

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }

    private fun formatTime(date: Date): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            items
        } else {
            items.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateItems(newItems: List<Item>) {
        items = newItems
        filter("") // Reset the filter
        notifyDataSetChanged()
    }
}