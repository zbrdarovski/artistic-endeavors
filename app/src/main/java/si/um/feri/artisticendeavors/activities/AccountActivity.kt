package si.um.feri.artisticendeavors.activities

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
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
import si.um.feri.artisticendeavors.ActivitySwitcher
import si.um.feri.artisticendeavors.Color
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.Toolbar
import si.um.feri.artisticendeavors.Validator
import si.um.feri.artisticendeavors.VisibilitySwitcher
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

    private lateinit var tag: String
    private lateinit var toolbar: Toolbar
    private lateinit var activitySwitcher: ActivitySwitcher
    private lateinit var validator: Validator
    private lateinit var visibilitySwitcher: VisibilitySwitcher
    private lateinit var color: Color

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
        val profileImageReference = storage.reference.child("images/$currentUsername.jpg")

        try {
            profileImageReference.delete().await()
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

            visibilitySwitcher.showPasswordWithButton(showHideButton, passwordEditText)

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
                .setTitle(color.colorize(getString(R.string.confirm_account_deletion), "#E36363"))
                .setView(passwordPromptLayout)
                .setPositiveButton(color.colorize(getString(R.string.delete), "#E36363")) { _, _ ->
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
                                        activitySwitcher.startNewActivity(
                                            this@AccountActivity,
                                            LoginActivity::class.java
                                        )

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
                .setNegativeButton(color.colorize(getString(R.string.cancel), "#84B589"), null)
                .create()
            passwordPrompt.show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tag = getString(R.string.account_activity)
        db = Firebase.firestore
        activitySwitcher = ActivitySwitcher()
        toolbar = Toolbar(
            this, auth, binding.includeToolbar.homeOption,
            binding.includeToolbar.profileOption, binding.includeToolbar.accountOption,
            binding.includeToolbar.actionSignOut
        )
        toolbar.bindToolbar()
        validator = Validator(this)
        visibilitySwitcher = VisibilitySwitcher(this)
        color = Color()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        db.collection("users").document(currentUserId!!).get().continueWith { userDocument ->
            val user = userDocument.result?.toObject(User::class.java)
            val username = user?.username
            imageRef = storageRef.child("images/${username}.jpg")
            loadProfileImage()
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

        // Show/Hide password and repeat password
        visibilitySwitcher.showPasswordWithImage(binding.showPass, binding.password)
        visibilitySwitcher.showPasswordWithImage(binding.showRepeatPass, binding.repeat)

        var tempCurr = ""
        var tmpNew: String

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

                binding.password.error = validator.passwordErrorMessage(newPassword)
                // Check if the repeat password matches the password
                val repeatPassword = binding.repeat.text.toString().trim()

                // Enable or disable the reset button based on the password requirements and password match
                binding.reset.isEnabled =
                    validator.isPasswordValid(newPassword) && newPassword == repeatPassword
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
                binding.reset.isEnabled = validator.isPasswordValid(newPassword) && isRepeatMatching
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

            visibilitySwitcher.showPasswordWithButton(showHideButton, input)

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
            builder.setTitle(color.colorize(getString(R.string.confirm_password_change), "#E36363"))
            builder.setView(layout)

            builder.setPositiveButton(
                color.colorize(
                    getString(R.string.change),
                    "#E36363"
                )
            ) { _, _ ->
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
                            if (validator.isPasswordValid(newPassword)) {
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

            builder.setNegativeButton(
                color.colorize(
                    getString(R.string.cancel),
                    "#84B589"
                )
            ) { dialog, _ ->
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