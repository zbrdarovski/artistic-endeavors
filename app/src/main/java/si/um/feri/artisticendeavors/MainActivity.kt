package si.um.feri.artisticendeavors

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.BuildConfig
import si.um.feri.artisticendeavors.ui.delete.DeleteActivity
import si.um.feri.artisticendeavors.ui.reset.ResetActivity
import timber.log.Timber

lateinit var us: String

class MainActivity : AppCompatActivity() {

    private lateinit var resetPass : Button
    private lateinit var deleteAcc : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resetPass = findViewById(R.id.reset_pass)
        deleteAcc = findViewById(R.id.delete_acc)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        resetPass.setOnClickListener {
            val intent = Intent(this, ResetActivity::class.java)
            startActivity(intent)
        }

        deleteAcc.setOnClickListener {
            val intent = Intent(this, DeleteActivity::class.java)
            startActivity(intent)
        }
    }
}