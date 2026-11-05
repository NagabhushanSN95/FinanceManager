package com.nagabhushansn95.financetracker.ui.common

/**
 * Orders text the way people read it: digit runs compare by value, so "FD #70" comes before "FD #100" and "A2" before
 * "A10". Letters compare case-insensitively; ties fall back to the plain string so the order is total.
 */
object NaturalOrder : Comparator<String> {
    private val CHUNK = Regex("""\d+|\D+""")

    override fun compare(a: String, b: String): Int {
        val xs = CHUNK.findAll(a).map { it.value }.toList()
        val ys = CHUNK.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(xs.size, ys.size)) {
            val x = xs[i]
            val y = ys[i]
            val c = if (x[0].isDigit() && y[0].isDigit()) {
                val xt = x.trimStart('0').ifEmpty { "0" }
                val yt = y.trimStart('0').ifEmpty { "0" }
                if (xt.length != yt.length) xt.length.compareTo(yt.length) else xt.compareTo(yt)
            } else x.compareTo(y, ignoreCase = true)
            if (c != 0) return c
        }
        return xs.size.compareTo(ys.size).takeIf { it != 0 } ?: a.compareTo(b)
    }
}
