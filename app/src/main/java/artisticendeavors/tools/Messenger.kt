package artisticendeavors.tools

import android.content.Context
import android.widget.Toast

class Messenger(private val context: Context) {
    fun message(text: String) {
        Toast.makeText(
            context,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }
}