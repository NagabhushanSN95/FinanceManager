package com.nagabhushansn95.financetracker.data.export

import java.io.OutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A minimal values-only .xlsx writer: sheets of rows of cells (text, numbers, dates, booleans), a bold header row,
 * frozen header, column widths. No formulas, no shared strings (inline strings keep the writer tiny), no Apache POI.
 * Dates are stored as Excel serial numbers with a date style so they sort and filter as dates.
 */
class XlsxWriter {
    class Sheet(val name: String, val header: List<String>, val widths: List<Int>? = null) {
        val rows = ArrayList<List<Any?>>()
        fun row(vararg cells: Any?) {
            rows += cells.toList()
        }

        fun row(cells: List<Any?>) {
            rows += cells
        }
    }

    private val sheets = ArrayList<Sheet>()

    fun sheet(name: String, header: List<String>, widths: List<Int>? = null): Sheet =
        Sheet(
            name.take(31).replace(Regex("[\\[\\]:*?/\\\\]"), " "),
            header,
            widths
        ).also { sheets += it }

    fun write(out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            fun entry(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            entry("[Content_Types].xml", contentTypes())
            entry("_rels/.rels", RELS)
            entry("xl/workbook.xml", workbook())
            entry("xl/_rels/workbook.xml.rels", workbookRels())
            entry("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, s -> entry("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        }
    }

    private fun contentTypes() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        sheets.indices.forEach { append("""<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""") }
        append("</Types>")
    }

    private fun workbook() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheets.forEachIndexed { i, s -> append("""<sheet name="${esc(s.name)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""") }
        append("</sheets></workbook>")
    }

    private fun workbookRels() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        sheets.indices.forEach { append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""") }
        append("""<Relationship Id="rId${sheets.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun sheetXml(s: Sheet) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        val widths = s.widths ?: s.header.map { (it.length + 4).coerceIn(10, 40) }
        append("<cols>")
        widths.forEachIndexed { i, w -> append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
        append("</cols><sheetData>")
        append("""<row r="1">""")
        s.header.forEachIndexed { c, h ->
            append(
                """<c r="${
                    ref(
                        c,
                        1
                    )
                }" t="inlineStr" s="1"><is><t>${esc(h)}</t></is></c>"""
            )
        }
        append("</row>")
        s.rows.forEachIndexed { r, cells ->
            val rowNo = r + 2
            append("""<row r="$rowNo">""")
            cells.forEachIndexed { c, v ->
                val ref = ref(c, rowNo)
                when (v) {
                    null -> Unit
                    is String -> if (v.isNotEmpty()) append(
                        """<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${
                            esc(
                                v
                            )
                        }</t></is></c>"""
                    )

                    is LocalDate -> append("""<c r="$ref" s="2"><v>${serial(v)}</v></c>""")
                    is Boolean -> append("""<c r="$ref" t="b"><v>${if (v) 1 else 0}</v></c>""")
                    is Number -> {
                        val d = v.toDouble()
                        if (!d.isNaN() && !d.isInfinite()) append("""<c r="$ref" s="3"><v>${plain(d)}</v></c>""")
                    }

                    else -> append("""<c r="$ref" t="inlineStr"><is><t>${esc(v.toString())}</t></is></c>""")
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    companion object {
        private val EPOCH: LocalDate = LocalDate.of(1899, 12, 30)
        fun serial(date: LocalDate): Long = ChronoUnit.DAYS.between(EPOCH, date)
        fun ref(column: Int, row: Int): String {
            var c = column
            val sb = StringBuilder()
            do {
                sb.insert(0, ('A' + c % 26)); c = c / 26 - 1
            } while (c >= 0)
            return "$sb$row"
        }

        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;")
            .filter { it == '\n' || it == '\t' || it.code >= 32 }

        private fun plain(d: Double) =
            if (d == d.toLong().toDouble() && kotlin.math.abs(d) < 1e15) d.toLong()
                .toString() else java.math.BigDecimal(d).setScale(6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()

        private const val RELS =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

        /** Styles: 0 default, 1 bold header, 2 date (yyyy-mm-dd), 3 number with thousands separators. */
        private const val STYLES =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="2"><numFmt numFmtId="164" formatCode="yyyy-mm-dd"/><numFmt numFmtId="165" formatCode="#,##0.00"/></numFmts><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="4"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs></styleSheet>"""
    }
}
