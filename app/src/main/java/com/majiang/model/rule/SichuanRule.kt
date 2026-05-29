package com.majiang.model.rule

import com.majiang.model.Tile
import com.majiang.model.Wind

class SichuanRule : MahjongRule {

    override val name: String = "四川麻将"
    override val description: String = "四川麻将规则，缺一门，必须缺一门才能胡牌"

    override fun canWin(hand: List<Tile>, melds: Int): Boolean {
        if (hand.size % 3 != 2) return false
        if (!checkMissingOneSuit(hand)) return false
        return checkStandardWin(hand) || checkSevenPairs(hand)
    }

    private fun checkMissingOneSuit(hand: List<Tile>): Boolean {
        val categories = hand.map { it.category }.filter {
            it == com.majiang.model.TileCategory.WAN ||
            it == com.majiang.model.TileCategory.TIAO ||
            it == com.majiang.model.TileCategory.TONG
        }.toSet()
        return categories.size <= 2
    }

    private fun checkStandardWin(hand: List<Tile>): Boolean {
        val tileCounts = mutableMapOf<Tile, Int>()
        hand.forEach { tile ->
            tileCounts[tile] = (tileCounts[tile] ?: 0) + 1
        }

        for ((tile, count) in tileCounts) {
            if (count >= 2) {
                val remaining = tileCounts.toMutableMap()
                remaining[tile] = (remaining[tile] ?: 0) - 2
                if (remaining[tile] == 0) remaining.remove(tile)
                if (canFormMelds(remaining)) return true
            }
        }
        return false
    }

    private fun canFormMelds(tileCounts: Map<Tile, Int>): Boolean {
        val counts = tileCounts.toMutableMap()
        if (counts.values.all { it == 0 }) return true

        val tile = counts.entries.firstOrNull { it.value > 0 }?.key ?: return true

        if (counts[tile]!! >= 3) {
            counts[tile] = counts[tile]!! - 3
            if (counts[tile] == 0) counts.remove(tile)
            if (canFormMelds(counts)) return true
            counts[tile] = (counts[tile] ?: 0) + 3
        }

        if (tile.isNumberTile) {
            val next1 = tile.nextInSuit()
            val next2 = next1?.nextInSuit()
            if (next1 != null && next2 != null &&
                (counts[next1] ?: 0) > 0 && (counts[next2] ?: 0) > 0
            ) {
                counts[tile] = (counts[tile] ?: 0) - 1
                counts[next1] = (counts[next1] ?: 0) - 1
                counts[next2] = (counts[next2] ?: 0) - 1
                if (counts[tile] == 0) counts.remove(tile)
                if (counts[next1] == 0) counts.remove(next1)
                if (counts[next2] == 0) counts.remove(next2)
                if (canFormMelds(counts)) return true
                counts[tile] = (counts[tile] ?: 0) + 1
                counts[next1] = (counts[next1] ?: 0) + 1
                counts[next2] = (counts[next2] ?: 0) + 1
            }
        }

        return false
    }

    private fun checkSevenPairs(hand: List<Tile>): Boolean {
        if (hand.size != 14) return false
        val counts = hand.groupingBy { it }.eachCount()
        return counts.size == 7 && counts.values.all { it == 2 }
    }

    override fun calculateScore(
        hand: List<Tile>,
        melds: Int,
        isDealer: Boolean,
        isSelfDraw: Boolean,
        roundWind: Wind,
        seatWind: Wind
    ): Int {
        var fan = 1

        if (checkSevenPairs(hand)) fan += 2

        val categories = hand.map { it.category }.filter {
            it == com.majiang.model.TileCategory.WAN ||
            it == com.majiang.model.TileCategory.TIAO ||
            it == com.majiang.model.TileCategory.TONG
        }.toSet()

        if (categories.size == 1) fan += 4

        if (isSelfDraw) fan += 1
        if (isDealer) fan += 1

        return fan * 100
    }

    override fun canPong(hand: List<Tile>, discardedTile: Tile): Boolean {
        return hand.count { it == discardedTile } >= 2
    }

    override fun canKong(hand: List<Tile>, discardedTile: Tile): Boolean {
        return hand.count { it == discardedTile } >= 3
    }

    override fun canChi(hand: List<Tile>, discardedTile: Tile): Boolean {
        return false
    }

    override fun getValidChiCombinations(hand: List<Tile>, discardedTile: Tile): List<List<Tile>> {
        return emptyList()
    }

    override fun isAllowedDiscard(tile: Tile, hand: List<Tile>): Boolean {
        return hand.contains(tile)
    }
}
