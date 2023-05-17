package si.um.feri.artisticendeavors.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
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
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.data.User
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

    private val tag: String = "AccountActivity"

    // Function to check if the new password satisfies Firebase's criteria
    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun loadProfileImage() {
        // Load profile image
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            // Use Picasso to load the image into the ImageView
            Picasso.get().load(uri).into(binding.profpic)
        }.addOnFailureListener { exception ->
            // Handle any errors that may occur
            Timber.e(tag, "Error downloading image: $exception")
        }
    }

    private suspend fun deleteUserData(currentUsername: String) {
        val auth = FirebaseAuth.getInstance()
        val fs = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        // Delete user's posts and images
        fs.collection("posts").whereEqualTo("user.username", currentUsername).get().await()
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

        try {
            profilePicRef.delete().await()
        } catch (e: Exception) {
            // Handle the exception (e.g., log the error, show a toast, etc.)
            Timber.e(tag, "Failed to delete profile picture: ${e.message}")
        }

        // Delete user's user document
        fs.collection("users").whereEqualTo("username", currentUsername).get().await()
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
            val passwordEditText = EditText(this)
            passwordEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.hint = "Enter current password"

            val showHideButton = Button(this)
            showHideButton.text = getString(R.string.show)
            showHideButton.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
            showHideButton.setBackgroundResource(R.drawable.custom_button)
            showHideButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.teal_200)
            showHideButton.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }

            var isCurrentPasswordVisible = false
            var startCursor: Int
            var endCursor: Int

            showHideButton.setOnClickListener {
                if (isCurrentPasswordVisible) {
                    // Hide password
                    // Preserve the cursor position after showing/hiding password
                    startCursor = passwordEditText.selectionStart
                    endCursor = passwordEditText.selectionEnd
                    passwordEditText.transformationMethod =
                        PasswordTransformationMethod.getInstance()
                    passwordEditText.setSelection(startCursor, endCursor)
                    showHideButton.text = getString(R.string.show)
                    isCurrentPasswordVisible = !isCurrentPasswordVisible
                } else {
                    // Show password
                    // Preserve the cursor position after showing/hiding password
                    startCursor = passwordEditText.selectionStart
                    endCursor = passwordEditText.selectionEnd
                    passwordEditText.transformationMethod =
                        HideReturnsTransformationMethod.getInstance()
                    passwordEditText.setSelection(startCursor, endCursor)
                    showHideButton.text = getString(R.string.hide)
                    isCurrentPasswordVisible = !isCurrentPasswordVisible
                }
            }

            val passwordPromptLayout = LinearLayout(this)

            passwordPromptLayout.orientation = LinearLayout.HORIZONTAL

            // Set layout parameters for the password layout
            passwordPromptLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Set layout parameters for the passwordEditText
            val passwordEditTextParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            passwordEditTextParams.weight = 1.0f // Take up available space
            passwordEditText.layoutParams = passwordEditTextParams

            passwordPromptLayout.addView(passwordEditText)
            passwordPromptLayout.addView(showHideButton)


            val confirmationDialog = AlertDialog.Builder(this).setTitle("Delete Account")
                .setPositiveButton("Yes") { _, _ ->
                    // Prompt the user to enter their password to confirm deletion
                    val passwordPrompt = AlertDialog.Builder(this).setTitle("Confirm Account Deletion")
                        .setView(passwordPromptLayout).setPositiveButton("Delete") { _, _ ->
                            val password = passwordEditText.text.toString()
                            if (password.isEmpty()) {
                                Toast.makeText(
                                    this,
                                    "Please enter the current password to confirm account deletion.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setPositiveButton
                            }
                            // Verify the password before deleting the account
                            val currentEmail = auth.currentUser?.email
                            val credential =
                                currentEmail?.let { EmailAuthProvider.getCredential(it, password) }
                            val user = FirebaseAuth.getInstance().currentUser
                            if (credential != null) {
                                user?.reauthenticate(credential)?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            deleteUserData(currentUsername)
                                            FirebaseAuth.getInstance().signOut()
                                            // Navigate to the login screen
                                            startActivity(
                                                Intent(
                                                    this@AccountActivity, LoginActivity::class.java
                                                )
                                            )
                                            finish()

                                            runOnUiThread {
                                                Toast.makeText(
                                                    this@AccountActivity,
                                                    "Account deleted successfully.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Incorrect password. Account deletion cancelled.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }.setNegativeButton("Cancel", null).create()

                    passwordPrompt.setView(passwordPromptLayout)
                    passwordPrompt.show()
                }.setNegativeButton("Cancel", null).create()

            val messageTextView = TextView(this)
            messageTextView.text = getString(R.string.deletion)
            messageTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
            messageTextView.gravity = Gravity.CENTER_HORIZONTAL

            // Set layout parameters for the messageTextView
            val messageTextViewParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            messageTextViewParams.weight = 1.0f // Spread text equally across the parent layout
            messageTextView.layoutParams = messageTextViewParams

            confirmationDialog.setView(messageTextView)

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

        db.collection("users").document(currentUserId!!).get().continueWith { userDocument ->
            val user = userDocument.result?.toObject(User::class.java)
            val username = user?.username
            imageRef = storageRef.child("images/${username}.jpg")
            loadProfileImage()
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
                this@AccountActivity, "You logged out successfully.", Toast.LENGTH_SHORT
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
                    ImageDecoder.createSource(contentResolver, imageUri), listener
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
                    Timber.d(tag, "Image uploaded successfully: $downloadUrl")

                    // Update the photo in the app
                    loadProfileImage()
                }.addOnFailureListener { exception ->
                    // Image upload failed
                    Timber.e(tag, "Image upload failed", exception)
                }
            }
        }

        // Call function to open gallery
        binding.change.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        // Declare variables to keep track of password visibility state and text selection
        var isPasswordVisible = false
        var startPass: Int
        var endPass: Int

        binding.showPass.setOnClickListener {
            // Update the password visibility and image resource based on the current state
            if (isPasswordVisible) {
                // Show password
                startPass = binding.password.selectionStart
                endPass = binding.password.selectionEnd
                binding.password.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.password.setSelection(startPass, endPass)
                binding.showPass.setImageResource(R.mipmap.ic_open)

                // Toggle the password visibility state
                isPasswordVisible = !isPasswordVisible
            } else {
                // Hide password
                startPass = binding.password.selectionStart
                endPass = binding.password.selectionEnd
                binding.password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.password.setSelection(startPass, endPass)
                binding.showPass.setImageResource(R.mipmap.ic_closed)

                // Toggle the password visibility state
                isPasswordVisible = !isPasswordVisible
            }
        }

        // Declare variables to keep track of password visibility state and text selection
        var isRepeatVisible = false
        var startRepeat: Int
        var endRepeat: Int

        binding.showRepeatPass.setOnClickListener {
            // Update the password visibility and image resource based on the current state
            if (isRepeatVisible) {
                // Show password
                startRepeat = binding.repeat.selectionStart
                endRepeat = binding.repeat.selectionEnd
                binding.repeat.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.repeat.setSelection(startRepeat, endRepeat)
                binding.showRepeatPass.setImageResource(R.mipmap.ic_open)

                // Toggle the password visibility state
                isRepeatVisible = !isRepeatVisible
            } else {
                // Hide password
                startRepeat = binding.repeat.selectionStart
                endRepeat = binding.repeat.selectionEnd
                binding.repeat.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.repeat.setSelection(startRepeat, endRepeat)
                binding.showRepeatPass.setImageResource(R.mipmap.ic_closed)

                // Toggle the password visibility state
                isRepeatVisible = !isRepeatVisible
            }
        }

        var tempCurr = ""
        var tmpNew = ""

        // Define the password requirements
        val passwordRequirements = listOf(
            "Password must be at least 8 characters long.",
            "Password must contain at least one lowercase letter.",
            "Password must contain at least one uppercase letter.",
            "Password must contain at least one digit.",
            "Password must contain at least one special character."
        )

        // Initialize the "Reset" button as disabled
        binding.reset.isEnabled = false

        // Add a TextWatcher to update the password requirements and enable/disable the reset button
        binding.password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed
            }

            override fun afterTextChanged(s: Editable?) {
                val newPassword = s?.toString()?.trim() ?: ""

                // Filter the password requirements based on the new password
                val unmetRequirements = passwordRequirements.filter { requirement ->
                    when (requirement) {
                        "Password must be at least 8 characters long." -> newPassword.length < 8

                        "Password must contain at least one lowercase letter." -> !newPassword.any { it.isLowerCase() }

                        "Password must contain at least one uppercase letter." -> !newPassword.any { it.isUpperCase() }

                        "Password must contain at least one digit." -> !newPassword.any { it.isDigit() }

                        "Password must contain at least one special character." -> !newPassword.any {
                            it.isLetterOrDigit().not()
                        }

                        else -> false
                    }
                }

                // Set the error message of binding.password based on the unmet requirements
                val passwordError = if (unmetRequirements.isNotEmpty()) {
                    unmetRequirements.joinToString("\n")
                } else {
                    null
                }
                binding.password.error = passwordError

                // Check if the repeat password matches the password
                val repeatPassword = binding.repeat.text.toString().trim()

                // Enable or disable the reset button based on the password requirements and password match
                binding.reset.isEnabled =
                    isPasswordValid(newPassword) && newPassword == repeatPassword
            }
        })

        // Add a TextWatcher to check if the repeat password matches the password
        binding.repeat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed
            }

            override fun afterTextChanged(s: Editable?) {
                val newPassword = binding.password.text.toString().trim()
                val repeatPassword = s?.toString()?.trim() ?: ""

                // Check if the repeat password matches the password
                val isRepeatMatching = newPassword == repeatPassword

                // Show/hide the password mismatch hint
                if (repeatPassword.isNotEmpty() && !isRepeatMatching) {
                    binding.repeat.error = "Passwords do not match"
                } else {
                    binding.repeat.error = null
                }

                // Enable or disable the reset button based on the password requirements and password match
                binding.reset.isEnabled = isPasswordValid(newPassword) && isRepeatMatching
            }
        })

        binding.reset.setOnClickListener {
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.transformationMethod = PasswordTransformationMethod.getInstance()

            // Set the value of the input variable to the stored value in temp
            input.setText(tempCurr)
            input.hint = "Enter current password"

            // Add a TextWatcher to update the temp variable whenever the input text changes
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // No action needed
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Update the temp variable with the new input text
                    tempCurr = s?.toString() ?: ""
                }

                override
                fun afterTextChanged(s: Editable?) {
                    // No action needed
                }
            })

            val showHideButton = Button(this)
            showHideButton.text = getString(R.string.show)
            showHideButton.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
            showHideButton.setBackgroundResource(R.drawable.custom_button)
            showHideButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.teal_200)
            showHideButton.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            }

            var isCurrentPasswordVisible = false
            var startCursor: Int
            var endCursor: Int

            showHideButton.setOnClickListener {
                if (isCurrentPasswordVisible) {
                    // Hide password
                    // Preserve the cursor position after showing/hiding password
                    startCursor = input.selectionStart
                    endCursor = input.selectionEnd
                    input.transformationMethod = PasswordTransformationMethod.getInstance()
                    input.setSelection(startCursor, endCursor)
                    showHideButton.text = getString(R.string.show)
                    isCurrentPasswordVisible = !isCurrentPasswordVisible
                } else {
                    // Show password
                    // Preserve the cursor position after showing/hiding password
                    startCursor = input.selectionStart
                    endCursor = input.selectionEnd
                    input.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    input.setSelection(startCursor, endCursor)
                    showHideButton.text = getString(R.string.hide)
                    isCurrentPasswordVisible = !isCurrentPasswordVisible
                }
            }

            val layout = RelativeLayout(this)
            layout.addView(input)
            layout.addView(showHideButton)

            val inputParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            inputParams.addRule(RelativeLayout.ALIGN_PARENT_START)
            inputParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            inputParams.addRule(RelativeLayout.LEFT_OF, showHideButton.id)
            input.layoutParams = inputParams

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Password Change")
            builder.setView(layout)

            builder.setPositiveButton("CHANGE") { _, _ ->
                val currentPasswordTemp = input.text.toString().trim()
                if (TextUtils.isEmpty(currentPasswordTemp)) {
                    Toast.makeText(
                        this@AccountActivity,
                        "Please enter the current password to confirm password change.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                tmpNew = binding.password.text.toString().trim()

                if (tempCurr == tmpNew) {
                    Toast.makeText(
                        this@AccountActivity,
                        "Current password can't be same as the new password.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // Re-authenticate the user with their current password
                    val credential =
                        EmailAuthProvider.getCredential(user.email!!, currentPasswordTemp)
                    user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                        if (reAuthTask.isSuccessful) {
                            // Password matches, continue with updating the password
                            val newPassword = binding.password.text.toString().trim()
                            val repeatPassword = binding.repeat.text.toString().trim()

                            // Check if the new password satisfies Firebase's criteria
                            if (isPasswordValid(newPassword)) {
                                if (newPassword == repeatPassword) {
                                    user
                                        .updatePassword(newPassword)
                                        .addOnCompleteListener { updateTask ->
                                            if (updateTask.isSuccessful) {
                                                Toast.makeText(
                                                    this@AccountActivity,
                                                    "Password updated successfully.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                binding.password.text.clear()
                                                binding.repeat.text.clear()
                                                tempCurr = ""
                                                tmpNew = ""
                                            } else {
                                                Toast.makeText(
                                                    this@AccountActivity,
                                                    "Failed to update the password. Please try again.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(
                                        this@AccountActivity,
                                        "Passwords do not match.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            } else {
                                Toast.makeText(
                                    this@AccountActivity,
                                    "New password must satisfy the criteria.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@AccountActivity,
                                "Incorrect current password. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        // Delete account
        binding.delete.setOnClickListener {
            deleteUserAccount()
        }
    }
}