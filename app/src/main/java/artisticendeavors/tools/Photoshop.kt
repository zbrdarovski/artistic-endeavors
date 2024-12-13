package artisticendeavors.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import coil.load
import com.google.firebase.storage.StorageReference
import artisticendeavors.R
import timber.log.Timber
import java.io.ByteArrayOutputStream

class Photoshop(
    private val context: Context,
    private val activityResultRegistry: ActivityResultRegistry
) {
    private val tag = "Photoshop"

    fun loadImage(imageReference: StorageReference, image: ImageView?) {
        imageReference.downloadUrl
            .addOnSuccessListener { uri ->
                // Use Picasso to load the image into the ImageView
                image?.load(uri)
            }
            .addOnFailureListener { exception ->
                // Handle any errors that may occur
                val errorMessage =
                    context.getString(R.string.error_downloading_image, exception)
                Timber.e(tag, errorMessage)
            }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private val listener = ImageDecoder.OnHeaderDecodedListener { decoder, _, _ ->
        // Scale the image to prevent using too much memory
        decoder.setTargetSize(100, 100)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun launch(
        imageReference: StorageReference,
        image: ImageView?
    ): ActivityResultLauncher<String> {
        val launcher = activityResultRegistry.register(
            "key", ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { imageUri ->
                // Convert image into Bitmap
                val bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, imageUri), listener
                )

                // Convert the Bitmap to ByteArray
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val data = byteArrayOutputStream.toByteArray()

                // Upload the image to Firebase Storage
                val uploadTask = imageReference.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl.toString()
                    Timber.d(
                        tag,
                        context.getString(R.string.image_uploaded_successfully, downloadUrl)
                    )
                    if (image != null) {
                        loadImage(imageReference, image)
                    }
                }.addOnFailureListener { exception ->
                    // Image upload failed
                    Timber.e(tag, R.string.image_upload_failed, exception)
                }
            }
        }

        return launcher
    }
}