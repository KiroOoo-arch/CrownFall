package com.crownfall.realm.domain.chess

/**
 * An O(1)-append immutable log of the moves played in a game.
 * Keeps game state cheap to copy during AI search while still supporting undo.
 */
class MoveLog private constructor(val last: Move?, val previous: MoveLog?) {

    val size: Int

    init {
        var count = 0
        var node: MoveLog? = this
        while (node?.last != null) {
            count++
            node = node.previous
        }
        size = count
    }

    fun append(move: Move): MoveLog = MoveLog(move, this)

    fun dropLast(): MoveLog = previous ?: empty

    fun toList(): List<Move> {
        val out = ArrayList<Move>(size)
        var node: MoveLog? = this
        while (node?.last != null) {
            out.add(node.last!!)
            node = node.previous
        }
        out.reverse()
        return out
    }

    fun lastMove(): Move? = last

    companion object {
        val empty: MoveLog = MoveLog(null, null)
    }
}
