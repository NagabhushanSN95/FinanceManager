package com.nagabhushansn95.financetracker.data.prices

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One market quote: price in [currency] as of [date] (the market's trading day). */
data class Quote(
    val symbol: String,
    val price: Double,
    val currency: String,
    val date: LocalDate,
    val name: String?
)

/**
 * Yahoo Finance's unofficial chart endpoint, used for NSE/BSE and US stocks and the USD/INR rate.
 * NSE symbols take a ".NS" suffix, BSE ".BO", US exchanges none. Stored with source YAHOO and shown "as of".
 */
class YahooQuotes(private val http: HttpClient) {
    /** The Yahoo ticker for an instrument, or null when it has no symbol. */
    fun ticker(instrument: Instrument): String? {
        val symbol = instrument.symbol?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (symbol.contains('.') || symbol.contains('=')) return symbol
        return when (instrument.exchange?.uppercase()) {
            null, "", "NSE" -> "$symbol.NS"
            "BSE" -> "$symbol.BO"
            else -> symbol
        }
    }

    suspend fun quote(ticker: String): Quote = parse(http.get(url(ticker)))

    suspend fun usdInr(): Quote = quote(USD_INR)

    companion object {
        const val USD_INR = "USDINR=X"
        private val MARKET_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

        fun url(ticker: String) = "https://query1.finance.yahoo.com/v8/finance/chart/${
            java.net.URLEncoder.encode(
                ticker,
                "UTF-8"
            )
        }?range=1d&interval=1d"

        /** Reads the price from a chart response; throws with Yahoo's message when the symbol is unknown. */
        fun parse(json: String): Quote {
            val chart = JSONObject(json).getJSONObject("chart")
            val results = chart.optJSONArray("result")
            if (results == null || results.length() == 0) {
                val error = chart.optJSONObject("error")
                throw IllegalStateException(
                    error?.optString("description")?.takeIf { it.isNotEmpty() } ?: "No data")
            }
            val meta = results.getJSONObject(0).getJSONObject("meta")
            val price = meta.optDouble("regularMarketPrice", Double.NaN)
            require(!price.isNaN()) { "No price in the response" }
            val time = meta.optLong("regularMarketTime", 0L)
            val zone =
                runCatching { ZoneId.of(meta.optString("exchangeTimezoneName", "")) }.getOrDefault(
                    MARKET_ZONE
                )
            val date = if (time > 0) Instant.ofEpochSecond(time).atZone(zone)
                .toLocalDate() else LocalDate.now()
            return Quote(
                symbol = meta.optString("symbol"),
                price = price,
                currency = meta.optString("currency", "INR").ifEmpty { "INR" },
                date = date,
                name = meta.optString("longName")
                    .ifEmpty { meta.optString("shortName").ifEmpty { null } },
            )
        }
    }
}
