package artisticendeavors.activities

import android.os.Build
import android.os.Bundle
import android.text.Editable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import artisticendeavors.tools.ActivitySwitcher
import artisticendeavors.tools.Color
import artisticendeavors.tools.Dialog
import artisticendeavors.tools.Messenger
import artisticendeavors.tools.Photoshop
import artisticendeavors.R
import artisticendeavors.data.User
import artisticendeavors.tools.Toolbar
import artisticendeavors.tools.Validator
import artisticendeavors.tools.VisibilitySwitcher
import artisticendeavors.databinding.ActivityAccountBinding
import timber.log.Timber

class AccountActivity : AppCompatActivity() {
    private val binding: ActivityAccountBinding by lazy {
        ActivityAccountBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val currentUser: FirebaseUser by lazy { auth.currentUser!! }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val profileImageReference: StorageReference by lazy {
        storage.reference.child("images/${currentUser.displayName}.jpg")
    }
    private val validator: Validator by lazy { Validator(this) }
    private val visibilitySwitcher: VisibilitySwitcher by lazy { VisibilitySwitcher(this) }
    private val messenger: Messenger by lazy { Messenger(this) }
    private val dialog: Dialog by lazy { Dialog(this, visibilitySwitcher, color) }

    private val tag: String by lazy { getString(R.string.account_activity) }
    private val color: Color by lazy { Color() }
    private val photoshop: Photoshop by lazy {
        Photoshop(this, this.activityResultRegistry)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.includeToolbar.apply {
            Toolbar(
                this@AccountActivity, auth, homeOption, profileOption, accountOption, actionSignOut
            ).bindToolbar()
        }

        // Load profile image
        photoshop.loadImage(profileImageReference, binding.profileImage)

        val userReference = db.collection("users")
            .whereEqualTo("username", auth.currentUser?.displayName)

        userReference.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0] // Only one matching user
                    val userData = documentSnapshot.toObject(User::class.java)
                    val description = userData?.biography

                    // Access the description field
                    if (description != null) {
                        // Use the description here
                        binding.textInputEditText.text = Editable.Factory.getInstance().newEditable(description)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle any potential errors
                val errorMessage = getString(R.string.error_getting_user_data, e)
                Timber.e(tag, errorMessage)
            }

        val galleryLauncher = photoshop.launch(profileImageReference, binding.profileImage)

        binding.change.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.updateBiography.isEnabled = false

        binding.textInputEditText.addTextChangedListener {
            binding.updateBiography.isEnabled = true
        }

        binding.updateBiography.setOnClickListener {
            val newDescription = binding.textInputEditText.text.toString()

            val userQuery = db.collection("users")
                .whereEqualTo("username", auth.currentUser?.displayName)
                .limit(1)

            userQuery.get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0]
                        val userRef = documentSnapshot.reference // Get the DocumentReference
                        val userData = documentSnapshot.toObject(User::class.java)

                        // Update the description field
                        userData?.biography = newDescription

                        // Update the document to database
                        if (userData != null) {
                            userRef.set(userData)
                                .addOnSuccessListener {
                                    // Document updated successfully
                                    messenger.message(getString(R.string.bio_updated_successfully))
                                    binding.updateBiography.isEnabled = false
                                }
                                .addOnFailureListener { e ->
                                    // Handle any potential errors
                                    val errorMessage = getString(R.string.error_updating_user_data, e)
                                    Timber.e(tag, errorMessage)
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle any potential errors
                    val errorMessage = getString(R.string.error_getting_user_data, e)
                    Timber.e(tag, errorMessage)
                }
        }

        visibilitySwitcher.showPasswordWithImage(binding.showPass, binding.password)
        visibilitySwitcher.showPasswordWithImage(binding.showRepeatPass, binding.repeat)

        binding.password.addTextChangedListener {
            binding.password.error = validator.passwordErrorMessage(it?.toString()?.trim() ?: "")
        }

        binding.repeat.addTextChangedListener {
            val newPassword = binding.password.text.toString().trim()
            val arePasswordsMatching =
                validator.arePasswordsMatching(newPassword, it?.toString()?.trim() ?: "")

            binding.repeat.error = if (!arePasswordsMatching) {
                getString(R.string.passwords_do_not_match)
            } else {
                null
            }

            binding.changePassword.isEnabled =
                validator.isPasswordValid(newPassword) && arePasswordsMatching
        }

        binding.changePassword.isEnabled = false

        binding.changePassword.setOnClickListener {
            dialog.createPasswordPrompt(
                getString(R.string.confirm_password_change), getString(R.string.change)
            ) { password ->
                handleChangePassword(password)
            }
        }

        binding.delete.setOnClickListener {
            dialog.createPasswordPrompt(
                getString(R.string.confirm_account_deletion), getString(R.string.delete)
            ) { password ->
                handleAccountDeletion(password)
            }
        }
    }

    private fun handleChangePassword(password: String) {
        val newPassword = binding.password.text.toString().trim()
        val repeatPassword = binding.repeat.text.toString().trim()

        currentUser.email?.let { email ->
            validator.authenticate(email, password, currentUser).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    if (validator.arePasswordsMatching(newPassword, repeatPassword)) {
                        currentUser.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    messenger.message(getString(R.string.password_updated_successfully))
                                    binding.password.text.clear()
                                    binding.repeat.text.clear()
                                } else {
                                    messenger.message(getString(R.string.failed_to_update_the_password_please_try_again))
                                }
                            }
                    } else {
                        messenger.message(getString(R.string.passwords_do_not_match))
                    }
                } else {
                    messenger.message(getString(R.string.incorrect_current_password_please_try_again))
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleAccountDeletion(password: String) {
        if (password.isEmpty()) {
            messenger.message(getString(R.string.please_enter_the_current_password_to_confirm_account_deletion))
            return
        }

        currentUser.email?.let { email ->
            validator.authenticate(email, password, currentUser).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    GlobalScope.launch(Dispatchers.Main) {
                        deleteUserData(currentUser.displayName)
                        messenger.message(getString(R.string.account_deleted_successfully))
                        val activitySwitcher = ActivitySwitcher()
                        activitySwitcher.startNewActivity(
                            this@AccountActivity, LoginActivity::class.java
                        )
                    }
                } else {
                    messenger.message(getString(R.string.incorrect_password_account_deletion_cancelled))
                }
            }
        }
    }

    private suspend fun deleteUserData(currentUsername: String?) {
        currentUsername?.let {
            val postsQuerySnapshot =
                db.collection("posts").whereEqualTo("user.username", it).get().await()

            for (document in postsQuerySnapshot) {
                val imageUrl = document.getString("image_url")
                if (imageUrl != null) {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().await()
                }
                document.reference.delete().await()
            }

            try {
                profileImageReference.delete().await()
            } catch (e: Exception) {
                val errorMessage = getString(R.string.failed_to_delete_profile_image, e)
                Timber.e(tag, errorMessage)
            }

            val usersQuerySnapshot =
                db.collection("users").whereEqualTo("username", it).get().await()

            for (document in usersQuerySnapshot) {
                document.reference.delete().await()
            }

            currentUser.delete().await()
        }
    }
}