package com.nuevoso.launcher.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuevoso.launcher.ui.theme.SolCyan
import com.nuevoso.launcher.ui.theme.SolGold
import com.nuevoso.launcher.ui.theme.SolTerracotta
import com.nuevoso.launcher.ui.theme.SolTerracottaDark

enum class OrbState { Idle, Thinking, Speaking, Listening }

@Composable
fun SolOrb(
    state: OrbState = OrbState.Idle,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 128.dp,
) {
    val breatheDuration = when (state) {
        OrbState.Idle      -> 5500
        OrbState.Thinking  -> 1100
        OrbState.Speaking  -> 3000
        OrbState.Listening -> 800
    }
    val glowTarget = when (state) {
        OrbState.Speaking, OrbState.Listening -> 0.65f
        OrbState.Thinking                     -> 0.50f
        OrbState.Idle                         -> 0.40f
    }

    val transition = key(state) { rememberInfiniteTransition(label = "orb-$state") }

    val breatheScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = glowTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == OrbState.Thinking) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == OrbState.Thinking) 8000 else 30000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring-rotation",
    )

    val ringFirstColor by animateColorAsState(
        targetValue = if (state == OrbState.Listening) SolCyan else SolTerracotta,
        animationSpec = tween(600),
        label = "ring-color",
    )

    Box(modifier = modifier.size(sizeDp)) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val maxRadius = size.minDimension / 2f
            drawOrb(
                maxRadius = maxRadius,
                breatheScale = breatheScale,
                glowAlpha = glowAlpha,
                ringRotation = ringRotation,
                ringFirstColor = ringFirstColor,
            )
        }
    }
}

private fun DrawScope.drawOrb(
    maxRadius: Float,
    breatheScale: Float,
    glowAlpha: Float,
    ringRotation: Float,
    ringFirstColor: Color,
) {
    val c = center

    // Halo layer — outermost soft glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SolTerracotta.copy(alpha = glowAlpha * 0.3f),
                Color.Transparent,
            ),
            center = c,
            radius = maxRadius * breatheScale,
        ),
        radius = maxRadius * breatheScale,
        center = c,
    )

    // Sweep ring
    rotate(degrees = ringRotation, pivot = c) {
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(ringFirstColor, SolGold, SolCyan, ringFirstColor),
                center = c,
            ),
            radius = maxRadius * 0.82f * breatheScale,
            center = c,
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    // Core circle
    val coreRadius = maxRadius * 0.65f * breatheScale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SolGold, SolTerracotta, SolTerracottaDark),
            center = c,
            radius = coreRadius,
        ),
        radius = coreRadius,
        center = c,
    )

    // Inner highlight for 3-D effect
    drawCircle(
        color = Color.White.copy(alpha = 0.22f),
        radius = coreRadius * 0.28f,
        center = Offset(
            x = c.x - coreRadius * 0.20f,
            y = c.y - coreRadius * 0.25f,
        ),
    )
}
