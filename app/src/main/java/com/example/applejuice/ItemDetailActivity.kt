package com.example.applejuice

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.animation.core.Transition
import androidx.lifecycle.lifecycleScope
import com.android.car.ui.toolbar.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.material.button.MaterialButton
import java.io.File

class ItemDetailActivity : AppCompatActivity() {
    private lateinit var item: Item

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        val itemId = intent.getSerializableExtra(EXTRA_ITEM_ID) as UUID

        lifecycleScope.launch {
            item = withContext(Dispatchers.IO) {
                ItemDatabase.get(applicationContext).itemDao().getItem(itemId)
            } ?: return@launch

            findViewById<TextView>(R.id.item_title).text = item.title
            findViewById<TextView>(R.id.item_description).text = item.description
            findViewById<TextView>(R.id.item_category).text = getString(R.string.category_format, item.category)
            findViewById<TextView>(R.id.item_date).text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(item.date)

            loadItemPhoto(item.photoUri)

            val photoView = findViewById<ImageView>(R.id.item_photo)
            item.photoUri?.let { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    val file = File(uri.path)
                    if (file.exists()) {
                        Glide.with(this@ItemDetailActivity)
                            .load(file)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.ic_error)
                            .into(photoView)
                    } else {
                        Log.e("ItemDetailActivity", "File does not exist: $uriString")
                        photoView.setImageResource(R.drawable.ic_error)
                    }
                } catch (e: Exception) {
                    Log.e("ItemDetailActivity", "Error loading image: $uriString", e)
                    photoView.setImageResource(R.drawable.ic_error)
                }
            } ?: run {
                Log.d("ItemDetailActivity", "No photo URI available")
                photoView.setImageResource(R.drawable.ic_placeholder)
            }

            findViewById<MaterialButton>(R.id.delete_button).setOnClickListener {
                showDeleteConfirmationDialog()
            }
        }
    }



    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_item_title))
            .setMessage(getString(R.string.delete_item_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                deleteItem()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }


    private fun deleteItem() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ItemDatabase.get(applicationContext).itemDao().deleteItem(item)
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "com.example.applejuice.item_id"
    }

    private fun loadItemPhoto(photoUri: String?) {
        val photoView = findViewById<ImageView>(R.id.item_photo)
        Log.d("ItemDetailActivity", "Attempting to load photo with URI: $photoUri")

        if (!photoUri.isNullOrEmpty()) {
            try {
                val file = File(filesDir, photoUri)
                if (file.exists()) {
                    Glide.with(this@ItemDetailActivity)
                        .load(file)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                            ) {
                                photoView.setImageDrawable(resource)
                                Log.d("ItemDetailActivity", "Glide load successful for file: ${file.absolutePath}")
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                // This method must be implemented, but we don't need to do anything here
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                super.onLoadFailed(errorDrawable)
                                Log.e("ItemDetailActivity", "Glide load failed for file: ${file.absolutePath}")
                            }
                        })
                } else {
                    Log.e("ItemDetailActivity", "Image file does not exist: ${file.absolutePath}")
                    photoView.setImageResource(R.drawable.ic_error)
                }
            } catch (e: Exception) {
                Log.e("ItemDetailActivity", "Error loading image: $photoUri", e)
                photoView.setImageResource(R.drawable.ic_error)
            }
        } else {
            Log.d("ItemDetailActivity", "No photo URI available")
            photoView.setImageResource(R.drawable.ic_placeholder)
        }
    }

}