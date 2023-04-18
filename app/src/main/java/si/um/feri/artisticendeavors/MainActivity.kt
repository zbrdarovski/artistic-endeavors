package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val action_signout: Button = findViewById(R.id.action_signout)

        action_signout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            Toast.makeText(
                this@MainActivity,
                "You logged out successfully.",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }
}