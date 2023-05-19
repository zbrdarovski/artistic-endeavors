package si.um.feri.artisticendeavors.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import si.um.feri.artisticendeavors.ActivitySwitcher
import si.um.feri.artisticendeavors.Color
import si.um.feri.artisticendeavors.Photoshop
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.Toolbar
import si.um.feri.artisticendeavors.adapters.ProfilePostAdapter
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.data.User
import si.um.feri.artisticendeavors.databinding.ActivityProfileBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var db: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: ProfilePostAdapter
    private lateinit var listenerRegistration: ListenerRegistration
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private lateinit var imageRef: StorageReference
    private lateinit var tag: String
    private lateinit var toolbar: Toolbar
    private lateinit var activitySwitcher: ActivitySwitcher
    private lateinit var color: Color
    private lateinit var photoshop: Photoshop

    private fun launchDialog(downloadUrl: String, imageUri: Uri) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(color.colorize(getString(R.string.new_post_description), "#E36363"))

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton(color.colorize(getString(R.string.save), "#E36363")) { _, _ ->
            val postText = input.text.toString().trim()
            if (postText.isNotEmpty()) {
                val currentUser = auth.currentUser
                val newPost = Post(
                    user = currentUser?.displayName?.let { User(username = it) },
                    description = postText,
                    creation_time_milliseconds = System.currentTimeMillis(),
                    image_url = downloadUrl
                )

                db.collection("posts")
                    .add(newPost)
                    .addOnSuccessListener { documentReference ->
                        val postId = documentReference.id
                        newPost.id = postId

                        db.collection("posts")
                            .document(postId)
                            .set(newPost)
                            .addOnSuccessListener {
                                val errorMessage =
                                    getString(R.string.document_snapshot_written_with_id, postId)
                                Timber.d(tag, errorMessage)
                            }
                            .addOnFailureListener { e ->
                                val errorMessage = getString(R.string.error_updating_document, e)
                                Timber.e(tag, errorMessage)
                            }
                    }
                    .addOnFailureListener { e ->
                        val errorMessage = getString(R.string.error_adding_document, e)
                        Timber.e(tag, errorMessage)
                    }
            }
        }
            .setNegativeButton(color.colorize(getString(R.string.cancel), "#84B589")) { dialog, _ ->
                dialog.cancel()
                imageRef.delete()
                    .addOnSuccessListener {
                        val errorMessage = getString(R.string.image_deleted_successfully)
                        Timber.d(tag, errorMessage)
                    }
                    .addOnFailureListener {
                        val errorMessage = getString(R.string.error_deleting_image, it)
                        Timber.d(tag, errorMessage)
                    }
            }

        val dialog = builder.create()

        // Show the progress bar in the dialog's message area
        val progressBar = ProgressBar(this)
        dialog.setView(progressBar, 50, 0, 50, 0)

        // Retrieve the bitmap from the selected URI
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Convert the Bitmap to ByteArray
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        // Set up the progress listener for the upload task
        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Image uploaded successfully
            val progress =
                (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progressBar.progress = progress
        }.addOnFailureListener { exception ->
            // Image upload failed
            Timber.e(tag, getString(R.string.image_upload_failed, exception))
        }.addOnProgressListener { taskSnapshot ->
            // Update the progress bar
            val progress =
                (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progressBar.progress = progress
        }.addOnCompleteListener {
            // Dismiss the dialog when the upload task completes
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    photoshop.handleImageSelection(imageUri) { downloadUrl ->
                        // Handle further actions specific to the first activity
                        launchDialog(downloadUrl, imageUri)
                    }
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tag = getString(R.string.profile_activity)
        db = Firebase.firestore
        activitySwitcher = ActivitySwitcher()
        toolbar = Toolbar(
            this, auth, binding.includeToolbar.homeOption,
            binding.includeToolbar.profileOption, binding.includeToolbar.accountOption,
            binding.includeToolbar.actionSignOut
        )
        toolbar.bindToolbar()
        color = Color()
        photoshop = Photoshop(this)

        posts = mutableListOf()
        adapter = ProfilePostAdapter(this, posts)

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        val currentUsername = auth.currentUser?.displayName

        binding.usern.text = currentUsername

        val imRef = storageRef.child("images/${currentUsername}.jpg")

        // Get the download URL as a string
        imRef.downloadUrl.addOnSuccessListener { uri ->
            val downloadUrl = uri.toString()
            // Use the download URL as needed
            // Example: Log the URL
            photoshop.loadProfileImage(downloadUrl, binding.profpic)
            Timber.d(tag, downloadUrl)
        }.addOnFailureListener { exception ->
            // Handle any errors that may occur
            Timber.e(tag, "Error getting download URL: $exception")
        }

        val postsReference = db.collection("posts").limit(20)
            .orderBy("creation_time_milliseconds", Query.Direction.DESCENDING)
        listenerRegistration = postsReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Timber.e(tag, exception?.message)
                return@addSnapshotListener
            }
            val listOfPosts = snapshot.toObjects(Post::class.java)
            val iterator = listOfPosts.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                if (p.user?.username != currentUsername) {
                    iterator.remove()
                }
            }
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

        // Add new post
        binding.fabCreate.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Create new post
                galleryLauncher.launch(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        listenerRegistration.remove()
    }
}