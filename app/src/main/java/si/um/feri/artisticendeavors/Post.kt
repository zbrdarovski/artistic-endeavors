package si.um.feri.artisticendeavors

data class Post(
    var creation_time_milliseconds: Long = 0,
    var description: String = "",
    var image_url: String = "",
    var user: User? = null
)