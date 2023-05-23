package si.um.feri.artisticendeavors.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import si.um.feri.artisticendeavors.tools.Toolbar
import si.um.feri.artisticendeavors.adapters.MainPostAdapter
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val db: FirebaseFirestore by lazy {
        Firebase.firestore
    }
    private val posts: MutableList<Post> = mutableListOf()
    private val adapter: MainPostAdapter by lazy {
        MainPostAdapter(this, posts)
    }
    private lateinit var listenerRegistration: ListenerRegistration
    private lateinit var toolbar: Toolbar

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            handleSnapshot(snapshot, exception)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration.remove()
    }

    private fun handleSnapshot(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
        if (exception != null || snapshot == null) {
            Timber.e(exception?.message)
            return
        }

        val listOfPosts = snapshot.toObjects(Post::class.java)
        binding.noPosts.visibility = if (listOfPosts.isEmpty()) View.VISIBLE else View.GONE

        val previousSize = posts.size
        posts.apply {
            clear()
            addAll(listOfPosts)
        }
        val newSize = posts.size

        if (previousSize == newSize) {
            // Notify all items changed if the size remains the same
            adapter.notifyItemRangeChanged(0, newSize)
        } else if (previousSize < newSize) {
            // New items added, notify only the new range
            adapter.notifyItemRangeInserted(previousSize, newSize - previousSize)
        } else {
            // Items removed, notify only the previous range
            adapter.notifyItemRangeRemoved(newSize, previousSize - newSize)
        }
    }
}