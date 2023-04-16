package si.um.feri.artisticendeavors.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.um.feri.artisticendeavors.data.LoginDataSource
import si.um.feri.artisticendeavors.data.LoginRepository

class LoginViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginRepository = LoginRepository(
                    dataSource = LoginDataSource()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}