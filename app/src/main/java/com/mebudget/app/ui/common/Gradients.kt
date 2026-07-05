package com.mebudget.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mebudget.app.ui.theme.Moss
import com.mebudget.app.ui.theme.Overspend
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
            gradientStart = Pine
            gradientEnd = Moss
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
