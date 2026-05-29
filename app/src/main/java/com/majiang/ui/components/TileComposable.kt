package com.majiang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majiang.model.Tile
import com.majiang.model.TileCategory

@Composable
fun TileComposable(
    tile: Tile,
    isSelected: Boolean = false,
    isRecommended: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        tile.isWildcard -> Color(0xFFFFEBEE)
        tile.category == TileCategory.WAN -> Color(0xFFFFEBEE)
        tile.category == TileCategory.TIAO -> Color(0xFFE8F5E9)
        tile.category == TileCategory.TONG -> Color(0xFFE3F2FD)
        tile.category == TileCategory.FENG -> Color(0xFFFFF3E0)
        tile.category == TileCategory.JIAN -> Color(0xFFF3E5F5)
        else -> Color(0xFFFFEBEE)
    }

    val textColor = when {
        tile.isWildcard -> Color(0xFFC62828)
        tile.category == TileCategory.WAN -> Color(0xFFC62828)
        tile.category == TileCategory.TIAO -> Color(0xFF2E7D32)
        tile.category == TileCategory.TONG -> Color(0xFF1565C0)
        tile.category == TileCategory.FENG -> Color(0xFFE65100)
        tile.category == TileCategory.JIAN -> when (tile) {
            Tile.JIAN_ZHONG -> Color(0xFFC62828)
            Tile.JIAN_FA -> Color(0xFF2E7D32)
            Tile.JIAN_BAI -> Color(0xFF424242)
            else -> Color(0xFF6A1B9A)
        }
        else -> Color(0xFFC62828)
    }

    val borderColor = when {
        tile.isWildcard && !isSelected && !isRecommended -> Color(0xFFFFCDD2)
        isSelected -> MaterialTheme.colorScheme.primary
        isRecommended -> Color(0xFF4CAF50)
        else -> Color(0xFFBDBDBD)
    }

    val borderWidth = when {
        tile.isWildcard && !isSelected && !isRecommended -> 2.dp
        isSelected -> 2.dp
        isRecommended -> 2.dp
        else -> 1.dp
    }

    Box(
        modifier = modifier
            .size(40.dp, 56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tile.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
