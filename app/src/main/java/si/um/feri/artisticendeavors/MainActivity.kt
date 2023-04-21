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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.databinding.ActivityMainBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val user = auth.currentUser

    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private val imageName = "${user?.displayName}.jpg"
    private val imageRef = storageRef.child(imageName)

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        imageRef.downloadUrl
            .addOnSuccessListener { uri ->
                // Use Picasso to load the image into the ImageView
                Picasso.get().load(uri).into(binding.profpic)
            }
            .addOnFailureListener { exception ->
                // Handle any errors that may occur
                Timber.e("TAG", "Error downloading image: $exception")
            }

        val listener = ImageDecoder.OnHeaderDecodedListener { decoder, _, _ ->
            // Scale the image to prevent using too much memory
            decoder.setTargetSize(100, 100)
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { imageUri ->
                val bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(contentResolver, imageUri),
                    listener
                )
                binding.profpic.setImageBitmap(bitmap)

                // Upload the image to Firebase Storage
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                val uploadTask = imageRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl.toString()
                    Timber.d("Image uploaded successfully: $downloadUrl")
                }.addOnFailureListener { exception ->
                    // Image upload failed
                    Timber.e("Image upload failed", exception)
                }
            }
        }

        binding.change.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.usern.text = user?.displayName
        Picasso.get().load(user?.photoUrl).into(binding.profpic)

        binding.actionSignout.setOnClickListener {
            auth.signOut()
            val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove("userId")
            editor.remove("token")
            editor.apply()

            Toast.makeText(
                this@MainActivity,
                "You logged out successfully.",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }
}