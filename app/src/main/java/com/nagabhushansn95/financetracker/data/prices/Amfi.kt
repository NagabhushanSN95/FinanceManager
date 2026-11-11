package com.nagabhushansn95.financetracker.data.prices

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One row of AMFI's NAVAll.txt. */
data class AmfiScheme(
    val code: String,
    val isinGrowth: String?,
    val isinReinvestment: String?,
    val name: String,
    val plan: String,
    val option: String,
    val nav: Double,
    val date: LocalDate,
) {
    val isDirect: Boolean
        get() = plan.contains(
            "direct",
            ignoreCase = true
        ) || option.contains("direct", ignoreCase = true)
    val isGrowth: Boolean get() = option.contains("growth", ignoreCase = true)
    val label: String
        get() = listOf(name, plan, option).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * AMFI's daily NAV list (every scheme, one file). Funds are matched by ISIN, then by the scheme code kept in
 * [Instrument.symbol] (exchange AMFI), then by name when exactly one Direct-Growth plan has that name, or has every
 * word of it (a renamed scheme keeps the old name in brackets).
 */
class AmfiNav(private val http: HttpClient) {
    suspend fun fetchAll(): List<AmfiScheme> = parse(http.get(URL))

    companion object {
        const val URL = "https://portal.amfiindia.com/spages/NAVAll.txt"
        const val EXCHANGE = "AMFI"
        private val DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

        fun parse(text: String): List<AmfiScheme> = text.lineSequence().mapNotNull { line ->
            val parts = line.split(';')
            if (parts.size < 8 || !parts[0].trim()
                    .all { it.isDigit() } || parts[0].isBlank()
            ) return@mapNotNull null
            val nav = parts[6].trim().toDoubleOrNull() ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(parts[7].trim(), DATE) }.getOrNull()
                ?: return@mapNotNull null
            AmfiScheme(
                code = parts[0].trim(),
                isinGrowth = parts[1].trim().takeIf { it.isNotEmpty() && it != "-" },
                isinReinvestment = parts[2].trim().takeIf { it.isNotEmpty() && it != "-" },
                name = parts[3].trim(),
                plan = parts[4].trim(),
                option = parts[5].trim(),
                nav = nav,
                date = date,
            )
        }.toList()

        private fun norm(s: String) =
            s.lowercase().replace("&", "and").replace(Regex("[^a-z0-9]+"), " ").trim()

        /** The scheme for an instrument, or null when none is certain. */
        fun match(instrument: Instrument, schemes: List<AmfiScheme>): AmfiScheme? {
            instrument.isin?.trim()?.takeIf { it.isNotEmpty() }?.let { isin ->
                schemes.firstOrNull { it.isinGrowth == isin || it.isinReinvestment == isin }
                    ?.let { return it }
            }
            instrument.symbol?.trim()?.takeIf { it.isNotEmpty() && it.all { c -> c.isDigit() } }
                ?.let { code ->
                    schemes.firstOrNull { it.code == code }?.let { return it }
                }
            val wanted = norm(instrument.name)
            val sameName = schemes.filter { norm(it.name) == wanted }
            val directGrowth = sameName.filter { it.isDirect && it.isGrowth }
            (directGrowth.singleOrNull() ?: sameName.singleOrNull())?.let { return it }
            // A renamed scheme keeps its old name in brackets ("ICICI Prudential Value Fund (erstwhile Value
            // Discovery Fund)"): every word of ours in its name, and exactly one Direct Growth plan, is a match.
            val words = wanted.split(' ').filter { it.isNotEmpty() }.toSet()
            if (words.size < 3) return null
            val containing = schemes.filter { s ->
                val theirs = norm(s.name).split(' ').toSet()
                words.all { it in theirs }
            }
            return containing.filter { it.isDirect && it.isGrowth }.singleOrNull()
        }

        /** Schemes whose name contains every word of [query]; for the "Link AMFI scheme" picker. */
        fun search(query: String, schemes: List<AmfiScheme>, limit: Int = 60): List<AmfiScheme> {
            val words = norm(query).split(' ').filter { it.isNotEmpty() }
            if (words.isEmpty()) return emptyList()
            return schemes.filter { s -> val n = norm(s.label); words.all { n.contains(it) } }
                .sortedWith(compareByDescending<AmfiScheme> { it.isDirect && it.isGrowth }.thenBy { it.label })
                .take(limit)
        }
    }
}
