package com.majiang.analyzer

import com.majiang.model.Tile
import com.majiang.model.TileCategory
import javax.inject.Inject
import javax.inject.Singleton

enum class DangerLevel {
    SAFE,
    CAUTION,
    DANGEROUS
}

@Singleton
class DangerAnalyzer @Inject constructor() {

    fun assessDanger(
        tile: Tile,
        playerDiscards: Map<Int, List<Tile>>,
        currentPlayerIndex: Int
    ): DangerLevel {
        val otherPlayersDiscards = playerDiscards.filterKeys { it != currentPlayerIndex }
        val allOtherDiscards = otherPlayersDiscards.values.flatten()

        if (isSafeDiscard(tile, allOtherDiscards)) return DangerLevel.SAFE
        if (isCautionDiscard(tile, allOtherDiscards)) return DangerLevel.CAUTION
        return DangerLevel.DANGEROUS
    }

    fun assessDangerDetailed(
        tile: Tile,
        playerDiscards: Map<Int, List<Tile>>,
        currentPlayerIndex: Int
    ): DangerAssessment {
        val otherPlayersDiscards = playerDiscards.filterKeys { it != currentPlayerIndex }
        val assessments = mutableMapOf<Int, DangerLevel>()

        for ((playerIndex, discards) in otherPlayersDiscards) {
            assessments[playerIndex] = assessDangerForPlayer(tile, discards)
        }

        val overallLevel = assessments.values.maxByOrNull { it.ordinal } ?: DangerLevel.SAFE

        return DangerAssessment(
            tile = tile,
            overallDanger = overallLevel,
            perPlayerDanger = assessments
        )
    }

    private fun isSafeDiscard(tile: Tile, otherDiscards: List<Tile>): Boolean {
        if (otherDiscards.contains(tile)) return true

        if (tile.isNumberTile) {
            val sameCategoryDiscards = otherDiscards.filter { it.category == tile.category }

            if (sameCategoryDiscards.count { it.number == tile.number - 1 } >= 2 &&
                sameCategoryDiscards.count { it.number == tile.number + 1 } >= 2
            ) return true

            val adjacentDiscards = sameCategoryDiscards.filter {
                kotlin.math.abs(it.number - tile.number) <= 2
            }
            if (adjacentDiscards.size >= 4) return true
        }

        if (tile.isHonorTile) {
            val sameTileDiscards = otherDiscards.filter { it == tile }
            if (sameTileDiscards.size >= 2) return true
        }

        return false
    }

    private fun isCautionDiscard(tile: Tile, otherDiscards: List<Tile>): Boolean {
        if (tile.isNumberTile) {
            val sameCategoryDiscards = otherDiscards.filter { it.category == tile.category }
            val adjacentDiscards = sameCategoryDiscards.filter {
                kotlin.math.abs(it.number - tile.number) <= 2
            }
            if (adjacentDiscards.isNotEmpty()) return true
        }

        if (tile.isHonorTile) {
            val sameTileDiscards = otherDiscards.filter { it == tile }
            if (sameTileDiscards.size >= 1) return true
        }

        return false
    }

    private fun assessDangerForPlayer(tile: Tile, playerDiscards: List<Tile>): DangerLevel {
        if (playerDiscards.contains(tile)) return DangerLevel.SAFE

        if (tile.isNumberTile) {
            val sameCategoryDiscards = playerDiscards.filter { it.category == tile.category }
            if (sameCategoryDiscards.isEmpty()) return DangerLevel.DANGEROUS

            val nearDiscards = sameCategoryDiscards.filter {
                kotlin.math.abs(it.number - tile.number) <= 1
            }
            if (nearDiscards.isEmpty()) return DangerLevel.DANGEROUS
            if (nearDiscards.size >= 2) return DangerLevel.CAUTION
        }

        if (tile.isHonorTile) {
            return if (playerDiscards.contains(tile)) DangerLevel.SAFE else DangerLevel.DANGEROUS
        }

        return DangerLevel.CAUTION
    }
}

data class DangerAssessment(
    val tile: Tile,
    val overallDanger: DangerLevel,
    val perPlayerDanger: Map<Int, DangerLevel>
)
