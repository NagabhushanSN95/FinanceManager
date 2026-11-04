package com.nagabhushansn95.financetracker.data.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class SummaryWorkbookTest {
    private lateinit var db: AppDatabase
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() = db.close()

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val out = HashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                out[e.name] = zip.readBytes().toString(Charsets.UTF_8); e = zip.nextEntry
            }
        }
        return out
    }

    @Test
    fun xlsxWriterCellsAndRefs() {
        assertEquals("A1", XlsxWriter.ref(0, 1))
        assertEquals("Z2", XlsxWriter.ref(25, 2))
        assertEquals("AA3", XlsxWriter.ref(26, 3))
        assertEquals("AB10", XlsxWriter.ref(27, 10))
        assertEquals(46388L, XlsxWriter.serial(LocalDate.of(2027, 1, 1)))
        val w = XlsxWriter()
        val s = w.sheet("Sheet:One", listOf("Text", "Number", "Date", "Flag"))
        s.row("a & b", 1234.5, LocalDate.of(2026, 11, 4), true)
        s.row(null, 7, null, false)
        val bytes = ByteArrayOutputStream().also { w.write(it) }.toByteArray()
        val files = unzip(bytes)
        assertTrue(
            files.keys.containsAll(
                listOf(
                    "[Content_Types].xml",
                    "_rels/.rels",
                    "xl/workbook.xml",
                    "xl/_rels/workbook.xml.rels",
                    "xl/styles.xml",
                    "xl/worksheets/sheet1.xml"
                )
            )
        )
        assertTrue(files["xl/workbook.xml"]!!.contains("""<sheet name="Sheet One" sheetId="1" r:id="rId1"/>"""))
        val sheet = files["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("""<c r="A2" t="inlineStr"><is><t xml:space="preserve">a &amp; b</t></is></c>"""))
        assertTrue(sheet.contains("""<c r="B2" s="3"><v>1234.5</v></c>"""))
        assertTrue(sheet.contains("""<c r="C2" s="2"><v>46330</v></c>"""))
        assertTrue(sheet.contains("""<c r="D2" t="b"><v>1</v></c>"""))
        assertTrue(sheet.contains("""<row r="3"><c r="B3" s="3"><v>7</v></c><c r="D3" t="b"><v>0</v></c></row>"""))
    }

    @Test
    fun workbookHasTheSheetsAndFigures() = runTest {
        InvestmentCategories.seedIfEmpty(db)
        val bank = db.accountDao().insert(
            Account(
                name = "Bank",
                accountType = AccountType.BANK,
                balance = 1000.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val epf = db.accountDao().insert(
            Account(
                name = "EPF",
                accountType = AccountType.PROVIDENT_FUND,
                balance = 9000.0,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        val fd = db.instrumentDao().insert(
            Instrument(
                name = "FD #1",
                instrumentType = InstrumentType.FIXED_DEPOSIT,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        db.instrumentDao().upsertFixedDeposit(
            FixedDeposit(
                instrumentId = fd,
                bankName = "SBI",
                periodMonths = 12,
                rate = 6.25,
                compoundingMonths = 3,
                maturityDate = LocalDate.of(2027, 2, 3)
            )
        )
        val gold = db.instrumentDao().insert(
            Instrument(
                name = "GOLDBEES",
                symbol = "GOLDBEES",
                instrumentType = InstrumentType.STOCK,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )

        fun t(
            date: String,
            kind: TransactionKind,
            amount: Double,
            instrument: Long? = null,
            from: Long? = null,
            to: Long? = null,
            qty: Double? = null,
            contributor: String? = null
        ) =
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.parse(date),
                kind = kind,
                fromAccountId = from,
                toAccountId = to,
                instrumentId = instrument,
                particulars = "row",
                amount = amount,
                quantity = qty,
                rate = qty?.let { amount / it },
                contributor = contributor
            )
        db.transactionDao().insertAll(
            listOf(
                t("2026-02-03", TransactionKind.INVEST, 100000.0, instrument = fd, from = bank),
                t(
                    "2026-03-01",
                    TransactionKind.INVEST,
                    5000.0,
                    instrument = gold,
                    from = bank,
                    qty = 50.0
                ),
                t("2026-03-05", TransactionKind.INCOME, 4500.0, to = epf, contributor = "Employer"),
            )
        )
        val bytes =
            ByteArrayOutputStream().also { SummaryWorkbook(db).write(it, LocalDate.of(2026, 7, 6)) }
                .toByteArray()
        val files = unzip(bytes)
        val workbook = files["xl/workbook.xml"]!!
        listOf(
            "Savings Yearly",
            "Savings Monthly",
            "Investment Summary",
            "Fixed Deposits",
            "Provident Funds",
            "Insurances",
            "Insurance Policies",
            "Mutual Funds Summary",
            "Stocks Summary",
            "Others",
            "Prices"
        )
            .forEach { assertTrue("missing sheet $it", workbook.contains("""name="$it"""")) }
        val yearly = files["xl/worksheets/sheet1.xml"]!!
        assertTrue(yearly.contains("<t>Fixed Deposits Savings</t>"))
        assertTrue(yearly.contains("""<t xml:space="preserve">2026</t>"""))
        assertTrue(yearly.contains("<v>109500</v>"))            // 100000 + 5000 + 4500
        val fds = files["xl/worksheets/sheet4.xml"]!!
        assertTrue(fds.contains("<v>106398.016214</v>"))              // maturity amount at 6.25% for a year (1,06,398.02)
        assertTrue(fds.contains("""<t xml:space="preserve">ACTIVE</t>"""))
        val stocks = files["xl/worksheets/sheet9.xml"]!!
        assertTrue(stocks.contains("""<t xml:space="preserve">GOLDBEES</t>"""))
        assertTrue(stocks.contains("<v>50</v>"))
        val pf = files["xl/worksheets/sheet5.xml"]!!
        assertTrue(pf.contains("""<t xml:space="preserve">Employer</t>"""))
        assertTrue(pf.contains("""<t xml:space="preserve">Credit</t>"""))
    }
}
