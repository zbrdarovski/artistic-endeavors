package si.um.feri.artisticendeavors.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.apache.commons.io.FileUtils
import si.um.feri.artisticendeavors.data.model.LoggedInUser
import si.um.feri.artisticendeavors.user.User
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

class LoginDataSource {

    fun login(email: String, password: String): Result<LoggedInUser> {
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

        var exists = false

        var count = 0
        var c = 0

        for (usr in userList) {
            if (usr.email == email) {
                exists = true
                count = c
            }
            c += 1
        }

        if (!exists) {
            val id = UUID.randomUUID().toString().replace("-", "")
            val u = User(id, email, password)
            userList.add(u)
            try {
                FileUtils.writeStringToFile(file, gson.toJson(userList))
                Timber.d("Save to file.")
            } catch (e: IOException) {
                Timber.d("Can't save " + file.path)
            }
        }

        val e = Throwable()
        return if(userList[count].getPassword() == password) {
            try {
                val user =
                    LoggedInUser(UUID.randomUUID().toString(), email)
                Result.Success(user)
            } catch (e: Throwable) {
                Result.Error(IOException("Error logging in", e))
            }
        } else Result.Error(IOException("Error logging in", e))
    }
}