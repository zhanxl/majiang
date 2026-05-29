package com.majiang.model.rule

import com.majiang.model.Tile
import com.majiang.model.TileCategory
import com.majiang.model.Wind

class GuangdongRule : MahjongRule {

    override val name: String = "广东麻将"
    override val description: String = "广东麻将规则，鸡胡起番，支持爆胡"

    override fun canWin(hand: List<Tile>, melds: Int): Boolean {
        if (hand.size % 3 != 2) return false
        return checkStandardWin(hand) || checkSevenPairs(hand) || checkThirteenOrphans(hand)
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

    private fun checkThirteenOrphans(hand: List<Tile>): Boolean {
        if (hand.size != 14) return false
        val terminalOrHonor = Tile.entries.filter { it.isTerminalOrHonor }
        val handSet = hand.toSet()
        if (handSet.size != 13) return false
        return terminalOrHonor.all { it in handSet }
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

        if (checkThirteenOrphans(hand)) fan += 13
        if (checkSevenPairs(hand)) fan += 2

        val allSameSuit = hand.map { it.category }.distinct().let { categories ->
            categories.size == 1 && categories.first() != TileCategory.FENG && categories.first() != TileCategory.JIAN
        }
        if (allSameSuit) fan += 3

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
        return getValidChiCombinations(hand, discardedTile).isNotEmpty()
    }

    override fun getValidChiCombinations(hand: List<Tile>, discardedTile: Tile): List<List<Tile>> {
        if (!discardedTile.isNumberTile) return emptyList()

        val combinations = mutableListOf<List<Tile>>()
        val num = discardedTile.number

        for (start in maxOf(1, num - 2)..minOf(7, num)) {
            val t1 = Tile.entries.firstOrNull {
                it.category == discardedTile.category && it.number == start
            }
            val t2 = Tile.entries.firstOrNull {
                it.category == discardedTile.category && it.number == start + 1
            }
            val t3 = Tile.entries.firstOrNull {
                it.category == discardedTile.category && it.number == start + 2
            }

            if (t1 != null && t2 != null && t3 != null) {
                val neededTiles = listOf(t1, t2, t3).filter { it != discardedTile }
                val hasAll = neededTiles.all { needed ->
                    hand.count { it == needed } >= 1
                }
                if (hasAll) {
                    combinations.add(listOf(t1, t2, t3))
                }
            }
        }

        return combinations
    }

    override fun isAllowedDiscard(tile: Tile, hand: List<Tile>): Boolean {
        return hand.contains(tile)
    }
}
