package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A quiet, thin completion mark restricted to the rendered course-name lines. */
@Composable
internal fun CourseNameText(name: String, struck: Boolean, modifier: Modifier = Modifier) {
    var layout by remember(name) { mutableStateOf<TextLayoutResult?>(null) }
    val strokeColor = LocalContentColor.current.copy(alpha = .55f)
    Text(name,
        modifier = modifier.semantics {
            if (struck) stateDescription = "已结束，名称已划线"
        }.drawWithContent {
            drawContent()
            if (struck) layout?.let { text ->
                for (line in 0 until text.lineCount) {
                    val left = text.getLineLeft(line)
                    val right = text.getLineRight(line)
                    val y = (text.getLineTop(line) + text.getLineBaseline(line)) / 2f + 1.dp.toPx()
                    if (right > left) drawLine(strokeColor,
                        Offset(left + .5.dp.toPx(), y), Offset(right - .5.dp.toPx(), y),
                        strokeWidth = .8.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        onTextLayout = { layout = it })
}
