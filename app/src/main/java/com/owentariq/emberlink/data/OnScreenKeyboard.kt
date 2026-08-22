package com.owentariq.emberlink.data

/**
 * Text entry over infrared.
 *
 * There is no IR command that carries a string — the protocol only has discrete key
 * presses. What there *is* on a Fire TV is an on-screen keyboard laid out as a fixed
 * grid, so typing is really a pathfinding problem: work out the D-pad route from the
 * currently highlighted cell to the letter you want, walk it, press OK, repeat.
 *
 * The Fire TV search keyboard is alphanumeric in a 6x6 grid — 26 letters plus 10
 * digits is exactly 36 cells — which is the default layout below. Other text fields
 * (Wi-Fi passwords, app logins) use different layouts, so the layout is editable and
 * the engine works with whatever grid you give it.
 */
data class KeyPosition(val row: Int, val col: Int)

enum class Move { UP, DOWN, LEFT, RIGHT, SELECT }

class OnScreenKeyboard(
    val rows: List<String>,
    val wrapEdges: Boolean = false,
) {
    val rowCount: Int get() = rows.size
    val colCount: Int get() = rows.maxOfOrNull { it.length } ?: 0

    private val index: Map<Char, KeyPosition> = buildMap {
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                // First occurrence wins, so a duplicated glyph resolves to the nearer cell.
                putIfAbsent(ch.lowercaseChar(), KeyPosition(r, c))
            }
        }
    }

    fun charAt(row: Int, col: Int): Char? = rows.getOrNull(row)?.getOrNull(col)

    fun positionOf(ch: Char): KeyPosition? = index[ch.lowercaseChar()]

    /** Characters this layout cannot produce, so the UI can warn before a run. */
    fun unsupported(text: String): Set<Char> =
        text.mapNotNull { ch -> ch.takeIf { positionOf(it) == null } }
            .map { it.lowercaseChar() }
            .toSet()

    /**
     * D-pad route from [from] to the cell holding [ch], ending in SELECT.
     *
     * Vertical first, then horizontal — an arbitrary but consistent choice, which
     * matters because a predictable route is one you can follow along with on screen.
     * When [wrapEdges] is on, a move that would cross more than half the grid is
     * flipped to go the short way around instead.
     */
    fun route(from: KeyPosition, ch: Char): Pair<List<Move>, KeyPosition>? {
        val target = positionOf(ch) ?: return null
        val moves = mutableListOf<Move>()

        var dRow = target.row - from.row
        if (wrapEdges && kotlin.math.abs(dRow) * 2 > rowCount) {
            dRow = if (dRow > 0) dRow - rowCount else dRow + rowCount
        }
        repeat(kotlin.math.abs(dRow)) { moves.add(if (dRow > 0) Move.DOWN else Move.UP) }

        var dCol = target.col - from.col
        if (wrapEdges && kotlin.math.abs(dCol) * 2 > colCount) {
            dCol = if (dCol > 0) dCol - colCount else dCol + colCount
        }
        repeat(kotlin.math.abs(dCol)) { moves.add(if (dCol > 0) Move.RIGHT else Move.LEFT) }

        moves.add(Move.SELECT)
        return moves to target
    }

    /**
     * Full route for a string, starting from [start].
     *
     * Unsupported characters are skipped rather than aborting the run — better to type
     * most of a search term than none of it.
     */
    fun routeText(text: String, start: KeyPosition): Pair<List<Move>, KeyPosition> {
        val all = mutableListOf<Move>()
        var cursor = start
        text.forEach { ch ->
            val (moves, next) = route(cursor, ch) ?: return@forEach
            all += moves
            cursor = next
        }
        return all to cursor
    }

    companion object {
        /** Legend for cells that aren't a literal glyph. */
        const val SPACE_GLYPH = '_'
        const val BACKSPACE_GLYPH = '<'

        /**
         * Fire TV search keyboard: a-z then 0-9, six across.
         *
         * Verify against your own TV before trusting a long run — Amazon has shipped
         * more than one arrangement, and a layout that is off by one column will type
         * confident nonsense.
         */
        val FIRE_TV_SEARCH = listOf(
            "abcdef",
            "ghijkl",
            "mnopqr",
            "stuvwx",
            "yz0123",
            "456789",
        )

        fun parse(text: String, wrapEdges: Boolean = false): OnScreenKeyboard =
            OnScreenKeyboard(
                rows = text.lines().map { it.trimEnd() }.filter { it.isNotEmpty() },
                wrapEdges = wrapEdges,
            )
    }
}
