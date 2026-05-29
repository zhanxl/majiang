package com.majiang.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majiang.analyzer.RecommendationPriority
import com.majiang.analyzer.TileRecommendation
import com.majiang.model.Tile

@Composable
fun TileHandComposable(
    tiles: List<Tile>,
    selectedTile: Tile? = null,
    recommendations: List<TileRecommendation> = emptyList(),
    onTileClick: ((Tile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tiles.forEach { tile ->
            val recommendation = recommendations.find { it.tile == tile }
            val isRecommended = recommendation != null &&
                recommendation.priority == RecommendationPriority.BEST

            TileComposable(
                tile = tile,
                isSelected = tile == selectedTile,
                isRecommended = isRecommended,
                onClick = onTileClick?.let { click -> { click(tile) } }
            )
        }
    }
}
