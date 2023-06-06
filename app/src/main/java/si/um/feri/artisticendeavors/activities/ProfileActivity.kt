package si.um.feri.artisticendeavors.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import si.um.feri.artisticendeavors.tools.ActivitySwitcher
import si.um.feri.artisticendeavors.tools.Photoshop
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.tools.Toolbar
import si.um.feri.artisticendeavors.adapters.ProfilePostAdapter
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.data.User
import si.um.feri.artisticendeavors.databinding.ActivityProfileBinding
import timber.log.Timber

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val posts: MutableList<Post> = mutableListOf()
    private val adapter: ProfilePostAdapter by lazy { ProfilePostAdapter(this, posts) }
    private lateinit var listenerRegistration: ListenerRegistration
    private val storage: FirebaseStorage by lazy { Firebase.storage }
    private val storageRef: StorageReference by lazy { storage.reference }
    private val tag: String by lazy { getString(R.string.profile_activity) }
    private val photoshop: Photoshop by lazy { Photoshop(this, activityResultRegistry) }
    private lateinit var toolbar: Toolbar

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = Toolbar(
            this,
            auth,
            binding.includeToolbar.homeOption,
            binding.includeToolbar.profileOption,
            binding.includeToolbar.accountOption,
            binding.includeToolbar.actionSignOut
        )
        toolbar.bindToolbar()

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        val currentUsername = auth.currentUser?.displayName
        binding.usern.text = currentUsername

        val profileImageReference = storageRef.child("images/${currentUsername}.jpg")

        photoshop.loadImage(profileImageReference, binding.profileImage)
        val userReference = db.collection("users")
            .whereEqualTo("username", auth.currentUser?.displayName)

        userReference.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0] // Only one matching user
                    val userData = documentSnapshot.toObject(User::class.java)
                    val description = userData?.biography

                    // Access the description field
                    if (description != null) {
                        // Use the description here
                        binding.biography.text = description
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle any potential errors
                val errorMessage = getString(R.string.error_getting_user_data, e)
                Timber.e(tag, errorMessage)
            }



        binding.addPost.setOnClickListener {
            val activitySwitcher = ActivitySwitcher()
            activitySwitcher.startNewActivity(this, AddPostActivity::class.java)
        }

        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)

        listenerRegistration = lazy {
            postsReference.addSnapshotListener { snapshot, exception ->
                if (exception != null || snapshot == null) {
                    Timber.e(tag, "Error fetching posts: $exception")
                    return@addSnapshotListener
                }

                val listOfPosts = snapshot.toObjects(Post::class.java)
                val filteredPosts = listOfPosts.filter { it.user?.username == currentUsername }

                binding.noPosts.visibility =
                    if (filteredPosts.isEmpty()) View.VISIBLE else View.GONE

                val diffCallback = ProfilePostDiffCallback(posts, filteredPosts)
                val diffResult = DiffUtil.calculateDiff(diffCallback)

                posts.clear()
                posts.addAll(filteredPosts)
                diffResult.dispatchUpdatesTo(adapter)
            }
        }.value
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration.remove()
    }

    private inner class ProfilePostDiffCallback(
        private val oldList: List<Post>, private val newList: List<Post>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}