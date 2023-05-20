package si.um.feri.artisticendeavors.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import si.um.feri.artisticendeavors.ActivitySwitcher
import si.um.feri.artisticendeavors.Photoshop
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.Toolbar
import si.um.feri.artisticendeavors.adapters.ProfilePostAdapter
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.databinding.ActivityProfileBinding
import timber.log.Timber

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: ProfilePostAdapter
    private lateinit var listenerRegistration: ListenerRegistration
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private lateinit var tag: String
    private lateinit var toolbar: Toolbar
    private lateinit var activitySwitcher: ActivitySwitcher
    private lateinit var photoshop: Photoshop

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tag = getString(R.string.profile_activity)
        db = Firebase.firestore
        activitySwitcher = ActivitySwitcher()
        toolbar = Toolbar(
            this, auth, binding.includeToolbar.homeOption,
            binding.includeToolbar.profileOption, binding.includeToolbar.accountOption,
            binding.includeToolbar.actionSignOut
        )
        toolbar.bindToolbar()

        posts = mutableListOf()
        adapter = ProfilePostAdapter(this, posts)

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        val activityResultRegistry =
            this.activityResultRegistry // Replace 'activity' with your actual Activity reference
        photoshop = Photoshop(this, activityResultRegistry)

        val currentUsername = auth.currentUser?.displayName

        binding.usern.text = currentUsername

        val profileImageReference = storageRef.child("images/${currentUsername}.jpg")

        // Load profile image
        photoshop.loadImage(profileImageReference, binding.profileImage)


        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Timber.e(tag, exception?.message)
                return@addSnapshotListener
            }
            val listOfPosts = snapshot.toObjects(Post::class.java)
            val iterator = listOfPosts.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                if (p.user?.username != currentUsername) {
                    iterator.remove()
                }
            }
            if (listOfPosts.isEmpty()) {
                binding.noPosts.visibility = View.VISIBLE
            } else {
                binding.noPosts.visibility = View.GONE
                posts.clear()
                posts.addAll(listOfPosts)
                adapter.apply {
                    notifyItemRangeRemoved(0, itemCount)
                    notifyItemRangeInserted(0, posts.size)
                }
            }
        }

        // Add new post
        binding.addPost.setOnClickListener {
            // TO - DO
            Toast.makeText(
                this@ProfileActivity,
                "TO - DO",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        listenerRegistration.remove()
    }
}