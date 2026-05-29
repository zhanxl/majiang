package com.majiang.model

enum class GamePhase {
    WAITING,
    DEALING,
    DRAW,
    DISCARD,
    CHI_PONG_KONG,
    WIN,
    DRAW_GAME
}

data class GameState(
    val players: List<Player> = emptyList(),
    val wall: TileSet = TileSet.shuffled(),
    val currentPlayerIndex: Int = 0,
    val phase: GamePhase = GamePhase.WAITING,
    val roundWind: Wind = Wind.EAST,
    val dealerIndex: Int = 0,
    val lastDiscard: Tile? = null,
    val lastDiscardPlayerIndex: Int? = null,
    val turnCount: Int = 0
) {
    val currentPlayer: Player
        get() = players.getOrElse(currentPlayerIndex) { players.first() }

    val dealer: Player
        get() = players.getOrElse(dealerIndex) { players.first() }

    val remainingWallCount: Int
        get() = wall.size

    val isWallEmpty: Boolean
        get() = wall.size <= 0

    fun allDiscardedTiles(): List<Tile> = players.flatMap { it.discards }

    fun allVisibleTiles(): List<Tile> {
        val visible = mutableListOf<Tile>()
        players.forEach { player ->
            visible.addAll(player.discards)
            player.melds.forEach { meld ->
                visible.addAll(meld.tiles)
            }
        }
        return visible
    }

    fun withPhase(newPhase: GamePhase): GameState = copy(phase = newPhase)

    fun withCurrentPlayer(index: Int): GameState = copy(currentPlayerIndex = index)

    fun withPlayer(index: Int, player: Player): GameState {
        if (index !in players.indices) return this
        val newPlayers = players.toMutableList()
        newPlayers[index] = player
        return copy(players = newPlayers)
    }

    fun nextTurn(): GameState = copy(
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size,
        turnCount = turnCount + 1
    )

    fun drawFromWall(): Pair<Tile?, GameState> {
        if (isWallEmpty) return null to copy(phase = GamePhase.DRAW_GAME)
        val (drawn, remaining) = wall.deal(1)
        return drawn.firstOrNull() to copy(wall = remaining)
    }

    companion object {
        fun createNewGame(
            playerNames: List<String>,
            rule: com.majiang.model.rule.MahjongRule
        ): GameState {
            val shuffledWall = TileSet.shuffled()
            val winds = Wind.entries
            val players = playerNames.mapIndexed { index, name ->
                Player(
                    name = name,
                    wind = winds[index % winds.size],
                    isDealer = index == 0
                )
            }

            var wall = shuffledWall
            val dealtPlayers = players.map { player ->
                val (hand, remainingWall) = wall.deal(13)
                wall = remainingWall
                player.withHand(hand)
            }

            return GameState(
                players = dealtPlayers,
                wall = wall,
                currentPlayerIndex = 0,
                phase = GamePhase.DRAW,
                dealerIndex = 0,
                roundWind = Wind.EAST
            )
        }
    }
}
