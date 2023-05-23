package si.um.feri.artisticendeavors.tools

import android.app.Activity
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.activities.AccountActivity
import si.um.feri.artisticendeavors.activities.LoginActivity
import si.um.feri.artisticendeavors.activities.MainActivity
import si.um.feri.artisticendeavors.activities.ProfileActivity


class Toolbar(private val activity: Activity, private val auth: FirebaseAuth,
              private val homeOption: ImageView, private val profileOption: TextView,
              private val accountOption: TextView, private val actionSignOut: ImageView) {

    private val activitySwitcher = ActivitySwitcher()

    fun bindToolbar() {
        // ActivitySwitcher to MainActivity
        homeOption.setOnClickListener {
            activitySwitcher.startNewActivity(activity, MainActivity::class.java)
        }

        // ActivitySwitcher to ProfileActivity
        profileOption.setOnClickListener {
            activitySwitcher.startNewActivity(activity, ProfileActivity::class.java)
        }

        // ActivitySwitcher to AccountActivity
        accountOption.setOnClickListener {
            activitySwitcher.startNewActivity(activity, AccountActivity::class.java)
        }

        // Sign out from the app
        actionSignOut.setOnClickListener {
            auth.signOut()
            activitySwitcher.startNewActivity(activity, LoginActivity::class.java)

            Toast.makeText(
                activity, activity.getString(R.string.you_logged_out_successfully), Toast.LENGTH_SHORT
            ).show()
        }
    }
}