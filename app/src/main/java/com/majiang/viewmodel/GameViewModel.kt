package com.majiang.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majiang.analyzer.RecommendationEngine
import com.majiang.analyzer.ReadyAnalyzer
import com.majiang.analyzer.TileRecommendation
import com.majiang.analyzer.WinChecker
import com.majiang.model.DiscardRecord
import com.majiang.model.GamePhase
import com.majiang.model.GameRecord
import com.majiang.model.GameState
import com.majiang.model.Player
import com.majiang.model.Tile
import com.majiang.model.TileSet
import com.majiang.model.Wind
import com.majiang.model.rule.ChangshaHongzhongRule
import com.majiang.model.rule.GuangdongRule
import com.majiang.model.rule.MahjongRule
import com.majiang.model.rule.SichuanRule
import com.majiang.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val gameState: GameState = GameState(),
    val recommendations: List<TileRecommendation> = emptyList(),
    val isReady: Boolean = false,
    val waitingTiles: List<Tile> = emptyList(),
    val selectedTile: Tile? = null,
    val isGameStarted: Boolean = false,
    val errorMessage: String? = null,
    val isWildcardMode: Boolean = false,
    val isShaGui: Boolean = false,
    val hongzhongCount: Int = 0
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val winChecker: WinChecker,
    private val readyAnalyzer: ReadyAnalyzer,
    private val recommendationEngine: RecommendationEngine,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentRule: MahjongRule = GuangdongRule()

    fun startGame(playerNames: List<String>, ruleName: String = "广东麻将") {
        currentRule = when (ruleName) {
            "四川麻将" -> SichuanRule()
            "长沙红中麻将" -> ChangshaHongzhongRule()
            else -> GuangdongRule()
        }

        val isWildcardMode = currentRule is ChangshaHongzhongRule

        val gameState = if (isWildcardMode) {
            createHongzhongGame(playerNames)
        } else {
            GameState.createNewGame(playerNames, currentRule)
        }

        _uiState.value = GameUiState(
            gameState = gameState,
            isGameStarted = true,
            isWildcardMode = isWildcardMode
        )
        updateAnalysis()
    }

    private fun createHongzhongGame(playerNames: List<String>): GameState {
        val hongzhongTiles = Tile.hongzhongSet()
        val fullSet = mutableListOf<Tile>()
        hongzhongTiles.forEach { tile ->
            repeat(4) { fullSet.add(tile) }
        }
        val shuffled = fullSet.shuffled()

        val winds = Wind.entries
        var wall = shuffled
        val players = playerNames.mapIndexed { index, name ->
            val (hand, remaining) = wall.let { tiles ->
                val dealt = tiles.take(13)
                dealt to tiles.drop(13)
            }
            wall = remaining
            Player(
                name = name,
                wind = winds[index % winds.size],
                isDealer = index == 0,
                hand = hand.sortedBy { it.ordinal }
            )
        }

        return GameState(
            players = players,
            wall = TileSet(wall),
            currentPlayerIndex = 0,
            phase = GamePhase.DRAW,
            dealerIndex = 0,
            roundWind = Wind.EAST
        )
    }

    fun drawTile() {
        val state = _uiState.value
        val gameState = state.gameState

        if (gameState.phase != GamePhase.DRAW) return

        val (drawnTile, newGameState) = gameState.drawFromWall()
        if (drawnTile == null) {
            _uiState.value = state.copy(
                gameState = newGameState,
                errorMessage = "牌墙已空，流局"
            )
            return
        }

        val updatedPlayer = gameState.currentPlayer.addTileToHand(drawnTile)
        val stateAfterDraw = newGameState
            .withPlayer(gameState.currentPlayerIndex, updatedPlayer)
            .withPhase(GamePhase.DISCARD)

        _uiState.value = state.copy(gameState = stateAfterDraw)
        updateAnalysis()
    }

    fun discardTile(tile: Tile) {
        val state = _uiState.value
        val gameState = state.gameState
        val currentPlayer = gameState.currentPlayer

        if (!currentPlayer.hasTileInHand(tile)) return

        val updatedPlayer = currentPlayer.addDiscard(tile)
        val stateAfterDiscard = gameState
            .withPlayer(gameState.currentPlayerIndex, updatedPlayer)
            .copy(lastDiscard = tile, lastDiscardPlayerIndex = gameState.currentPlayerIndex)
            .nextTurn()
            .withPhase(GamePhase.DRAW)

        _uiState.value = state.copy(
            gameState = stateAfterDiscard,
            selectedTile = null
        )
        updateAnalysis()
    }

    fun selectTile(tile: Tile) {
        _uiState.value = _uiState.value.copy(selectedTile = tile)
    }

    fun setRule(ruleName: String) {
        currentRule = when (ruleName) {
            "四川麻将" -> SichuanRule()
            "长沙红中麻将" -> ChangshaHongzhongRule()
            else -> GuangdongRule()
        }
    }

    fun endGame(winnerIndex: Int?) {
        val state = _uiState.value
        val gameState = state.gameState

        val record = GameRecord(
            playerNames = gameState.players.map { it.name },
            winnerIndex = winnerIndex,
            scores = gameState.players.map { 0 },
            ruleName = currentRule.name,
            roundWind = gameState.roundWind,
            discardHistory = gameState.players.flatMapIndexed { index, player ->
                player.discards.mapIndexed { turnNum, tile ->
                    DiscardRecord(index, tile, turnNum)
                }
            }
        )

        viewModelScope.launch {
            gameRepository.saveGame(record)
        }

        _uiState.value = state.copy(
            gameState = gameState.withPhase(GamePhase.WIN),
            isGameStarted = false
        )
    }

    fun resetGame() {
        _uiState.value = GameUiState()
    }

    private fun updateAnalysis() {
        val state = _uiState.value
        val currentPlayer = state.gameState.currentPlayer
        val visibleTiles = state.gameState.allVisibleTiles()

        val hongzhongCount = if (state.isWildcardMode) {
            currentPlayer.hand.count { it.isWildcard }
        } else 0

        val isShaGui = state.isWildcardMode && !currentPlayer.hand.contains(Tile.JIAN_ZHONG)

        val readyResult = if (state.isWildcardMode) {
            readyAnalyzer.analyzeReadyWithWildcard(currentPlayer.hand, visibleTiles)
        } else {
            readyAnalyzer.analyzeReady(currentPlayer.hand, visibleTiles)
        }

        val recommendations = recommendationEngine.getRecommendations(
            hand = currentPlayer.hand,
            visibleTiles = visibleTiles,
            playerDiscards = state.gameState.players.mapIndexed { index, player ->
                index to player.discards
            }.toMap(),
            currentPlayerIndex = state.gameState.currentPlayerIndex
        )

        _uiState.value = state.copy(
            recommendations = recommendations,
            isReady = readyResult.isReady,
            waitingTiles = readyResult.waitingTiles.map { it.tile },
            hongzhongCount = hongzhongCount,
            isShaGui = isShaGui
        )
    }
}
