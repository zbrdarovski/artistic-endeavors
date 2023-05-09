package si.um.feri.artisticendeavors

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.databinding.ProfileItemPostBinding
import timber.log.Timber

class ProfilePostAdapter(private val posts: MutableList<Post>) :
    RecyclerView.Adapter<ProfilePostAdapter.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private val db = Firebase.firestore
    private val currentUserId = auth.currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ProfileItemPostBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
        holder.setDeleteClickListener(post)
    }

    inner class ViewHolder(private val binding: ProfileItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val username = post.user?.username as String
            binding.tvUsername.text = username
            binding.tvDescription.text = post.description
            Picasso.get().load(post.image_url).into(binding.ivPost)

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
                post.creation_time_milliseconds?.let { DateUtils.getRelativeTimeSpanString(it) }
        }

        fun setDeleteClickListener(post: Post) {
            binding.deletePost.setOnClickListener {
                val builder = AlertDialog.Builder(binding.root.context)
                builder.setTitle("Delete Post")
                    .setMessage("Are you sure you want to delete this post?")
                    .setPositiveButton("Yes") { _, _ ->
                        // Get the position of the post in the list
                        val index = posts.indexOf(post)

                        // Remove the post from the list and notify the adapter
                        if (index != -1) {
                            posts.removeAt(index)
                            notifyItemRemoved(index)
                        }

                        // Delete the post
                        val postRef = db.collection("posts").document(post.id!!)
                        postRef.delete()
                            .addOnSuccessListener {
                                Timber.d("Post deleted successfully")
                            }
                            .addOnFailureListener { e ->
                                Timber.w("Error deleting post", e)
                            }
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}