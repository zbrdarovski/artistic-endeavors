package artisticendeavors.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
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
import artisticendeavors.tools.ActivitySwitcher
import artisticendeavors.tools.Photoshop
import artisticendeavors.R
import artisticendeavors.tools.Toolbar
import artisticendeavors.adapters.ProfilePostAdapter
import artisticendeavors.data.Post
import artisticendeavors.data.User
import artisticendeavors.databinding.ActivityProfileBinding
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
    private var selectedCategory: String = ""

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

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = parent?.getItemAtPosition(position).toString()
                adapter.clear() // Clear the adapter
                updatePostsReference()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle when no category is selected
            }
        }

        updatePostsReference()
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration.remove()
    }

    private fun updatePostsReference() {
        val currentUsername = auth.currentUser?.displayName
        val postsReference = db.collection("posts")
            .whereEqualTo("user.username", currentUsername)
            .whereEqualTo("category", selectedCategory)
            .limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)

        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Timber.e(tag, "Error fetching posts: $exception")
                return@addSnapshotListener
            }

            val listOfPosts = mutableListOf<Post>()
            for (document in snapshot.documents) {
                val post = document.toObject(Post::class.java)
                if (post != null) {
                    listOfPosts.add(post)
                }
            }

            val isEmpty = listOfPosts.isEmpty()

            if (!isEmpty) {
                val diffCallback = ProfilePostDiffCallback(posts, listOfPosts)
                val diffResult = DiffUtil.calculateDiff(diffCallback)

                posts.clear()
                posts.addAll(listOfPosts)
                diffResult.dispatchUpdatesTo(adapter)
            }
        }
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
