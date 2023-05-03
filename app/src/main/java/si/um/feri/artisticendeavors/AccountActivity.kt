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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.databinding.ActivityAccountBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream

class AccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: FirebaseFirestore

    private lateinit var imageRef: StorageReference

    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    private val storage = Firebase.storage
    private val storageRef = storage.reference

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = Firebase.firestore

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        db.collection("users").document(currentUserId!!)
            .get()
            .continueWith { userDocument ->
                val user = userDocument.result?.toObject(User::class.java)
                val username = user?.username
                binding.usern.text = username
                imageRef = storageRef.child("images/${username}.jpg")

                // Load profile image
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        // Use Picasso to load the image into the ImageView
                        Picasso.get().load(uri).into(binding.profpic)
                    }
                    .addOnFailureListener { exception ->
                        // Handle any errors that may occur
                        Timber.e("TAG", "Error downloading image: $exception")
                    }
            }

        // Switch to MainActivity
        binding.homeOption.setOnClickListener {
            val intent = Intent(this@AccountActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Switch to ProfileActivity
        binding.profileOption.setOnClickListener {
            val intent = Intent(this@AccountActivity, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Switch to AccountActivity
        binding.accountOption.setOnClickListener {
            val intent = Intent(this@AccountActivity, AccountActivity::class.java)
            startActivity(intent)
        }

        // Sign out from the app
        binding.actionSignOut.setOnClickListener {
            this.auth.signOut()
            val intent = Intent(this@AccountActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()

            Toast.makeText(
                this@AccountActivity,
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
                val uploadTask = imageRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl.toString()
                    Timber.d("Image uploaded successfully: $downloadUrl")
                    val intent = Intent(this@AccountActivity, AccountActivity::class.java)
                    startActivity(intent)
                }.addOnFailureListener { exception ->
                    // Image upload failed
                    Timber.e("Image upload failed", exception)
                }
            }
        }

        // Call function to open gallery
        binding.change.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }
}