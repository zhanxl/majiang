package com.majiang.vision.strategy

import com.majiang.model.Tile
import com.majiang.vision.RecognitionResult

class ManualRecognitionStrategy : RecognitionStrategy {

    override val name: String = "手动输入"
    override val isAvailable: Boolean = true
    override val requiresNetwork: Boolean = false

    private val _selectedTiles = mutableListOf<Tile>()
    val selectedTiles: List<Tile> get() = _selectedTiles.toList()

    fun addTile(tile: Tile) {
        _selectedTiles.add(tile)
    }

    fun removeTile(tile: Tile): Boolean {
        return _selectedTiles.remove(tile)
    }

    fun removeLastTile(): Tile? {
        if (_selectedTiles.isEmpty()) return null
        return _selectedTiles.removeAt(_selectedTiles.size - 1)
    }

    fun clearTiles() {
        _selectedTiles.clear()
    }

    fun setTiles(tiles: List<Tile>) {
        _selectedTiles.clear()
        _selectedTiles.addAll(tiles)
    }

    override suspend fun recognize(bitmap: android.graphics.Bitmap): List<RecognitionResult> {
        return _selectedTiles.map { tile ->
            RecognitionResult(
                tile = tile,
                confidence = 1.0f,
                boundingBox = null,
                label = tile.displayName
            )
        }
    }
}
