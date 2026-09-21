package cn.microvideo.novel.plugin.model

import java.awt.Color
import java.awt.Font

enum class ThemePreset(val displayName: String, val fgHex: String, val bgHex: String) {
    FOLLOW_IDE("跟随 IDE", "", ""),
    PARCHMENT("羊皮纸", "#3B2E2A", "#F4ECD8"),
    DARK_CODE("深色代码", "#A9B7C6", "#2B2B2B"),
    PURE_BLACK("纯黑极简", "#999999", "#000000"),
    CUSTOM("自定义", "#C7C7C7", "#2B2B2B");

    companion object {
        fun fromName(name: String): ThemePreset = entries.find { it.name.equals(name, true) } ?: FOLLOW_IDE
    }
}

/**
 * 阅读配置数据类，持久化到 IDEA 配置中
 */
data class NovelConfig(
    var themePreset: String = ThemePreset.FOLLOW_IDE.name,
    var fontName: String = "Microsoft YaHei",
    var fontSize: Int = 16,
    var fontColor: String = "#A9B7C6",
    var backgroundColor: String = "#2B2B2B",
    var lineSpacingMultiplier: Float = 1.6f,
    var paragraphSpacingMultiplier: Float = 1.0f,
    var maxWidth: Int = 800,
    var indentFirstLine: Boolean = true,
    var camouflageMode: Boolean = false,
    var camouflageType: String = "CODE_LOG",
    var autoLoadLast: Boolean = true
) {
    companion object {
        fun colorToHex(color: Color): String {
            return String.format("#%02X%02X%02X", color.red, color.green, color.blue)
        }
    }

    fun getFont(): Font {
        return Font(fontName, Font.PLAIN, fontSize)
    }
}
