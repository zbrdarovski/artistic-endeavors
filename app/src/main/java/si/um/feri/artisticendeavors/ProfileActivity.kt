package si.um.feri.artisticendeavors

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
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

        FirebaseApp.initializeApp(this)

        val imagesRef = storageRef.child("users/${user?.displayName}")
        imagesRef.listAll().addOnSuccessListener { result ->
            result.items.forEachIndexed { index, imageRef ->
                if (index > 0) {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Load the image into ImageView using Picasso
                        val imageView = findViewById<ImageView>(R.id.ivImage)
                        Picasso.get().load(uri).into(imageView)
                    }.addOnFailureListener { exception ->
                        // Handle error while fetching URL of the image
                        Timber.e("Failed to fetch URL of image", exception)
                    }
                }
            }
        }.addOnFailureListener { exception ->
            // Handle error while listing images
            Timber.e("Failed to list images", exception)
        }

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

        postAdapter = PostAdapter()
        binding.rvContacts.adapter = postAdapter
        binding.rvContacts.layoutManager = LinearLayoutManager(this)

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
                val newImageRef = storageRef.child("users/${user?.displayName}/${newImageName}")
                val uploadTask = newImageRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl.toString()
                    Timber.d("Image uploaded successfully: $downloadUrl")

                    // Add the new post to the list and notify the adapter
                    val newPost = Post(i, "desc", bitmap)
                    posts.add(newPost)
                    postAdapter.notifyDataSetChanged()
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