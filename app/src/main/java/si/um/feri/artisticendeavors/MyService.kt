package si.um.feri.artisticendeavors

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

class MyService : Service() {

    private val tag = "MyService"

    private var isOnline = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d(tag, "Service started")

        // Start a thread to check the online connection status periodically
        Thread {
            while (true) {
                // Code to check the online connection status goes here

                isOnline = true // Or false, depending on the connection status

                // Broadcast the updated connection status to the app
                val inte = Intent("com.example.myapp.ONLINE_STATUS")
                inte.putExtra("isOnline", isOnline)
                sendBroadcast(inte)

                // Wait for some time before checking again
                Thread.sleep(5000) // Check every 5 seconds
            }
        }.start()

        // Return START_STICKY to indicate that the service should be restarted if it is killed by the system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service does not provide binding, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d(tag, "Service destroyed")
    }
}