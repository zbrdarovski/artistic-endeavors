package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import si.um.feri.artisticendeavors.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()

    // If the user is logged out, go back to the LoginActivity
    private var authStateListener =
        AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        // Switch to MainActivity
        binding.homeOption.setOnClickListener {
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Switch to ProfileActivity
        binding.profileOption.setOnClickListener {
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Switch to AccountActivity
        binding.accountOption.setOnClickListener {
            val intent = Intent(this@MainActivity, AccountActivity::class.java)
            startActivity(intent)
        }

        // Sign out from the app
        binding.actionSignout.setOnClickListener {
            this.auth.signOut()

            Toast.makeText(
                this@MainActivity,
                "You logged out successfully.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Add state listener
    @Override
    override fun onStart() {
        super.onStart()
        this.auth.addAuthStateListener(authStateListener)
    }

    // Remove state listener
    @Override
    override fun onStop() {
        super.onStop()
        this.auth.removeAuthStateListener(authStateListener)
    }
}