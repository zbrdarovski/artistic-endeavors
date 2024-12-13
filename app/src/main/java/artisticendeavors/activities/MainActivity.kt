package artisticendeavors.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
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
import artisticendeavors.tools.Toolbar
import artisticendeavors.adapters.MainPostAdapter
import artisticendeavors.data.Post
import artisticendeavors.databinding.ActivityMainBinding
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

    private var currentFilter: String? = null

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

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle when no category is selected
            }
        }

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

    @SuppressLint("NotifyDataSetChanged")
    private fun handleSnapshot(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
        if (exception != null || snapshot == null) {
            Timber.e(exception?.message)
            return
        }

        val listOfPosts = snapshot.toObjects(Post::class.java)

        posts.clear()
        posts.addAll(listOfPosts.filter { post ->
            currentFilter?.let {
                post.category == it
            } ?: true
        })

        adapter.notifyDataSetChanged()
    }

    private fun applyFilter() {
        currentFilter = binding.categorySpinner.selectedItem.toString()
        listenerRegistration.remove()

        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
        if (currentFilter != null) {
            postsReference.whereEqualTo("category", currentFilter)
        }

        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            handleSnapshot(snapshot, exception)
        }
    }
}