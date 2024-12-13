package artisticendeavors.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import artisticendeavors.tools.ActivitySwitcher
import artisticendeavors.tools.Messenger
import artisticendeavors.R
import artisticendeavors.data.Post
import artisticendeavors.data.User
import artisticendeavors.databinding.ActivityAddPostBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.*

class AddPostActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val storage: FirebaseStorage by lazy { Firebase.storage }
    private val messenger by lazy { Messenger(this) }
    private lateinit var binding: ActivityAddPostBinding

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tag = "AddPostActivity"

        var downloadUrl = ""

        val galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val imageUri = result.data?.data
                    if (imageUri != null) {
                        if (downloadUrl != "") {
                            val storage = Firebase.storage
                            val stRef = storage.getReferenceFromUrl(downloadUrl)
                            stRef.delete().addOnSuccessListener {
                                // File deleted successfully
                                Timber.i(getString(R.string.image_deleted_successfully))
                            }.addOnFailureListener { exception ->
                                // An error occurred while deleting the file
                                // Handle the failure accordingly
                                Timber.e(
                                    getString(R.string.error_deleting_image),
                                    exception.message.toString()
                                )
                            }
                        }
                        // Convert image into Bitmap
                        val bitmap = ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(contentResolver, imageUri)
                        )
                        // Convert the Bitmap to ByteArray
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream
                        )
                        val data = byteArrayOutputStream.toByteArray()

                        // Upload the image to Firebase Storage with a random UUID filename
                        val storageRef = storage.reference
                        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
                        val uploadTask = imageRef.putBytes(data)

                        uploadTask.addOnSuccessListener {
                            // Image uploaded successfully
                            imageRef.downloadUrl.addOnSuccessListener { uri ->
                                binding.progress.visibility = View.GONE
                                downloadUrl = uri.toString()
                                binding.imageView.load(downloadUrl)
                                binding.sendButton.isEnabled = true
                                binding.imageView.isEnabled = true // Enable the imageView
                            }.addOnFailureListener { exception ->
                                val errorMessage = getString(
                                    R.string.error_getting_download_url, exception
                                )
                                Timber.e(tag, errorMessage)
                                binding.progress.visibility = View.GONE
                                binding.imageView.isEnabled = true
                            }
                        }.addOnFailureListener { exception ->
                            Timber.e(tag, exception.message)
                            binding.progress.visibility = View.GONE
                            binding.imageView.isEnabled = true
                        }
                    } else {
                        // Enable the imageView
                        binding.progress.visibility = View.GONE
                        binding.imageView.isEnabled = true
                    }
                } else {
                    // Enable the imageView
                    binding.progress.visibility = View.GONE
                    binding.imageView.isEnabled = true
                }
            }

        binding.sendButton.isEnabled = false

        binding.sendButton.setOnClickListener {
            val postText = binding.inputText.text.toString().trim()
            if (postText.isNotEmpty()) {
                val currentUser = auth.currentUser

                if (currentUser != null) {
                    val userId = currentUser.uid
                    val userRef = db.collection("users").document(userId)

                    userRef.get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val username = documentSnapshot.getString("username") ?: ""
                            val biography = documentSnapshot.getString("biography") ?: ""

                            val user = User(
                                username = username, biography = biography, id = userId
                            )

                            val newPost = Post(
                                user = user,
                                description = postText,
                                creation_time_milliseconds = System.currentTimeMillis(),
                                image_url = downloadUrl,
                                category = binding.categorySpinner.selectedItem.toString()
                            )

                            // Add the post to the database
                            db.collection("posts").add(newPost)
                                .addOnSuccessListener { documentReference ->
                                    val postId = documentReference.id
                                    newPost.id = postId
                                    db.collection("posts").document(postId).set(newPost)
                                        .addOnSuccessListener {
                                            // Log the document ID to verify that it was added correctly
                                            val errorMessage = getString(
                                                R.string.document_snapshot_written_with_id, postId
                                            )
                                            Timber.d(tag, errorMessage)
                                        }.addOnFailureListener { e ->
                                            val errorMessage =
                                                getString(R.string.error_updating_document, e)
                                            Timber.e(tag, errorMessage)
                                        }

                                    val activitySwitcher = ActivitySwitcher()
                                    activitySwitcher.startNewActivity(
                                        this, ProfileActivity::class.java
                                    )
                                }.addOnFailureListener { e ->
                                    val errorMessage = getString(R.string.error_adding_document, e)
                                    Timber.e(tag, errorMessage)
                                }

                        } else {
                            // Document doesn't exist, handle accordingly
                            messenger.message(getString(R.string.error_loading_user_data, ""))
                        }
                    }.addOnFailureListener { e ->
                        // Error retrieving user data, handle accordingly
                        val errorMessage = getString(R.string.error_getting_user_data, e)
                        Timber.e(tag, errorMessage)
                    }

                } else {
                    // Current user is null, handle accordingly
                    messenger.message(getString(R.string.option_sign_in))
                }
            } else {
                messenger.message(getString(R.string.please_enter_post_description))
            }
        }

        binding.cancelButton.setOnClickListener {
            if (binding.sendButton.isEnabled) {
                val storage = Firebase.storage
                val storageRef = storage.getReferenceFromUrl(downloadUrl)
                storageRef.delete().addOnSuccessListener {
                    // File deleted successfully
                    Timber.i(getString(R.string.image_deleted_successfully))
                }.addOnFailureListener { exception ->
                    // An error occurred while deleting the file
                    // Handle the failure accordingly
                    Timber.e(
                        getString(R.string.error_deleting_image), exception.message.toString()
                    )
                }
            }
            val activitySwitcher = ActivitySwitcher()
            activitySwitcher.startNewActivity(this, ProfileActivity::class.java)
        }

        binding.imageView.setOnClickListener {
            binding.imageView.isEnabled = false
            binding.progress.visibility = View.VISIBLE
            // Launch the gallery
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }
    }
}