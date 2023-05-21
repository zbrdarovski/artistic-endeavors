package si.um.feri.artisticendeavors.activities

import android.os.Build
import android.os.Bundle
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
import si.um.feri.artisticendeavors.ActivitySwitcher
import si.um.feri.artisticendeavors.Color
import si.um.feri.artisticendeavors.Dialog
import si.um.feri.artisticendeavors.Messenger
import si.um.feri.artisticendeavors.Photoshop
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.Toolbar
import si.um.feri.artisticendeavors.Validator
import si.um.feri.artisticendeavors.VisibilitySwitcher
import si.um.feri.artisticendeavors.databinding.ActivityAccountBinding
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

        val galleryLauncher = photoshop.launch(profileImageReference, binding.profileImage)

        binding.change.setOnClickListener {
            galleryLauncher.launch("image/*")
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