package com.mebudget.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mebudget.app.ui.theme.AccentBlue
import com.mebudget.app.ui.theme.Overspend
import com.mebudget.app.ui.theme.Success
import com.mebudget.app.ui.theme.Warning

enum class TransactionType { Expense, Transfer, Adjustment }

@Composable
fun TransactionTypeBadge(
    type: TransactionType,
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        TransactionType.Expense -> Overspend
        TransactionType.Transfer -> AccentBlue
        TransactionType.Adjustment -> Warning
    }
    Canvas(modifier = modifier.size(8.dp)) {
        when (type) {
            TransactionType.Expense -> drawCircle(color)
            TransactionType.Transfer -> drawRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
            TransactionType.Adjustment -> drawTriangle(color)
        }
    }
}

enum class BudgetStatus { OnTrack, Warning, Overspent }

@Composable
fun BudgetStatusIndicator(
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        BudgetStatus.OnTrack -> Success
        BudgetStatus.Warning -> Warning
        BudgetStatus.Overspent -> Overspend
    }
    Canvas(modifier = modifier.size(8.dp)) {
        when (status) {
            BudgetStatus.OnTrack -> drawCircle(color)
            BudgetStatus.Warning -> drawTriangle(color)
            BudgetStatus.Overspent -> drawDiamond(color)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawRect(color = color)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun EmptyStateIllustration(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier.size(80.dp)) {
        val w = size.width
        val h = size.height
        // Bottom rectangle
        drawRect(
            color = color,
            topLeft = Offset(w * 0.1f, h * 0.6f),
            size = Size(w * 0.8f, h * 0.3f)
        )
        // Middle rectangle (offset left)
        drawRect(
            color = color.copy(alpha = 0.7f),
            topLeft = Offset(w * 0.0f, h * 0.35f),
            size = Size(w * 0.65f, h * 0.25f)
        )
        // Top rectangle (offset right)
        drawRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(w * 0.35f, h * 0.1f),
            size = Size(w * 0.55f, h * 0.25f)
        )
        // Intersecting circle
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = w * 0.2f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}

private fun DrawScope.drawTriangle(color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width / 2f, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, color, style = Fill)
}

private fun DrawScope.drawDiamond(color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width / 2f, 0f)
        lineTo(size.width, size.height / 2f)
        lineTo(size.width / 2f, size.height)
        lineTo(0f, size.height / 2f)
        close()
    }
    drawPath(path, color, style = Fill)
}