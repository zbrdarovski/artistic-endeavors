package artisticendeavors.tools

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

class Color {
    fun colorize(text: String, colorHex: String): Spannable {
        val spannableString = SpannableString(text)
        val colorSpan = ForegroundColorSpan(Color.parseColor(colorHex))
        spannableString.setSpan(colorSpan, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }
}