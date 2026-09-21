package cn.microvideo.novel.plugin.model

/**
 * 小说章节数据模型
 */
data class Chapter(
    val index: Int,
    val title: String,
    val content: StringBuilder = StringBuilder()
) {
    val text: String
        get() = content.toString()

    fun append(line: String) {
        content.append(line).append("\n")
    }
}
