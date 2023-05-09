package si.um.feri.artisticendeavors

data class Post(
    val creation_time_milliseconds: Long? = null,
    var description: String? = null,
    var id: String? = null,
    var image_url: String? = null,
    val user: User? = null
)
