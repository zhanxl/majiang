package com.majiang.model

enum class Wind(val displayName: String) {
    EAST("东"),
    SOUTH("南"),
    WEST("西"),
    NORTH("北")
}

enum class MeldType {
    CHI,
    PONG,
    KONG,
    AN_KONG
}

data class Meld(
    val type: MeldType,
    val tiles: List<Tile>
)

data class Player(
    val name: String,
    val wind: Wind,
    val hand: List<Tile> = emptyList(),
    val melds: List<Meld> = emptyList(),
    val discards: List<Tile> = emptyList(),
    val isDealer: Boolean = false
) {
    val totalTiles: Int
        get() = hand.size + melds.sumOf { meld ->
            when (meld.type) {
                MeldType.KONG, MeldType.AN_KONG -> 4
                else -> 3
            }
        }

    val isReady: Boolean
        get() = hand.size % 3 == 2

    fun withHand(newHand: List<Tile>): Player = copy(hand = newHand.sortedBy { it.ordinal })

    fun addDiscard(tile: Tile): Player = copy(
        hand = hand - tile,
        discards = discards + tile
    )

    fun addMeld(meld: Meld): Player = copy(
        melds = melds + meld
    )

    fun addTileToHand(tile: Tile): Player = copy(
        hand = (hand + tile).sortedBy { it.ordinal }
    )

    fun removeTileFromHand(tile: Tile): Player = copy(
        hand = hand - tile
    )

    fun hasTileInHand(tile: Tile): Boolean = hand.contains(tile)

    fun countInHand(tile: Tile): Int = hand.count { it == tile }
}
