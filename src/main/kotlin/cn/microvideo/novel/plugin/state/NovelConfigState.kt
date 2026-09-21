package cn.microvideo.novel.plugin.state

import cn.microvideo.novel.plugin.model.NovelConfig
import cn.microvideo.novel.plugin.model.ThemePreset
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * IDEA 持久化配置服务
 */
@State(
    name = "NovelConfigState",
    storages = [Storage("novel-reader.xml")]
)
class NovelConfigState : PersistentStateComponent<NovelConfigState.State> {

    data class State(
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
        var autoLoadLast: Boolean = true,
        var lastBookPath: String = "",
        var lastChapterIndex: Int = 0,
        var lastCharOffset: Int = 0
    )

    private var state: State = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun toModel(): NovelConfig {
        return NovelConfig(
            themePreset = state.themePreset,
            fontName = state.fontName,
            fontSize = state.fontSize,
            fontColor = state.fontColor,
            backgroundColor = state.backgroundColor,
            lineSpacingMultiplier = state.lineSpacingMultiplier,
            paragraphSpacingMultiplier = state.paragraphSpacingMultiplier,
            maxWidth = state.maxWidth,
            indentFirstLine = state.indentFirstLine,
            camouflageMode = state.camouflageMode,
            camouflageType = state.camouflageType,
            autoLoadLast = state.autoLoadLast
        )
    }

    fun fromModel(config: NovelConfig) {
        state.themePreset = config.themePreset
        state.fontName = config.fontName
        state.fontSize = config.fontSize
        state.fontColor = config.fontColor
        state.backgroundColor = config.backgroundColor
        state.lineSpacingMultiplier = config.lineSpacingMultiplier
        state.paragraphSpacingMultiplier = config.paragraphSpacingMultiplier
        state.maxWidth = config.maxWidth
        state.indentFirstLine = config.indentFirstLine
        state.camouflageMode = config.camouflageMode
        state.camouflageType = config.camouflageType
        state.autoLoadLast = config.autoLoadLast
    }

    fun getLastBookPath(): String = state.lastBookPath
    fun setLastBookPath(path: String) {
        state.lastBookPath = path
    }

    fun getLastProgress(): Pair<Int, Int> = Pair(state.lastChapterIndex, state.lastCharOffset)
    fun setLastProgress(chapterIndex: Int, charOffset: Int) {
        state.lastChapterIndex = chapterIndex
        state.lastCharOffset = charOffset
    }

    companion object {
        fun getInstance(): NovelConfigState {
            return ApplicationManager.getApplication().getService(NovelConfigState::class.java)
        }
    }
}
