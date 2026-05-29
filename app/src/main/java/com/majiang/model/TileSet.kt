package com.majiang.model

data class TileSet(
    val tiles: List<Tile> = buildFullSet()
) {
    companion object {
        fun buildFullSet(): List<Tile> {
            val fullSet = mutableListOf<Tile>()
            Tile.entries.forEach { tile ->
                repeat(4) { fullSet.add(tile) }
            }
            return fullSet
        }

        fun shuffled(): TileSet = TileSet(buildFullSet().shuffled())
    }

    val size: Int get() = tiles.size

    fun shuffled(): TileSet = TileSet(tiles.shuffled())

    fun deal(count: Int): Pair<List<Tile>, TileSet> {
        val dealt = tiles.take(count)
        val remaining = tiles.drop(count)
        return dealt to TileSet(remaining)
    }

    fun removeTiles(vararg tilesToRemove: Tile): TileSet {
        val mutableTiles = tiles.toMutableList()
        tilesToRemove.forEach { tile ->
            mutableTiles.remove(tile)
        }
        return TileSet(mutableTiles.toList())
    }

    fun countOf(tile: Tile): Int = tiles.count { it == tile }

    fun remainingCountOf(tile: Tile, visibleTiles: List<Tile>): Int {
        return countOf(tile) - visibleTiles.count { it == tile }
    }
}
