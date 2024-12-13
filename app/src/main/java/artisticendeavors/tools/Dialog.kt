package artisticendeavors.tools

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import artisticendeavors.R

class Dialog(
    private val context: Context,
    private val visibilitySwitcher: VisibilitySwitcher,
    private val color: Color
) {
    fun createPasswordPrompt(
        title: String,
        positiveButtonLabel: String,
        onPositiveButtonClicked: (password: String) -> Unit
    ) {
        val passwordEditText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = context.getString(R.string.enter_current_password)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val fieldMargin = convertDpToPx(8) // Set the desired margin for the input field
                setMargins(fieldMargin, 0, fieldMargin, 0)
            }
        }

        val showHideButton = Button(context).apply {
            text = context.getString(R.string.show)
            setTextColor(ContextCompat.getColor(context, R.color.teal_700))
            setBackgroundResource(R.drawable.custom_button)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.teal_200)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val buttonMargin = convertDpToPx(4) // Set the desired margin for the button
                setMargins(buttonMargin, buttonMargin, buttonMargin, buttonMargin)
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
        }

        visibilitySwitcher.showPasswordWithButton(showHideButton, passwordEditText)

        val passwordPromptLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val contentMargin = convertDpToPx(16) // Set the desired margin for content
                setMargins(contentMargin, contentMargin, contentMargin, contentMargin)
            }
        }

        val inputButtonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val inputButtonMargin =
                    convertDpToPx(4) // Set the desired margin between input and button
                setMargins(0, inputButtonMargin, 0, 0)
            }
        }

        inputButtonLayout.addView(
            passwordEditText, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            )
        )

        inputButtonLayout.addView(showHideButton)

        passwordPromptLayout.addView(inputButtonLayout)

        val passwordPrompt = AlertDialog.Builder(context).setTitle(color.colorize(title, "#E36363"))
            .setView(passwordPromptLayout)
            .setPositiveButton(color.colorize(positiveButtonLabel, "#E36363")) { _, _ ->
                val password = passwordEditText.text.toString()
                onPositiveButtonClicked(password)
            }.setNegativeButton(color.colorize(context.getString(R.string.cancel), "#84B589"), null)
            .create()

        passwordPrompt.show()
    }

    // Function to convert density-independent pixels (dp) to pixels (px)
    private fun convertDpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}