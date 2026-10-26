package com.pxy.depositbook.data.backup

import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.util.DateUtils
import com.pxy.depositbook.util.InterestCalculator
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 导出为 .xlsx，表头与你原来的 Excel「总账」保持一致，方便直接对照。
 * 不依赖 Apache POI，直接写 OOXML，体积小、速度快。
 */
object XlsxExporter {

    fun export(deposits: List<Deposit>, today: LocalDate = DateUtils.today()): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->

            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(CONTENT_TYPES.toByteArray(Charsets.UTF_8))

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(ROOT_RELS.toByteArray(Charsets.UTF_8))

            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(WORKBOOK.toByteArray(Charsets.UTF_8))

            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(WORKBOOK_RELS.toByteArray(Charsets.UTF_8))

            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(STYLES.toByteArray(Charsets.UTF_8))

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml(deposits, today).toByteArray(Charsets.UTF_8))
        }
        return out.toByteArray()
    }

    private fun sheetXml(deposits: List<Deposit>, today: LocalDate): String {
        val headers = listOf(
            "理财账户", "本金", "产品名称", "预期年化利率",
            "日利息", "需存天数", "存入天数", "剩余天数",
            "预估资产", "到期日", "存入时间", "已经获得利息",
        )
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<cols>")
        sb.append("""<col min="1" max="1" width="26" customWidth="1"/>""")
        sb.append("""<col min="2" max="2" width="14" customWidth="1"/>""")
        sb.append("""<col min="3" max="3" width="22" customWidth="1"/>""")
        sb.append("""<col min="4" max="5" width="13" customWidth="1"/>""")
        sb.append("""<col min="6" max="8" width="10" customWidth="1"/>""")
        sb.append("""<col min="9" max="9" width="15" customWidth="1"/>""")
        sb.append("""<col min="10" max="11" width="13" customWidth="1"/>""")
        sb.append("""<col min="12" max="12" width="15" customWidth="1"/>""")
        sb.append("</cols>")
        sb.append("<sheetData>")

        // 表头行
        sb.append("""<row r="1">""")
        headers.forEachIndexed { i, h ->
            sb.append(cellStr(colName(i) + "1", h, style = 1))
        }
        sb.append("</row>")

        var row = 2
        var totalPrincipal = 0.0
        var totalInterest = 0.0
        var totalValue = 0.0

        deposits.sortedBy { it.endDate }.forEach { d ->
            val daysTotal = DateUtils.daysBetween(d.startDate, d.endDate)
            val passed = InterestCalculator.daysPassed(d, today)
            val left = InterestCalculator.daysLeft(d, today)
            val daily = InterestCalculator.dailyInterest(d.principal, d.annualRate)
            val earned = InterestCalculator.interestEarned(d, today)
            val valueNow = InterestCalculator.valueNow(d, today)

            totalPrincipal += d.principal
            totalInterest += earned
            totalValue += valueNow

            sb.append("""<row r="$row">""")
            sb.append(cellStr("A$row", d.accountName))
            sb.append(cellNum("B$row", d.principal, 2))
            sb.append(cellStr("C$row", d.productName ?: ""))
            sb.append(cellNum("D$row", d.annualRate, 4, isPercent = true))
            sb.append(cellNum("E$row", daily, 2))
            sb.append(cellNum("F$row", daysTotal.toDouble(), 0))
            sb.append(cellNum("G$row", passed.toDouble(), 0))
            sb.append(cellNum("H$row", left.toDouble(), 0))
            sb.append(cellNum("I$row", valueNow, 2))
            sb.append(cellDate("J$row", d.endDate))
            sb.append(cellDate("K$row", d.startDate))
            sb.append(cellNum("L$row", earned, 2))
            sb.append("</row>")
            row++
        }

        // 合计行
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A$row", "合计", style = 1))
        sb.append(cellNum("B$row", totalPrincipal, 2, style = 1))
        sb.append(cellStr("C$row", ""))
        sb.append(cellStr("D$row", ""))
        sb.append(cellNum("E$row", deposits.sumOf {
            InterestCalculator.dailyInterest(it.principal, it.annualRate)
        }, 2, style = 1))
        sb.append(cellStr("F$row", ""))
        sb.append(cellStr("G$row", ""))
        sb.append(cellStr("H$row", ""))
        sb.append(cellNum("I$row", totalValue, 2, style = 1))
        sb.append(cellStr("J$row", ""))
        sb.append(cellStr("K$row", ""))
        sb.append(cellNum("L$row", totalInterest, 2, style = 1))
        sb.append("</row>")

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colName(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
            if (i < 0) break
        }
        return sb.toString()
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun cellStr(ref: String, value: String, style: Int = 0): String =
        """<c r="$ref" t="inlineStr"${if (style > 0) """ s="$style"""" else ""}><is><t xml:space="preserve">${escape(value)}</t></is></c>"""

    private fun cellDate(ref: String, millis: Long): String {
        // Excel 序列号：1899-12-30 起算
        val days = java.time.temporal.ChronoUnit.DAYS
            .between(LocalDate.of(1899, 12, 30), DateUtils.toDate(millis))
        return """<c r="$ref" s="5"><v>$days</v></c>"""
    }

    private fun cellNum(
        ref: String,
        value: Double,
        decimals: Int,
        isPercent: Boolean = false,
        style: Int = 0,
    ): String {
        val shown = value
        val formatted = String.format("%.${decimals}f", shown)
        val s = when {
            style > 0 -> style
            isPercent -> 3
            decimals == 0 -> 4
            else -> 2
        }
        return """<c r="$ref" s="$s"><v>$formatted</v></c>"""
    }

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="总账" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private const val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="2"><numFmt numFmtId="176" formatCode="#,##0.00"/><numFmt numFmtId="177" formatCode="yyyy-mm-dd"/></numFmts>
<fonts count="2">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
</fonts>
<fills count="3">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFDCE3EA"/><bgColor indexed="64"/></patternFill></fill>
</fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="6">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
<xf numFmtId="176" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="10" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="1" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="177" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""
}
