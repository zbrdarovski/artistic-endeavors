package artisticendeavors.tools

import android.app.Activity
import android.content.Context
import android.content.Intent

class ActivitySwitcher {
    fun startNewActivity(context: Context, targetActivity: Class<*>) {
        val intent = Intent(context, targetActivity)
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
    }
}