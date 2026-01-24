package com.example.forgeint.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

object RichTextFormatter {

    // Compile patterns once
    private val BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*")
    private val ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)") // Adjusted to avoid grabbing **
    private val CODE_PATTERN = Pattern.compile("`(.*?)`")
    private val SUB_PATTERN = Pattern.compile("(?<=.|^)_\\{?([^}]*)\\}?")
    private val SUP_PATTERN = Pattern.compile("(?<=.|^)\\^\\{?([^}]*)\\}?")
    private val FRACTION_PATTERN = Pattern.compile("\\[(.*?)/(.*?)\\]")

    // Pattern to catch all symbol keys starting with \
    private val SYMBOL_TOKEN_PATTERN = Pattern.compile("\\\\[a-zA-Z]+")

    // Reusable Styles to reduce allocation on Watch
    private val TRANSPARENT_STYLE = SpanStyle(color = Color.Transparent, fontSize = 0.sp)
    private val CODE_STYLE = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color.DarkGray.copy(alpha = 0.4f),
        color = Color(0xFF00E5FF)
    )
    private val NUMERATOR_STYLE = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 9.sp)
    private val DENOMINATOR_STYLE = SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 9.sp)

    private val SYMBOL_MAP = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\theta" to "θ", "\\pi" to "π", "\\sigma" to "σ", "\\omega" to "ω",
        "\\Delta" to "Δ", "\\Omega" to "Ω", "\\lambda" to "λ", "\\mu" to "μ",
        "\\phi" to "φ", "\\infinity" to "∞", "\\sqrt" to "√", "\\pm" to "±",
        "\\times" to "×", "\\div" to "÷", "\\degree" to "°", "\\rightarrow" to "→",
        "\\therefore" to "∴", "\\because" to "∵"
    )

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun format(text: String, primaryColor: Color): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")


        val processedText = if (text.contains("\\")) {
            SYMBOL_TOKEN_PATTERN.matcher(text).replaceAll { matchResult ->
                SYMBOL_MAP[matchResult.group()] ?: matchResult.group()
            }
        } else {
            text
        }

        return buildAnnotatedString {
            append(processedText)

            // Base Text Style
            addStyle(SpanStyle(color = primaryColor), 0, processedText.length)


            if (processedText.contains("`")) applyStyle(CODE_PATTERN, processedText, CODE_STYLE)
            if (processedText.contains("**")) applyStyle(BOLD_PATTERN, processedText, SpanStyle(fontWeight = FontWeight.Bold))
            if (processedText.contains("*")) applyStyle(ITALIC_PATTERN, processedText, SpanStyle(fontStyle = FontStyle.Italic))
            if (processedText.contains("^")) applyMathShift(SUP_PATTERN, processedText, BaselineShift.Superscript)
            if (processedText.contains("_")) applyMathShift(SUB_PATTERN, processedText, BaselineShift.Subscript)

            // Fraction styling
            if (processedText.contains("[")) applyFractions(processedText)
        }
    }

    private fun AnnotatedString.Builder.applyStyle(pattern: Pattern, text: String, style: SpanStyle) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start(1)
            val end = matcher.end(1)
            addStyle(style, start, end)
            // Hide the markers (e.g. **)
            addStyle(TRANSPARENT_STYLE, matcher.start(), start)
            addStyle(TRANSPARENT_STYLE, end, matcher.end())
        }
    }

    private fun AnnotatedString.Builder.applyMathShift(pattern: Pattern, text: String, shift: BaselineShift) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start(1)
            val end = matcher.end(1)
            addStyle(SpanStyle(baselineShift = shift, fontSize = 10.sp), start, end)
            addStyle(TRANSPARENT_STYLE, matcher.start(), start)
            addStyle(TRANSPARENT_STYLE, end, matcher.end())
        }
    }

    private fun AnnotatedString.Builder.applyFractions(text: String) {
        val matcher = FRACTION_PATTERN.matcher(text)
        while (matcher.find()) {
            val numStart = matcher.start(1)
            val numEnd = matcher.end(1)
            val denStart = matcher.start(2)
            val denEnd = matcher.end(2)

            addStyle(NUMERATOR_STYLE, numStart, numEnd)
            addStyle(DENOMINATOR_STYLE, denStart, denEnd)

            // Hide brackets [ and / and ]
            addStyle(TRANSPARENT_STYLE, matcher.start(), numStart)
            addStyle(TRANSPARENT_STYLE, numEnd, denStart) // Hides the /
            addStyle(TRANSPARENT_STYLE, denEnd, matcher.end())
        }
    }
}