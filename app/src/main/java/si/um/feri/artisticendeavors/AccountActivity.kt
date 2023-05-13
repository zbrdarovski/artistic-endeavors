package si.um.feri.artisticendeavors

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    private suspend fun deleteUserData(currentUsername: String) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        // Delete user's posts and images
        firestore.collection("posts")
            .whereEqualTo("user.username", currentUsername)
            .get().await()
            .forEach { document ->
                // Delete the post image
                val imageUrl = document.getString("image_url")
                if (imageUrl != null) {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().await()
                }

                // Delete the post document
                document.reference.delete().await()
            }

        // Delete user's profile picture
        val profilePicRef = storage.reference.child("images/$currentUsername.jpg")
        profilePicRef.delete().await()

        // Delete user's user document
        firestore.collection("users")
            .whereEqualTo("username", currentUsername)
            .get().await()
            .forEach { document ->
                document.reference.delete().await()
            }

        // Delete user's account
        auth.currentUser?.delete()?.await()
    }

    // Assuming this code is inside an Activity or Fragment
    @OptIn(DelicateCoroutinesApi::class)
    private fun deleteUserAccount() {
        val currentUsername = FirebaseAuth.getInstance().currentUser?.displayName

        if (currentUsername != null) {
            val blackColor = ContextCompat.getColor(this, R.color.black)
            val redColor = ContextCompat.getColor(this, R.color.red)

            val title = SpannableString("Delete Account").apply {
                setSpan(ForegroundColorSpan(blackColor), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val message = SpannableString("Are you sure you want to delete your account? This action cannot be undone.").apply {
                setSpan(ForegroundColorSpan(redColor), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Show a confirmation dialog to the user before proceeding
            val confirmationDialog = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    // Call the deleteUserData function and log the user out
                    GlobalScope.launch(Dispatchers.IO) {
                        deleteUserData(currentUsername)
                        FirebaseAuth.getInstance().signOut()
                        // Navigate to the login screen
                        startActivity(Intent(this@AccountActivity, LoginActivity::class.java))
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            confirmationDialog.show()
        }
    }

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
            finish()
        }

        // Switch to ProfileActivity
        binding.profileOption.setOnClickListener {
            val intent = Intent(this@AccountActivity, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Switch to AccountActivity
        binding.accountOption.setOnClickListener {
            val intent = Intent(this@AccountActivity, AccountActivity::class.java)
            startActivity(intent)
            finish()
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
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val data = byteArrayOutputStream.toByteArray()

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

        var startPass: Int
        var endPass: Int

        binding.showPass.setOnCheckedChangeListener { _, isChecked ->
            // checkbox status is changed from uncheck to checked.
            if (!isChecked) {
                // show password
                startPass = binding.password.selectionStart
                endPass = binding.password.selectionEnd
                binding.password.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.password.setSelection(startPass, endPass)
            } else {
                // hide password
                startPass = binding.password.selectionStart
                endPass = binding.password.selectionEnd
                binding.password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.password.setSelection(startPass, endPass)
            }
        }

        var startRepeat: Int
        var endRepeat: Int

        binding.showRepeatPass.setOnCheckedChangeListener { _, isChecked ->
            // checkbox status is changed from uncheck to checked.
            if (!isChecked) {
                // show repeat
                startRepeat = binding.repeat.selectionStart
                endRepeat = binding.repeat.selectionEnd
                binding.repeat.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.repeat.setSelection(startRepeat, endRepeat)
            } else {
                // hide repeat
                startRepeat = binding.repeat.selectionStart
                endRepeat = binding.repeat.selectionEnd
                binding.repeat.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.repeat.setSelection(startRepeat, endRepeat)
            }
        }

        // Reset password
        binding.reset.setOnClickListener {
            when {
                // Check whether password's field is empty
                TextUtils.isEmpty(binding.password.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@AccountActivity,
                        "Please enter new password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Check whether password repeat's field is empty
                TextUtils.isEmpty(binding.repeat.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@AccountActivity,
                        "Please repeat password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Double check password
                binding.password.text.toString() != binding.repeat.text.toString() -> {
                    Toast.makeText(
                        this@AccountActivity,
                        "Passwords don't match.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // If everything checks out, update password
                else -> {
                    val newPassword = binding.password.text.toString().trim { it <= ' ' }
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        // Re - authenticate the user with their current password
                        val credential = EmailAuthProvider.getCredential(user.email!!, newPassword)
                        user.reauthenticate(credential)
                            .addOnCompleteListener { reAuthTask ->
                                if (reAuthTask.isSuccessful) {
                                    // Password matches, don't update
                                    Toast.makeText(
                                        this@AccountActivity,
                                        "New password must be different from the old password.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // Password doesn't match, update password
                                    user.updatePassword(newPassword)
                                        .addOnCompleteListener { updateTask ->
                                            if (updateTask.isSuccessful) {
                                                binding.password.text.clear()
                                                binding.repeat.text.clear()
                                                Toast.makeText(
                                                    this@AccountActivity,
                                                    "Password updated successfully!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this@AccountActivity,
                                                    "Failed to update password.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                }
                            }
                    } else {
                        Toast.makeText(
                            this@AccountActivity,
                            "Failed to retrieve current user.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // Delete account
        binding.delete.setOnClickListener {
            deleteUserAccount()
        }
    }
}