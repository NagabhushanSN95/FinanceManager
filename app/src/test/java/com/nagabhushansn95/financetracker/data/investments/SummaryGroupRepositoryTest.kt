package com.nagabhushansn95.financetracker.data.investments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.repository.EditDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SummaryGroupRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: EditDataRepository

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repo = EditDataRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun groupsAreSeededOnceThenAddedEditedOrderedAndDeleted() = runTest {
        SummaryGroups.seedIfEmpty(db)
        assertEquals(listOf("Liquid", "In Zerodha"), db.summaryGroupDao().getAll().map { it.name })
        val id = repo.addSummaryGroup("  Nifty 50 ", "instrument:4,instrument:93")
        var all = db.summaryGroupDao().getAll()
        assertEquals(listOf("Liquid", "In Zerodha", "Nifty 50"), all.map { it.name })
        assertEquals("instrument:4,instrument:93", all.last().members)
        repo.updateSummaryGroup(all.last().copy(name = "Nifty 50 ETFs", members = "category:14"))
        assertEquals("category:14", db.summaryGroupDao().getAll().last().members)
        repo.reorderSummaryGroups(listOf(id, all[0].id, all[1].id))
        assertEquals(
            listOf("Nifty 50 ETFs", "Liquid", "In Zerodha"),
            db.summaryGroupDao().getAll().map { it.name })
        repo.deleteSummaryGroup(all[1].id)
        all = db.summaryGroupDao().getAll()
        assertEquals(listOf("Nifty 50 ETFs", "Liquid"), all.map { it.name })
        assertTrue(runCatching { repo.addSummaryGroup(" ", "pf") }.isFailure)
        // Seeding does not come back after the user has been given the defaults once.
        all.forEach { db.summaryGroupDao().delete(it.id) }
        SummaryGroups.seedIfEmpty(db)
        assertEquals(0, db.summaryGroupDao().count())
    }
}
