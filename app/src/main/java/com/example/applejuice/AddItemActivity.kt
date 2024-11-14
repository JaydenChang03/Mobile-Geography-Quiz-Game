package com.example.applejuice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AddItemActivity : AppCompatActivity() {

    private lateinit var titleField: EditText
    private lateinit var descriptionField: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var photoButton: Button
    private lateinit var photoView: ImageView
    private var photoUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            handleSelectedImage(uri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        titleField = findViewById(R.id.item_title)
        descriptionField = findViewById(R.id.item_description)
        categorySpinner = findViewById(R.id.item_category)
        datePicker = findViewById(R.id.item_date)
        timePicker = findViewById(R.id.item_time)
        photoButton = findViewById(R.id.item_camera)
        photoView = findViewById(R.id.item_photo)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        setupActionBar()

        setupActionBar()
        setupCategorySpinner()
        setupSaveButton()
        setupPhotoButton()
    }

    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("Uncategorized", "Work", "Personal", "Shopping", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setupSaveButton() {
        findViewById<Button>(R.id.item_save)?.setOnClickListener {
            val localUri = photoUri?.let { saveImageToInternalStorage(it) }

            val newItem = Item(
                title = titleField.text.toString(),
                description = descriptionField.text.toString(),
                category = categorySpinner.selectedItem.toString(),
                date = getDateTimeFromPickers(),
                photoUri = localUri
            )

            Log.d("AddItemActivity", "Saving item with photoUri: ${newItem.photoUri}")

            lifecycleScope.launch(Dispatchers.IO) {
                ItemDatabase.get(applicationContext).itemDao().addItem(newItem)
                finish()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            val outputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("AddItemActivity", "Image saved to internal storage: $fileName")
            fileName
        } catch (e: Exception) {
            Log.e("AddItemActivity", "Error saving image to internal storage", e)
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun setupPhotoButton() {
        photoButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            photoUri = uri
            photoView.setImageURI(uri)
            Log.d("AddItemActivity", "Image selected: $uri")
        } catch (e: Exception) {
            Log.e("AddItemActivity", "Error handling selected image", e)
            Toast.makeText(this, "Error selecting image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHOTO && resultCode == Activity.RESULT_OK) {
            photoUri = data?.data
            photoView.setImageURI(photoUri)
        }
    }

    private fun getDateTimeFromPickers(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(
            datePicker.year,
            datePicker.month,
            datePicker.dayOfMonth,
            timePicker.hour,
            timePicker.minute
        )
        return calendar.time
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


    companion object {
        private const val REQUEST_PHOTO = 2
    }
}