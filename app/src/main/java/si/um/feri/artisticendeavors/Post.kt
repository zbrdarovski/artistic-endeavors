package si.um.feri.artisticendeavors

import android.graphics.Bitmap

data class Post(
    val id: Int,
    val content: String,
    val bitmap: Bitmap
)