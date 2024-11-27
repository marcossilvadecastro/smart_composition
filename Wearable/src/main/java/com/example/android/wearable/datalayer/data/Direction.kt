package com.example.android.wearable.datalayer.data

enum class Direction(index: Int) {
    NONE(-1),
    NORTHWEST(0),
    NORTHEAST(1),
    SOUTHWEST(2),
    SOUTHEAST(3)
}

fun Int.toDirection(): Direction {
    return if (this < 0 || this > Direction.entries.size) {
        Direction.NONE
    } else {
        Direction.entries[this]
    }
}
