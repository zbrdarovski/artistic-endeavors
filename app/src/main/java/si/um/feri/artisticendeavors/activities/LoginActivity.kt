package si.um.feri.artisticendeavors.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    private fun resendConfirmationEmail() {
        when {
            // Check email
            TextUtils.isEmpty(binding.loginUsername.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.please_enter_email),
                    Toast.LENGTH_SHORT
                ).show()
                binding.actionLogin.isEnabled = true
            }

            // Check password
            TextUtils.isEmpty(binding.loginPassword.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.please_enter_password),
                    Toast.LENGTH_SHORT
                ).show()
                binding.actionLogin.isEnabled = true
            }

            // If everything checks out sign in with email and password to Firebase
            else -> {
                val email: String = binding.loginUsername.text.toString().trim { it <= ' ' }
                val pass: String = binding.loginPassword.text.toString().trim { it <= ' ' }

                this.auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = task.result?.user
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { verificationTask ->
                                    if (verificationTask.isSuccessful) {
                                        // Verification email sent successfully
                                        Toast.makeText(
                                            this@LoginActivity,
                                            getString(R.string.verification_email_sent_successfully),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // Failed to send verification email
                                        Toast.makeText(
                                            this@LoginActivity,
                                            getString(R.string.failed_to_send_verification_email),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                task.exception!!.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.actionLogin.isEnabled = true
                        }
                        auth.signOut()
                    }
            }
        }
    }

    private fun resetPassword() {
        when {
            // Check email
            TextUtils.isEmpty(binding.loginUsername.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.please_enter_email),
                    Toast.LENGTH_SHORT
                ).show()
                binding.actionLogin.isEnabled = true
            }

            // If everything checks out sign in with email and password to Firebase
            else -> {
                val email: String = binding.loginUsername.text.toString().trim { it <= ' ' }
                val auth = FirebaseAuth.getInstance()
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                getString(R.string.password_reset_email_sent),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                task.exception?.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        // If the user is logged in, skip the LoginActivity and go to the MainActivity
        if (auth.currentUser != null) {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Switch from LoginActivity to RegisterActivity
        binding.signupOption.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
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
                startPass = binding.loginPassword.selectionStart
                endPass = binding.loginPassword.selectionEnd
                binding.loginPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.loginPassword.setSelection(startPass, endPass)
                binding.showPass.setImageResource(R.mipmap.ic_open)

                // Toggle the password visibility state
                isPasswordVisible = !isPasswordVisible
            } else {
                // Hide password
                startPass = binding.loginPassword.selectionStart
                endPass = binding.loginPassword.selectionEnd
                binding.loginPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.loginPassword.setSelection(startPass, endPass)
                binding.showPass.setImageResource(R.mipmap.ic_closed)

                // Toggle the password visibility state
                isPasswordVisible = !isPasswordVisible
            }
        }

        binding.resend.setOnClickListener {
            resendConfirmationEmail()
        }

        binding.resetPassword.setOnClickListener {
            resetPassword()
        }

        // Login to the app
        binding.actionLogin.setOnClickListener {
            binding.actionLogin.isEnabled = false
            when {
                // Check email
                TextUtils.isEmpty(binding.loginUsername.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.please_enter_email),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.actionLogin.isEnabled = true
                }

                // Check password
                TextUtils.isEmpty(binding.loginPassword.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.please_enter_password),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.actionLogin.isEnabled = true
                }

                // If everything checks out sign in with email and password to Firebase
                else -> {
                    val email: String = binding.loginUsername.text.toString().trim { it <= ' ' }
                    val pass: String = binding.loginPassword.text.toString().trim { it <= ' ' }

                    this.auth.signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = this.auth.currentUser
                                if (user != null && user.isEmailVerified) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        getString(R.string.you_logged_in_successfully),
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent =
                                        Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    intent.putExtra("user_id", user.uid)
                                    intent.putExtra("email_id", email)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        getString(R.string.please_verify_your_email_address),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    binding.actionLogin.isEnabled = true
                                }
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.actionLogin.isEnabled = true
                            }
                        }
                }
            }
        }
    }
}