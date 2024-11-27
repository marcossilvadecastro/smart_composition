package com.example.android.wearable.datalayer.data

enum class ZLevel(val index: Int) {
    NONE(-1),
    LEVELED(0),
    UP(1),
    DOWN(2)
}


fun Int.toZLevel(): ZLevel {
    return if (this < 0 || this > ZLevel.entries.size) {
        ZLevel.NONE
    } else {
        ZLevel.entries[this]
    }
}
