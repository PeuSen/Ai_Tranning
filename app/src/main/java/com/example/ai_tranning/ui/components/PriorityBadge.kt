package com.example.ai_tranning.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PriorityBadge(priority: String) {
    val (backgroundColor, textColor) = when (priority) {
        "HIGH" -> Color(0xFFFFCDD2) to Color(0xFFC62828)
        "MEDIUM" -> Color(0xFFFFF9C4) to Color(0xFFF9A825)
        "LOW" -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = priority,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}