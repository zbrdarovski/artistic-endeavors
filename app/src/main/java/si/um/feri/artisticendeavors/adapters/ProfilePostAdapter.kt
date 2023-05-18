package si.um.feri.artisticendeavors.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.activities.FullSizeImageActivity
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.databinding.ProfileItemPostBinding
import timber.log.Timber

class ProfilePostAdapter(private val context: Context, private val posts: MutableList<Post>) :
    RecyclerView.Adapter<ProfilePostAdapter.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val tag: String = "ProfilePostAdapter"

    fun color(text: String, colorHex: String): Spannable {
        val spannableString = SpannableString(text)
        val colorSpan = ForegroundColorSpan(Color.parseColor(colorHex))
        spannableString.setSpan(colorSpan, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }

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
        holder.setEditClickListener(post)
    }

    inner class ViewHolder(private val binding: ProfileItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val username = post.user?.username as String
            binding.tvUsername.text = username
            binding.tvDescription.text = post.description
            Picasso.get().load(post.image_url).into(binding.ivPost)

            // Add this code to open the full-size image when the user clicks on it
            binding.ivPost.setOnClickListener {
                val intent = Intent(binding.root.context, FullSizeImageActivity::class.java)
                intent.putExtra("image_url", post.image_url)
                binding.root.context.startActivity(intent)
            }

            db.collection("users").document(currentUserId!!)
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

            binding.tvRelativeTime.text =
                post.creation_time_milliseconds?.let { DateUtils.getRelativeTimeSpanString(it) }
        }

        fun setDeleteClickListener(post: Post) {
            binding.deletePost.setOnClickListener {
                val builder = AlertDialog.Builder(binding.root.context)
                builder.setTitle(
                    color(
                        context.getString(R.string.confirm_post_deletion),
                        "#E36363"
                    )
                )
                    .setPositiveButton(
                        color(
                            context.getString(R.string.delete),
                            "#E36363"
                        )
                    ) { _, _ ->
                        // Get the position of the post in the list
                        val index = posts.indexOf(post)

                        // Remove the post from the list and notify the adapter
                        if (index != -1) {
                            posts.removeAt(index)
                            notifyItemRemoved(index)
                        }

                        // Delete the post
                        val postRef = db.collection("posts").document(post.id!!)
                        postRef.get().addOnSuccessListener { documentSnapshot ->
                            val p = documentSnapshot.toObject(Post::class.java)
                            p?.image_url?.let { imageUrl ->
                                val imageRef = Firebase.storage.getReferenceFromUrl(imageUrl)
                                imageRef.delete().addOnSuccessListener {
                                    val errorMessage =
                                        context.getString(R.string.image_deleted_successfully)
                                    Timber.d(tag, errorMessage)
                                }.addOnFailureListener { e ->
                                    val errorMessage =
                                        context.getString(R.string.error_deleting_image, e)
                                    Timber.w(tag, errorMessage)
                                }
                            }
                            postRef.delete()
                                .addOnSuccessListener {
                                    val errorMessage =
                                        context.getString(R.string.post_deleted_successfully)
                                    Timber.d(tag, errorMessage)
                                }
                                .addOnFailureListener { e ->
                                    val errorMessage =
                                        context.getString(R.string.error_deleting_post, e)
                                    Timber.w(tag, errorMessage)
                                }
                        }
                    }
                    .setNegativeButton(
                        color(
                            context.getString(R.string.cancel),
                            "#84B589"
                        )
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        fun setEditClickListener(post: Post) {
            binding.editPost.setOnClickListener {
                // Create an EditText view for user input
                val editText = EditText(binding.root.context)
                editText.setText(post.description)

                // Create an AlertDialog to display the EditText view
                val builder = AlertDialog.Builder(binding.root.context)
                builder.setTitle(color(context.getString(R.string.edit_post), "#E36363"))
                    .setView(editText)
                    .setPositiveButton(
                        color(
                            context.getString(R.string.save),
                            "#E36363"
                        )
                    ) { _, _ ->
                        // Get the new description entered by the user
                        val newDescription = editText.text.toString().trim()

                        // Update the post's description in the database
                        val postRef = db.collection("posts").document(post.id!!)
                        postRef.update("description", newDescription)
                            .addOnSuccessListener {
                                // Update the post object and notify the adapter
                                post.description = newDescription
                                notifyItemChanged(posts.indexOf(post))
                            }
                            .addOnFailureListener { e ->
                                val errorMessage =
                                    context.getString(R.string.error_updating_post_description, e)
                                Timber.w(tag, errorMessage)
                            }
                    }
                    .setNegativeButton(
                        color(
                            context.getString(R.string.cancel),
                            "#84B589"
                        )
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}