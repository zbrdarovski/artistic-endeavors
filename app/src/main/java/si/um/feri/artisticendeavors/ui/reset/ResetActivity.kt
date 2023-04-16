package si.um.feri.artisticendeavors.ui.reset

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
import java.util.regex.Pattern

class ResetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset)

        val passwordReset: EditText = findViewById(R.id.password_reset)
        val passwordResetRepeat: EditText = findViewById(R.id.password_repeat_reset)

        val reset: Button = findViewById(R.id.reset)

        reset.setOnClickListener {
            when {
                !isPasswordValid(passwordReset.text.toString()) -> {
                    showPasswordDialog()
                }
                passwordReset.text.toString() != passwordResetRepeat.text.toString() -> {
                    showPasswordRepeatDialog()
                }
                else -> {
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
                        val u = userList[count]
                        u.setPassword(passwordReset.text.toString())
                        userList[count] = u
                        try {
                            FileUtils.writeStringToFile(file, gson.toJson(userList))
                            Timber.d("Save to file.")
                        } catch (e: IOException) {
                            Timber.d("Can't save " + file.path)
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showPasswordDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(R.string.invalid_password)
        val b = dialogBuilder.create()
        b.show()
    }

    private fun showPasswordRepeatDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(R.string.invalid_password_repeat)
        val b = dialogBuilder.create()
        b.show()
    }

    private fun isPasswordValid(password: String): Boolean {
        var valid = true

        if (password.length < 8) {
            valid = false
        }

        var exp = ".*[0-9].*"
        var pattern = Pattern.compile(exp, Pattern.CASE_INSENSITIVE)
        var matcher = pattern.matcher(password)
        if (!matcher.matches()) {
            valid = false
        }

        exp = ".*[A-Z].*"
        pattern = Pattern.compile(exp)
        matcher = pattern.matcher(password)
        if (!matcher.matches()) {
            valid = false
        }

        exp = ".*[a-z].*"
        pattern = Pattern.compile(exp)
        matcher = pattern.matcher(password)
        if (!matcher.matches()) {
            valid = false
        }

        exp = ".*[~!@#\$%\\^&*()\\-_=+\\|\\[{\\]};:'\",<.>/?].*"
        pattern = Pattern.compile(exp)
        matcher = pattern.matcher(password)
        if (!matcher.matches()) {
            valid = false
        }
        return valid
    }
}