package si.um.feri.artisticendeavors.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import si.um.feri.artisticendeavors.ActivitySwitcher
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.Toolbar
import si.um.feri.artisticendeavors.adapters.MainPostAdapter
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.databinding.ActivityMainBinding
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: MainPostAdapter
    private lateinit var listenerRegistration: ListenerRegistration

    private lateinit var tag: String
    private lateinit var toolbar: Toolbar
    private lateinit var activitySwitcher: ActivitySwitcher

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tag = getString(R.string.main_activity)
        db = Firebase.firestore
        activitySwitcher = ActivitySwitcher()
        toolbar = Toolbar(
            this, auth, binding.includeToolbar.homeOption,
            binding.includeToolbar.profileOption, binding.includeToolbar.accountOption,
            binding.includeToolbar.actionSignOut
        )
        toolbar.bindToolbar()

        posts = mutableListOf()
        adapter = MainPostAdapter(this, posts)

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)


        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Timber.e(tag, exception?.message)
                return@addSnapshotListener
            }
            val listOfPosts = snapshot.toObjects(Post::class.java)
            if (listOfPosts.isEmpty()) {
                binding.noPosts.visibility = View.VISIBLE
            } else {
                binding.noPosts.visibility = View.GONE
                posts.clear()
                posts.addAll(listOfPosts)
                adapter.apply {
                    notifyItemRangeRemoved(0, itemCount)
                    notifyItemRangeInserted(0, posts.size)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration.remove()
    }
}