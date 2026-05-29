package com.majiang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majiang.analyzer.RecommendationPriority

@Composable
fun RecommendationBadge(
    priority: RecommendationPriority,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, text) = when (priority) {
        RecommendationPriority.BEST -> Color(0xFF4CAF50) to "最佳"
        RecommendationPriority.GOOD -> Color(0xFF8BC34A) to "推荐"
        RecommendationPriority.NEUTRAL -> Color(0xFFFFC107) to "一般"
        RecommendationPriority.AVOID -> Color(0xFFFF9800) to "避免"
        RecommendationPriority.DANGEROUS -> Color(0xFFF44336) to "危险"
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .size(32.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
