package artisticendeavors.activities

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import artisticendeavors.tools.ActivitySwitcher
import artisticendeavors.tools.Messenger
import artisticendeavors.R
import artisticendeavors.tools.VisibilitySwitcher
import artisticendeavors.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val activitySwitcher: ActivitySwitcher by lazy {
        ActivitySwitcher()
    }
    private val visibilitySwitcher: VisibilitySwitcher by lazy {
        VisibilitySwitcher(this)
    }
    private val messenger: Messenger by lazy {
        Messenger(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (auth.currentUser != null) {
            activitySwitcher.startNewActivity(this@LoginActivity, MainActivity::class.java)
            return
        }

        binding.signUp.setOnClickListener {
            activitySwitcher.startNewActivity(this@LoginActivity, RegisterActivity::class.java)
        }

        visibilitySwitcher.showPasswordWithImage(binding.showPass, binding.loginPassword)

        binding.resendAuthentication.setOnClickListener {
            binding.resendAuthentication.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                sendEmailAction(EmailAction.RESEND)
            }
        }

        binding.resetPassword.setOnClickListener {
            binding.resetPassword.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                sendEmailAction(EmailAction.RESET_PASSWORD)
            }
        }

        binding.actionLogin.setOnClickListener {
            binding.actionLogin.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                login()
            }
        }
    }

    private fun EditText.trimmedText(): String {
        return text.toString().trim()
    }

    private suspend fun sendEmailAction(action: EmailAction) {
        val email = binding.loginUsername.trimmedText()
        val password = binding.loginPassword.trimmedText()

        when (action) {
            EmailAction.RESEND -> {
                if (isEmpty(email, password)) {
                    binding.resendAuthentication.isEnabled = true
                    return
                }
                signIn(email, password)?.let { user ->
                    sendEmailVerification(user)
                }
            }

            EmailAction.RESET_PASSWORD -> {
                if (isEmpty(email, null)) {
                    binding.resetPassword.isEnabled = true
                    return
                }
                try {
                    auth.sendPasswordResetEmail(email).await()
                    messenger.message(getString(R.string.password_reset_email_sent))
                } catch (e: Exception) {
                    binding.resetPassword.isEnabled = true
                    e.message?.let { messenger.message(it) }
                }
            }
        }
    }

    private suspend fun sendEmailVerification(user: FirebaseUser) {
        try {
            user.sendEmailVerification().await()
            messenger.message(getString(R.string.verification_email_sent_successfully))
        } catch (e: Exception) {
            binding.resendAuthentication.isEnabled = true
            e.message?.let { messenger.message(it) }
        }
    }

    private suspend fun login() {
        val email = binding.loginUsername.trimmedText()
        val password = binding.loginPassword.trimmedText()

        if (isEmpty(email, password)) {
            binding.actionLogin.isEnabled = true
            return
        }

        signIn(email, password)?.let { user ->
            if (user.isEmailVerified) {
                messenger.message(getString(R.string.you_logged_in_successfully))
                activitySwitcher.startNewActivity(this@LoginActivity, MainActivity::class.java)
            } else {
                binding.actionLogin.isEnabled = true
                messenger.message(getString(R.string.please_verify_your_email_address))
            }
        }
    }

    private fun isEmpty(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty()) {
            messenger.message(getString(R.string.please_enter_email))
            return true
        }

        if (password != null) {
            if (password.isEmpty()) {
                messenger.message(getString(R.string.please_enter_password))
                return true
            }
        }

        return false
    }

    private suspend fun signIn(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user
        } catch (e: Exception) {
            binding.actionLogin.isEnabled = true
            messenger.message(e.toString())
            null
        }
    }

    private enum class EmailAction {
        RESEND, RESET_PASSWORD
    }
}