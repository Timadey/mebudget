package com.mebudget.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mebudget.app.ui.theme.Moss
import com.mebudget.app.ui.theme.Overspend
import com.mebudget.app.ui.theme.AccentBlue
import com.mebudget.app.ui.theme.Pine
import com.mebudget.app.ui.theme.Rust
import com.mebudget.app.ui.theme.Warning

fun progressBarBrush(progress: Float, isOverspent: Boolean): Brush {
    val gradientStart: Color
    val gradientEnd: Color
    when {
        isOverspent -> {
            gradientStart = Rust
            gradientEnd = Overspend
        }
        progress < 0.2f -> {
            gradientStart = Warning
            gradientEnd = Rust
        }
        else -> {
            gradientStart = AccentBlue
            gradientEnd = AccentBlue.copy(alpha = 0.7f)
        }
    }
    return Brush.horizontalGradient(listOf(gradientStart, gradientEnd))
}

@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.Transparent
) {
    val brush = progressBarBrush(progress, progress < 0.2f)
    val overspent = progress < 0.0f
    val isOverspent = progress < 0.2f || progress > 1.0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (trackColor != Color.Transparent) {
                    Modifier.background(trackColor)
                } else Modifier
            )
            .drawBehind {
                drawRoundRect(
                    brush = brush,
                    cornerRadius = CornerRadius(size.height / 2f),
                    size = Size(size.width * animatedProgress, size.height)
                )
            }
    )
}

@Composable
fun BlockProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    segments: Int = 12
) {
    val resolvedColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary
    val resolvedTrackColor = if (trackColor != Color.Unspecified) trackColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val filledSegments = (progress * segments).toInt().coerceIn(0, segments)

    Box(
        modifier = modifier
            .clipToBounds()
            .drawBehind {
                val segmentWidth = size.width / segments
                for (i in 0 until segments) {
                    val left = segmentWidth * i
                    val c = if (i < filledSegments) resolvedColor else resolvedTrackColor
                    drawRect(
                        color = c,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth - 2.dp.toPx(), size.height)
                    )
                }
            }
    )
}
