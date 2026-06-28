package com.example.ai_tranning.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ai_tranning.ui.theme.StatusDoneBg
import com.example.ai_tranning.ui.theme.StatusDoneFg
import com.example.ai_tranning.ui.theme.StatusInProgressBg
import com.example.ai_tranning.ui.theme.StatusInProgressFg
import com.example.ai_tranning.ui.theme.StatusTodoBg
import com.example.ai_tranning.ui.theme.StatusTodoFg

/**
 * Small colored pill that labels a task's workflow status, following the design system:
 * `TODO` → gray, `IN_PROGRESS` → blue, `DONE` → green. Any other value falls back to theme surface
 * colors. The underscore in `IN_PROGRESS` is rendered as a space.
 *
 * @param status the status string to display (`"TODO"`, `"IN_PROGRESS"`, or `"DONE"`).
 */
@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "TODO" -> StatusTodoBg to StatusTodoFg
        "IN_PROGRESS" -> StatusInProgressBg to StatusInProgressFg
        "DONE" -> StatusDoneBg to StatusDoneFg
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = status.replace("_", " "),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}