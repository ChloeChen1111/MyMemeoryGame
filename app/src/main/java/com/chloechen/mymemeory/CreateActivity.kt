package com.chloechen.mymemeory

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chloechen.mymemeory.models.BoardSize
import com.chloechen.mymemeory.utils.BitmapScaler
import com.chloechen.mymemeory.utils.EXTRA_BOARD_SIZE
import com.chloechen.mymemeory.utils.isPermissionGranted
import com.chloechen.mymemeory.utils.requestPermission
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CreateActivity"
        private const val READ_EXTERNAL_PHOTOS_CODE = 101
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE

        private const val PICK_PHOTO_CODE = 655

        private const val MIN_GAME_LENGTH = 3
        private const val MAZ_GAME_LENGTH = 14
    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private val chosenImageUris = mutableListOf<Uri>()
    private var numImagesRequired = -1

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"

//        btnSave.setOnClickListener {
//            saveDataToFirebase()
//        }
//        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAZ_GAME_LENGTH))
//
//        etGameName.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                btnSave.isEnabled = shouldEnableSaveButton()
//            }
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
        rvImagePicker.adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener {
            override fun onPlaceholderClicker() {
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    Log.i(ContentValues.TAG, "Permission is already granted.")
                    launchIntentForPhotos()
                }else{
                    Log.i(ContentValues.TAG, "Permission is NOTTT already granted.")
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })

        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            launchIntentForPhotos()
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            }else{
                Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data back from the launched activity, user likely canceled flow")
            return
        }
        Log.i(TAG, "onActivityResult")
        val selectedUri = data.data
        val clipData = data.clipData
        if (clipData != null) {
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        imagePickerAdapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }
    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) {
            return false
        }
        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_LENGTH) {
            return false
        }
        return true
    }
    private fun saveDataToFirebase() {
        Log.i(TAG, "Going to save data to Firebase")
        val customGameName = etGameName.text.toString().trim()
  //      firebaseAnalytics.logEvent("creation_save_attempt") {
//            param("game_name", customGameName)
//        }
//        btnSave.isEnabled = false
//        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
//            if (document != null && document.data != null) {
//                AlertDialog.Builder(this)
//                    .setTitle("Name taken")
//                    .setMessage("A game already exists with the name '$customGameName'. Please choose another")
//                    .setPositiveButton("OK", null)
//                    .show()
//                btnSave.isEnabled = true
//            } else {
//                handleImageUploading(customGameName)
//            }
//        }.addOnFailureListener {exception ->
//            Log.e(TAG, "Encountered error while saving memory game", exception)
//            Toast.makeText(this, "Encountered error while saving memory game", Toast.LENGTH_SHORT).show()
//        }

    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 260)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }




}