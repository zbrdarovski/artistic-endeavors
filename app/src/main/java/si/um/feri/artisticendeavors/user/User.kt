package si.um.feri.artisticendeavors.user

class User(val id: String, val email: String, private var password: String) {
    fun setPassword (pw: String){
        password = pw
    }

    fun getPassword(): String {
        return password
    }
}

