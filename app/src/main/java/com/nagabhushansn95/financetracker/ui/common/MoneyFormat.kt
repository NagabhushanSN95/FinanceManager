package com.nagabhushansn95.financetracker.ui.common

import java.text.NumberFormat
import java.util.Locale

/** Formats amounts with the grouping the user is used to (Indian lakh/crore grouping for INR). */
object MoneyFormat {
    private val indian: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val western: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun symbol(currency: String): String = when (currency) {
        "INR" -> "₹"
        "USD" -> "$"
        "SGD" -> "S$"
        "AUD" -> "A$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$currency "
    }

    /**
     * Short form for tables: "₹1.58 Cr", "₹39.00 L", "₹45,210" (under a lakh, no paise). Foreign currencies use
     * "$12.3K" / "$1.2M".
     */
    fun compact(value: Double, currency: String = "INR"): String {
        val sign = if (value < 0) "-" else ""
        val v = kotlin.math.abs(value)
        val body = if (currency == "INR") when {
            v >= 1e7 -> "%.2f Cr".format(v / 1e7)
            v >= 1e5 -> "%.2f L".format(v / 1e5)
            else -> indianWhole.format(v)
        } else when {
            v >= 1e6 -> "%.2fM".format(v / 1e6)
            v >= 1e3 -> "%.1fK".format(v / 1e3)
            else -> westernWhole.format(v)
        }
        return sign + symbol(currency) + body
    }

    private val indianWhole: NumberFormat =
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    private val westernWhole: NumberFormat =
        NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }

    fun amount(value: Double, currency: String = "INR"): String {
        val format = if (currency == "INR") indian else western
        val sign = if (value < 0) "-" else ""
        return sign + symbol(currency) + format.format(kotlin.math.abs(value))
    }
}
