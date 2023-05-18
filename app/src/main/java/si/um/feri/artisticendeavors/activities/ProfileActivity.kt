package si.um.feri.artisticendeavors.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.EditText
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
import com.squareup.picasso.Picasso
import si.um.feri.artisticendeavors.ActivitySwitcher
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.Toolbar
import si.um.feri.artisticendeavors.adapters.ProfilePostAdapter
import si.um.feri.artisticendeavors.data.Post
import si.um.feri.artisticendeavors.data.User
import si.um.feri.artisticendeavors.databinding.ActivityProfileBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.UUID

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
    private var downloadUrl: String? = null
    private lateinit var postText: String
    private lateinit var tag: String
    private lateinit var toolbar: Toolbar
    private lateinit var activitySwitcher: ActivitySwitcher

    private fun color(text: String, colorHex: String): Spannable {
        val spannableString = SpannableString(text)
        val colorSpan = ForegroundColorSpan(Color.parseColor(colorHex))
        spannableString.setSpan(colorSpan, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }

    @RequiresApi(Build.VERSION_CODES.P)
    val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    // Convert image into Bitmap
                    val bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(contentResolver, imageUri)
                    )

                    // Convert the Bitmap to ByteArray
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val data = byteArrayOutputStream.toByteArray()

                    imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

                    // Upload the image to Firebase Storage
                    binding.progress.visibility = View.VISIBLE

                    val uploadTask = imageRef.putBytes(data)
                    uploadTask.addOnSuccessListener {
                        binding.progress.visibility = View.GONE
                        // Image uploaded successfully
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            downloadUrl = uri.toString()
                            val message =
                                getString(R.string.image_uploaded_successfully, downloadUrl)
                            Timber.d(tag, message)

                            // Launch the dialog for adding a post description
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(
                                color(
                                    getString(R.string.new_post_description),
                                    "#E36363"
                                )
                            )

                            // Set up the input
                            val input = EditText(this)
                            input.inputType = InputType.TYPE_CLASS_TEXT
                            builder.setView(input)

                            // Set up the buttons
                            builder.setPositiveButton(
                                color(
                                    getString(R.string.save),
                                    "#E36363"
                                )
                            ) { _, _ ->
                                postText = input.text.toString().trim()
                                if (postText.isNotEmpty()) {
                                    val currentUser = auth.currentUser
                                    // Create a new post object with the image URL and description
                                    val newPost = Post(
                                        user = currentUser?.displayName?.let { it1 -> User(username = it1) },
                                        description = postText,
                                        creation_time_milliseconds = System.currentTimeMillis(),
                                        image_url = downloadUrl
                                    )

                                    db.collection("posts")
                                        .add(newPost)
                                        .addOnSuccessListener { documentReference ->
                                            // Get the document ID and update the new post object with it
                                            val postId = documentReference.id
                                            newPost.id = postId

                                            // Update the post document with the generated ID
                                            db.collection("posts")
                                                .document(postId)
                                                .set(newPost)
                                                .addOnSuccessListener {
                                                    // Log the document ID to verify that it was added correctly
                                                    val errorMessage =
                                                        getString(
                                                            R.string.document_snapshot_written_with_id,
                                                            postId
                                                        )
                                                    Timber.d(tag, errorMessage)
                                                }
                                                .addOnFailureListener { e ->

                                                    val errorMessage =
                                                        getString(
                                                            R.string.error_updating_document,
                                                            e
                                                        )
                                                    Timber.e(tag, errorMessage)
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            val errorMessage =
                                                getString(R.string.error_adding_document, e)
                                            Timber.e(tag, errorMessage)
                                        }
                                }
                            }
                                .setNegativeButton(
                                    color(
                                        getString(R.string.cancel),
                                        "#84B589"
                                    )
                                ) { dialog, _ ->
                                    dialog.cancel()
                                    // Delete the file
                                    imageRef.delete()
                                        .addOnSuccessListener {
                                            // File deleted successfully
                                            val errorMessage =
                                                getString(R.string.image_deleted_successfully)
                                            Timber.d(tag, errorMessage)
                                        }
                                        .addOnFailureListener {
                                            // Uh-oh, an error occurred!
                                            val errorMessage =
                                                getString(R.string.error_deleting_image, it)
                                            Timber.d(tag, errorMessage)
                                        }
                                }

                            builder.show()
                        }.addOnFailureListener { exception ->
                            val errorMessage =
                                getString(R.string.error_getting_download_url, exception)
                            Timber.e(tag, errorMessage)
                        }
                    }.addOnFailureListener { exception ->
                        binding.progress.visibility = View.GONE
                        // Image upload failed
                        val errorMessage =
                            getString(R.string.image_upload_failed, exception)
                        Timber.e(tag, errorMessage)
                    }.addOnProgressListener { taskSnapshot ->
                        // Update the progress bar
                        val progress =
                            (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                        binding.progressBar.progress = progress
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

        posts = mutableListOf()
        adapter = ProfilePostAdapter(this, posts)

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(this)

        val currentUsername = auth.currentUser?.displayName

        binding.usern.text = currentUsername

        val imRef = storageRef.child("images/${currentUsername}.jpg")

        // Load profile image
        imRef.downloadUrl
            .addOnSuccessListener { uri ->
                // Use Picasso to load the image into the ImageView
                Picasso.get().load(uri).into(binding.profpic)
            }
            .addOnFailureListener { exception ->
                // Handle any errors that may occur
                val errorMessage =
                    getString(R.string.error_downloading_image, exception)
                Timber.e(tag, errorMessage)
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
                // Launch the gallery
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                galleryLauncher.launch(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        listenerRegistration.remove()
    }
}