package com.majiang.analyzer

import com.majiang.model.Tile
import javax.inject.Inject
import javax.inject.Singleton

data class TileRecommendation(
    val tile: Tile,
    val priority: RecommendationPriority,
    val shantenAfterDiscard: Int,
    val waitingTilesAfterDiscard: Int,
    val reason: String
)

enum class RecommendationPriority {
    BEST,
    GOOD,
    NEUTRAL,
    AVOID,
    DANGEROUS
}

@Singleton
class RecommendationEngine @Inject constructor(
    private val winChecker: WinChecker,
    private val readyAnalyzer: ReadyAnalyzer,
    private val dangerAnalyzer: DangerAnalyzer
) {

    fun getRecommendations(
        hand: List<Tile>,
        visibleTiles: List<Tile>,
        playerDiscards: Map<Int, List<Tile>>,
        currentPlayerIndex: Int
    ): List<TileRecommendation> {
        if (hand.size % 3 != 2) return emptyList()

        val currentShanten = readyAnalyzer.calculateShanten(hand)

        if (currentShanten == -1) {
            return hand.distinctBy { it }.map { tile ->
                TileRecommendation(
                    tile = tile,
                    priority = RecommendationPriority.BEST,
                    shantenAfterDiscard = -1,
                    waitingTilesAfterDiscard = 0,
                    reason = "已胡牌"
                )
            }
        }

        val recommendations = hand.distinctBy { it }.map { tile ->
            val handAfterDiscard = hand - tile
            val shantenAfter = readyAnalyzer.calculateShanten(handAfterDiscard)
            val waitingResult = readyAnalyzer.findWaitingTiles(handAfterDiscard, visibleTiles)
            val dangerLevel = dangerAnalyzer.assessDanger(tile, playerDiscards, currentPlayerIndex)

            val priority = determinePriority(
                currentShanten = currentShanten,
                shantenAfter = shantenAfter,
                waitingCount = waitingResult.size,
                dangerLevel = dangerLevel
            )

            val reason = buildReason(shantenAfter, waitingResult.size, dangerLevel)

            TileRecommendation(
                tile = tile,
                priority = priority,
                shantenAfterDiscard = shantenAfter,
                waitingTilesAfterDiscard = waitingResult.size,
                reason = reason
            )
        }

        return recommendations.sortedWith(compareBy { it.priority.ordinal })
    }

    private fun determinePriority(
        currentShanten: Int,
        shantenAfter: Int,
        waitingCount: Int,
        dangerLevel: DangerLevel
    ): RecommendationPriority {
        if (shantenAfter < currentShanten) {
            return RecommendationPriority.BEST
        }

        if (shantenAfter == currentShanten) {
            return when {
                waitingCount >= 8 -> RecommendationPriority.BEST
                waitingCount >= 5 -> RecommendationPriority.GOOD
                dangerLevel == DangerLevel.SAFE -> RecommendationPriority.GOOD
                dangerLevel == DangerLevel.DANGEROUS -> RecommendationPriority.AVOID
                else -> RecommendationPriority.NEUTRAL
            }
        }

        return when (dangerLevel) {
            DangerLevel.SAFE -> RecommendationPriority.NEUTRAL
            DangerLevel.CAUTION -> RecommendationPriority.AVOID
            DangerLevel.DANGEROUS -> RecommendationPriority.DANGEROUS
        }
    }

    private fun buildReason(shantenAfter: Int, waitingCount: Int, dangerLevel: DangerLevel): String {
        val parts = mutableListOf<String>()
        if (shantenAfter == 0) {
            parts.add("听牌")
        } else if (shantenAfter == 1) {
            parts.add("一向听")
        } else {
            parts.add("${shantenAfter}向听")
        }
        parts.add("${waitingCount}面听")
        when (dangerLevel) {
            DangerLevel.SAFE -> parts.add("安全牌")
            DangerLevel.CAUTION -> parts.add("需注意")
            DangerLevel.DANGEROUS -> parts.add("危险牌")
        }
        return parts.joinToString("，")
    }
}
