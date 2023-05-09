package si.um.feri.artisticendeavors

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import si.um.feri.artisticendeavors.databinding.ActivityProfileBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.UUID

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: ProfilePostAdapter
    private lateinit var listenerRegistration: ListenerRegistration
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private lateinit var imageRef: StorageReference
    private var downloadUrl: String? = null
    private lateinit var postText: String

    @RequiresApi(Build.VERSION_CODES.P)
    val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    // Convert image into Bitmap
                    val bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(contentResolver, imageUri)
                    )

                    // Convert the Bitmap to ByteArray
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val data = byteArrayOutputStream.toByteArray()

                    imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

                    // Upload the image to Firebase Storage
                    binding.loadingPhoto.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE

                    val uploadTask = imageRef.putBytes(data)
                    uploadTask.addOnSuccessListener {
                        binding.loadingPhoto.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE
                        // Image uploaded successfully
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            downloadUrl = uri.toString()
                            Timber.d("Image uploaded successfully: $downloadUrl")

                            val currentUser = auth.currentUser
                            // Create a new post object with the image URL
                            val newPost = Post(
                                user = currentUser?.displayName?.let { it1 -> User(username = it1) },
                                description = postText,
                                creation_time_milliseconds = System.currentTimeMillis(),
                                image_url = downloadUrl
                            )

                            db.collection("posts")
                                .add(newPost)
                                .addOnSuccessListener { documentReference ->
                                    // Get the document ID and update the new post object with it
                                    val postId = documentReference.id
                                    newPost.id = postId

                                    // Update the post document with the generated ID
                                    db.collection("posts")
                                        .document(postId)
                                        .set(newPost)
                                        .addOnSuccessListener {
                                            // Log the document ID to verify that it was added correctly
                                            Timber.d("DocumentSnapshot written with ID: $postId")
                                        }
                                        .addOnFailureListener { e ->
                                            Timber.e(e, "Error updating document")
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Timber.e(e, "Error adding document")
                                }
                        }.addOnFailureListener { exception ->
                            Timber.e(exception, "Error getting download URL")
                        }
                    }.addOnFailureListener { exception ->
                        binding.loadingPhoto.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE
                        // Image upload failed
                        Timber.e(exception, "Image upload failed")
                    }.addOnProgressListener { taskSnapshot ->
                        // Update the progress bar
                        val progress =
                            (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                        binding.progressBar.progress = progress
                    }
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore
        posts = mutableListOf()
        adapter = ProfilePostAdapter(posts)

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        val currentUsername = auth.currentUser?.displayName

        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Timber.e(exception, exception?.message)
                return@addSnapshotListener
            }
            val listOfPosts = snapshot.toObjects(Post::class.java)
            val iterator = listOfPosts.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                if (p.user?.username != currentUsername) {
                    iterator.remove()
                }
            }
            if (listOfPosts.isEmpty()) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Unfortunately, there are currently no posts to display. :(",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                posts.clear()
                posts.addAll(listOfPosts)
                adapter.apply {
                    notifyItemRangeRemoved(0, itemCount)
                    notifyItemRangeInserted(0, posts.size)
                }
            }
        }

        // Add new post
        binding.fabCreate.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("New post description:")

            // Set up the input
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            // Set up the buttons
            builder.setPositiveButton("Post") { _, _ ->
                postText = input.text.toString().trim()
                if (postText.isNotEmpty()) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        // Launch the gallery
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "image/*"
                        galleryLauncher.launch(intent)
                    }
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        }

        // Switch to MainActivity
        binding.homeOption.setOnClickListener {
            val intent = Intent(this@ProfileActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Switch to ProfileActivity
        binding.profileOption.setOnClickListener {
            val intent = Intent(this@ProfileActivity, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Switch to AccountActivity
        binding.accountOption.setOnClickListener {
            val intent = Intent(this@ProfileActivity, AccountActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Sign out from the app
        binding.actionSignOut.setOnClickListener {
            this.auth.signOut()
            val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()

            Toast.makeText(
                this@ProfileActivity,
                "You logged out successfully.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        listenerRegistration.remove()
    }
}