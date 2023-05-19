package si.um.feri.artisticendeavors

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.UUID

class Photoshop(private val context: Context) {
    private val tag = "Photoshop"

    fun loadProfileImage(downloadUrl: String, profileImage: ImageView?) {
        // Load profile image
        try {
            // Use Picasso to load the image into the ImageView
            Picasso.get().load(downloadUrl).into(profileImage)
        } catch (e: Exception) {
            Timber.e(tag, e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun handleImageSelection(imageUri: Uri, furtherAction: (String) -> Unit) {
        val contentResolver = context.contentResolver

        // Convert image into Bitmap
        val bitmap = ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, imageUri)
        )

        // Convert the Bitmap to ByteArray
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        // Upload the image to Firebase Storage
        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            // Image uploaded successfully
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                val message = context.getString(R.string.image_uploaded_successfully, downloadUrl)
                Timber.d(tag, message)

                // Perform further actions
                furtherAction(downloadUrl)
            }.addOnFailureListener { exception ->
                val errorMessage = context.getString(R.string.error_getting_download_url, exception)
                Timber.e(tag, errorMessage)
            }
        }.addOnFailureListener { exception ->
            // Image upload failed
            val errorMessage = context.getString(R.string.image_upload_failed, exception)
            Timber.e(tag, errorMessage)
        }
    }
}