package com.example.artbookkotlin

import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.artbookkotlin.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitmap: Bitmap? = null
    private lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)

        if (info == "new") {
            binding.artnameText.setText("")
            binding.artistnameText.setText("")
            binding.yearnameText.setText("")
            binding.saveButtonClicked.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.ic_launcher_background)
        } else {
            binding.saveButtonClicked.visibility = View.INVISIBLE
            val selectedID = intent.getIntExtra("id", 1)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedID.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                binding.artnameText.setText(cursor.getString(artNameIx))
                binding.artistnameText.setText(cursor.getString(artistNameIx))
                binding.yearnameText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }
    }

    fun saveButtonClicked(view: View) {
        val artName = binding.artnameText.text.toString()
        val artistName = binding.artistnameText.text.toString()
        val year = binding.yearnameText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")
                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)
                statement.execute()

                Toast.makeText(this, "Art saved successfully!", Toast.LENGTH_SHORT).show()

                // kayıt sonrası MainActivity'e dön
                val intent = Intent(this@ArtActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio: Double = width.toDouble() / height.toDouble()

        if (bitmapRatio > 1) {
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        } else {
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun selectImage(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view, "Permission needed for Gallery", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission") {
                            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                        }.show()
                } else {
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(view, "Permission needed for Gallery", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission") {
                            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }.show()
                } else {
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    val imageData = intentFromResult.data
                    if (imageData != null) {
                        try {
                            selectedBitmap = if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(this@ArtActivity.contentResolver, imageData)
                                ImageDecoder.decodeBitmap(source)
                            } else {
                                MediaStore.Images.Media.getBitmap(contentResolver, imageData)
                            }
                            binding.imageView.setImageBitmap(selectedBitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                Toast.makeText(this@ArtActivity, "Permission Needed", Toast.LENGTH_LONG).show()
            }
        }
    }
}
