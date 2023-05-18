package si.um.feri.artisticendeavors.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    // Validate the username
    private fun isUsernameValid(username: String): Boolean {
        val usernamePattern = "^[a-z0-9._]+$"
        return username.length in 8..20 && username.matches(usernamePattern.toRegex()) &&
                !username.startsWith("_") && !username.startsWith(".") &&
                !username.endsWith("_") && !username.endsWith(".") &&
                !username.contains("..") && !username.contains("__") &&
                !username.contains("._") && !username.contains("_.") &&
                !username.contains("_.") && !username.contains("._")
    }


    // Validate the password
    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun registerUser() {
        val email: String = binding.email.text.toString().trim { it <= ' ' }
        val password: String = binding.password.text.toString().trim { it <= ' ' }
        val username: String = binding.username.text.toString().trim { it <= ' ' }
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Check if username is taken
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Username is taken
                    Toast.makeText(
                        this,
                        getString(R.string.this_username_is_already_taken),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    // Username is available
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val firebaseUser = authResult.user
                            firebaseUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.verification_email_has_been_sent_to_your_inbox_please_verify_your_email_before_logging_in),
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Save the username as the user's display name
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build()
                                    firebaseUser.updateProfile(profileUpdates)

                                    // Create user with only the username
                                    val newUser = hashMapOf(
                                        "username" to username
                                    )

                                    // Add user to Fs
                                    db.collection("users")
                                        .document(authResult.user!!.uid)
                                        .set(newUser)
                                        .addOnSuccessListener {
                                            auth.signOut()

                                            // Redirect to LoginActivity
                                            val intent = Intent(this, LoginActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                this,
                                                e.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            binding.actionRegister.isEnabled = true
                                        }
                                }
                                ?.addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        e.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    binding.actionRegister.isEnabled = true
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            binding.actionRegister.isEnabled = true
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                binding.actionRegister.isEnabled = true
            }
    }

    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Define the username requirements
        val usernameRequirements = listOf(
            getString(R.string.username_must_be_eight_to_twenty_characters_long),
            getString(R.string.username_can_only_contain_lowercase_letters_digits_underscores_and_dots),
            getString(R.string.username_cannot_start_with_underscore_or_dot),
            getString(R.string.username_cannot_end_with_underscore_or_dot),
            getString(R.string.username_cannot_have_multiple_underscores_or_dots_in_a_row),
            getString(R.string.username_cannot_have_underscore_next_to_dot)
        )

        // Define the password requirements
        val passwordRequirements = listOf(
            getString(R.string.password_must_be_at_least_eight_characters_long),
            getString(R.string.password_must_contain_at_least_one_lowercase_letter),
            getString(R.string.password_must_contain_at_least_one_uppercase_letter),
            getString(R.string.password_must_contain_at_least_one_digit),
            getString(R.string.password_must_contain_at_least_one_special_character)
        )

        // Switch from RegisterActivity to LoginActivity
        binding.option.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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
                binding.password.transformationMethod =
                    PasswordTransformationMethod.getInstance()
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
                binding.repeat.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.repeat.setSelection(startRepeat, endRepeat)
                binding.showRepeatPass.setImageResource(R.mipmap.ic_open)

                // Toggle the password visibility state
                isRepeatVisible = !isRepeatVisible
            } else {
                // Hide password
                startRepeat = binding.repeat.selectionStart
                endRepeat = binding.repeat.selectionEnd
                binding.repeat.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.repeat.setSelection(startRepeat, endRepeat)
                binding.showRepeatPass.setImageResource(R.mipmap.ic_closed)

                // Toggle the password visibility state
                isRepeatVisible = !isRepeatVisible
            }
        }

        // Add text change listeners to handle validations
        binding.email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Validate email format
                if (Patterns.EMAIL_ADDRESS.matcher(s.toString()).matches()) {
                    binding.email.error = null
                } else {
                    binding.email.error = getString(R.string.invalid_email_format)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Add a TextWatcher to update the username requirements
        binding.username.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed
            }

            override fun afterTextChanged(s: Editable?) {
                val username = s?.toString()?.trim() ?: ""

                // Filter the username requirements based on the new username
                val unmetRequirements = usernameRequirements.filter { requirement ->
                    when (requirement) {
                        getString(R.string.username_must_be_eight_to_twenty_characters_long) -> username.length !in 8..20

                        getString(R.string.username_can_only_contain_lowercase_letters_digits_underscores_and_dots) -> !username.matches(
                            "^[a-z0-9._]+\$".toRegex()
                        )

                        getString(R.string.username_cannot_start_with_underscore_or_dot) -> username.startsWith(
                            "_"
                        ) || username.startsWith(".")

                        getString(R.string.username_cannot_end_with_underscore_or_dot) -> username.endsWith(
                            "_"
                        ) || username.endsWith(".")

                        getString(R.string.username_cannot_have_multiple_underscores_or_dots_in_a_row) -> username.contains(
                            ".."
                        ) || username.contains("__") || username.contains("._") || username.contains(
                            "_."
                        ) || username.contains("_.") || username.contains("._")

                        getString(R.string.username_cannot_have_underscore_next_to_dot) -> username.contains(
                            "_."
                        ) || username.contains("._")

                        else -> false
                    }
                }

                // Set the error message of binding.username based on the unmet requirements
                val usernameError = if (unmetRequirements.isNotEmpty()) {
                    unmetRequirements.joinToString("\n")
                } else {
                    null
                }
                binding.username.error = usernameError
            }
        })

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

                        getString(R.string.password_must_contain_at_least_one_uppercase_letter) -> !newPassword.any { it.isUpperCase() }

                        getString(R.string.password_must_contain_at_least_one_digit) -> !newPassword.any { it.isDigit() }

                        getString(R.string.password_must_contain_at_least_one_special_character) -> !newPassword.any {
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
            }
        })

        binding.repeat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed
            }

            override fun afterTextChanged(s: Editable?) {
                val password = binding.password.text.toString()
                val repeatPassword = binding.repeat.text.toString()

                // Set the error message of binding.password based on the password match
                binding.repeat.error =
                    if (password == repeatPassword) null else getString(R.string.passwords_do_not_match)
            }
        })

        // Register a new user to Firebase
        // Register a new user to Firebase
        binding.actionRegister.setOnClickListener {
            val isEmailValid =
                Patterns.EMAIL_ADDRESS.matcher(binding.email.text.toString()).matches()
            val isUsernameValid = isUsernameValid(binding.username.text.toString())
            val isPasswordValid = isPasswordValid(binding.password.text.toString())
            val isPasswordsMatch =
                binding.password.text.toString() == binding.repeat.text.toString()

            if (isEmailValid && isUsernameValid && isPasswordValid && isPasswordsMatch) {
                binding.actionRegister.isEnabled = false
                registerUser()
            }
        }
    }
}