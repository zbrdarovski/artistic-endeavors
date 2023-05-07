package si.um.feri.artisticendeavors

data class Post(
    val creation_time_milliseconds: Long? = null,
    val description: String? = null,
    val id: String? = null,
    val image_url: String? = null,
    val user: User? = null
)
