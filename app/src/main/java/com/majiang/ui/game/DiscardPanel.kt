package com.majiang.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majiang.model.Player
import com.majiang.model.Tile
import com.majiang.ui.components.TileComposable

@Composable
fun DiscardPanel(
    players: List<Player>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "出牌记录",
            style = MaterialTheme.typography.titleMedium
        )

        players.forEachIndexed { index, player ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "${player.wind.displayName} ${player.name}",
                        style = MaterialTheme.typography.labelLarge
                    )

                    if (player.discards.isEmpty()) {
                        Text(
                            text = "暂无出牌",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(player.discards) { tile ->
                                TileComposable(
                                    tile = tile,
                                    modifier = Modifier.size(28.dp, 40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
