package artisticendeavors.tools

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import artisticendeavors.R

class Validator(private val context: Context) {
    // Define the username requirements
    private val usernameRequirements = listOf(
        context.getString(R.string.username_must_be_eight_to_twenty_characters_long),
        context.getString(R.string.username_can_only_contain_lowercase_letters_digits_underscores_and_dots),
        context.getString(R.string.username_cannot_start_with_underscore_or_dot),
        context.getString(R.string.username_cannot_end_with_underscore_or_dot),
        context.getString(R.string.username_cannot_have_multiple_underscores_or_dots_in_a_row),
        context.getString(R.string.username_cannot_have_underscore_next_to_dot)
    )

    fun usernameErrorMessage(username: String): String? {
        // Filter the username requirements based on the new username
        val unmetUsernameRequirements = usernameRequirements.filter { requirement ->
            when (requirement) {
                context.getString(R.string.username_must_be_eight_to_twenty_characters_long) -> username.length !in 8..20

                context.getString(R.string.username_can_only_contain_lowercase_letters_digits_underscores_and_dots) -> !username.matches(
                    "^[a-z0-9._]+\$".toRegex()
                )

                context.getString(R.string.username_cannot_start_with_underscore_or_dot) -> username.startsWith(
                    "_"
                ) || username.startsWith(".")

                context.getString(R.string.username_cannot_end_with_underscore_or_dot) -> username.endsWith(
                    "_"
                ) || username.endsWith(".")

                context.getString(R.string.username_cannot_have_multiple_underscores_or_dots_in_a_row) -> username.contains(
                    ".."
                ) || username.contains("__") || username.contains("._") || username.contains(
                    "_."
                ) || username.contains("_.") || username.contains("._")

                context.getString(R.string.username_cannot_have_underscore_next_to_dot) -> username.contains(
                    "_."
                ) || username.contains("._")

                else -> false
            }
        }

        return if (unmetUsernameRequirements.isNotEmpty()) {
            unmetUsernameRequirements.joinToString("\n")
        } else {
            null
        }
    }

    // Define the password requirements
    private val passwordRequirements = listOf(
        context.getString(R.string.password_must_be_at_least_eight_characters_long),
        context.getString(R.string.password_must_contain_at_least_one_lowercase_letter),
        context.getString(R.string.password_must_contain_at_least_one_uppercase_letter),
        context.getString(R.string.password_must_contain_at_least_one_digit),
        context.getString(R.string.password_must_contain_at_least_one_special_character)
    )

    fun passwordErrorMessage(newPassword: String): String? {
        // Filter the password requirements based on the new password
        val unmetRequirements = passwordRequirements.filter { requirement ->
            when (requirement) {
                context.getString(R.string.password_must_be_at_least_eight_characters_long) -> newPassword.length < 8

                context.getString(R.string.password_must_contain_at_least_one_lowercase_letter) -> !newPassword.any { it.isLowerCase() }

                context.getString(R.string.password_must_contain_at_least_one_uppercase_letter) -> !newPassword.any { it.isUpperCase() }

                context.getString(R.string.password_must_contain_at_least_one_digit) -> !newPassword.any { it.isDigit() }

                context.getString(R.string.password_must_contain_at_least_one_special_character) -> !newPassword.any {
                    it.isLetterOrDigit().not()
                }

                else -> false
            }
        }

        // Set the error message of binding.password based on the unmet requirements
        return if (unmetRequirements.isNotEmpty()) {
            unmetRequirements.joinToString("\n")
        } else {
            null
        }
    }

    // Validate the username
    fun isUsernameValid(username: String): Boolean {
        val usernamePattern = "^[a-z0-9._]+$"
        return username.length in 8..20 && username.matches(usernamePattern.toRegex()) && !username.startsWith(
            "_"
        ) && !username.startsWith(".") && !username.endsWith("_") && !username.endsWith(".") && !username.contains(
            ".."
        ) && !username.contains("__") && !username.contains("._") && !username.contains("_.") && !username.contains(
            "_."
        ) && !username.contains("._")
    }

    // Validate the password
    fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    fun arePasswordsMatching(password: String, repeat: String): Boolean {
        return password == repeat
    }

    fun authenticate(email: String, password: String, currentUser: FirebaseUser): Task<Void> {
        val authCredential = EmailAuthProvider.getCredential(email, password)
        return currentUser.reauthenticate(authCredential)
    }
}