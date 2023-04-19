package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val email: EditText = findViewById(R.id.email)
        val username: EditText = findViewById(R.id.username)
        val password: EditText = findViewById(R.id.password)
        val repeat: EditText = findViewById(R.id.repeat)
        val action: Button = findViewById(R.id.action_register)
        val option: TextView = findViewById(R.id.option)

        val pattern = Regex("^(?=.{8,20}\$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])\$")

        option.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        action.setOnClickListener {
            when {
                TextUtils.isEmpty(email.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                TextUtils.isEmpty(username.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter username.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                !pattern.containsMatchIn(username.text.toString().trim { it <= ' ' }) -> {
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

                TextUtils.isEmpty(password.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                TextUtils.isEmpty(repeat.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please repeat password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                password.text.toString() != repeat.text.toString() -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Passwords don't match.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    val em: String = email.text.toString().trim { it <= ' ' }
                    val pass: String = password.text.toString().trim { it <= ' ' }
                    val auth = FirebaseAuth.getInstance()
                    auth.createUserWithEmailAndPassword(em, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                val firebaseUser: FirebaseUser = task.result!!.user!!

                                auth.currentUser?.let { user ->
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(username.text.toString().trim { it <= ' ' }).build()


                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            user.updateProfile(profileUpdates).await()
                                        } catch(e:Exception){
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