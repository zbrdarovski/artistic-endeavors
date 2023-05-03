package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import si.um.feri.artisticendeavors.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: PostAdapter

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore
        posts = mutableListOf()
        adapter = PostAdapter(posts)

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        var isEmpty = true

        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
        postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Timber.e(exception?.message)
                return@addSnapshotListener
            }
            val listOfPosts = snapshot.toObjects(Post::class.java)
            if (listOfPosts.isNotEmpty()) {
                isEmpty = false
            }
            posts.clear()
            posts.addAll(listOfPosts)
            adapter.notifyDataSetChanged()
        }

        if (isEmpty) {
            val post = Post(
                creation_time_milliseconds = System.currentTimeMillis(),
                description = "Unfortunately, there are currently no posts to display. :(",
                image_url = "https://picsum.photos/200/300",
                user = auth.currentUser?.displayName?.let { User(it) }
            )
            posts.clear()
            posts.add(post)
            adapter.notifyDataSetChanged()
        }

        // Switch to MainActivity
        binding.homeOption.setOnClickListener {
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Switch to ProfileActivity
        binding.profileOption.setOnClickListener {
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Switch to AccountActivity
        binding.accountOption.setOnClickListener {
            val intent = Intent(this@MainActivity, AccountActivity::class.java)
            startActivity(intent)
        }

        // Sign out from the app
        binding.actionSignOut.setOnClickListener {
            this.auth.signOut()
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()

            Toast.makeText(
                this@MainActivity,
                "You logged out successfully.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}