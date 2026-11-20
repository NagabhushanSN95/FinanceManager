package com.nagabhushansn95.financetracker.data.importer.v4

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class V4TypeParserTest {
    @Test
    fun parsesAllShapes() {
        assertEquals(V4Type.Credit(V4Endpoint(true, 1)), V4TypeParser.parse("Credit Wallet01"))
        assertEquals(V4Type.Credit(V4Endpoint(false, 3)), V4TypeParser.parse("Credit Bank03"))
        assertEquals(
            V4Type.Debit(V4Endpoint(true, 1), 2),
            V4TypeParser.parse("Debit Wallet01 Exp02")
        )
        assertEquals(
            V4Type.Debit(V4Endpoint(false, 16), 11),
            V4TypeParser.parse("Debit Bank16 Exp11")
        )
        assertEquals(
            V4Type.Transfer(V4Endpoint(false, 17), V4Endpoint(false, 16)),
            V4TypeParser.parse("Transfer Bank17 Bank16")
        )
        assertEquals(
            V4Type.Transfer(V4Endpoint(true, 1), V4Endpoint(true, 10)),
            V4TypeParser.parse("Transfer Wallet01 Wallet10")
        )
    }

    @Test
    fun rejectsGarbage() {
        assertThrows(V4ImportException::class.java) { V4TypeParser.parse("Credit") }
        assertThrows(V4ImportException::class.java) { V4TypeParser.parse("Debit Wallet01") }
        assertThrows(V4ImportException::class.java) { V4TypeParser.parse("Transfer Bank01") }
    }
}
