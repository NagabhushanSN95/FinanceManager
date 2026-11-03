package com.nagabhushansn95.financetracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {
    @Test
    fun compactUsesLakhAndCrore() {
        assertEquals("₹1.58 Cr", MoneyFormat.compact(15_783_838.92))
        assertEquals("₹39.00 L", MoneyFormat.compact(3_900_000.0))
        assertEquals("₹45,210", MoneyFormat.compact(45_210.4))
        assertEquals("-₹5,207", MoneyFormat.compact(-5_207.13))
        assertEquals("₹0", MoneyFormat.compact(0.0))
        assertEquals("$12.3K", MoneyFormat.compact(12_345.0, "USD"))
    }
}
