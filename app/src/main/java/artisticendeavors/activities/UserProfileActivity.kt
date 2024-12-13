package artisticendeavors.activities

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import artisticendeavors.R
import artisticendeavors.adapters.UserProfilePostAdapter
import artisticendeavors.data.Post
import artisticendeavors.data.User
import artisticendeavors.databinding.ActivityUserProfileBinding
import artisticendeavors.tools.Photoshop
import timber.log.Timber

class UserProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserProfileBinding
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val posts: MutableList<Post> = mutableListOf()
    private val adapter: UserProfilePostAdapter by lazy { UserProfilePostAdapter(this, posts) }
    private lateinit var listenerRegistration: ListenerRegistration
    private val storage: FirebaseStorage by lazy { Firebase.storage }
    private val storageRef: StorageReference by lazy { storage.reference }
    private val tag: String by lazy { getString(R.string.user_profile_activity) }
    private val photoshop: Photoshop by lazy { Photoshop(this, activityResultRegistry) }
    private var userId: String? = null
    private var selectedCategory: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        userId = intent.getStringExtra("user_id")

        if (userId != null) {
            val userReference = db.collection("users").document(userId!!)

            userReference.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    val description = document.getString("biography")

                    // Create a User object and set its id
                    val user = User(
                        username = username ?: "", biography = description ?: "", id = userId
                    )

                    // Access the username, description, and profile image URL
                    if (user.username.isNotEmpty()) {
                        binding.usern.text = user.username
                        val profileImageReference = storageRef.child("images/${user.username}.jpg")
                        photoshop.loadImage(profileImageReference, binding.profileImage)
                    }

                    if (user.biography.isNotEmpty()) {
                        // Use the description here
                        binding.biography.text = user.biography
                    }
                }
            }.addOnFailureListener { e ->
                // Handle any potential errors
                val errorMessage = getString(R.string.error_getting_user_data, e)
                Timber.e(tag, errorMessage)
            }
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
        val postsReference =
            db.collection("posts")
                .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
                .whereEqualTo("user.id", userId)
                .whereEqualTo("category", selectedCategory)

        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Timber.e(tag, "Error fetching posts: $exception")
                return@addSnapshotListener
            }

            val listOfPosts = snapshot.toObjects(Post::class.java)

            val diffCallback = UserProfilePostDiffCallback(posts, listOfPosts)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            posts.clear()
            posts.addAll(listOfPosts)
            diffResult.dispatchUpdatesTo(adapter)
        }
    }

    private inner class UserProfilePostDiffCallback(
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