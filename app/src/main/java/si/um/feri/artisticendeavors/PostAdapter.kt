package si.um.feri.artisticendeavors

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.databinding.ItemPostBinding
import timber.log.Timber

class PostAdapter(private val context: Context, private val posts: List<Post>)
    : RecyclerView.Adapter<PostAdapter.ViewHolder>() {


    // Usually involves inflating a layout from XML and returning the holder - THIS IS EXPENSIVE
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // Returns the total count of items in the list
    override fun getItemCount() = posts.size

    // Involves populating data into the item through holder - NOT expensive
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Timber.i("onBindViewHolder at position $position")
        val post = posts[position]
        holder.bind(post)
    }

    inner class ViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.tvTitle.text = post.title
            val description = post.description
            val formattedDescription = context.applicationContext.getString(R.string.post_description, description)
            binding.tvDescription.text = formattedDescription

            Picasso.get().load(post.imageUrl).into(binding.ivPost)
        }
    }
}