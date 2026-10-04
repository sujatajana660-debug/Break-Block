package com.example.model

enum class BoosterType(
    val title: String,
    val description: String
) {
    BOMB("Boom 4x4", "Blasts and clears a 4x4 block area!"),
    LINE("Line Breaker", "Breaks an entire row of blocks!"),
    PLUS("+ Cross Breaker", "Breaks blocks in all 4 directions (+ shaped)!"),
    UNDO("Undo Move", "Reverts your last placed block!")
}
