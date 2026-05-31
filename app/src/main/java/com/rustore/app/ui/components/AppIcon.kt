package com.rustore.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun AppIcon(
    iconUrl: String?,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = iconUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
    )
}

private fun DrawScope.drawLetterText(
    textMeasurer: TextMeasurer,
    letter: String,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = (canvasSize.width * 0.42f).sp,
        fontWeight = FontWeight.Bold
    )
    val textLayoutResult = textMeasurer.measure(letter, textStyle)
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = androidx.compose.ui.geometry.Offset(
            (canvasSize.width - textLayoutResult.size.width) / 2f,
            (canvasSize.height - textLayoutResult.size.height) / 2f
        )
    )
}
