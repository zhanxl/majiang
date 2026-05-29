package com.majiang.analyzer

import com.majiang.model.Tile
import com.majiang.model.TileSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonteCarloSimulator @Inject constructor(
    private val winChecker: WinChecker
) {

    data class SimulationResult(
        val winProbability: Double,
        val simulationCount: Int,
        val winCount: Int
    )

    fun simulateWinProbability(
        hand: List<Tile>,
        visibleTiles: List<Tile>,
        simulationCount: Int = DEFAULT_SIMULATION_COUNT
    ): SimulationResult {
        var winCount = 0

        val remainingTiles = buildRemainingTiles(hand, visibleTiles)

        repeat(simulationCount) {
            val shuffled = remainingTiles.shuffled()
            val neededTiles = 14 - hand.size
            if (shuffled.size < neededTiles) return@repeat

            val drawnTiles = shuffled.take(neededTiles)
            val fullHand = hand + drawnTiles

            if (winChecker.isWinningHand(fullHand)) {
                winCount++
            }
        }

        val probability = if (simulationCount > 0) {
            winCount.toDouble() / simulationCount
        } else 0.0

        return SimulationResult(
            winProbability = probability,
            simulationCount = simulationCount,
            winCount = winCount
        )
    }

    fun simulateBestDiscard(
        hand: List<Tile>,
        visibleTiles: List<Tile>,
        simulationCount: Int = DEFAULT_SIMULATION_COUNT
    ): Map<Tile, Double> {
        val results = mutableMapOf<Tile, Double>()

        for (tile in hand.distinctBy { it }) {
            val handAfterDiscard = hand - tile
            val result = simulateWinProbability(handAfterDiscard, visibleTiles, simulationCount)
            results[tile] = result.winProbability
        }

        return results
    }

    private fun buildRemainingTiles(hand: List<Tile>, visibleTiles: List<Tile>): List<Tile> {
        val allVisible = hand + visibleTiles
        val counts = allVisible.groupingBy { it }.eachCount()

        val remaining = mutableListOf<Tile>()
        for (tile in Tile.entries) {
            val usedCount = counts[tile] ?: 0
            val remainingCount = 4 - usedCount
            repeat(remainingCount) { remaining.add(tile) }
        }

        return remaining
    }

    companion object {
        const val DEFAULT_SIMULATION_COUNT = 1000
        const val FAST_SIMULATION_COUNT = 100
        const val ACCURATE_SIMULATION_COUNT = 5000
    }
}
