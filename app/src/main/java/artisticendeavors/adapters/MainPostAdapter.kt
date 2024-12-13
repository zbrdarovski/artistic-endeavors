package artisticendeavors.adapters

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import artisticendeavors.R
import artisticendeavors.activities.FullSizeImageActivity
import artisticendeavors.activities.UserProfileActivity
import artisticendeavors.data.Post
import artisticendeavors.databinding.MainItemPostBinding
import timber.log.Timber

class MainPostAdapter(private val context: Context, private val posts: MutableList<Post>) :
    RecyclerView.Adapter<MainPostAdapter.ViewHolder>() {

    private val tag = "MainPostAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MainItemPostBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    inner class ViewHolder(private val binding: MainItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val username = post.user?.username ?: ""
            binding.tvUsername.text = username
            binding.tvDescription.text = post.description
            binding.ivPost.load(post.image_url)
            binding.tvCategory.text = post.category

            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val db = FirebaseFirestore.getInstance()
            val currentUserId = user?.uid

            // Load the profile picture into the ImageView
            db.collection("users").document(post.user?.id ?: "").get()
                .addOnSuccessListener { documentSnapshot ->
                    val profileImageUrl = documentSnapshot.getString("profile_image_url")
                    profileImageUrl?.let {
                        val imageRef = storageRef.child(it)
                        binding.ivProfileImage.load(imageRef)
                    }
                }
                .addOnFailureListener { e ->
                    // Handle any errors that may occur
                    Timber.e("Error fetching profile image URL: $e")
                }

            // Add click listener to the username TextView
            binding.tvUsername.setOnClickListener {
                val intent = Intent(binding.root.context, UserProfileActivity::class.java)
                intent.putExtra("user_id", post.user?.id) // Pass the post user's ID to the profile activity
                binding.root.context.startActivity(intent)
            }

            // Add click listener to the profile image ImageView
            binding.ivProfileImage.setOnClickListener {
                val intent = Intent(binding.root.context, UserProfileActivity::class.java)
                intent.putExtra("user_id", post.user?.id) // Pass the post user's ID to the profile activity
                binding.root.context.startActivity(intent)
            }

            // Add click listener to open the full-size image when the user clicks on it
            binding.ivPost.setOnClickListener {
                val intent = Intent(binding.root.context, FullSizeImageActivity::class.java)
                intent.putExtra("image_url", post.image_url)
                binding.root.context.startActivity(intent)
            }

            if (currentUserId != null) {
                db.collection("users").document(currentUserId)
                db.collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener {
                        val imageRef = storageRef.child("images/${username}.jpg")
                        imageRef.downloadUrl
                            .addOnSuccessListener { uri ->
                                // Use Picasso to load the image into the ImageView
                                binding.ivProfileImage.load(uri)
                            }
                            .addOnFailureListener { exception ->
                                // Handle any errors that may occur
                                val errorMessage =
                                    context.getString(R.string.error_downloading_image, exception)
                                Timber.e(tag, errorMessage)
                            }
                    }
                    .addOnFailureListener { exception ->
                        // Handle any errors that may occur
                        val errorMessage =
                            context.getString(R.string.error_loading_user_data, exception)
                        Timber.e(tag, errorMessage)
                    }
            }

            binding.tvRelativeTime.text =
                post.creation_time_milliseconds?.let { DateUtils.getRelativeTimeSpanString(it) }
        }
    }
}
