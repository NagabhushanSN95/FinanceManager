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

    fun amount(value: Double, currency: String = "INR"): String {
        val format = if (currency == "INR") indian else western
        val sign = if (value < 0) "-" else ""
        return sign + symbol(currency) + format.format(kotlin.math.abs(value))
    }
}
