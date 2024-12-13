package artisticendeavors.tools

import android.content.Context
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import artisticendeavors.R

class VisibilitySwitcher(private val context: Context) {
    fun showPasswordWithImage(showPassword: ImageView, password: EditText){
        // Declare variables to keep track of password visibility state and text selection
        var isPasswordVisible = false
        var startOfPassword: Int
        var endOfPassword: Int

        showPassword.setOnClickListener {
            // Update the password visibility and image resource based on the current state
            if (isPasswordVisible) {
                // Show password
                startOfPassword = password.selectionStart
                endOfPassword = password.selectionEnd
                password.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                password.setSelection(startOfPassword, endOfPassword)
                showPassword.setImageResource(R.mipmap.ic_open)

                // Toggle the password visibility state
                isPasswordVisible = !isPasswordVisible
            } else {
                // Hide password
                startOfPassword = password.selectionStart
                endOfPassword = password.selectionEnd
                password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                password.setSelection(startOfPassword, endOfPassword)
                showPassword.setImageResource(R.mipmap.ic_closed)

                // Toggle the password visibility state
                isPasswordVisible = !isPasswordVisible
            }
        }
    }

    fun showPasswordWithButton(showHideButton: Button, input: EditText) {
        var isCurrentPasswordVisible = false
        var startCursor: Int
        var endCursor: Int

        showHideButton.setOnClickListener {
            if (isCurrentPasswordVisible) {
                // Hide password
                // Preserve the cursor position after showing/hiding password
                startCursor = input.selectionStart
                endCursor = input.selectionEnd
                input.transformationMethod = PasswordTransformationMethod.getInstance()
                input.setSelection(startCursor, endCursor)
                showHideButton.text = context.getString(R.string.show)
                isCurrentPasswordVisible = !isCurrentPasswordVisible
            } else {
                // Show password
                // Preserve the cursor position after showing/hiding password
                startCursor = input.selectionStart
                endCursor = input.selectionEnd
                input.transformationMethod = HideReturnsTransformationMethod.getInstance()
                input.setSelection(startCursor, endCursor)
                showHideButton.text = context.getString(R.string.hide)
                isCurrentPasswordVisible = !isCurrentPasswordVisible
            }
        }
    }
}