package com.example.forgeint.presentation

import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RichTextFormatterBenchmark {

    @Test
    fun format_richText_benchmark() {
        val text =
            "This is **bold**, *italic*, `code`, ^{sup}, _{sub}, [1/2], and \\alpha symbols."
        val primary = Color.White

        // Warmup to reduce JIT/cold-start noise.
        repeat(50) { RichTextFormatter.format(text, primary, fastMode = false) }

        val iterations = 500
        val start = SystemClock.elapsedRealtimeNanos()
        repeat(iterations) { RichTextFormatter.format(text, primary, fastMode = false) }
        val elapsed = SystemClock.elapsedRealtimeNanos() - start

        val avgNs = elapsed / iterations
        Log.i("RTFBench", "format_richText avg ns = $avgNs")
    }

    @Test
    fun format_plainText_benchmark() {
        val text = "Plain text without any markers."
        val primary = Color.White

        repeat(50) { RichTextFormatter.format(text, primary, fastMode = false) }

        val iterations = 500
        val start = SystemClock.elapsedRealtimeNanos()
        repeat(iterations) { RichTextFormatter.format(text, primary, fastMode = false) }
        val elapsed = SystemClock.elapsedRealtimeNanos() - start

        val avgNs = elapsed / iterations
        Log.i("RTFBench", "format_plainText avg ns = $avgNs")
    }
}
