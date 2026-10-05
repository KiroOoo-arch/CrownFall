package com.crownfall.realm.domain.ai

/**
 * Fixed-size, allocation-free transposition table keyed by Zobrist hash.
 * Entry replacement is "always replace" which is good enough for a mobile
 * search and keeps the memory footprint predictable.
 */
class TranspositionTable(sizePower: Int = 16) {

    private val mask: Int = (1 shl sizePower) - 1

    private val keys = LongArray(1 shl sizePower)
    private val moves = IntArray(1 shl sizePower)
    private val scores = IntArray(1 shl sizePower)
    private val depths = IntArray(1 shl sizePower)
    private val flags = IntArray(1 shl sizePower)

    var hits: Long = 0
        private set
    var probes: Long = 0
        private set

    fun clear() {
        keys.fill(0L)
        moves.fill(0)
        scores.fill(0)
        depths.fill(0)
        flags.fill(0)
        hits = 0
        probes = 0
    }

    fun store(key: Long, move: Int, score: Int, depth: Int, flag: Int) {
        val index = (key.toInt() xor (key ushr 32).toInt()) and mask
        keys[index] = key
        moves[index] = move
        scores[index] = score
        depths[index] = depth
        flags[index] = flag
    }

    fun probe(key: Long, depth: Int, alpha: Int, beta: Int): Entry? {
        probes++
        val index = (key.toInt() xor (key ushr 32).toInt()) and mask
        if (keys[index] != key) return null
        hits++
        val storedScore = scores[index]
        if (depths[index] < depth) return Entry(moves[index], storedScore, depths[index], FLAG_NONE)
        val flag = flags[index]
        val usable = when (flag) {
            FLAG_EXACT -> true
            FLAG_LOWER -> storedScore >= beta
            FLAG_UPPER -> storedScore <= alpha
            else -> false
        }
        return Entry(moves[index], storedScore, depths[index], flag)
    }

    /** Best move discovered for this position, even from a shallow entry. */
    fun bestMove(key: Long): Int {
        val index = (key.toInt() xor (key ushr 32).toInt()) and mask
        return if (keys[index] == key) moves[index] else 0
    }

    class Entry(val move: Int, val score: Int, val depth: Int, val flag: Int) {
        val isUsable: Boolean
            get() = flag == FLAG_EXACT || flag == FLAG_LOWER || flag == FLAG_UPPER
    }

    companion object {
        const val FLAG_NONE = 0
        const val FLAG_EXACT = 1
        const val FLAG_LOWER = 2
        const val FLAG_UPPER = 3
    }
}
