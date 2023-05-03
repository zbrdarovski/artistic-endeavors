package si.um.feri.artisticendeavors

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.databinding.ItemPostBinding
import timber.log.Timber

class PostAdapter(private val posts: List<Post>) :
    RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPostBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    inner class ViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            val username = post.user?.username as String
            binding.tvUsername.text = username
            binding.tvDescription.text = post.description
            Picasso.get().load(post.image_url).into(binding.ivPost)

            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val storage = Firebase.storage
            val storageRef = storage.reference
            val db = Firebase.firestore
            val currentUserId = user?.uid

            db.collection("users").document(currentUserId!!)
                .get()
                .continueWith {
                    val imageRef = storageRef.child("images/${username}.jpg")
                    imageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            // Use Picasso to load the image into the ImageView
                            Picasso.get().load(uri).into(binding.ivProfileImage)
                        }
                        .addOnFailureListener { exception ->
                            // Handle any errors that may occur
                            Timber.e("TAG", "Error downloading image: $exception")
                        }
                }
            binding.tvRelativeTime.text =
                DateUtils.getRelativeTimeSpanString(post.creation_time_milliseconds)
        }
    }
}