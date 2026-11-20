package com.nagabhushansn95.financetracker.data.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveBackupTest {
    @Test
    fun multipartBodyHasMetadataThenFile() {
        val body = DriveBackup.multipartBody(
            """{"name":"x.db"}""",
            "application/vnd.sqlite3",
            byteArrayOf(1, 2, 3)
        ).toString(Charsets.ISO_8859_1)
        val boundary = "--finance_tracker_upload_boundary"
        assertTrue(body.startsWith("$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n{\"name\":\"x.db\"}\r\n$boundary\r\nContent-Type: application/vnd.sqlite3\r\n\r\n"))
        assertTrue(body.endsWith("\u0001\u0002\u0003\r\n$boundary--"))
        assertEquals(
            2,
            Regex("""\r\n--finance_tracker_upload_boundary(\r\n|--)""").findAll(body).count()
        )
    }
}

class DriveFolderPathTest {
    @Test
    fun levelsSplitAndTrim() {
        assertEquals(
            listOf("Backups", "Apps", "Finance Tracker"),
            DriveBackup.levels("Backups/Apps/Finance Tracker")
        )
        assertEquals(listOf("Backups", "Apps"), DriveBackup.levels(" / Backups / Apps / "))
        assertEquals(emptyList<String>(), DriveBackup.levels(""))
        assertEquals(listOf("A", "B"), DriveBackup.levels("A\\B"))
    }
}
