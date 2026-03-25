package com.example.forgeint.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

object RichTextFormatter {

    private val boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*")
    private val codePattern = Pattern.compile("`(.*?)`")
    private val bulletPattern = Pattern.compile("(?m)^\\s*[-*]\\s+(.*)")

    private val transparentStyle = SpanStyle(color = Color.Transparent, fontSize = 0.sp)
    private val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color.DarkGray.copy(alpha = 0.4f),
        color = Color(0xFF00E5FF)
    )

    private val symbolMap = linkedMapOf(
        "\\alpha" to "\u03B1",
        "\\beta" to "\u03B2",
        "\\gamma" to "\u03B3",
        "\\delta" to "\u03B4",
        "\\epsilon" to "\u03B5",
        "\\varepsilon" to "\u03F5",
        "\\zeta" to "\u03B6",
        "\\eta" to "\u03B7",
        "\\theta" to "\u03B8",
        "\\vartheta" to "\u03D1",
        "\\iota" to "\u03B9",
        "\\kappa" to "\u03BA",
        "\\lambda" to "\u03BB",
        "\\mu" to "\u03BC",
        "\\nu" to "\u03BD",
        "\\xi" to "\u03BE",
        "\\pi" to "\u03C0",
        "\\varpi" to "\u03D6",
        "\\rho" to "\u03C1",
        "\\varrho" to "\u03F1",
        "\\sigma" to "\u03C3",
        "\\varsigma" to "\u03C2",
        "\\tau" to "\u03C4",
        "\\upsilon" to "\u03C5",
        "\\phi" to "\u03C6",
        "\\varphi" to "\u03D5",
        "\\chi" to "\u03C7",
        "\\psi" to "\u03C8",
        "\\omega" to "\u03C9",
        "\\Gamma" to "\u0393",
        "\\Delta" to "\u0394",
        "\\Theta" to "\u0398",
        "\\Lambda" to "\u039B",
        "\\Xi" to "\u039E",
        "\\Pi" to "\u03A0",
        "\\Sigma" to "\u03A3",
        "\\Upsilon" to "\u03A5",
        "\\Phi" to "\u03A6",
        "\\Psi" to "\u03A8",
        "\\Omega" to "\u03A9",
        "\\pm" to "\u00B1",
        "\\mp" to "\u2213",
        "\\times" to "\u00D7",
        "\\div" to "\u00F7",
        "\\cdot" to "\u22C5",
        "\\ast" to "\u2217",
        "\\star" to "\u22C6",
        "\\circ" to "\u2218",
        "\\bullet" to "\u2022",
        "\\oplus" to "\u2295",
        "\\otimes" to "\u2297",
        "\\sum" to "\u2211",
        "\\prod" to "\u220F",
        "\\coprod" to "\u2210",
        "\\int" to "\u222B",
        "\\iint" to "\u222C",
        "\\iiint" to "\u222D",
        "\\oint" to "\u222E",
        "\\infty" to "\u221E",
        "\\infinity" to "\u221E",
        "\\partial" to "\u2202",
        "\\nabla" to "\u2207",
        "\\approx" to "\u2248",
        "\\sim" to "\u223C",
        "\\simeq" to "\u2243",
        "\\cong" to "\u2245",
        "\\equiv" to "\u2261",
        "\\neq" to "\u2260",
        "\\ne" to "\u2260",
        "\\leq" to "\u2264",
        "\\le" to "\u2264",
        "\\geq" to "\u2265",
        "\\ge" to "\u2265",
        "\\ll" to "\u226A",
        "\\gg" to "\u226B",
        "\\propto" to "\u221D",
        "\\parallel" to "\u2225",
        "\\perp" to "\u27C2",
        "\\angle" to "\u2220",
        "\\triangle" to "\u25B3",
        "\\forall" to "\u2200",
        "\\exists" to "\u2203",
        "\\nexists" to "\u2204",
        "\\neg" to "\u00AC",
        "\\land" to "\u2227",
        "\\lor" to "\u2228",
        "\\Rightarrow" to "\u21D2",
        "\\Leftarrow" to "\u21D0",
        "\\Leftrightarrow" to "\u21D4",
        "\\implies" to "\u21D2",
        "\\iff" to "\u21D4",
        "\\rightarrow" to "\u2192",
        "\\leftarrow" to "\u2190",
        "\\leftrightarrow" to "\u2194",
        "\\to" to "\u2192",
        "\\mapsto" to "\u21A6",
        "\\uparrow" to "\u2191",
        "\\downarrow" to "\u2193",
        "\\cup" to "\u222A",
        "\\cap" to "\u2229",
        "\\subset" to "\u2282",
        "\\subseteq" to "\u2286",
        "\\supset" to "\u2283",
        "\\supseteq" to "\u2287",
        "\\in" to "\u2208",
        "\\notin" to "\u2209",
        "\\ni" to "\u220B",
        "\\emptyset" to "\u2205",
        "\\varnothing" to "\u2205",
        "\\mathbb{R}" to "\u211D",
        "\\mathbb{N}" to "\u2115",
        "\\mathbb{Z}" to "\u2124",
        "\\mathbb{Q}" to "\u211A",
        "\\mathbb{C}" to "\u2102",
        "\\therefore" to "\u2234",
        "\\because" to "\u2235",
        "\\degree" to "\u00B0",
        "\\prime" to "\u2032",
        "\\ldots" to "\u2026",
        "\\cdots" to "\u22EF",
        "\\vdots" to "\u22EE",
        "\\ddots" to "\u22F1"
    )

    private val superscriptMap = mapOf(
        '0' to '\u2070', '1' to '\u00B9', '2' to '\u00B2', '3' to '\u00B3', '4' to '\u2074',
        '5' to '\u2075', '6' to '\u2076', '7' to '\u2077', '8' to '\u2078', '9' to '\u2079',
        '+' to '\u207A', '-' to '\u207B', '=' to '\u207C', '(' to '\u207D', ')' to '\u207E',
        'n' to '\u207F', 'i' to '\u2071'
    )

    private val subscriptMap = mapOf(
        '0' to '\u2080', '1' to '\u2081', '2' to '\u2082', '3' to '\u2083', '4' to '\u2084',
        '5' to '\u2085', '6' to '\u2086', '7' to '\u2087', '8' to '\u2088', '9' to '\u2089',
        '+' to '\u208A', '-' to '\u208B', '=' to '\u208C', '(' to '\u208D', ')' to '\u208E',
        'a' to '\u2090', 'e' to '\u2091', 'h' to '\u2095', 'i' to '\u1D62', 'j' to '\u2C7C',
        'k' to '\u2096', 'l' to '\u2097', 'm' to '\u2098', 'n' to '\u2099', 'o' to '\u2092',
        'p' to '\u209A', 'r' to '\u1D63', 's' to '\u209B', 't' to '\u209C', 'u' to '\u1D64',
        'v' to '\u1D65', 'x' to '\u2093'
    )

    fun format(text: String, primaryColor: Color): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        var processedText = preprocessLatex(text)
        processedText = bulletPattern.matcher(processedText).replaceAll("\u2022 $1")

        return buildAnnotatedString {
            append(processedText)
            addStyle(SpanStyle(color = primaryColor), 0, processedText.length)

            if (processedText.contains("`")) {
                applyStyle(codePattern, processedText, codeStyle)
            }
            if (processedText.contains("**")) {
                applyStyle(boldPattern, processedText, SpanStyle(fontWeight = FontWeight.Bold))
            }
        }
    }

    private fun preprocessLatex(text: String): String {
        var out = text
        out = out.replace(Regex("""\\\[(.+?)\\]""", setOf(RegexOption.DOT_MATCHES_ALL))) { it.groupValues[1] }
        out = out.replace(Regex("""\\\((.+?)\)""", setOf(RegexOption.DOT_MATCHES_ALL))) { it.groupValues[1] }
        out = out.replace(Regex("""\$\$(.+?)\$\$""", RegexOption.DOT_MATCHES_ALL)) { it.groupValues[1] }
        out = out.replace(Regex("""\$([^$\n]+)\$""")) { it.groupValues[1] }
        out = out.replace(Regex("""\\text\{([^{}]*)\}""")) { it.groupValues[1] }
        out = out.replace(Regex("""\\mathrm\{([^{}]*)\}""")) { it.groupValues[1] }
        out = out.replace(Regex("""\\mathbf\{([^{}]*)\}""")) { it.groupValues[1] }
        out = replaceFractions(out)
        out = replaceRoots(out)
        out = replaceScripts(out, true)
        out = replaceScripts(out, false)
        for ((latex, symbol) in symbolMap) {
            out = out.replace(latex, symbol)
        }
        out = out.replace("\\,", " ")
        out = out.replace("\\;", " ")
        out = out.replace("\\:", " ")
        out = out.replace("\\!", "")
        out = out.replace("\\left", "")
        out = out.replace("\\right", "")
        out = out.replace("\\%", "%")
        out = out.replace("\\_", "_")
        out = out.replace("\\^", "^")
        out = out.replace("\\{", "{")
        out = out.replace("\\}", "}")
        return out
    }

    private fun replaceFractions(text: String): String {
        var out = text
        val fractionRegex = Regex("""\\frac\s*\{([^{}]+)\}\s*\{([^{}]+)\}""")
        repeat(8) {
            if (!fractionRegex.containsMatchIn(out)) return@repeat
            out = out.replace(fractionRegex) { match ->
                val numerator = match.groupValues[1].trim()
                val denominator = match.groupValues[2].trim()
                val compactNumerator = toSuperScript(numerator)
                val compactDenominator = toSubScript(denominator)
                if (compactNumerator != null && compactDenominator != null) {
                    compactNumerator + "\u2044" + compactDenominator
                } else {
                    "($numerator)\u2044($denominator)"
                }
            }
        }
        return out
    }

    private fun replaceRoots(text: String): String {
        var out = text
        out = out.replace(Regex("""\\sqrt\{([^{}]+)\}""")) { match ->
            "\u221A(${match.groupValues[1]})"
        }
        out = out.replace(Regex("""\\sqrt\[([^]]+)]\{([^{}]+)\}""")) { match ->
            "${match.groupValues[1]}\u221A(${match.groupValues[2]})"
        }
        return out
    }

    private fun replaceScripts(text: String, superscript: Boolean): String {
        var out = text
        val bracedPattern = if (superscript) {
            Regex("""\^\{([^{}]+)\}""")
        } else {
            Regex("""_\{([^{}]+)\}""")
        }
        val singlePattern = if (superscript) {
            Regex("""\^([A-Za-z0-9+\-=()])""")
        } else {
            Regex("""_([A-Za-z0-9+\-=()])""")
        }

        repeat(6) {
            out = out.replace(bracedPattern) { match ->
                val token = match.groupValues[1]
                val converted = if (superscript) toSuperScript(token) else toSubScript(token)
                converted ?: if (superscript) "^($token)" else "_($token)"
            }
            out = out.replace(singlePattern) { match ->
                val token = match.groupValues[1]
                val converted = if (superscript) toSuperScript(token) else toSubScript(token)
                converted ?: if (superscript) "^$token" else "_$token"
            }
        }
        return out
    }

    private fun toSuperScript(text: String): String? {
        val builder = StringBuilder()
        for (char in text) {
            val converted = superscriptMap[char.lowercaseChar()] ?: superscriptMap[char]
            if (converted == null) return null
            builder.append(converted)
        }
        return builder.toString()
    }

    private fun toSubScript(text: String): String? {
        val builder = StringBuilder()
        for (char in text) {
            val converted = subscriptMap[char.lowercaseChar()] ?: subscriptMap[char]
            if (converted == null) return null
            builder.append(converted)
        }
        return builder.toString()
    }

    private fun AnnotatedString.Builder.applyStyle(pattern: Pattern, text: String, style: SpanStyle) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start(1)
            val end = matcher.end(1)
            addStyle(style, start, end)
            addStyle(transparentStyle, matcher.start(), start)
            addStyle(transparentStyle, end, matcher.end())
        }
    }
}
