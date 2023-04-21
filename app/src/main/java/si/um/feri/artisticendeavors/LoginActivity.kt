package si.um.feri.artisticendeavors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import si.um.feri.artisticendeavors.databinding.ActivityLoginBinding
import timber.log.Timber

class LoginActivity : ComponentActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get a reference to the Firebase authentication instance
        val auth = FirebaseAuth.getInstance()

        // Get a reference to the shared preferences
        var prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        // Get the user ID and auth token from shared preferences
        var userId = prefs.getString("userId", null)
        var token = prefs.getString("token", null)

        // If the user ID and auth token are not null, sign in with the token
        if (userId != null && token != null) {
            auth.signInWithCustomToken(token).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user?.uid == userId) {
                        val intent =
                            Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Please log in.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        task.exception!!.message.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                    Timber.e(task.exception!!.message.toString())
                }
            }
        }

        // Set up a listener for changes to the authentication state
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // The user is not authenticated, but don't clear the shared preferences
            } else {
                // The user is authenticated
                // Save the user ID and auth token to shared preferences
                val editor = prefs.edit()
                editor.putString("userId", user.uid)
                user.getIdToken(false).addOnSuccessListener { result ->
                    editor.putString("token", result.token)
                    editor.apply()
                }
            }
        }

        binding.signupOption.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.actionLogin.setOnClickListener {
            when {
                TextUtils.isEmpty(binding.loginUsername.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                TextUtils.isEmpty(binding.loginPassword.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    val email: String = binding.loginUsername.text.toString().trim { it <= ' ' }
                    val pass: String = binding.loginPassword.text.toString().trim { it <= ' ' }

                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                val user = FirebaseAuth.getInstance().currentUser
                                userId = user?.uid
                                token = user?.getIdToken(false)?.result?.token
                                prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                                val editor = prefs.edit()
                                editor.putString("userId", userId)
                                editor.putString("token", token)
                                editor.apply()

                                Toast.makeText(
                                    this@LoginActivity,
                                    "You are logged in successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent =
                                    Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra(
                                    "user_id",
                                    FirebaseAuth.getInstance().currentUser!!.uid
                                )
                                intent.putExtra("email_id", email)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val userId = prefs.getString("userId", null)
        val token = prefs.getString("token", null)
        if (userId != null && token != null) {
            FirebaseAuth.getInstance().signInWithCustomToken(token)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user?.uid == userId) {
                            val intent =
                                Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Please log in.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Timber.e(task.exception!!.message.toString())
                    }
                }
        } else {
            Toast.makeText(
                this@LoginActivity,
                "Please log in.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}