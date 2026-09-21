package cn.microvideo.novel.plugin.model

/**
 * 小说书籍数据模型
 */
data class Book(
    val title: String,
    val path: String,
    val chapters: List<Chapter> = emptyList()
) {
    fun totalChapters(): Int = chapters.size

    fun totalChars(): Long = chapters.sumOf { it.text.length.toLong() }
}
