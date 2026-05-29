package com.majiang.analyzer

import com.majiang.model.Tile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadyAnalyzer @Inject constructor(
    private val winChecker: WinChecker
) {

    data class ReadyResult(
        val isReady: Boolean,
        val waitingTiles: List<WaitingTile>,
        val shantenCount: Int
    )

    data class WaitingTile(
        val tile: Tile,
        val remainingCount: Int,
        val isWinningTile: Boolean
    )

    fun analyzeReady(
        hand: List<Tile>,
        visibleTiles: List<Tile>,
        totalTiles: Int = 136
    ): ReadyResult {
        val waitingTiles = findWaitingTiles(hand, visibleTiles, totalTiles)
        val shantenCount = calculateShanten(hand)
        val isReady = shantenCount == 0 && waitingTiles.isNotEmpty()

        return ReadyResult(
            isReady = isReady,
            waitingTiles = waitingTiles,
            shantenCount = shantenCount
        )
    }

    fun analyzeReadyWithWildcard(
        hand: List<Tile>,
        visibleTiles: List<Tile>,
        wildcardTile: Tile = Tile.JIAN_ZHONG,
        totalTiles: Int = 112
    ): ReadyResult {
        val waitingTiles = findWaitingTilesWithWildcard(hand, visibleTiles, wildcardTile, totalTiles)
        val shantenCount = calculateShantenWithWildcard(hand, wildcardTile)
        val isReady = shantenCount == 0 && waitingTiles.isNotEmpty()

        return ReadyResult(
            isReady = isReady,
            waitingTiles = waitingTiles,
            shantenCount = shantenCount
        )
    }

    fun findWaitingTiles(
        hand: List<Tile>,
        visibleTiles: List<Tile>,
        totalTiles: Int = 136
    ): List<WaitingTile> {
        if (hand.size % 3 != 1) return emptyList()

        val visibleCounts = visibleTiles.groupingBy { it }.eachCount()
        val waitingTiles = mutableListOf<WaitingTile>()

        for (tile in Tile.entries) {
            val testHand = hand + tile
            if (winChecker.isWinningHand(testHand)) {
                val usedCount = visibleCounts[tile] ?: 0
                val remainingCount = 4 - usedCount
                if (remainingCount > 0) {
                    waitingTiles.add(
                        WaitingTile(
                            tile = tile,
                            remainingCount = remainingCount,
                            isWinningTile = true
                        )
                    )
                }
            }
        }

        return waitingTiles
    }

    fun findWaitingTilesWithWildcard(
        hand: List<Tile>,
        visibleTiles: List<Tile>,
        wildcardTile: Tile = Tile.JIAN_ZHONG,
        totalTiles: Int = 112
    ): List<WaitingTile> {
        if (hand.size % 3 != 1) return emptyList()

        val validTiles = Tile.hongzhongSet()
        val visibleCounts = visibleTiles.groupingBy { it }.eachCount()
        val waitingTiles = mutableListOf<WaitingTile>()

        for (tile in validTiles) {
            val testHand = hand + tile
            if (winChecker.isWinningHandWithWildcard(testHand, wildcardTile)) {
                val usedCount = visibleCounts[tile] ?: 0
                val remainingCount = 4 - usedCount
                if (remainingCount > 0) {
                    waitingTiles.add(
                        WaitingTile(
                            tile = tile,
                            remainingCount = remainingCount,
                            isWinningTile = true
                        )
                    )
                }
            }
        }

        return waitingTiles
    }

    fun calculateShanten(hand: List<Tile>): Int {
        if (winChecker.isWinningHand(hand)) return -1

        val meldsNeeded = (hand.size - 2) / 3
        val tileCounts = mutableMapOf<Tile, Int>()
        hand.forEach { tile ->
            tileCounts[tile] = (tileCounts[tile] ?: 0) + 1
        }

        var minShanten = 8

        for ((tile, count) in tileCounts) {
            if (count >= 2) {
                val remaining = tileCounts.toMutableMap()
                remaining[tile] = (remaining[tile] ?: 0) - 2
                if (remaining[tile] == 0) remaining.remove(tile)
                val shanten = calculateShantenForMelds(remaining, meldsNeeded)
                minShanten = minOf(minShanten, shanten)
            }
        }

        val shantenWithoutPair = calculateShantenForMelds(tileCounts, meldsNeeded) + 1
        minShanten = minOf(minShanten, shantenWithoutPair)

        return minShanten
    }

    fun calculateShantenWithWildcard(
        hand: List<Tile>,
        wildcardTile: Tile = Tile.JIAN_ZHONG
    ): Int {
        if (winChecker.isWinningHandWithWildcard(hand, wildcardTile)) return -1

        val wildcardCount = hand.count { it == wildcardTile }
        val normalTiles = hand.filter { it != wildcardTile }
        val meldsNeeded = (hand.size - 2) / 3

        val tileCounts = mutableMapOf<Tile, Int>()
        normalTiles.forEach { tile ->
            tileCounts[tile] = (tileCounts[tile] ?: 0) + 1
        }

        var minShanten = 8

        for ((tile, count) in tileCounts) {
            if (count >= 2) {
                val remaining = tileCounts.toMutableMap()
                remaining[tile] = (remaining[tile] ?: 0) - 2
                if (remaining[tile] == 0) remaining.remove(tile)
                val shanten = calculateShantenForMeldsWithWildcard(remaining, meldsNeeded, wildcardCount)
                minShanten = minOf(minShanten, shanten)
            }
        }

        if (wildcardCount >= 2) {
            val shanten = calculateShantenForMeldsWithWildcard(tileCounts, meldsNeeded, wildcardCount - 2)
            minShanten = minOf(minShanten, shanten)
        }

        if (wildcardCount >= 1) {
            for (tile in Tile.entries) {
                if (tile == wildcardTile) continue
                val remaining = tileCounts.toMutableMap()
                remaining[tile] = (remaining[tile] ?: 0) + 1
                if ((remaining[tile] ?: 0) >= 2) {
                    remaining[tile] = (remaining[tile] ?: 0) - 2
                    if (remaining[tile] == 0) remaining.remove(tile)
                    val shanten = calculateShantenForMeldsWithWildcard(remaining, meldsNeeded, wildcardCount - 1)
                    minShanten = minOf(minShanten, shanten)
                }
            }
        }

        val shantenWithoutPair = calculateShantenForMeldsWithWildcard(tileCounts, meldsNeeded, wildcardCount) + 1
        minShanten = minOf(minShanten, shantenWithoutPair)

        return minShanten
    }

    private fun calculateShantenForMelds(tileCounts: Map<Tile, Int>, meldsNeeded: Int): Int {
        var completeMelds = 0
        var partialMelds = 0
        val counts = tileCounts.toMutableMap()

        for ((tile, count) in counts.toList()) {
            if (count >= 3) {
                completeMelds++
                counts[tile] = (counts[tile] ?: 0) - 3
            }
        }

        for (tile in Tile.entries) {
            if ((counts[tile] ?: 0) > 0 && tile.isNumberTile) {
                val next1 = tile.nextInSuit()
                val next2 = next1?.nextInSuit()
                if (next1 != null && (counts[next1] ?: 0) > 0) {
                    partialMelds++
                    counts[tile] = (counts[tile] ?: 0) - 1
                    counts[next1] = (counts[next1] ?: 0) - 1
                } else if (next2 != null && (counts[next2] ?: 0) > 0) {
                    partialMelds++
                    counts[tile] = (counts[tile] ?: 0) - 1
                    counts[next2] = (counts[next2] ?: 0) - 1
                }
            }
        }

        val pairs = counts.values.count { it >= 2 }
        partialMelds += pairs

        val totalUseful = completeMelds + partialMelds
        return meldsNeeded * 2 - totalUseful
    }

    private fun calculateShantenForMeldsWithWildcard(
        tileCounts: Map<Tile, Int>,
        meldsNeeded: Int,
        wildcardsRemaining: Int
    ): Int {
        var completeMelds = 0
        var partialMelds = 0
        val counts = tileCounts.toMutableMap()

        for ((tile, count) in counts.toList()) {
            if (count >= 3) {
                completeMelds++
                counts[tile] = (counts[tile] ?: 0) - 3
            }
        }

        for (tile in Tile.entries) {
            if ((counts[tile] ?: 0) > 0 && tile.isNumberTile) {
                val next1 = tile.nextInSuit()
                val next2 = next1?.nextInSuit()
                if (next1 != null && (counts[next1] ?: 0) > 0) {
                    partialMelds++
                    counts[tile] = (counts[tile] ?: 0) - 1
                    counts[next1] = (counts[next1] ?: 0) - 1
                } else if (next2 != null && (counts[next2] ?: 0) > 0) {
                    partialMelds++
                    counts[tile] = (counts[tile] ?: 0) - 1
                    counts[next2] = (counts[next2] ?: 0) - 1
                }
            }
        }

        val pairs = counts.values.count { it >= 2 }
        partialMelds += pairs

        partialMelds += wildcardsRemaining

        val totalUseful = completeMelds + partialMelds
        return maxOf(0, meldsNeeded * 2 - totalUseful)
    }
}
