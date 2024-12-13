package artisticendeavors.activities

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import artisticendeavors.tools.ActivitySwitcher
import artisticendeavors.tools.Messenger
import artisticendeavors.R
import artisticendeavors.tools.Validator
import artisticendeavors.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private val activitySwitcher by lazy { ActivitySwitcher() }
    private val validator by lazy { Validator(this) }
    private val messenger by lazy { Messenger(this) }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.showPass.setOnClickListener {
            binding.password.showPasswordWithImage(binding.showPass)
        }

        binding.showRepeatPass.setOnClickListener {
            binding.repeat.showPasswordWithImage(binding.showRepeatPass)
        }
    }

    private fun setupTextWatchers() {
        binding.email.addTextChangedListener {
            binding.email.validateEmailFormat()
        }

        binding.username.addTextChangedListener {
            binding.username.validateUsername()
        }

        binding.password.addTextChangedListener {
            binding.password.validatePassword()
            binding.repeat.validateRepeatPassword(binding.password.text.toString())
        }

        binding.repeat.addTextChangedListener {
            binding.repeat.validateRepeatPassword(binding.password.text.toString())
        }
    }

    private fun setupClickListeners() {
        binding.option.setOnClickListener {
            activitySwitcher.startNewActivity(this@RegisterActivity, LoginActivity::class.java)
        }

        binding.actionRegister.setOnClickListener {
            if (isValidInput()) {
                binding.actionRegister.isEnabled = false
                registerUser()
            }
        }
    }

    private fun isValidInput(): Boolean {
        val email = binding.email.text.toString().trim()
        val username = binding.username.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val repeatPassword = binding.repeat.text.toString().trim()

        return when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.email.error = getString(R.string.invalid_email_format)
                false
            }

            !validator.isUsernameValid(username) -> {
                binding.username.error = validator.usernameErrorMessage(username)
                false
            }

            !validator.isPasswordValid(password) -> {
                binding.password.error = validator.passwordErrorMessage(password)
                false
            }

            password != repeatPassword -> {
                binding.repeat.error = getString(R.string.passwords_do_not_match)
                false
            }

            else -> true
        }
    }

    private fun registerUser() {
        val username = binding.username.text.toString().trim()

        db.collection("users").whereEqualTo("username", username).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    messenger.message(getString(R.string.this_username_is_already_taken))
                    binding.actionRegister.isEnabled = true
                } else {
                    val email = binding.email.text.toString().trim()
                    val password = binding.password.text.toString().trim()

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val firebaseUser = authResult.user
                            val userId = firebaseUser?.uid
                            firebaseUser?.sendEmailVerification()?.addOnSuccessListener {
                                messenger.message(getString(R.string.verification_email_has_been_sent_to_your_inbox_please_verify_your_email_before_logging_in))

                                val profileUpdates =
                                    UserProfileChangeRequest.Builder().setDisplayName(username)
                                        .build()
                                firebaseUser.updateProfile(profileUpdates)

                                val newUser = hashMapOf(
                                    "username" to username,
                                    "id" to userId
                                )

                                db.collection("users").document(userId!!).set(newUser)
                                    .addOnSuccessListener {
                                        auth.signOut()
                                        activitySwitcher.startNewActivity(
                                            this@RegisterActivity,
                                            LoginActivity::class.java
                                        )
                                    }.addOnFailureListener { e ->
                                        messenger.message(e.message.toString())
                                        binding.actionRegister.isEnabled = true
                                    }
                            }?.addOnFailureListener { e ->
                                messenger.message(e.message.toString())
                                binding.actionRegister.isEnabled = true
                            }
                        }.addOnFailureListener { e ->
                            messenger.message(e.message.toString())
                            binding.actionRegister.isEnabled = true
                        }
                }
            }.addOnFailureListener { e ->
                messenger.message(e.message.toString())
                binding.actionRegister.isEnabled = true
            }
    }

    private fun EditText.showPasswordWithImage(showPassView: ImageView) {
        val drawableRes =
            if (transformationMethod == HideReturnsTransformationMethod.getInstance()) {
                R.mipmap.ic_open
            } else {
                R.mipmap.ic_closed
            }
        showPassView.setImageResource(drawableRes)

        transformationMethod =
            if (transformationMethod == HideReturnsTransformationMethod.getInstance()) {
                PasswordTransformationMethod.getInstance()
            } else {
                HideReturnsTransformationMethod.getInstance()
            }

        setSelection(text?.length ?: 0)
    }

    private fun EditText.validateEmailFormat() {
        val email = text.toString().trim()
        error = if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) null
        else context.getString(R.string.invalid_email_format)
    }

    private fun EditText.validateUsername() {
        val username = text.toString().trim()
        error = validator.usernameErrorMessage(username)
    }

    private fun EditText.validatePassword() {
        val password = text.toString().trim()
        error = validator.passwordErrorMessage(password)
    }

    private fun EditText.validateRepeatPassword(password: String) {
        val repeatPassword = text.toString().trim()
        error = if (password == repeatPassword) null
        else context.getString(R.string.passwords_do_not_match)
    }
}