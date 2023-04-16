package si.um.feri.artisticendeavors.data

import si.um.feri.artisticendeavors.data.model.LoggedInUser

class LoginRepository(val dataSource: LoginDataSource) {

    private var user: LoggedInUser? = null

    init {
        user = null
    }

    fun login(username: String, password: String): Result<LoggedInUser> {
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
    }
}