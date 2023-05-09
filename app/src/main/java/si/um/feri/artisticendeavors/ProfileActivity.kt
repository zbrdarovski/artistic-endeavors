package si.um.feri.artisticendeavors

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.text.InputType
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

    @RequiresApi(Build.VERSION_CODES.P)
    val listener = ImageDecoder.OnHeaderDecodedListener { decoder, _, _ ->
        // Scale the image to prevent using too much memory
        decoder.setTargetSize(100, 100)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            // Convert image into Bitmap
            val bitmap = ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(contentResolver, imageUri),
                listener
            )

            // Convert the Bitmap to ByteArray
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

            // Upload the image to Firebase Storage
            val uploadTask = imageRef.putBytes(data)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                // Image uploaded successfully
                downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl.toString()
                Timber.d("Image uploaded successfully: $downloadUrl")
            }.addOnFailureListener { exception ->
                // Image upload failed
                Timber.e(exception, "Image upload failed")
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
            builder.setTitle("Add new post")

            // Set up the input
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            // Set up the buttons
            builder.setPositiveButton("Post") { _, _ ->
                val postText = input.text.toString().trim()
                if (postText.isNotEmpty()) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        // Choose image from gallery
                        galleryLauncher.launch("image/*")
                        val newPost = Post(
                            id = null,
                            user = currentUser.displayName?.let { it1 ->
                                User(
                                    username = it1
                                )
                            },
                            description = postText,
                            creation_time_milliseconds = System.currentTimeMillis(),
                            image_url = null // Set default value
                        )
                        db.collection("posts")
                            .add(newPost)
                            .addOnSuccessListener { documentReference ->
                                Timber.d("DocumentSnapshot written with ID: ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Error adding document")
                            }
                    }
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        }
    }

        override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        listenerRegistration.remove()
    }
}
