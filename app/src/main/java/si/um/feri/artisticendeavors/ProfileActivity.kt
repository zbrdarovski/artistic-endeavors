package si.um.feri.artisticendeavors

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import si.um.feri.artisticendeavors.databinding.ActivityProfileBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val user = this.auth.currentUser

    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private var i: Int = 0

    private val posts = mutableListOf<Post>()

    private lateinit var postAdapter: PostAdapter

    // If the user is logged out, go back to the LoginActivity
    private var authStateListener =
        AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postAdapter = PostAdapter(posts)
        binding.rvContacts.adapter = postAdapter
        binding.rvContacts.layoutManager = LinearLayoutManager(this)

        loadPostsFromFirebase()

        // Switch to MainActivity
        binding.homeOption.setOnClickListener {
            val intent = Intent(this@ProfileActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Switch to ProfileActivity
        binding.profileOption.setOnClickListener {
            val intent = Intent(this@ProfileActivity, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Switch to AccountActivity
        binding.accountOption.setOnClickListener {
            val intent = Intent(this@ProfileActivity, AccountActivity::class.java)
            startActivity(intent)
        }

        // Sign out from the app
        binding.actionSignout.setOnClickListener {
            this.auth.signOut()

            Toast.makeText(
                this@ProfileActivity,
                "You logged out successfully.",
                Toast.LENGTH_SHORT
            ).show()
        }

        val listener = ImageDecoder.OnHeaderDecodedListener { decoder, _, _ ->
            // Scale the image to prevent using too much memory
            decoder.setTargetSize(100, 100)
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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

                // Upload the image to Firebase Storage
                val newImageName = "${user?.displayName}${++i}.jpg"
                val newImageRef = storageRef.child("users/${user?.displayName}/" + newImageName)
                val uploadTask = newImageRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl.toString()
                    Timber.d("Image uploaded successfully: $downloadUrl")

                    // Add the new post to the list and notify the adapter
                    val newPost = Post("desc", downloadUrl)
                    posts.add(newPost)
                    postAdapter.notifyItemInserted(posts.size - 1)
                    val intent = Intent(this@ProfileActivity, ProfileActivity::class.java)
                    startActivity(intent)
                }.addOnFailureListener { exception ->
                    // Image upload failed
                    Timber.e("Image upload failed", exception)
                }
            }
        }

        // Call function to open gallery
        binding.add.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun loadPostsFromFirebase() {
        val imagesRef = storageRef.child("users/${user?.displayName}/")
        imagesRef.listAll()
            .addOnSuccessListener { listResult ->
                for ((index, imageRef) in listResult.items.withIndex()) {
                    if (index == 0) continue // skip the 0th image
                    i++
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        val newPost = Post("desc", downloadUrl.toString())
                        posts.add(newPost)
                        postAdapter.notifyItemInserted(posts.size - 1)
                    }.addOnFailureListener { exception ->
                        // Handle any errors that may occur
                        Timber.e("Error getting download URL", exception)
                    }
                }
            }.addOnFailureListener { exception ->
                // Handle any errors that may occur
                Timber.e("Error listing images", exception)
            }
    }


    // Add state listener
    @Override
    override fun onStart() {
        super.onStart()
        this.auth.addAuthStateListener(authStateListener)
    }

    // Remove state listener
    @Override
    override fun onStop() {
        super.onStop()
        this.auth.removeAuthStateListener(authStateListener)
    }
}