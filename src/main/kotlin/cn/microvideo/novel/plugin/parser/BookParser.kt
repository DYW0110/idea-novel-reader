package cn.microvideo.novel.plugin.parser

import cn.microvideo.novel.plugin.model.Book
import java.io.File

/**
 * 书籍解析器接口
 */
interface BookParser {

    /**
     * 是否支持该文件
     */
    fun support(file: File): Boolean

    /**
     * 解析书籍
     */
    fun parse(file: File): Book
}
