package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import si.um.feri.artisticendeavors.databinding.ActivityRegisterBinding


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Regex to check the username:
            1.) Username is 8-20 characters long.
            2.) Allowed characters: alphanumeric characters, underscore and dot.
            3.) No underscore or dot at the end or the beginning.
            4.) Underscore and dot can't be next to each other.
            5.) Underscore or dot can't be used multiple times in a row. */

        val pattern = Regex("^(?=.{8,20}\$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])\$")

        // Switch from RegisterActivity to LoginActivity
        binding.option.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Register a new user to Firebase
        binding.actionRegister.setOnClickListener {
            when {
                // Check whether email's field is emtpy
                TextUtils.isEmpty(binding.email.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Check whether username's field is empty
                TextUtils.isEmpty(binding.username.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter username.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Verify username with regex
                !pattern.containsMatchIn(binding.username.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Username is 8-20 characters long." +
                                "Allowed characters: alphanumeric characters, underscore and dot." +
                                "No underscore or dot at the end or the beginning." +
                                "Underscore and dot can't be next to each other." +
                                "Underscore or dot can't be used multiple times in a row.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Check whether password's field is empty
                TextUtils.isEmpty(binding.password.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Check whether password repeat's field is empty
                TextUtils.isEmpty(binding.repeat.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please repeat password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Double check password
                binding.password.text.toString() != binding.repeat.text.toString() -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Passwords don't match.",
                        Toast.LENGTH_SHORT
                    ).show()
                }


                // If everything checks out register a new user
                else -> {
                    val em: String = binding.email.text.toString().trim { it <= ' ' }
                    val pass: String = binding.password.text.toString().trim { it <= ' ' }
                    val auth = FirebaseAuth.getInstance()
                    auth.createUserWithEmailAndPassword(em, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                val firebaseUser: FirebaseUser = task.result!!.user!!

                                auth.currentUser?.let { user ->
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(
                                            binding.username.text.toString().trim { it <= ' ' })
                                        .build()


                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            user.updateProfile(profileUpdates).await()
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    this@RegisterActivity,
                                                    e.message,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }

                                Toast.makeText(
                                    this@RegisterActivity,
                                    "You are registered successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent =
                                    Intent(this@RegisterActivity, LoginActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("user_id", firebaseUser.uid)
                                intent.putExtra("email_id", em)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
        }
    }
}