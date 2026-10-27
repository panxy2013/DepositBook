package com.pxy.depositbook.util

import com.pxy.depositbook.data.entity.Deposit
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 极简 xlsx 解析器：从你原有的 Excel「总账」sheet 中读取存款记录。
 *
 * 依赖的列（按表头文字匹配，不依赖列顺序）：
 * 理财账户 / 本金 / 产品名称 / 预期年化利率 / 到期日 / 存入时间
 *
 * 只使用 Android 自带的 XML API，不引入 Apache POI，避免安装包过大。
 */
object XlsxImporter {

    private val HEADER_ALIASES = mapOf(
        "account" to listOf("理财账户", "账户", "Account"),
        "principal" to listOf("本金", "Principal"),
        "product" to listOf("产品名称", "产品", "Product"),
        "rate" to listOf("预期年化利率", "年化利率", "年化", "Annual Rate", "Rate", "Interest Rate"),
        "end" to listOf("到期日", "到期日期", "Maturity", "Maturity Date"),
        "start" to listOf("存入时间", "存入日期", "起息日", "Deposit Date", "Start Date", "Start"),
    )

    data class ImportResult(
        val deposits: List<Deposit>,
        val skippedRows: Int,
        val sheetName: String,
    )

    fun parse(pathOrBytes: java.io.File): ImportResult = parse(pathOrBytes.readBytes())

    fun parse(bytes: ByteArray): ImportResult {
        val shared = mutableListOf<String>()
        val sheets = linkedMapOf<String, String>() // sheetName -> xml
        var workbookSheetOrder = mutableListOf<String>()

        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> shared += parseSharedStrings(zip.readBytes())
                    entry.name == "xl/workbook.xml" -> workbookSheetOrder = parseSheetOrder(zip.readBytes())
                    entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml") ->
                        sheets[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        // 优先取名为「总账 / General Ledger」的 sheet，否则取第一个
        val targetEntry = if (workbookSheetOrder.isNotEmpty()) {
            val idx = workbookSheetOrder.indexOfFirst { it.contains("总账") || it.equals("General Ledger", ignoreCase = true) }
            val name = if (idx >= 0) "xl/worksheets/sheet${idx + 1}.xml" else "xl/worksheets/sheet1.xml"
            name
        } else "xl/worksheets/sheet1.xml"

        val xml = sheets[targetEntry] ?: sheets.values.firstOrNull()
            ?: return ImportResult(emptyList(), 0, "")

        val rows = parseSheetRows(xml, shared)
        if (rows.isEmpty()) return ImportResult(emptyList(), 0, "")

        val headerIndex = rows.indexOfFirst { row -> row.any { it.contains("本金") || it.contains("Principal", ignoreCase = true) } }
        if (headerIndex < 0) return ImportResult(emptyList(), rows.size, "")

        val header = rows[headerIndex]
        val colMap = mutableMapOf<String, Int>()
        header.forEachIndexed { i, cell ->
            HEADER_ALIASES.forEach { (key, aliases) ->
                if (aliases.any { cell.contains(it) } && key !in colMap) colMap[key] = i
            }
        }

        val accountCol = colMap["account"] ?: return ImportResult(emptyList(), rows.size, "")
        val principalCol = colMap["principal"] ?: return ImportResult(emptyList(), rows.size, "")
        val rateCol = colMap["rate"]
        val startCol = colMap["start"]
        val endCol = colMap["end"]

        val list = mutableListOf<Deposit>()
        var skipped = 0
        rows.drop(headerIndex + 1).forEach { row ->
            fun cell(key: String): String? = colMap[key]?.let { row.getOrNull(it) }?.takeIf { it.isNotBlank() }

            val account = cell("account")?.takeIf { !it.contains("累计") && !it.contains("Balance", ignoreCase = true) && !it.contains("Total", ignoreCase = true) && !it.contains("余额") }
            val principal = cell("principal")?.toDoubleOrNull()
            if (account == null || principal == null || principal <= 0) {
                if (row.any { it.isNotBlank() }) skipped++
                return@forEach
            }
            val rate = when {
                rateCol == null -> 0.0
                else -> cell("rate")?.trim()?.let { raw ->
                    raw.removeSuffix("%").toDoubleOrNull()?.let { v ->
                        if (raw.contains("%")) v / 100.0 else if (v > 1) v / 100.0 else v
                    }
                } ?: 0.0
            }
            val start = cell("start")?.let { parseExcelDate(it) }
            val end = cell("end")?.let { parseExcelDate(it) }
            if (start == null || end == null) {
                skipped++
                return@forEach
            }
            list += Deposit(
                accountName = cleanBankName(account),
                productName = cell("product"),
                principal = principal,
                annualRate = rate,
                startDate = start,
                endDate = end,
            )
        }
        return ImportResult(list, skipped, "")
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(bytes.inputStream())
        val nodes = doc.getElementsByTagName("si")
        return (0 until nodes.length).map { i ->
            val t = nodes.item(i).let { node ->
                val texts = node.childNodes
                val sb = StringBuilder()
                for (j in 0 until texts.length) {
                    val child = texts.item(j)
                    if (child.nodeName == "t") sb.append(child.textContent)
                    if (child.nodeName == "r") {
                        val inner = child.childNodes
                        for (k in 0 until inner.length) {
                            if (inner.item(k).nodeName == "t") sb.append(inner.item(k).textContent)
                        }
                    }
                }
                sb.toString()
            }
            t
        }
    }

    private fun parseSheetOrder(bytes: ByteArray): MutableList<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(bytes.inputStream())
        val nodes = doc.getElementsByTagName("sheet")
        return (0 until nodes.length).mapNotNull {
            nodes.item(it).attributes?.getNamedItem("name")?.nodeValue
        }.toMutableList()
    }

    /** 返回 row -> list(cell text)，按 A,B,C... 顺序对齐 */
    private fun parseSheetRows(xml: String, shared: List<String>): List<List<String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream())
        val rowNodes = doc.getElementsByTagName("row")
        val result = mutableListOf<List<String>>()
        for (r in 0 until rowNodes.length) {
            val rowNode = rowNodes.item(r)
            val cells = rowNode.childNodes
            val map = sortedMapOf<Int, String>()
            for (c in 0 until cells.length) {
                val cell = cells.item(c)
                if (cell.nodeName != "c") continue
                val attrs = cell.attributes ?: continue
                val ref = attrs.getNamedItem("r")?.nodeValue ?: "A1"
                val col = colIndex(ref)
                val type = attrs.getNamedItem("t")?.nodeValue
                var value = ""
                val children = cell.childNodes
                for (k in 0 until children.length) {
                    val child = children.item(k)
                    if (child.nodeName == "v") value = child.textContent
                    if (child.nodeName == "is") value = child.textContent
                }
                val text = when (type) {
                    "s" -> value.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                    "str", "inlineStr" -> value
                    else -> value
                }
                map[col] = text
            }
            if (map.isNotEmpty()) {
                val max = map.keys.max()
                result += (0..max).map { map[it] ?: "" }
            } else {
                result += emptyList<String>()
            }
        }
        return result
    }

    private fun colIndex(ref: String): Int {
        var sum = 0
        for (ch in ref) {
            if (ch in 'A'..'Z') sum = sum * 26 + (ch - 'A' + 1) else break
        }
        return (sum - 1).coerceAtLeast(0)
    }

    private val COMMON_NAMES = listOf(
        "潘鹏", "姜红慧", "翟桂华", "潘星伊", "姥姥", "奶奶", "爷爷", "姥爷",
    )

    /**
     * 去掉账户名里的人名，只保留银行名称。
     * 例：中关村银行潘鹏 -> 中关村银行；辽宁振兴银行13520716455 -> 辽宁振兴银行
     */
    private fun cleanBankName(raw: String): String {
        var name = raw.trim()
        COMMON_NAMES.forEach { person -> name = name.replace(person, "") }
        name = name.replace(Regex("\\d{5,}"), "")
        name = name.replace(Regex("[-—·\\s]+$"), "")
        return name.trim().ifEmpty { raw.trim() }
    }

    private fun parseExcelDate(raw: String): Long? {
        val s = raw.trim()
        s.toDoubleOrNull()?.let { serial ->
            if (serial > 20000) {
                // Excel 序列号：1899-12-30 起算
                val days = serial.toLong()
                val date = LocalDate.of(1899, 12, 30).plusDays(days)
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        val patterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "yyyy-M-d", "yyyy/M/d")
        patterns.forEach { p ->
            try {
                val d = LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern(p))
                return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return null
    }
}
