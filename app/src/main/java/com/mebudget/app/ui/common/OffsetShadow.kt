package com.mebudget.app.ui.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
fun Modifier.offsetShadow(
    offset: Dp = 4.dp,
    color: Color = Color.Black
): Modifier = this.drawBehind {
    val offsetPx = offset.toPx()
    drawRect(
        color = color,
        topLeft = Offset(offsetPx, offsetPx),
        size = Size(size.width, size.height)
    )
}
