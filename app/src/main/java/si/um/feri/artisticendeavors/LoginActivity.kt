package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
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
                    "Please enter email.",
                    Toast.LENGTH_SHORT
                ).show()
                binding.actionLogin.isEnabled = true
            }

            // Check password
            TextUtils.isEmpty(binding.loginPassword.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@LoginActivity,
                    "Please enter password.",
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
                                            "Verification email sent successfully.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // Failed to send verification email
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Failed to send verification email.",
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

        var startPass: Int
        var endPass: Int

        binding.showPass.setOnCheckedChangeListener { _, isChecked ->
            // checkbox status is changed from uncheck to checked.
            if (!isChecked) {
                // show password
                startPass = binding.loginPassword.selectionStart
                endPass = binding.loginPassword.selectionEnd
                binding.loginPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.loginPassword.setSelection(startPass, endPass)
            } else {
                // hide password
                startPass = binding.loginPassword.selectionStart
                endPass = binding.loginPassword.selectionEnd
                binding.loginPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.loginPassword.setSelection(startPass, endPass)
            }
        }

        binding.resend.setOnClickListener {
            resendConfirmationEmail()
        }

        // Login to the app
        binding.actionLogin.setOnClickListener {
            binding.actionLogin.isEnabled = false
            when {
                // Check email
                TextUtils.isEmpty(binding.loginUsername.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter email.",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.actionLogin.isEnabled = true
                }

                // Check password
                TextUtils.isEmpty(binding.loginPassword.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter password.",
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
                                        "You are logged in successfully.",
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
                                        "Please verify your email address.",
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