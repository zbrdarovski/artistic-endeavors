package si.um.feri.artisticendeavors.activities

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

    private val tag: String = getString(R.string.account_activity)

    private fun color(text: String, colorHex: String): Spannable {
        val spannableString = SpannableString(text)
        val colorSpan = ForegroundColorSpan(Color.parseColor(colorHex))
        spannableString.setSpan(colorSpan, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }

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
            val errorMessage = getString(R.string.error_downloading_image, exception)
            Timber.e(tag, errorMessage)
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
            val errorMessage = getString(R.string.failed_to_delete_profile_image, e)
            Timber.e(tag, errorMessage)
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
            passwordEditText.hint = getString(R.string.enter_current_password)

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
            passwordPromptLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val passwordEditTextParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            passwordEditTextParams.weight = 1.0f // Take up available space
            passwordEditText.layoutParams = passwordEditTextParams

            passwordPromptLayout.addView(passwordEditText)
            passwordPromptLayout.addView(showHideButton)

            val passwordPrompt = AlertDialog.Builder(this)
                .setTitle(color(getString(R.string.confirm_account_deletion), "#E36363"))
                .setView(passwordPromptLayout)
                .setPositiveButton(color(getString(R.string.delete), "#E36363")) { _, _ ->
                    val password = passwordEditText.text.toString()
                    if (password.isEmpty()) {
                        Toast.makeText(
                            this,
                            getString(R.string.please_enter_the_current_password_to_confirm_account_deletion),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setPositiveButton
                    }
                    val currentEmail = auth.currentUser?.email
                    val credential = currentEmail?.let {
                        EmailAuthProvider.getCredential(it, password)
                    }
                    val user = FirebaseAuth.getInstance().currentUser
                    if (credential != null) {
                        user?.reauthenticate(credential)
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    GlobalScope.launch(Dispatchers.IO) {
                                        deleteUserData(currentUsername)
                                        FirebaseAuth.getInstance().signOut()
                                        startActivity(
                                            Intent(
                                                this@AccountActivity,
                                                LoginActivity::class.java
                                            )
                                        )
                                        finish()

                                        runOnUiThread {
                                            Toast.makeText(
                                                this@AccountActivity,
                                                getString(R.string.account_deleted_successfully),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.incorrect_password_account_deletion_cancelled),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }
                .setNegativeButton(color(getString(R.string.cancel), "#84B589"), null)
                .create()
            passwordPrompt.show()
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
                this@AccountActivity, getString(R.string.you_logged_out_successfully), Toast.LENGTH_SHORT
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
                    Timber.d(tag, getString(R.string.image_uploaded_successfully, downloadUrl))

                    // Update the photo in the app
                    loadProfileImage()
                }.addOnFailureListener { exception ->
                    // Image upload failed
                    Timber.e(tag, R.string.image_upload_failed, exception)
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
        var tmpNew: String

        // Define the password requirements
        val passwordRequirements = listOf(
            getString(R.string.password_must_be_at_least_eight_characters_long),
            getString(R.string.password_must_contain_at_least_one_lowercase_letter),
            getString(R.string.password_must_contain_at_least_one_uppercase_letter),
            getString(R.string.password_must_contain_at_least_one_digit),
            getString(R.string.password_must_contain_at_least_one_special_character)
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
                        getString(R.string.password_must_be_at_least_eight_characters_long) -> newPassword.length < 8

                        getString(R.string.password_must_contain_at_least_one_lowercase_letter) -> !newPassword.any { it.isLowerCase() }

                        getString(R.string.password_must_contain_at_least_one_uppercase_letter)  -> !newPassword.any { it.isUpperCase() }

                        getString(R.string.password_must_contain_at_least_one_digit)  -> !newPassword.any { it.isDigit() }

                        getString(R.string.password_must_contain_at_least_one_special_character)  -> !newPassword.any {
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
                    binding.repeat.error = getString(R.string.passwords_do_not_match)
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
            input.setText(tempCurr)
            input.hint = getString(R.string.enter_current_password)

            val showHideButton = Button(this)
            showHideButton.text = getString(R.string.show)
            showHideButton.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
            showHideButton.setBackgroundResource(R.drawable.custom_button)
            showHideButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.teal_200)

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

            val layout = LinearLayout(this)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layout.layoutParams = layoutParams
            layout.orientation = LinearLayout.HORIZONTAL

            val inputParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputParams.weight = 1f
            input.layoutParams = inputParams
            layout.addView(input)

            val buttonParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            showHideButton.layoutParams = buttonParams
            layout.addView(showHideButton)

            val builder = AlertDialog.Builder(this)
            builder.setTitle(color(getString(R.string.confirm_password_change), "#E36363"))
            builder.setView(layout)

            builder.setPositiveButton(color(getString(R.string.change), "#E36363")) { _, _ ->
                val currentPasswordTemp = input.text.toString().trim()
                if (TextUtils.isEmpty(currentPasswordTemp)) {
                    Toast.makeText(
                        this@AccountActivity,
                        getString(R.string.please_enter_the_current_password_to_confirm_password_change),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                tmpNew = binding.password.text.toString().trim()

                if (tempCurr == tmpNew) {
                    Toast.makeText(
                        this@AccountActivity,
                        getString(R.string.current_password_cant_be_same_as_the_new_password),
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
                                    user.updatePassword(newPassword)
                                        .addOnCompleteListener { updateTask ->
                                            if (updateTask.isSuccessful) {
                                                Toast.makeText(
                                                    this@AccountActivity,
                                                    getString(R.string.password_updated_successfully),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                binding.password.text.clear()
                                                binding.repeat.text.clear()
                                                tempCurr = ""
                                                tmpNew = ""
                                            } else {
                                                Toast.makeText(
                                                    this@AccountActivity,
                                                    getString(R.string.failed_to_update_the_password_please_try_again),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(
                                        this@AccountActivity,
                                        getString(R.string.passwords_do_not_match),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    this@AccountActivity,
                                    getString(R.string.new_password_must_satisfy_the_criteria),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@AccountActivity,
                                getString(R.string.incorrect_current_password_please_try_again),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                ?.setTextColor(Color.parseColor("#84B589"))
            dialog.show()
        }

        // Delete account
        binding.delete.setOnClickListener {
            deleteUserAccount()
        }
    }
}