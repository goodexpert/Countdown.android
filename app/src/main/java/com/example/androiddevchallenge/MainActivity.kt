/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androiddevchallenge.ui.theme.MyTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                MyApp()
            }
        }
    }
}

enum class CountdownState { IDLE, PAUSE, PLAYING, STOP }

private fun computeCurrentValue(sconds: Int, velocity: Float): Int {
    return (sconds + (velocity + 60 - 1) * -1).toInt() / 60 * 60
}

private fun computeNearestValue(seconds: Int): Int {
    val sign = if (seconds >= 0) 0 else -1
    return (seconds + (60 - 1) * sign) / 60 * 60
}

private fun computeTransitionDuration(duration: Long, velocity: Float): Long {
    return (Math.abs(velocity) + duration - 1).toLong() / duration * duration
}

@Composable
fun CarouselRuler(
    milliseconds: Long,
    onChanged: (Long, Long) -> Unit,
    onStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val DEFAULT_DURATION = 250L
    val DEFAULT_UNITS = 250
    val DEFAULT_HEIGHT = 80
    val DEFAULT_PADDING = 32
    val MIN_VELOCITY = 80
    val MAX_MINUTES = 60
    val TEXT_LINE_HEIGHT = 23
    val TEXT_SIZE = 14

    val prevTouchX = remember { mutableStateOf(0f) }
    val tracker = remember { mutableStateOf(VelocityTracker.obtain()) }
    val velocityTracker = tracker.value

    val painter = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        color = Color.White.toArgb()
    }
    var currentTimer = (milliseconds / 1000).toInt()

    Canvas(
        modifier = modifier
            .height(DEFAULT_HEIGHT.dp)
            .padding(DEFAULT_PADDING.dp, 0.dp)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        prevTouchX.value = event.x
                        velocityTracker.clear()
                        velocityTracker.addMovement(event)
                        onStopped()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentTimer += (prevTouchX.value - event.x).toInt() * 2
                        onChanged(currentTimer * 1000L, 0L)

                        prevTouchX.value = event.x
                        velocityTracker.addMovement(event)
                    }
                    MotionEvent.ACTION_UP -> {
                        velocityTracker.computeCurrentVelocity(DEFAULT_UNITS)

                        if (Math.abs(velocityTracker.xVelocity) > MIN_VELOCITY) {
                            val transitionDuration = computeTransitionDuration(
                                DEFAULT_DURATION,
                                velocityTracker.xVelocity
                            )
                            val value = computeCurrentValue(currentTimer, velocityTracker.xVelocity)
                            onChanged(value * 1000L, transitionDuration)
                        } else {
                            val value = computeNearestValue(currentTimer)
                            onChanged(value * 1000L, DEFAULT_DURATION)
                        }
                    }
                }
                true
            }
    ) {
        val lineHeight = size.height - TEXT_LINE_HEIGHT.dp.toPx()
        val nY = lineHeight
        val radius = size.width / 2
        val offsetX = size.width / 2
        val minZ = radius / 9
        val degree = 360.0 / MAX_MINUTES
        val offset = (currentTimer * degree / 60.0) + 90.0
        val shortLineHeight = lineHeight / 2F

        for (i in 0 until MAX_MINUTES) {
            val radian = Math.toRadians(i * degree * -1 + offset)
            val nX = (Math.cos(radian) * radius + offsetX).toFloat()
            val nZ = (Math.sin(radian) * radius).toFloat()
            if (nZ < minZ) continue

            val length = if (i % 5 == 0) lineHeight else shortLineHeight
            val opacity = (0xFF * (nZ - minZ) / (radius - minZ)).toInt()

            // TODO: draw a line of ruler
            drawLine(
                color = Color.White.copy(opacity / 255f),
                start = Offset(nX, nY),
                end = Offset(nX, nY - length)
            )

            if (i % 5 == 0) {
                painter.apply {
                    alpha = opacity
                    textSize = TEXT_SIZE.sp.toPx()
                }

                drawIntoCanvas {
                    it.nativeCanvas.drawText(i.toString(), nX, size.height, painter)
                }
            }
        }
    }
}

@Composable
fun CircularTimer(
    current: Long,
    timeout: Long,
    modifier: Modifier = Modifier
) {
    val timeString = "%02d:%02d.%03d".format(current / 1000 / 60 % 60, current / 1000 % 60, current % 1000)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = modifier.fillMaxSize(),
        ) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.minDimension / 3
            val innerRadius = outerRadius - 4.dp.toPx()

            val radius = outerRadius - 2.dp.toPx()
            val progressSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
            val sweepAngle = 360f * current / timeout

            drawCircle(
                color = Color.LightGray,
                center = centerOffset,
                radius = outerRadius,
                style = Stroke(width = 1.dp.toPx())
            )

            drawCircle(
                color = Color.LightGray,
                center = centerOffset,
                radius = innerRadius,
                style = Stroke(width = 1.dp.toPx())
            )

            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = progressSize,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Text(
            text = timeString,
            color = Color.White,
            style = MaterialTheme.typography.h4
        )
    }
}

@Composable
fun PlayButton(
    isRunning: Boolean,
    onChanged: (CountdownState) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Button(
            modifier = Modifier.wrapContentSize(),
            onClick = { onChanged(if (isRunning) CountdownState.PAUSE else CountdownState.PLAYING) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Image(
                painter = painterResource(id = if (isRunning) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = null
            )
        }
    }
}

@Composable
fun CountdownTimer(
    current: Long,
    timeout: Long,
    maxTime: Long,
    animationDuration: Int,
    state: CountdownState,
    onChangedState: (CountdownState) -> Unit,
    onChangedTimeout: (Long, Long) -> Unit,
    onFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = animateIntAsState(
        targetValue = (current / 1000).toInt(),
        animationSpec = tween(durationMillis = animationDuration, easing = LinearOutSlowInEasing),
        finishedListener = { seconds ->
            if (state == CountdownState.IDLE) {
                val temp = seconds % maxTime
                onFinished((if (temp > 0) temp else temp + maxTime) * 1000L)
            }
        }
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colors.background)
    ) {
        val isAnimation = (state != CountdownState.PLAYING && animationDuration != 0)
        val milliseconds = seconds.value * 1000L

        val carouselTime = (if (isAnimation) milliseconds else current) % maxTime
        val currentTime = if (carouselTime > 0) carouselTime else carouselTime + maxTime

        val timeoutTemp = (if (isAnimation) milliseconds else timeout) % maxTime
        val timeoutTime = if (timeoutTemp > 0) timeoutTemp else timeoutTemp + maxTime

        CircularTimer(
            current = currentTime,
            timeout = timeoutTime,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        CarouselRuler(
            milliseconds = carouselTime,
            onChanged = onChangedTimeout,
            onStopped = { onChangedState(CountdownState.STOP) },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )

        PlayButton(
            isRunning = state === CountdownState.PLAYING,
            onChanged = { onChangedState(it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

// Start building your app here!
@Composable
fun MyApp() {
    val COUNT_DOWN_INTERVAL = 40L
    val DEFAULT_TIMEOUT = 900_000L
    val MAX_TIMEOUT = 3600_000L

    val current = remember { mutableStateOf(DEFAULT_TIMEOUT) }
    val timeout = remember { mutableStateOf(DEFAULT_TIMEOUT) }
    val animationDuration = remember { mutableStateOf(0) }

    val state = remember { mutableStateOf(CountdownState.IDLE) }
    val countDownTimer = remember { mutableStateOf<CountDownTimer?>(null) }
    val coroutineScope = rememberCoroutineScope()

    CountdownTimer(
        current = current.value,
        timeout = timeout.value,
        maxTime = MAX_TIMEOUT,
        animationDuration = animationDuration.value,
        state = state.value,
        onChangedState = { newState ->
            coroutineScope.launch {
                when (newState) {
                    CountdownState.PLAYING -> {
                        if (state.value != CountdownState.PLAYING) {
                            val countDown = current.value
                            countDownTimer.value = object : CountDownTimer(countDown, COUNT_DOWN_INTERVAL) {
                                override fun onTick(millisUntilFinished: Long) {
                                    current.value = millisUntilFinished
                                }

                                override fun onFinish() {
                                    current.value = 0
                                    state.value = CountdownState.STOP
                                }
                            }.start()
                        }
                    }
                    else -> {
                        if (state.value == CountdownState.PLAYING) {
                            countDownTimer.value?.cancel()
                            countDownTimer.value = null
                        }
                    }
                }
                state.value = newState
            }
        },
        onChangedTimeout = { milliseconds, duration ->
            coroutineScope.launch {
                if (state.value != CountdownState.IDLE) {
                    countDownTimer.value?.cancel()
                    countDownTimer.value = null
                    state.value = CountdownState.IDLE
                }

                current.value = milliseconds
                timeout.value = milliseconds
                animationDuration.value = duration.toInt()
            }
        },
        onFinished = { milliseconds ->
            coroutineScope.launch {
                current.value = milliseconds
                timeout.value = milliseconds
                animationDuration.value = 0
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        MyApp()
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        MyApp()
    }
}
