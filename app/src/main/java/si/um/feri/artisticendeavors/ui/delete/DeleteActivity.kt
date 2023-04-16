package si.um.feri.artisticendeavors.ui.delete

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.apache.commons.io.FileUtils
import si.um.feri.artisticendeavors.MainActivity
import si.um.feri.artisticendeavors.R
import si.um.feri.artisticendeavors.us
import si.um.feri.artisticendeavors.user.User
import timber.log.Timber
import java.io.File
import java.io.IOException

class DeleteActivity : AppCompatActivity() {

    private lateinit var buttonYes: Button
    private lateinit var buttonNo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete)

        buttonYes = findViewById(R.id.button_affirmative)
        buttonNo = findViewById(R.id.button_negative)

        buttonYes.setOnClickListener {
            val userList: MutableList<User>
            val gson: Gson = GsonBuilder().setPrettyPrinting().create()
            val file = File("/data/data/si.um.feri.artisticendeavors/files", "user.json")

            userList = try {
                Timber.d("My file data:${FileUtils.readFileToString(file)}")
                gson.fromJson(
                    FileUtils.readFileToString(file),
                    object : TypeToken<List<User>>() {}.type
                )
            } catch (e: IOException) {
                Timber.d("No file init data.")
                mutableListOf()
            }

            var count = 0
            var counted = 0
            var exists = false


            for (usr in userList) {
                if (usr.email == us) {
                    exists = true
                    count = counted
                }
                counted += 1
            }
            if (exists) {
                userList.removeAt(count)
                try {
                    FileUtils.writeStringToFile(file, gson.toJson(userList))
                    Timber.d("Save to file.")
                } catch (e: IOException) {
                    Timber.d("Can't save " + file.path)
                }
            }
            val intent = Intent(this, MainActivity::class.java)
            this.startActivity(intent)
            finishAffinity()
        }

        buttonNo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}