package cn.microvideo.novel.plugin.service

import cn.microvideo.novel.plugin.model.Book
import cn.microvideo.novel.plugin.parser.BookParser
import cn.microvideo.novel.plugin.parser.TxtBookParser
import java.io.File

/**
 * 书籍管理器：解析、缓存、进度保存
 */
class BookService {

    private val parsers = mutableListOf<BookParser>(TxtBookParser())
    private var currentBook: Book? = null

    fun openBook(file: File): Book? {
        val parser = parsers.find { it.support(file) } ?: return null
        val book = parser.parse(file)
        currentBook = book
        return book
    }

    fun currentBook(): Book? = currentBook

    fun registerParser(parser: BookParser) {
        parsers.add(parser)
    }
}
