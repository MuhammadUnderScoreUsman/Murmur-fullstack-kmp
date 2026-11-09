package com.example.chatappkross.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private val Ink = Color(0xFF1B1640)
private val Coral = Color(0xFFFF7A59)

/**
 * Murmur's Android splash: dark ink background, flat coral and white bubbles that pop in with a bounce,
 * "typing" dots, and a wordmark. No assets: the logo is drawn in code.
 *
 * Don't show it by hand: wrap your whole app in [SplashGate] (below). Restoring the saved session takes a few
 * milliseconds, so without a minimum time the splash would be gone before a single frame of it is drawn.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val back = remember { Animatable(0f) }
    val front = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val pop = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        launch { back.animateTo(1f, pop) }
        launch { delay(130); front.animateTo(1f, pop) }
        launch { delay(380); wordmark.animateTo(1f, tween(500)) }
    }

    // One looping 0..1 value drives the three dots, each a little behind the one before
    val phase by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "phase",
    )

    Box(modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(128.dp)) {
                val s = size.width / 108f // the logo is designed on a 108 x 108 grid

                // Coral bubble, behind
                scale(back.value.coerceAtLeast(0.001f), pivot = Offset(70f * s, 60f * s)) {
                    bubble(44f, 40f, 42f, 34f, listOf(62f to 72f, 78f to 72f, 80f to 84f), Coral, s, outline = null)
                }

                // White bubble, in front, with a ring in the background colour so it separates from the coral one
                scale(front.value.coerceAtLeast(0.001f), pivot = Offset(40f * s, 43f * s)) {
                    bubble(22f, 26f, 46f, 34f, listOf(32f to 58f, 46f to 58f, 32f to 70f), Color.White, s, outline = Ink)
                    for (i in 0..2) {
                        val hop = max(0f, sin(2f * PI.toFloat() * (phase - i * 0.1f))) // 0 most of the time, a quick hop
                        drawCircle(Ink, radius = 2.8f * s, center = Offset((37f + 8f * i) * s, (43f - 3.5f * hop) * s))
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
            Text(
                text = "Murmur",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.alpha(wordmark.value),
            )
        }
    }
}

private fun DrawScope.bubble(
    x: Float, y: Float, w: Float, h: Float,
    tail: List<Pair<Float, Float>>,
    color: Color, s: Float, outline: Color?,
) {
    val corner = CornerRadius(14f * s)
    val tailPath = Path().apply {
        moveTo(tail[0].first * s, tail[0].second * s)
        tail.drop(1).forEach { lineTo(it.first * s, it.second * s) }
        close()
    }
    if (outline != null) {
        val ring = Stroke(width = 6f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawRoundRect(outline, Offset(x * s, y * s), Size(w * s, h * s), corner, style = ring)
        drawPath(tailPath, outline, style = ring)
    }
    drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), corner)
    drawPath(tailPath, color)
}

/**
 * Shows [SplashScreen] until the session is known AND at least [minMillis] have passed, then fades into [content].
 * Wrap everything, including the NavHost, so the NavHost is only created once the session is known and its start
 * destination (login or home) is already right. No screen needs a "splash" destination.
 *
 *     setContent {
 *         val session by sessionManager.state.collectAsStateWithLifecycle()
 *         SplashGate(isLoading = session is SessionState.Loading) {
 *             ChatAppRoot(...)
 *         }
 *     }
 */
@Composable
fun SplashGate(
    isLoading: Boolean,
    minMillis: Long = 1800,
    content: @Composable () -> Unit,
) {
    // Saveable, so rotating the phone doesn't replay the splash
    var minTimePassed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minMillis)
        minTimePassed = true
    }

    Crossfade(targetState = isLoading || !minTimePassed, animationSpec = tween(400), label = "splash") { showSplash ->
        if (showSplash) SplashScreen() else content()
    }
}