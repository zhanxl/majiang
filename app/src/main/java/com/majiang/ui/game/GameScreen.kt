package com.majiang.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.majiang.analyzer.RecommendationPriority
import com.majiang.analyzer.TileRecommendation
import com.majiang.model.GamePhase
import com.majiang.ui.components.RecommendationBadge
import com.majiang.ui.components.TileComposable
import com.majiang.ui.components.TileHandComposable
import com.majiang.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("麻将分析") }
            )
        }
    ) { padding ->
        if (!uiState.isGameStarted) {
            GameSetupContent(
                onStartGame = { names, rule -> viewModel.startGame(names, rule) },
                modifier = Modifier.padding(padding)
            )
        } else {
            GamePlayContent(
                uiState = uiState,
                onDrawTile = { viewModel.drawTile() },
                onDiscardTile = { viewModel.discardTile(it) },
                onSelectTile = { viewModel.selectTile(it) },
                onEndGame = { viewModel.endGame(it) },
                onReset = { viewModel.resetGame() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun GameSetupContent(
    onStartGame: (List<String>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "麻将分析系统",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onStartGame(listOf("玩家1", "玩家2", "玩家3", "玩家4"), "广东麻将") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始游戏（广东麻将）")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { onStartGame(listOf("玩家1", "玩家2", "玩家3", "玩家4"), "四川麻将") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始游戏（四川麻将）")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { onStartGame(listOf("玩家1", "玩家2", "玩家3", "玩家4"), "长沙红中麻将") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始游戏（长沙红中麻将）")
        }
    }
}

@Composable
private fun GamePlayContent(
    uiState: com.majiang.viewmodel.GameUiState,
    onDrawTile: () -> Unit,
    onDiscardTile: (com.majiang.model.Tile) -> Unit,
    onSelectTile: (com.majiang.model.Tile) -> Unit,
    onEndGame: (Int?) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState = uiState.gameState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "剩余牌: ${gameState.remainingWallCount} | 回合: ${gameState.turnCount}",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        for ((index, player) in gameState.players.withIndex()) {
            PlayerSection(
                player = player,
                playerIndex = index,
                isCurrentPlayer = index == gameState.currentPlayerIndex,
                selectedTile = if (index == gameState.currentPlayerIndex) uiState.selectedTile else null,
                recommendations = if (index == gameState.currentPlayerIndex) uiState.recommendations else emptyList(),
                onTileClick = onSelectTile
            )
            if (index < gameState.players.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isReady) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("听牌！", style = MaterialTheme.typography.titleMedium)
                    if (uiState.waitingTiles.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(uiState.waitingTiles) { tile ->
                                TileComposable(tile = tile)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (gameState.phase == GamePhase.DRAW) {
                Button(onClick = onDrawTile, modifier = Modifier.weight(1f)) {
                    Text("摸牌")
                }
            }
            if (gameState.phase == GamePhase.DISCARD && uiState.selectedTile != null) {
                Button(
                    onClick = { onDiscardTile(uiState.selectedTile) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("出牌")
                }
            }
            OutlinedButton(onClick = { onEndGame(null) }, modifier = Modifier.weight(1f)) {
                Text("流局")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Text("重置")
            }
        }
    }
}

@Composable
private fun PlayerSection(
    player: com.majiang.model.Player,
    playerIndex: Int,
    isCurrentPlayer: Boolean,
    selectedTile: com.majiang.model.Tile?,
    recommendations: List<TileRecommendation>,
    onTileClick: (com.majiang.model.Tile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${player.wind.displayName} ${player.name}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (isCurrentPlayer) {
                    Text("← 当前", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TileHandComposable(
                tiles = player.hand,
                selectedTile = selectedTile,
                recommendations = recommendations,
                onTileClick = onTileClick
            )

            if (player.discards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("出牌:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(player.discards) { tile ->
                        TileComposable(tile = tile, modifier = Modifier.size(28.dp, 40.dp))
                    }
                }
            }
        }
    }
}
