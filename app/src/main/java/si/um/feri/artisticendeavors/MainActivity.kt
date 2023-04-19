package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import si.um.feri.artisticendeavors.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    companion object {
        val IMAGE_REQUEST_CODE = 1_000;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.change.setOnClickListener {
            pickImageFromGallery()
        }

        binding.usern.text = user?.displayName
        Picasso.get().load(user?.photoUrl).into(binding.profpic)

        binding.actionSignout.setOnClickListener {
                auth.signOut()

                Toast.makeText(
                    this@MainActivity,
                    "You logged out successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data
            binding.profpic.setImageURI(uri)
            user?.let { user ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri)
                    .build()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        user.updateProfile(profileUpdates).await()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Successfully updated profile",
                                Toast.LENGTH_LONG).show()
                        }
                    } catch(e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                        }
                    }

                }
            }
        }
    }
}