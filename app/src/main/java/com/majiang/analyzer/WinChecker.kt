package com.majiang.analyzer

import com.majiang.model.Tile
import com.majiang.model.TileCategory
import com.majiang.model.rule.MahjongRule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WinChecker @Inject constructor() {

    fun checkWin(hand: List<Tile>, meldCount: Int, rule: MahjongRule): WinResult {
        val isStandardWin = checkStandardWin(hand)
        val isSevenPairs = checkSevenPairs(hand)
        val isThirteenOrphans = checkThirteenOrphans(hand)

        val isWin = isStandardWin || isSevenPairs || isThirteenOrphans

        return WinResult(
            isWin = isWin,
            isStandardWin = isStandardWin,
            isSevenPairs = isSevenPairs,
            isThirteenOrphans = isThirteenOrphans
        )
    }

    fun isWinningHand(hand: List<Tile>): Boolean {
        if (hand.size % 3 != 2) return false
        return checkStandardWin(hand) || checkSevenPairs(hand) || checkThirteenOrphans(hand)
    }

    fun isWinningTile(hand: List<Tile>, tile: Tile): Boolean {
        val testHand = hand + tile
        return isWinningHand(testHand)
    }

    private fun checkStandardWin(hand: List<Tile>): Boolean {
        if (hand.size % 3 != 2) return false

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
        if (counts.isEmpty() || counts.values.all { it == 0 }) return true

        val tile = counts.entries.firstOrNull { it.value > 0 }?.key ?: return true

        if ((counts[tile] ?: 0) >= 3) {
            counts[tile] = (counts[tile] ?: 0) - 3
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
}

data class WinResult(
    val isWin: Boolean,
    val isStandardWin: Boolean,
    val isSevenPairs: Boolean,
    val isThirteenOrphans: Boolean
)
