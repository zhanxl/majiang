package com.majiang.model.rule

import com.majiang.model.Tile
import com.majiang.model.TileCategory
import com.majiang.model.Wind

class ChangshaHongzhongRule : MahjongRule {

    override val name: String = "长沙红中麻将"
    override val description: String = "长沙红中麻将，112张牌，红中为万能牌，不能吃牌，扎鸟杀鬼"

    val hongzhongTile: Tile = Tile.JIAN_ZHONG

    fun getTileSet(): List<Tile> {
        val tiles = mutableListOf<Tile>()
        Tile.entries.filter {
            it.category == TileCategory.WAN ||
            it.category == TileCategory.TIAO ||
            it.category == TileCategory.TONG ||
            it == Tile.JIAN_ZHONG
        }.forEach { tile ->
            repeat(4) { tiles.add(tile) }
        }
        return tiles
    }

    fun isHongzhong(tile: Tile): Boolean = tile == Tile.JIAN_ZHONG

    fun countHongzhong(hand: List<Tile>): Int = hand.count { isHongzhong(it) }

    fun isShaGui(hand: List<Tile>): Boolean = !hand.contains(Tile.JIAN_ZHONG)

    override fun canWin(hand: List<Tile>, melds: Int): Boolean {
        if (hand.size % 3 != 2) return false

        if (countHongzhong(hand) == 4 && hand.size == 14) return true

        return checkWinWithWildcard(hand)
    }

    private fun checkWinWithWildcard(hand: List<Tile>): Boolean {
        val wildcardCount = countHongzhong(hand)
        val normalTiles = hand.filter { !isHongzhong(it) }

        val tileCounts = mutableMapOf<Tile, Int>()
        normalTiles.forEach { tile ->
            tileCounts[tile] = (tileCounts[tile] ?: 0) + 1
        }

        for ((tile, count) in tileCounts) {
            if (count >= 2) {
                val remaining = tileCounts.toMutableMap()
                remaining[tile] = (remaining[tile] ?: 0) - 2
                if (remaining[tile] == 0) remaining.remove(tile)
                if (canFormMeldsWithWildcard(remaining, wildcardCount)) return true
            }
        }

        if (wildcardCount >= 2) {
            if (canFormMeldsWithWildcard(tileCounts, wildcardCount - 2)) return true
        }

        if (wildcardCount >= 1) {
            for (tile in Tile.entries) {
                if (tile == Tile.JIAN_ZHONG) continue
                val remaining = tileCounts.toMutableMap()
                remaining[tile] = (remaining[tile] ?: 0) + 1
                if ((remaining[tile] ?: 0) >= 2) {
                    remaining[tile] = (remaining[tile] ?: 0) - 2
                    if (remaining[tile] == 0) remaining.remove(tile)
                    if (canFormMeldsWithWildcard(remaining, wildcardCount - 1)) return true
                }
            }
        }

        return false
    }

    private fun canFormMeldsWithWildcard(
        tileCounts: Map<Tile, Int>,
        wildcardsRemaining: Int
    ): Boolean {
        val counts = tileCounts.toMutableMap()
        if (counts.isEmpty() || counts.values.all { it == 0 }) return true

        val tile = counts.entries.firstOrNull { it.value > 0 }?.key ?: return true

        if ((counts[tile] ?: 0) >= 3) {
            counts[tile] = (counts[tile] ?: 0) - 3
            if (counts[tile] == 0) counts.remove(tile)
            if (canFormMeldsWithWildcard(counts, wildcardsRemaining)) return true
            counts[tile] = (counts[tile] ?: 0) + 3
        }

        if (tile.isNumberTile) {
            val next1 = tile.nextInSuit()
            val next2 = next1?.nextInSuit()
            if (next1 != null && next2 != null) {
                if ((counts[next1] ?: 0) > 0 && (counts[next2] ?: 0) > 0) {
                    counts[tile] = (counts[tile] ?: 0) - 1
                    counts[next1] = (counts[next1] ?: 0) - 1
                    counts[next2] = (counts[next2] ?: 0) - 1
                    if (counts[tile] == 0) counts.remove(tile)
                    if (counts[next1] == 0) counts.remove(next1)
                    if (counts[next2] == 0) counts.remove(next2)
                    if (canFormMeldsWithWildcard(counts, wildcardsRemaining)) return true
                    counts[tile] = (counts[tile] ?: 0) + 1
                    counts[next1] = (counts[next1] ?: 0) + 1
                    counts[next2] = (counts[next2] ?: 0) + 1
                }

                if (wildcardsRemaining >= 1 && (counts[next1] ?: 0) > 0) {
                    counts[tile] = (counts[tile] ?: 0) - 1
                    counts[next1] = (counts[next1] ?: 0) - 1
                    if (counts[tile] == 0) counts.remove(tile)
                    if (counts[next1] == 0) counts.remove(next1)
                    if (canFormMeldsWithWildcard(counts, wildcardsRemaining - 1)) return true
                    counts[tile] = (counts[tile] ?: 0) + 1
                    counts[next1] = (counts[next1] ?: 0) + 1
                }

                if (wildcardsRemaining >= 1 && (counts[next2] ?: 0) > 0) {
                    counts[tile] = (counts[tile] ?: 0) - 1
                    counts[next2] = (counts[next2] ?: 0) - 1
                    if (counts[tile] == 0) counts.remove(tile)
                    if (counts[next2] == 0) counts.remove(next2)
                    if (canFormMeldsWithWildcard(counts, wildcardsRemaining - 1)) return true
                    counts[tile] = (counts[tile] ?: 0) + 1
                    counts[next2] = (counts[next2] ?: 0) + 1
                }

                if (wildcardsRemaining >= 2) {
                    counts[tile] = (counts[tile] ?: 0) - 1
                    if (counts[tile] == 0) counts.remove(tile)
                    if (canFormMeldsWithWildcard(counts, wildcardsRemaining - 2)) return true
                    counts[tile] = (counts[tile] ?: 0) + 1
                }
            }
        }

        if (wildcardsRemaining >= 3) {
            counts[tile] = (counts[tile] ?: 0) - 1
            if (counts[tile] == 0) counts.remove(tile)
            if (canFormMeldsWithWildcard(counts, wildcardsRemaining - 3)) return true
            counts[tile] = (counts[tile] ?: 0) + 1
        }

        return false
    }

    override fun calculateScore(
        hand: List<Tile>,
        melds: Int,
        isDealer: Boolean,
        isSelfDraw: Boolean,
        roundWind: Wind,
        seatWind: Wind
    ): Int {
        var fan = 2

        val normalTiles = hand.filter { !isHongzhong(it) }
        val allSameSuit = normalTiles.map { it.category }.distinct().let { categories ->
            categories.size == 1 &&
            categories.first() != TileCategory.FENG &&
            categories.first() != TileCategory.JIAN
        }
        if (allSameSuit) fan *= 4

        val isPengPengHu = checkPengPengHu(hand)
        if (isPengPengHu) fan *= 4

        if (isShaGui(hand)) fan *= 2

        if (isDealer) fan += 1

        return fan * 100
    }

    private fun checkPengPengHu(hand: List<Tile>): Boolean {
        val tileCounts = mutableMapOf<Tile, Int>()
        hand.forEach { tile ->
            tileCounts[tile] = (tileCounts[tile] ?: 0) + 1
        }

        val wildcardCount = countHongzhong(hand)
        var pairs = 0
        var triplets = 0

        for ((_, count) in tileCounts) {
            when {
                count >= 3 -> triplets++
                count == 2 -> pairs++
            }
        }

        return pairs == 1 && triplets >= 4
    }

    fun calculateZhaNiao(birdTile: Tile, playerCount: Int = 4): ZhaNiaoResult {
        val birdPoint = when {
            birdTile == Tile.JIAN_ZHONG -> 10
            birdTile.isNumberTile -> birdTile.number
            else -> 0
        }

        val hitPlayerIndex = if (birdPoint > 0) {
            (birdPoint - 1) % playerCount
        } else -1

        return ZhaNiaoResult(
            birdTile = birdTile,
            birdPoint = birdPoint,
            hitPlayerIndex = hitPlayerIndex,
            isHit = hitPlayerIndex >= 0
        )
    }

    override fun canPong(hand: List<Tile>, discardedTile: Tile): Boolean {
        if (isHongzhong(discardedTile)) return false
        return hand.count { it == discardedTile } >= 2
    }

    override fun canKong(hand: List<Tile>, discardedTile: Tile): Boolean {
        if (isHongzhong(discardedTile)) return false
        return hand.count { it == discardedTile } >= 3
    }

    override fun canChi(hand: List<Tile>, discardedTile: Tile): Boolean = false

    override fun getValidChiCombinations(hand: List<Tile>, discardedTile: Tile): List<List<Tile>> =
        emptyList()

    override fun isAllowedDiscard(tile: Tile, hand: List<Tile>): Boolean {
        return hand.contains(tile)
    }
}

data class ZhaNiaoResult(
    val birdTile: Tile,
    val birdPoint: Int,
    val hitPlayerIndex: Int,
    val isHit: Boolean
)
