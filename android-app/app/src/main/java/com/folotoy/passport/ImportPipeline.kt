package com.folotoy.passport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

enum class ImportConfidence { HIGH, MEDIUM, LOW }

data class ImportCandidate(
    val id: String,
    val text: String,
    val source: String,
    val confidence: ImportConfidence,
    val selected: Boolean = true,
    val duplicate: Boolean = false,
)

data class ExtractedTableRow(
    val rowNumber: Int,
    val cells: List<String>,
)

data class ExtractedTable(
    val source: String,
    val headers: List<String>,
    val rows: List<ExtractedTableRow>,
)

data class ImportExtraction(
    val candidates: List<ImportCandidate>,
    val tables: List<ExtractedTable> = emptyList(),
    val selectedColumn: String? = null,
    val notice: String? = null,
    internal val baseCandidates: List<ImportCandidate> = candidates,
) {
    val availableColumns: List<String>
        get() = tables.flatMap(ExtractedTable::headers)
            .filter(String::isNotBlank)
            .distinctBy(CandidateClassifier::normalizedKey)
}

internal data class OcrLine(
    val text: String,
    val height: Int,
    val top: Int,
    val left: Int,
    val width: Int = 0,
) {
    val centerX: Double get() = left + width / 2.0
    val centerY: Double get() = top + height / 2.0
}

internal data class RecognizedText(
    val lines: List<OcrLine>,
    val elements: List<OcrLine>,
)

sealed interface ImportSource {
    data class Pasted(val value: String) : ImportSource
    data class Document(val uri: Uri, val mimeType: String?, val displayName: String?) : ImportSource
}

object ImportEngine {
    const val MAX_FILE_BYTES = 20 * 1024 * 1024
    private const val MAX_WEB_BYTES = 2 * 1024 * 1024
    private const val MAX_PDF_PAGES = 30

    suspend fun extract(
        context: Context,
        source: ImportSource,
        onProgress: (String) -> Unit = {},
    ): ImportExtraction = withContext(Dispatchers.IO) {
        when (source) {
            is ImportSource.Pasted -> {
                val trimmed = source.value.trim()
                if (trimmed.matches(Regex("https://\\S+", RegexOption.IGNORE_CASE))) {
                    onProgress("正在读取网页")
                    buildExtraction(extractWeb(trimmed))
                } else extractStructuredText(trimmed, "粘贴文本")
            }
            is ImportSource.Document -> {
                checkFileSize(context, source.uri)
                val name = source.displayName.orEmpty().lowercase(Locale.ROOT)
                val mime = source.mimeType.orEmpty().lowercase(Locale.ROOT)
                when {
                    mime.startsWith("image/") -> {
                        onProgress("正在识别图片")
                        extractImage(context, source.uri, source.displayName ?: "图片")
                    }
                    mime == "application/pdf" || name.endsWith(".pdf") -> {
                        onProgress("正在识别 PDF")
                        extractPdf(context, source.uri, onProgress)
                    }
                    name.endsWith(".csv") || mime == "text/csv" || mime == "text/tab-separated-values" -> {
                        onProgress("正在读取 CSV")
                        extractCsv(readLimited(context, source.uri))
                    }
                    name.endsWith(".xls") || name.endsWith(".xlsx") || mime.contains("excel") || mime.contains("spreadsheet") -> {
                        onProgress("正在读取工作簿")
                        extractWorkbook(readLimited(context, source.uri))
                    }
                    mime.startsWith("text/") -> extractStructuredText(
                        decodeText(readLimited(context, source.uri)), source.displayName ?: "文本文件",
                    )
                    else -> error("暂不支持这种文件格式")
                }
            }
        }
    }

    fun selectColumn(extraction: ImportExtraction, column: String): ImportExtraction {
        val tableCandidates = extraction.tables.flatMap { table ->
            CandidateClassifier.fromExtractedTable(table, column)
        }
        return extraction.copy(
            candidates = deduplicate(extraction.baseCandidates + tableCandidates),
            selectedColumn = column,
        )
    }

    private fun readLimited(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            if (descriptor.length > MAX_FILE_BYTES) error("文件超过 20 MB")
        }
        val output = ByteArrayOutputStream()
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_FILE_BYTES) error("文件超过 20 MB")
                output.write(buffer, 0, count)
            }
        } ?: error("无法打开文件")
        return output.toByteArray()
    }

    private fun checkFileSize(context: Context, uri: Uri) {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            if (descriptor.length > MAX_FILE_BYTES) error("文件超过 20 MB")
        }
    }

    private fun extractWorkbook(bytes: ByteArray): ImportExtraction {
        val formatter = DataFormatter(Locale.getDefault())
        return WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val evaluator = workbook.creationHelper.createFormulaEvaluator()
            val tables = mutableListOf<ExtractedTable>()
            val fallback = mutableListOf<ImportCandidate>()
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                if (workbook.isSheetHidden(sheetIndex) || workbook.isSheetVeryHidden(sheetIndex)) continue
                val sheet = workbook.getSheetAt(sheetIndex)
                val rows = sheet.map { row ->
                    val last = row.lastCellNum.toInt().coerceAtLeast(0)
                    (0 until last).map { formatter.formatCellValue(row.getCell(it), evaluator).trim() }
                }
                val table = CandidateClassifier.extractTable(rows, sheet.sheetName, firstRowNumber = 1)
                if (table != null) tables += table
                else fallback += CandidateClassifier.fromTable(rows, sheet.sheetName, firstRowNumber = 1)
            }
            buildExtraction(fallback, tables)
        }
    }

    private fun extractCsv(bytes: ByteArray): ImportExtraction {
        val text = decodeText(bytes)
        val first = text.lineSequence().firstOrNull().orEmpty()
        val delimiter = listOf(',', '\t', ';', '|').maxByOrNull { first.count { char -> char == it } } ?: ','
        val rows = parseDelimited(text, delimiter)
        val table = CandidateClassifier.extractTable(rows, "CSV", firstRowNumber = 1)
        return if (table != null) buildExtraction(tables = listOf(table))
        else buildExtraction(CandidateClassifier.fromTable(rows, "CSV", firstRowNumber = 1))
    }

    internal fun extractStructuredText(text: String, source: String): ImportExtraction {
        val rows = sniffStructuredRows(text)
        val table = rows?.let { CandidateClassifier.extractTable(it, source, firstRowNumber = 1) }
        return if (table != null) buildExtraction(tables = listOf(table))
        else buildExtraction(CandidateClassifier.fromLines(text.lineSequence().toList(), source))
    }

    private fun sniffStructuredRows(text: String): List<List<String>>? {
        val nonBlank = text.lineSequence().filter(String::isNotBlank).toList()
        if (nonBlank.size < 2) return null
        val candidates = buildList {
            listOf('\t', ',', ';', '|').forEach { delimiter ->
                if (nonBlank.any { it.contains(delimiter) }) add(parseDelimited(text, delimiter))
            }
            if (nonBlank.any { Regex("[\\t ]{2,}").containsMatchIn(it) }) {
                add(nonBlank.map { row -> row.trim().split(Regex("[\\t ]{2,}")).map(String::trim) })
            }
        }
        return candidates.mapNotNull { rows ->
            val populated = rows.filter { row -> row.any(String::isNotBlank) }
            val widths = populated.map { it.size }.filter { it >= 2 }
            if (widths.size < 2) null
            else {
                val width = widths.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: return@mapNotNull null
                val consistency = width.value.toDouble() / populated.size
                val hasHeader = populated.take(10).any(CandidateClassifier::hasRecognizedHeader)
                if (consistency >= 0.8 && (hasHeader || populated.size >= 3)) width.value to populated else null
            }
        }.maxWithOrNull(compareBy<Pair<Int, List<List<String>>>> { it.first }.thenBy { it.second.size })?.second
    }

    internal fun parseDelimited(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> { cell.append('"'); index++ }
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> { row.add(cell.toString()); cell.clear() }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf()
                }
                else -> cell.append(char)
            }
            index++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }

    private suspend fun extractImage(context: Context, uri: Uri, source: String): ImportExtraction {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2400) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("无法解码图片")
        return try {
            val extracted = CandidateClassifier.fromOcrText(recognize(InputImage.fromBitmap(bitmap, 0)), source)
            buildExtraction(extracted.baseCandidates, extracted.tables, extracted.notice)
        }
        finally { bitmap.recycle() }
    }

    private suspend fun extractPdf(context: Context, uri: Uri, onProgress: (String) -> Unit): ImportExtraction {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("无法打开 PDF")
        return descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount > MAX_PDF_PAGES) error("PDF 超过 30 页")
                val base = mutableListOf<ImportCandidate>()
                val tables = mutableListOf<ExtractedTable>()
                var usedFallback = false
                for (pageIndex in 0 until renderer.pageCount) {
                    coroutineContext.ensureActive()
                    onProgress("正在识别 PDF ${pageIndex + 1}/${renderer.pageCount} 页")
                    renderer.openPage(pageIndex).use { page ->
                        val scale = (2400f / maxOf(page.width, page.height)).coerceIn(0.25f, 2f)
                        val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                        try {
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val extracted = CandidateClassifier.fromOcrText(
                                recognize(InputImage.fromBitmap(bitmap, 0)),
                                "PDF 第 ${pageIndex + 1} 页",
                                preferProminentText = false,
                            )
                            base += extracted.baseCandidates
                            tables += extracted.tables
                            usedFallback = usedFallback || extracted.notice != null
                        } finally { bitmap.recycle() }
                    }
                }
                buildExtraction(base, tables, if (usedFallback) "部分页面未识别到可靠表格，已按普通阅读顺序处理" else null)
            }
        }
    }

    private suspend fun recognize(image: InputImage): RecognizedText {
        val chinese = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val primary = chinese.process(image).await().let(::recognizedText)
            if (primary.lines.any { it.text.isNotBlank() }) primary
            else latin.process(image).await().let(::recognizedText)
        } finally { chinese.close(); latin.close() }
    }

    private fun recognizedText(result: com.google.mlkit.vision.text.Text): RecognizedText {
        val lines = result.textBlocks.flatMap { it.lines }
        fun token(text: String, box: android.graphics.Rect?) = box?.let {
            OcrLine(text, it.height(), it.top, it.left, it.width())
        }
        return RecognizedText(
            lines = lines.mapNotNull { token(it.text, it.boundingBox) },
            elements = lines.flatMap { it.elements }.mapNotNull { token(it.text, it.boundingBox) },
        )
    }

    private fun extractWeb(url: String): List<ImportCandidate> {
        val document = fetchPublicDocument(url)
        val values = buildList {
            document.select("li").forEach { add(it.text()) }
            document.select("tr").forEach { row -> add(row.select("th,td").joinToString("\t") { it.text() }) }
            document.select("script[type=application/ld+json]").forEach { script ->
                val json = script.data()
                if (json.contains("ItemList", true) || json.contains("itemListElement", true)) {
                    Regex("\"name\"\\s*:\\s*\"([^\"]{1,100})\"").findAll(json).forEach { add(it.groupValues[1]) }
                }
            }
            if (isEmpty()) addAll(document.body().wholeText().lineSequence().toList())
        }
        return CandidateClassifier.fromLines(values, "网页")
    }

    private fun fetchPublicDocument(initialUrl: String): org.jsoup.nodes.Document {
        var current = initialUrl
        repeat(6) { redirectCount ->
            requirePublicHttps(current)
            val connection = URL(current).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("User-Agent", "OtterChecking/0.1 checklist-import")
                val code = connection.responseCode
                if (code in 300..399) {
                    if (redirectCount == 5) error("网页重定向次数过多")
                    val location = connection.getHeaderField("Location") ?: error("网页重定向缺少目标")
                    current = URI(current).resolve(location).toString()
                    return@repeat
                }
                require(code in 200..299) { "网页读取失败（HTTP $code）" }
                if (connection.contentLengthLong > MAX_WEB_BYTES) error("网页正文超过 2 MB")
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_WEB_BYTES) error("网页正文超过 2 MB")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                return Jsoup.parse(ByteArrayInputStream(bytes), null, current)
            } finally { connection.disconnect() }
        }
        error("网页重定向次数过多")
    }

    private fun requirePublicHttps(url: String) {
        val uri = URI(url)
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank()) { "网页导入仅支持 HTTPS" }
        InetAddress.getAllByName(uri.host).forEach { address ->
            require(!address.isAnyLocalAddress && !address.isLoopbackAddress && !address.isLinkLocalAddress && !address.isSiteLocalAddress) {
                "不能导入本地网络地址"
            }
        }
    }

    private fun decodeText(bytes: ByteArray): String {
        val content = if (bytes.size >= 3 && bytes[0] == 0xef.toByte() && bytes[1] == 0xbb.toByte() && bytes[2] == 0xbf.toByte()) bytes.copyOfRange(3, bytes.size) else bytes
        return try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content)).toString()
        } catch (_: CharacterCodingException) { content.toString(charset("GB18030")) }
    }

    private fun deduplicate(values: List<ImportCandidate>): List<ImportCandidate> {
        val seen = mutableSetOf<String>()
        return values.map { item ->
            val key = CandidateClassifier.normalizedKey(item.text)
            if (!seen.add(key)) item.copy(selected = false, duplicate = true) else item
        }.take(500)
    }

    private fun buildExtraction(
        baseCandidates: List<ImportCandidate> = emptyList(),
        tables: List<ExtractedTable> = emptyList(),
        notice: String? = null,
    ): ImportExtraction {
        val availableHeaders = tables.flatMap(ExtractedTable::headers).filter(String::isNotBlank)
        val selected = CandidateClassifier.preferredColumn(availableHeaders)
        val tableCandidates = selected?.let { column ->
            tables.flatMap { CandidateClassifier.fromExtractedTable(it, column) }
        }.orEmpty()
        val finalizedBase = deduplicate(baseCandidates)
        return ImportExtraction(
            candidates = deduplicate(finalizedBase + tableCandidates),
            tables = tables,
            selectedColumn = selected,
            notice = notice,
            baseCandidates = finalizedBase,
        )
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}

internal object CandidateClassifier {
    private val headerWords = setOf("地点", "目的地", "景点", "打卡", "行程", "名称", "place", "destination", "location")
    private val tableHeaderWords = setOf(
        "时间", "日期", "出发", "出发地", "目的地", "地点", "名称", "景点",
        "里程", "距离", "车行耗时", "旅行耗时", "游玩耗时", "总时长", "备注",
    )
    private val preferredTableHeaders = listOf("目的地", "地点", "景点", "名称")
    private val prefix = Regex("^\\s*(?:[-*•·□☐☑✓]|\\d{1,3}[.)、．](?!\\d)|[（(]?[一二三四五六七八九十]+[)）、.．])\\s*")
    private val dateOnly = Regex("^\\d{2,4}[-/.年]\\d{1,2}(?:[-/.月]\\d{1,2}日?)?(?:周[一二三四五六日天])?$|^\\d{1,2}:\\d{2}$|^\\d{1,2}\\.\\d{1,2}周[一二三四五六日天]$|^\\d{1,2}周[一二三四五六日天]$")
    private val priceOnly = Regex("^(?:¥|￥)?\\s*\\d+(?:\\.\\d{1,2})?\\s*(?:元|rmb)?$", RegexOption.IGNORE_CASE)
    private val pageOnly = Regex("^(?:第\\s*)?\\d+\\s*(?:页|/\\s*\\d+)?$")
    private val screenshotNoise = Regex("^(?:总览|添加备注|备注\\d*|添加(?:\\s*\\d+\\s*条.*)?|编辑|问AI|吃喝|其他|住宿|活动|交通|餐饮|景点|[0-9０-９。、~～·+中不]{1,8})$", RegexOption.IGNORE_CASE)
    private val routeSummary = Regex(".*\\d+(?:\\.\\d+)?\\s*公里.*(?:分钟|小时).*")
    private val screenshotFile = Regex("^(?:screenshot|img|wx_camera)[-_].*", RegexOption.IGNORE_CASE)
    private val placeHint = Regex("(?:Cafe|Coffee|酒店|民宿|客栈|餐厅|咖啡|景区|公园|博物馆|广场|车站|机场|大学|寺|街|路|山|湖|店|馆|园|城|村)$", RegexOption.IGNORE_CASE)
    private val itineraryCategory = Regex("^(?:吃喝|其他|住宿|活动|交通|餐饮|景点)$")

    fun fromLines(lines: List<String>, source: String): List<ImportCandidate> = lines.mapIndexedNotNull { index, value ->
        candidate(value, "$source · 第 ${index + 1} 行", index)
    }

    fun fromOcrText(text: RecognizedText, source: String, preferProminentText: Boolean = true): ImportExtraction {
        val table = extractOcrTable(text.elements.ifEmpty { text.lines }, source)
        if (table != null) {
            val selected = preferredColumn(table.headers)
            return ImportExtraction(
                candidates = selected?.let { fromExtractedTable(table, it) }.orEmpty(),
                tables = listOf(table),
                selectedColumn = selected,
                baseCandidates = emptyList(),
            )
        }
        val candidates = posterCandidates(text.lines, source, preferProminentText)
        return ImportExtraction(
            candidates = candidates,
            notice = "未识别到可靠表格，已按普通阅读顺序处理",
            baseCandidates = candidates,
        )
    }

    fun fromOcrLines(lines: List<OcrLine>, source: String, preferProminentText: Boolean = true): List<ImportCandidate> =
        fromOcrText(RecognizedText(lines, lines), source, preferProminentText).candidates

    private fun posterCandidates(lines: List<OcrLine>, source: String, preferProminentText: Boolean): List<ImportCandidate> {
        if (lines.isEmpty()) return emptyList()
        val heights = lines.map { it.height }.filter { it > 0 }.sorted()
        val median = heights.getOrNull(heights.size / 2)?.toDouble() ?: 1.0
        val hasVisualHierarchy = preferProminentText && (heights.maxOrNull() ?: 0) >= median * 1.2
        val itineraryStart = lines.filter { itineraryCategory.matches(it.text.trim()) }.minOfOrNull(OcrLine::top)
        return lines.sortedWith(compareBy(OcrLine::centerY, OcrLine::centerX)).mapIndexedNotNull { index, line ->
            if (itineraryStart != null && line.top < itineraryStart - median * 2) return@mapIndexedNotNull null
            val cleaned = clean(line.text)
            val prominent = !hasVisualHierarchy || line.height >= median * 1.15
            val placeLike = placeHint.containsMatchIn(cleaned) || prefix.containsMatchIn(line.text)
            if (!prominent && !placeLike) return@mapIndexedNotNull null
            candidate(line.text, "$source · 文字区域 ${index + 1}", index, if (prominent) 2 else 0)
        }
    }

    /**
     * ML Kit does not promise reading order between cells, so table screenshots must not use
     * text-block order. Rebuild visual rows first, then read a semantically useful column from
     * top to bottom. A null result means this does not look sufficiently like a table.
     */
    private fun extractOcrTable(lines: List<OcrLine>, source: String): ExtractedTable? {
        val heights = lines.map { it.height }.filter { it > 0 }.sorted()
        val medianHeight = heights.getOrNull(heights.size / 2)?.toDouble() ?: return null
        val rows = clusterVisualRows(lines, maxOf(8.0, medianHeight * 0.75))
        val headerRowIndex = rows.indices.maxByOrNull { index ->
            rows[index].count { tableHeader(it.text) != null }
        } ?: return null
        val headerCells = rows[headerRowIndex]
            .mapNotNull { line -> tableHeader(line.text)?.let { label -> label to line } }
            .sortedBy { it.second.centerX }
        if (headerCells.map { it.first }.distinct().size < 2) return null

        if (preferredColumn(headerCells.map { it.first }) == null) return null
        val boundaries = headerCells.indices.map { index ->
            val center = headerCells[index].second.centerX
            val left = headerCells.getOrNull(index - 1)?.let { (it.second.centerX + center) / 2.0 } ?: Double.NEGATIVE_INFINITY
            val right = headerCells.getOrNull(index + 1)?.let { (center + it.second.centerX) / 2.0 } ?: Double.POSITIVE_INFINITY
            left to right
        }
        val extractedRows = rows.drop(headerRowIndex + 1).mapIndexed { rowIndex, row ->
            val cells = boundaries.map { (left, right) ->
                row.filter { it.centerX >= left && it.centerX < right }
                    .sortedBy(OcrLine::left)
                    .joinToString(" ") { it.text }
                    .trim()
            }
            ExtractedTableRow(rowNumber = headerRowIndex + rowIndex + 2, cells = cells)
        }
        return ExtractedTable(source, headerCells.map { it.first }, extractedRows)
    }

    private fun clusterVisualRows(lines: List<OcrLine>, tolerance: Double): List<List<OcrLine>> {
        val rows = mutableListOf<MutableList<OcrLine>>()
        lines.sortedWith(compareBy(OcrLine::centerY, OcrLine::centerX)).forEach { line ->
            val row = rows.lastOrNull()
            val rowCenter = row?.map(OcrLine::centerY)?.average()
            if (row == null || rowCenter == null || kotlin.math.abs(line.centerY - rowCenter) > tolerance) {
                rows += mutableListOf(line)
            } else {
                row += line
            }
        }
        return rows.map { it.sortedBy(OcrLine::centerX) }
    }

    private fun tableHeader(value: String): String? {
        val key = normalizedKey(value)
        return tableHeaderWords.firstOrNull { header -> key == normalizedKey(header) }
    }

    fun hasRecognizedHeader(row: List<String>): Boolean = row.any { tableHeader(it) != null }

    fun preferredColumn(headers: List<String>): String? = preferredTableHeaders.firstNotNullOfOrNull { preferred ->
        headers.firstOrNull { normalizedKey(it) == normalizedKey(preferred) }
    }

    fun extractTable(rows: List<List<String>>, source: String, firstRowNumber: Int): ExtractedTable? {
        val headerRow = rows.take(10).indexOfFirst { row ->
            row.count { tableHeader(it) != null } >= 1 && preferredColumn(row) != null
        }
        if (headerRow < 0) return null
        val width = rows[headerRow].size
        val headers = rows[headerRow].mapIndexed { index, value -> value.trim().ifBlank { "第 ${index + 1} 列" } }
        val data = rows.drop(headerRow + 1).mapIndexed { index, row ->
            ExtractedTableRow(
                rowNumber = firstRowNumber + headerRow + index + 1,
                cells = (0 until width).map { row.getOrNull(it).orEmpty().trim() },
            )
        }
        return ExtractedTable(source, headers, data)
    }

    fun fromExtractedTable(table: ExtractedTable, column: String): List<ImportCandidate> {
        val columnIndex = table.headers.indexOfFirst { normalizedKey(it) == normalizedKey(column) }
        if (columnIndex < 0) return emptyList()
        val header = table.headers[columnIndex]
        return table.rows.mapIndexedNotNull { index, row ->
            candidate(
                row.cells.getOrNull(columnIndex).orEmpty(),
                "${table.source} · 第 ${row.rowNumber} 行 · ${header}列",
                index,
                scoreBoost = 1,
            )
        }
    }

    fun fromTable(rows: List<List<String>>, source: String, firstRowNumber: Int): List<ImportCandidate> {
        extractTable(rows, source, firstRowNumber)?.let { table ->
            preferredColumn(table.headers)?.let { return fromExtractedTable(table, it) }
        }
        val headerRow = rows.take(10).indexOfFirst(::hasRecognizedHeader)
        return rows.mapIndexedNotNull { rowIndex, row ->
            if (rowIndex == headerRow) return@mapIndexedNotNull null
            val value = row.filter(String::isNotBlank).maxByOrNull(::score).orEmpty()
            candidate(value, "$source · 第 ${rowIndex + firstRowNumber} 行", rowIndex)
        }
    }

    private fun candidate(raw: String, source: String, index: Int, scoreBoost: Int = 0): ImportCandidate? {
        val text = clean(raw)
        if (text.isBlank() || text.length > 100 || shouldReject(text) || normalizedKey(text) in headerWords) return null
        val score = score(raw) + scoreBoost
        if (score < 0) return null
        val confidence = when { score >= 4 -> ImportConfidence.HIGH; score >= 2 -> ImportConfidence.MEDIUM; else -> ImportConfidence.LOW }
        return ImportCandidate("${source.hashCode()}-$index-${text.hashCode()}", text, source, confidence)
    }

    private fun score(value: String): Int {
        val text = value.trim()
        var score = 0
        if (prefix.containsMatchIn(text)) score += 3
        if (text.length in 2..30) score += 2 else if (text.length <= 60) score += 1
        if (text.any { it.isLetter() }) score += 1
        if (text.contains(Regex("景区|公园|博物馆|广场|酒店|车站|机场|大学|寺|街|路|山|湖|店|馆|园|城|村|place|park|museum", RegexOption.IGNORE_CASE))) score += 2
        if (shouldReject(text)) score -= 10
        if (text.count { it == '，' || it == ',' || it == '。' } >= 3) score -= 2
        return score
    }

    private fun shouldReject(text: String): Boolean = dateOnly.matches(text) || priceOnly.matches(text) || pageOnly.matches(text) ||
        screenshotNoise.matches(text) || routeSummary.matches(text) || screenshotFile.matches(text) ||
        text.matches(Regex("https?://\\S+", RegexOption.IGNORE_CASE)) || text.startsWith("合计") || text.startsWith("总计")

    private fun clean(value: String): String = prefix.replace(value.trim(), "").replace(Regex("\\s+"), " ").trim(' ', '|', '｜')
    fun normalizedKey(value: String): String = clean(value).lowercase(Locale.ROOT).replace(Regex("[\\p{Punct}，。；：、\u3000\\s]"), "")
}
