package com.nagabhushansn95.financetracker.data.importer.v4

/** One endpoint of a v4 type string: `Wallet01` or `Bank16`. */
data class V4Endpoint(val isWallet: Boolean, val id: Int) {
    val key: String get() = (if (isWallet) "W" else "B") + id
    override fun toString(): String =
        (if (isWallet) "Wallet" else "Bank") + id.toString().padStart(2, '0')
}

sealed class V4Type {
    data class Credit(val to: V4Endpoint) : V4Type()
    data class Debit(val from: V4Endpoint, val expTypeId: Int) : V4Type()
    data class Transfer(val from: V4Endpoint, val to: V4Endpoint) : V4Type()
}

/**
 * Parses the positional v4 type strings:
 *   `Credit Wallet01`, `Credit Bank03`, `Debit Wallet01 Exp02`, `Debit Bank05 Exp11`, `Transfer Bank17 Bank16`.
 * Ids are two digits in v4, but any width is accepted here.
 */
object V4TypeParser {
    private val endpoint = "(Wallet|Bank)(\\d+)"
    private val credit = Regex("^Credit $endpoint$")
    private val debit = Regex("^Debit $endpoint Exp(\\d+)$")
    private val transfer = Regex("^Transfer $endpoint $endpoint$")

    fun parse(type: String): V4Type {
        val t = type.trim()
        credit.matchEntire(t)
            ?.let { m -> return V4Type.Credit(ep(m.groupValues[1], m.groupValues[2])) }
        debit.matchEntire(t)?.let { m ->
            return V4Type.Debit(
                ep(m.groupValues[1], m.groupValues[2]),
                m.groupValues[3].toInt()
            )
        }
        transfer.matchEntire(t)?.let { m ->
            return V4Type.Transfer(
                ep(m.groupValues[1], m.groupValues[2]),
                ep(m.groupValues[3], m.groupValues[4])
            )
        }
        throw V4ImportException("Unrecognised v4 transaction type: '$type'")
    }

    private fun ep(kind: String, id: String) = V4Endpoint(kind == "Wallet", id.toInt())
}

class V4ImportException(message: String) : RuntimeException(message)
