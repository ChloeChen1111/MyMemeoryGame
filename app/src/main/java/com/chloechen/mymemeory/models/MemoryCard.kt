package com.chloechen.mymemeory.models

data class MemoryCard(
    val identifer: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false

)