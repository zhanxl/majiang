package com.majiang.model.rule

import com.majiang.model.Tile

interface MahjongRule {
    val name: String
    val description: String

    fun canWin(hand: List<Tile>, melds: Int): Boolean

    fun calculateScore(
        hand: List<Tile>,
        melds: Int,
        isDealer: Boolean,
        isSelfDraw: Boolean,
        roundWind: com.majiang.model.Wind,
        seatWind: com.majiang.model.Wind
    ): Int

    fun canPong(hand: List<Tile>, discardedTile: Tile): Boolean

    fun canKong(hand: List<Tile>, discardedTile: Tile): Boolean

    fun canChi(hand: List<Tile>, discardedTile: Tile): Boolean

    fun getValidChiCombinations(hand: List<Tile>, discardedTile: Tile): List<List<Tile>>

    fun isAllowedDiscard(tile: Tile, hand: List<Tile>): Boolean
}
