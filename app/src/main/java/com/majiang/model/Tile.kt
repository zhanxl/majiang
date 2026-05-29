package com.majiang.model

enum class TileCategory {
    WAN,
    TIAO,
    TONG,
    FENG,
    JIAN
}

enum class Tile(
    val category: TileCategory,
    val number: Int,
    val displayName: String
) {
    WAN_1(TileCategory.WAN, 1, "一万"),
    WAN_2(TileCategory.WAN, 2, "二万"),
    WAN_3(TileCategory.WAN, 3, "三万"),
    WAN_4(TileCategory.WAN, 4, "四万"),
    WAN_5(TileCategory.WAN, 5, "五万"),
    WAN_6(TileCategory.WAN, 6, "六万"),
    WAN_7(TileCategory.WAN, 7, "七万"),
    WAN_8(TileCategory.WAN, 8, "八万"),
    WAN_9(TileCategory.WAN, 9, "九万"),

    TIAO_1(TileCategory.TIAO, 1, "一条"),
    TIAO_2(TileCategory.TIAO, 2, "二条"),
    TIAO_3(TileCategory.TIAO, 3, "三条"),
    TIAO_4(TileCategory.TIAO, 4, "四条"),
    TIAO_5(TileCategory.TIAO, 5, "五条"),
    TIAO_6(TileCategory.TIAO, 6, "六条"),
    TIAO_7(TileCategory.TIAO, 7, "七条"),
    TIAO_8(TileCategory.TIAO, 8, "八条"),
    TIAO_9(TileCategory.TIAO, 9, "九条"),

    TONG_1(TileCategory.TONG, 1, "一筒"),
    TONG_2(TileCategory.TONG, 2, "二筒"),
    TONG_3(TileCategory.TONG, 3, "三筒"),
    TONG_4(TileCategory.TONG, 4, "四筒"),
    TONG_5(TileCategory.TONG, 5, "五筒"),
    TONG_6(TileCategory.TONG, 6, "六筒"),
    TONG_7(TileCategory.TONG, 7, "七筒"),
    TONG_8(TileCategory.TONG, 8, "八筒"),
    TONG_9(TileCategory.TONG, 9, "九筒"),

    FENG_DONG(TileCategory.FENG, 1, "东"),
    FENG_NAN(TileCategory.FENG, 2, "南"),
    FENG_XI(TileCategory.FENG, 3, "西"),
    FENG_BEI(TileCategory.FENG, 4, "北"),

    JIAN_ZHONG(TileCategory.JIAN, 1, "中"),
    JIAN_FA(TileCategory.JIAN, 2, "发"),
    JIAN_BAI(TileCategory.JIAN, 3, "白");

    val isNumberTile: Boolean
        get() = category == TileCategory.WAN || category == TileCategory.TIAO || category == TileCategory.TONG

    val isHonorTile: Boolean
        get() = category == TileCategory.FENG || category == TileCategory.JIAN

    val isTerminal: Boolean
        get() = isNumberTile && (number == 1 || number == 9)

    val isTerminalOrHonor: Boolean
        get() = isTerminal || isHonorTile

    fun nextInSuit(): Tile? {
        if (!isNumberTile || number >= 9) return null
        return entries.firstOrNull {
            it.category == category && it.number == number + 1
        }
    }

    fun prevInSuit(): Tile? {
        if (!isNumberTile || number <= 1) return null
        return entries.firstOrNull {
            it.category == category && it.number == number - 1
        }
    }

    companion object {
        fun wanTiles(): List<Tile> = entries.filter { it.category == TileCategory.WAN }
        fun tiaoTiles(): List<Tile> = entries.filter { it.category == TileCategory.TIAO }
        fun tongTiles(): List<Tile> = entries.filter { it.category == TileCategory.TONG }
        fun fengTiles(): List<Tile> = entries.filter { it.category == TileCategory.FENG }
        fun jianTiles(): List<Tile> = entries.filter { it.category == TileCategory.JIAN }
        fun numberTiles(): List<Tile> = entries.filter { it.isNumberTile }
        fun honorTiles(): List<Tile> = entries.filter { it.isHonorTile }
    }
}
