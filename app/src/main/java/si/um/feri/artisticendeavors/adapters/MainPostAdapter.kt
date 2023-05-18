package si.um.feri.artisticendeavors.adapters

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.activities.FullSizeImageActivity
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.databinding.MainItemPostBinding
import timber.log.Timber

class MainPostAdapter(private val context: Context, private val posts: List<Post>) :
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
            val username = post.user?.username as String
            binding.tvUsername.text = username
            binding.tvDescription.text = post.description
            Picasso.get().load(post.image_url).into(binding.ivPost)

            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val db = FirebaseFirestore.getInstance()
            val currentUserId = user?.uid

            // Add this code to open the full-size image when the user clicks on it
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
                                Picasso.get().load(uri).into(binding.ivProfileImage)
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