package com.example.forgeint.mlmath

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType

fun f(x: Double, y: Double): Double = (x * x) + (y * y)

fun dfdx(x: Double, y: Double): Double = 2.0 * x

fun dfdy(x: Double, y: Double): Double = 2.0 * y

fun fPrime(x: Double): Double = 2.0 * x

data class Point2D(val x: Double, val y: Double)

fun gradientDescent(
    start: Point2D,
    learningRate: Double,
    numIterations: Int,
): Point2D {
    var x = start.x
    var y = start.y

    repeat(numIterations) {
        val gx = dfdx(x, y)
        val gy = dfdy(x, y)
        x -= learningRate * gx
        y -= learningRate * gy
    }
    return Point2D(x, y)
}

@Composable
fun GradientDescentCanvasDemo(
    modifier: Modifier = Modifier,
    learningRate: Double = 0.01,
    frameDelayMs: Long = 28L
) {
    val worldHalfRange = 6.0

    var startPoint by remember { mutableStateOf(Point2D(4.5, 4.5)) }
    var startInput by remember { mutableStateOf("4.5") }
    var learningRateInput by remember { mutableStateOf(learningRate.toString()) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var currentLearningRate by remember { mutableStateOf(learningRate) }

    var current by remember { mutableStateOf(startPoint) }
    var running by remember { mutableStateOf(true) }
    val path = remember {
        mutableStateListOf<Point2D>().apply { add(startPoint) }
    }
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    val scope = rememberCoroutineScope()

    LaunchedEffect(running, currentLearningRate, frameDelayMs) {
        while (running) {
            val next = gradientDescent(
                start = current,
                learningRate = currentLearningRate,
                numIterations = 1
            )
            current = next
            path.add(next)
            if (path.size > 220) path.removeAt(0)

            if (hypot(next.x, next.y) < 0.01) {
                running = false
            }
            delay(frameDelayMs)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101114))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    EditValuesPage(
                        startInput = startInput,
                        learningRateInput = learningRateInput,
                        inputError = inputError,
                        running = running,
                        onStartInputChange = { value ->
                            if (value.all { it.isDigit() || it == '.' || it == '-' }) {
                                startInput = value
                                inputError = null
                            }
                        },
                        onLearningRateChange = { value ->
                            if (value.all { it.isDigit() || it == '.' || it == '-' }) {
                                learningRateInput = value
                                inputError = null
                            }
                        },
                        onApply = {
                            val parsedStart = startInput.toDoubleOrNull()
                            val parsedLearningRate = learningRateInput.toDoubleOrNull()
                            if (parsedStart == null || parsedLearningRate == null || parsedLearningRate <= 0.0) {
                                inputError = "Enter valid values"
                            } else {
                                val nextStart = Point2D(parsedStart, parsedStart)
                                startPoint = nextStart
                                currentLearningRate = parsedLearningRate
                                current = nextStart
                                path.clear()
                                path.add(nextStart)
                                running = false
                                inputError = null
                                scope.launch { pagerState.animateScrollToPage(1) }
                            }
                        },
                        onToggleRunning = { running = !running },
                        onReset = {
                            current = startPoint
                            path.clear()
                            path.add(startPoint)
                            running = true
                        }
                    )
                }

                1 -> {
                    OverviewPage(
                        current = current,
                        path = path,
                        worldHalfRange = worldHalfRange,
                        learningRate = currentLearningRate,
                        running = running,
                        onToggleRunning = { running = !running },
                        onReset = {
                            current = startPoint
                            path.clear()
                            path.add(startPoint)
                            running = true
                        },
                        onSwipeToEdit = { scope.launch { pagerState.animateScrollToPage(0) } },
                        onSwipeToFullscreen = { scope.launch { pagerState.animateScrollToPage(2) } }
                    )
                }

                else -> {
                    FullScreenCurvePage(
                        current = current,
                        path = path,
                        worldHalfRange = worldHalfRange,
                        onBackToOverview = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewPage(
    current: Point2D,
    path: List<Point2D>,
    worldHalfRange: Double,
    learningRate: Double,
    running: Boolean,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onSwipeToEdit: () -> Unit,
    onSwipeToFullscreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "x=${"%.4f".format(current.x)}  y=${"%.4f".format(current.y)}",
            color = Color.White
        )
        Text(
            text = "f=${"%.6f".format(f(current.x, current.y))}",
            color = Color(0xFFBAC5DB)
        )
        Text(
            text = "rate=${"%.4f".format(learningRate)}",
            color = Color(0xFF8FB4FF)
        )
        SwipeHintRow(
            leftLabel = "Edit values",
            rightLabel = "Full curve",
            onLeftClick = onSwipeToEdit,
            onRightClick = onSwipeToFullscreen
        )
        GradientChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            current = current,
            path = path,
            worldHalfRange = worldHalfRange
        )
        PlaybackControls(
            running = running,
            onToggleRunning = onToggleRunning,
            onReset = onReset
        )
    }
}

@Composable
private fun EditValuesPage(
    startInput: String,
    learningRateInput: String,
    inputError: String?,
    running: Boolean,
    onStartInputChange: (String) -> Unit,
    onLearningRateChange: (String) -> Unit,
    onApply: () -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Swipe right to return", color = Color(0xFFBAC5DB))
        Text("Start X value", color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFF3A4A67), shape = RoundedCornerShape(14.dp))
                .background(Color(0xFF181D29), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = startInput,
                onValueChange = onStartInputChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text("Learning rate", color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFF3A4A67), shape = RoundedCornerShape(14.dp))
                .background(Color(0xFF181D29), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = learningRateInput,
                onValueChange = onLearningRateChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        inputError?.let {
            Text(text = it, color = Color(0xFFFF8A80))
        }
        Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
            Text("Apply")
        }
        PlaybackControls(
            running = running,
            onToggleRunning = onToggleRunning,
            onReset = onReset
        )
    }
}

@Composable
private fun FullScreenCurvePage(
    current: Point2D,
    path: List<Point2D>,
    worldHalfRange: Double,
    onBackToOverview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Swipe left to return", color = Color(0xFFBAC5DB))
        GradientChart(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            current = current,
            path = path,
            worldHalfRange = worldHalfRange
        )
        Text(
            text = "Tap to return",
            color = Color(0xFF7F8DA7),
            modifier = Modifier.clickable(onClick = onBackToOverview)
        )
    }
}

@Composable
private fun PlaybackControls(
    running: Boolean,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onToggleRunning, modifier = Modifier.weight(1f)) {
            Text(if (running) "Pause" else "Start")
        }
        Button(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text("Reset")
        }
    }
}

@Composable
private fun SwipeHintRow(
    leftLabel: String,
    rightLabel: String,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onLeftClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = leftLabel,
                tint = Color(0xFF56CCF2),
                modifier = Modifier.size(18.dp)
            )
            Text(leftLabel, color = Color(0xFFBAC5DB))
        }
        Row(
            modifier = Modifier.clickable(onClick = onRightClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(rightLabel, color = Color(0xFFBAC5DB))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = rightLabel,
                tint = Color(0xFF56CCF2),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GradientChart(
    modifier: Modifier,
    current: Point2D,
    path: List<Point2D>,
    worldHalfRange: Double
) {
    Canvas(
        modifier = modifier.background(Color(0xFF17191F), RoundedCornerShape(18.dp))
    ) {
        fun mapToCanvas(point: Point2D): Offset {
            val px = ((point.x + worldHalfRange) / (2.0 * worldHalfRange) * size.width).toFloat()
            val py = (size.height - ((point.y + worldHalfRange) / (2.0 * worldHalfRange) * size.height)).toFloat()
            return Offset(px, py)
        }

        val origin = mapToCanvas(Point2D(0.0, 0.0))
        val pixelsPerUnit = size.minDimension / (2f * worldHalfRange.toFloat())
        val maxRadius = worldHalfRange.toFloat() * pixelsPerUnit

        val filledContourColors = listOf(
            Color(0xFF172338),
            Color(0xFF132C48),
            Color(0xFF103660),
            Color(0xFF0C4575),
            Color(0xFF0A5687),
            Color(0xFF0A6A96)
        )
        filledContourColors.forEachIndexed { index, color ->
            val t = 1f - (index / filledContourColors.lastIndex.toFloat()) * 0.85f
            drawCircle(
                color = color,
                radius = maxRadius * t,
                center = origin
            )
        }

        drawLine(
            color = Color(0xFF3E4250),
            start = Offset(0f, origin.y),
            end = Offset(size.width, origin.y),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFF3E4250),
            start = Offset(origin.x, 0f),
            end = Offset(origin.x, size.height),
            strokeWidth = 2f
        )

        val axisLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#BAC5DB")
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 10.sp.toPx()
            isAntiAlias = true
        }

        for (mark in 1..5) {
            val radius = maxRadius * mark / 5f
            drawCircle(
                color = Color(0xFF2A2F3A),
                radius = radius,
                center = origin,
                style = Stroke(width = 1f)
            )
        }

        val ticks = listOf(-worldHalfRange, -worldHalfRange / 2.0, 0.0, worldHalfRange / 2.0, worldHalfRange)
        ticks.forEach { tick ->
            val xTickOffset = mapToCanvas(Point2D(tick, 0.0))
            drawLine(
                color = Color(0xFF5A6070),
                start = Offset(xTickOffset.x, origin.y - 6f),
                end = Offset(xTickOffset.x, origin.y + 6f),
                strokeWidth = 1.5f
            )
            drawContext.canvas.nativeCanvas.drawText(
                "%.1f".format(tick),
                xTickOffset.x,
                (origin.y + 18.dp.toPx()).coerceAtMost(size.height - 4.dp.toPx()),
                axisLabelPaint
            )

            val yTickOffset = mapToCanvas(Point2D(0.0, tick))
            drawLine(
                color = Color(0xFF5A6070),
                start = Offset(origin.x - 6f, yTickOffset.y),
                end = Offset(origin.x + 6f, yTickOffset.y),
                strokeWidth = 1.5f
            )
            if (tick != 0.0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(tick),
                    (origin.x + 22.dp.toPx()).coerceAtMost(size.width - 10.dp.toPx()),
                    yTickOffset.y + 4.dp.toPx(),
                    axisLabelPaint
                )
            }
        }

        for (i in 1 until path.size) {
            val a = mapToCanvas(path[i - 1])
            val b = mapToCanvas(path[i])
            drawLine(
                color = Color(0xFF56CCF2),
                start = a,
                end = b,
                strokeWidth = 4f
            )
        }

        val currentOffset = mapToCanvas(current)
        drawCircle(
            color = Color(0xFFFFD166),
            radius = 9f,
            center = currentOffset
        )
        drawCircle(
            color = Color(0xFF4ADE80),
            radius = 6f,
            center = origin
        )
    }
}
