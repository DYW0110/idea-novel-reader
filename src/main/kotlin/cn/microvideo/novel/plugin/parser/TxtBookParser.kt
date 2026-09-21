package cn.microvideo.novel.plugin.parser

import cn.microvideo.novel.plugin.model.Book
import cn.microvideo.novel.plugin.model.Chapter
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.regex.Pattern

/**
 * TXT 小说解析器
 * 自动识别章节标题并分章
 */
class TxtBookParser : BookParser {

    private val chapterPatterns = listOf(
        Pattern.compile("^\\s*(第[零一二三四五六七八九十百千万\\d]+章\\s*.*)$", Pattern.MULTILINE),
        Pattern.compile("^\\s*(第[零一二三四五六七八九十百千万\\d]+节\\s*.*)$", Pattern.MULTILINE),
        Pattern.compile("^\\s*(第[零一二三四五六七八九十百千万\\d]+卷\\s*.*)$", Pattern.MULTILINE),
        Pattern.compile("^\\s*(Chapter\\s*\\d+.*)$", Pattern.MULTILINE or Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*(\\d+\\s*[、.]\\s*.*)$", Pattern.MULTILINE)
    )

    override fun support(file: File): Boolean {
        return file.extension.equals("txt", ignoreCase = true)
    }

    override fun parse(file: File): Book {
        val charset = detectCharset(file)
        val text = file.readText(charset)
        val chapters = splitChapters(text)
        return Book(
            title = file.nameWithoutExtension,
            path = file.absolutePath,
            chapters = chapters
        )
    }

    private fun splitChapters(text: String): List<Chapter> {
        val lines = text.lines()
        val chapters = mutableListOf<Chapter>()
        var current: Chapter? = null
        var index = 0

        for (line in lines) {
            val title = extractChapterTitle(line)
            if (title != null) {
                if (current != null && current.text.isNotBlank()) {
                    chapters.add(current)
                }
                index++
                current = Chapter(index = index, title = title.trim())
                current.append(line.trim())
            } else {
                if (current == null) {
                    index++
                    current = Chapter(index = index, title = "序章/前言")
                }
                current.append(line)
            }
        }

        if (current != null && current.text.isNotBlank()) {
            chapters.add(current)
        }

        if (chapters.isEmpty()) {
            chapters.add(Chapter(1, "正文", StringBuilder(text)))
        }

        return chapters
    }

    private fun extractChapterTitle(line: String): String? {
        for (pattern in chapterPatterns) {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }

    private fun detectCharset(file: File): Charset {
        val bytes = file.readBytes()
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Charsets.UTF_8
        }
        return try {
            val utf8 = String(bytes, Charsets.UTF_8)
            if (utf8.contains("\uFFFD")) {
                Charset.forName("GBK")
            } else {
                Charsets.UTF_8
            }
        } catch (e: Exception) {
            Charset.forName("GBK")
        }
    }
}
