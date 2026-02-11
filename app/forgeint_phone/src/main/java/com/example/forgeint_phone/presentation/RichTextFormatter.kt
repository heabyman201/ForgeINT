package com.example.forgeint.presentation

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

    private val BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*")

    private val CODE_PATTERN = Pattern.compile("`(.*?)`")
    private val SUB_PATTERN = Pattern.compile("(?<=.|^)_\\{?([^}]*)\\}?")

    private val SYMBOL_TOKEN_PATTERN = Pattern.compile("\\\\[a-zA-Z]+")
    private val BULLET_PATTERN = Pattern.compile("(?m)^\\s*[-*]\\s+(.*)")

    private val TRANSPARENT_STYLE = SpanStyle(color = Color.Transparent, fontSize = 0.sp)
    private val CODE_STYLE = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color.DarkGray.copy(alpha = 0.4f),
        color = Color(0xFF00E5FF)
    )
    private val NUMERATOR_STYLE = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp)
    private val DENOMINATOR_STYLE = SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 10.sp)

    private val SYMBOL_MAP = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\theta" to "θ", "\\pi" to "π", "\\sigma" to "σ", "\\omega" to "ω",
        "\\Delta" to "Δ", "\\Omega" to "Ω", "\\lambda" to "λ", "\\mu" to "μ",
        "\\phi" to "φ", "\\infinity" to "∞", "\\sqrt" to "√", "\\pm" to "±",
        "\\times" to "×", "\\div" to "÷", "\\degree" to "°", "\\rightarrow" to "→",
        "\\therefore" to "∴", "\\because" to "∵", "\\neq" to "≠", "\\le" to "≤",
        "\\ge" to "≥", "\\in" to "∈", "\\notin" to "∉", "\\subset" to "⊂",
        "\\supset" to "⊃", "\\cap" to "∩", "\\cup" to "∪", "\\forall" to "∀",
        "\\exists" to "∃", "\\nabla" to "∇", "\\partial" to "∂"
    )

    fun format(text: String, primaryColor: Color): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        var processedText = text
        // Handle LaTeX blocks \[ ... ] and ( ... )
        // Use literal strings or properly escaped templates

        processedText = processedText.replace("\\(", "").replace("\\)", "")

        // Replace symbols
        if (processedText.contains("\\")) {
            val matcher = SYMBOL_TOKEN_PATTERN.matcher(processedText)
            val sb = StringBuilder()
            var lastEnd = 0
            while (matcher.find()) {
                sb.append(processedText.substring(lastEnd, matcher.start()))
                val token = matcher.group()
                sb.append(SYMBOL_MAP[token] ?: token)
                lastEnd = matcher.end()
            }
            sb.append(processedText.substring(lastEnd))
            processedText = sb.toString()
        }

        // Replace bullets
        val bulletMatcher = BULLET_PATTERN.matcher(processedText)
        processedText = bulletMatcher.replaceAll("• $1")

        return buildAnnotatedString {
            append(processedText)
            addStyle(SpanStyle(color = primaryColor), 0, processedText.length)

            if (processedText.contains("`")) applyStyle(CODE_PATTERN, processedText, CODE_STYLE)
            if (processedText.contains("**")) applyStyle(BOLD_PATTERN, processedText, SpanStyle(fontWeight = FontWeight.Bold))

            if (processedText.contains("_")) applyMathShift(SUB_PATTERN, processedText, BaselineShift.Subscript)

        }
    }

    private fun AnnotatedString.Builder.applyStyle(pattern: Pattern, text: String, style: SpanStyle) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start(1)
            val end = matcher.end(1)
            addStyle(style, start, end)
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


}
