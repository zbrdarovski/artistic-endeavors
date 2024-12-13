package artisticendeavors.activities

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import artisticendeavors.tools.ActivitySwitcher
import artisticendeavors.tools.Messenger
import artisticendeavors.R
import artisticendeavors.databinding.ActivityAddPostBinding
import timber.log.Timber
import java.util.*

class EditPostDescriptionActivity : AppCompatActivity() {

    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val messenger by lazy { Messenger(this) }
    private val activitySwitcher: ActivitySwitcher by lazy { ActivitySwitcher() }
    private lateinit var binding: ActivityAddPostBinding

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tag = "EditPostDescriptionActivity"

        val imageUrl = intent.getStringExtra("image_url")
        binding.imageView.load(imageUrl)

        val description = intent.getStringExtra("description")
        binding.inputText.setText(description)

        binding.inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this implementation
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed for this implementation
            }

            override fun afterTextChanged(s: Editable?) {
                binding.sendButton.isEnabled = s.toString() != description
            }
        })

        binding.sendButton.isEnabled = false

        binding.sendButton.setOnClickListener {
            if (!binding.inputText.text.isNullOrEmpty()) {
                val newDescription = binding.inputText.text.toString().trim()
                val postId = intent.getStringExtra("post_id")
                val postRef = db.collection("posts").document(postId!!)
                postRef.update("description", newDescription).addOnSuccessListener {
                    activitySwitcher.startNewActivity(this, ProfileActivity::class.java)
                    messenger.message("Post updated successfully")
                }.addOnFailureListener { e ->
                    val errorMessage = getString(R.string.error_updating_post_description, e)
                    Timber.w(tag, errorMessage)
                }
            } else {
                messenger.message("Post description is necessary")
            }
        }

        binding.cancelButton.setOnClickListener {
            activitySwitcher.startNewActivity(this, ProfileActivity::class.java)
        }
    }
}