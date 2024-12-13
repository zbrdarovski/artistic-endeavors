package artisticendeavors.data

data class Post(
    val creation_time_milliseconds: Long? = null,
    var description: String? = null,
    var id: String? = null,
    var image_url: String? = null,
    val category: String? = null,
    val user: User? = null
)
