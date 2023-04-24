package si.um.feri.artisticendeavors

import java.util.concurrent.ThreadLocalRandom

data class Post(val title: String, val description: String) {
    private val rand = ThreadLocalRandom.current()
    private val randomNumber = rand.nextLong()
    val imageUrl = "https://picsum.photos/150?random=$randomNumber"
}