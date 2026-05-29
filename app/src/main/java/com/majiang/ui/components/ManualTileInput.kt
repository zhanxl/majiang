package com.majiang.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun ManualTileInput(
    selectedTiles: List<Tile>,
    onTileAdded: (Tile) -> Unit,
    onTileRemoved: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    showHongzhongOnly: Boolean = false
) {
    val validCategories = if (showHongzhongOnly) {
        listOf(TileCategory.WAN, TileCategory.TIAO, TileCategory.TONG, TileCategory.JIAN)
    } else {
        TileCategory.entries
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选手牌 (${selectedTiles.size}张)",
                style = MaterialTheme.typography.titleSmall
            )
            Row {
                if (selectedTiles.isNotEmpty()) {
                    IconButton(onClick = { onTileRemoved(selectedTiles.lastIndex) }) {
                        Icon(Icons.Default.Close, contentDescription = "撤销", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onClear) {
                        Text("清空", fontSize = 12.sp)
                    }
                }
            }
        }

        if (selectedTiles.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(selectedTiles) { tile ->
                    TileComposable(
                        tile = tile,
                        onClick = { onTileRemoved(selectedTiles.indexOf(tile)) },
                        modifier = Modifier.size(36.dp, 50.dp)
                    )
                }
            }
        } else {
            Text(
                text = "点击下方牌面添加手牌",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TileCategoryTabs(
            categories = validCategories,
            onTileClick = onTileAdded,
            tileCounts = selectedTiles.groupingBy { it }.eachCount()
        )
    }
}

@Composable
private fun TileCategoryTabs(
    categories: List<TileCategory>,
    onTileClick: (Tile) -> Unit,
    tileCounts: Map<Tile, Int>
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabTitles = categories.map { cat ->
        when (cat) {
            TileCategory.WAN -> "万"
            TileCategory.TIAO -> "条"
            TileCategory.TONG -> "筒"
            TileCategory.FENG -> "风"
            TileCategory.JIAN -> "箭"
        }
    }

    Column {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        val selectedCategory = categories.getOrElse(selectedTab) { TileCategory.WAN }
        val tiles = Tile.entries.filter { it.category == selectedCategory }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tiles.forEach { tile ->
                val count = tileCounts[tile] ?: 0
                val isMaxed = count >= 4

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isMaxed) { onTileClick(tile) }
                        .padding(4.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        MiniTileView(tile = tile)

                        if (count > 0) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$count",
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTileView(tile: Tile) {
    val backgroundColor = when (tile.category) {
        TileCategory.WAN -> Color(0xFFFFEBEE)
        TileCategory.TIAO -> Color(0xFFE8F5E9)
        TileCategory.TONG -> Color(0xFFE3F2FD)
        TileCategory.FENG -> Color(0xFFFFF3E0)
        TileCategory.JIAN -> Color(0xFFF3E5F5)
    }

    val textColor = when (tile.category) {
        TileCategory.WAN -> Color(0xFFC62828)
        TileCategory.TIAO -> Color(0xFF2E7D32)
        TileCategory.TONG -> Color(0xFF1565C0)
        TileCategory.FENG -> Color(0xFFE65100)
        TileCategory.JIAN -> when (tile) {
            Tile.JIAN_ZHONG -> Color(0xFFC62828)
            Tile.JIAN_FA -> Color(0xFF2E7D32)
            Tile.JIAN_BAI -> Color(0xFF424242)
            else -> Color(0xFF6A1B9A)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.size(44.dp, 60.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp, 60.dp)
        ) {
            Text(
                text = tile.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
