package cn.microvideo.novel.plugin.config

import cn.microvideo.novel.plugin.model.NovelConfig
import cn.microvideo.novel.plugin.state.NovelConfigState
import cn.microvideo.novel.plugin.ui.ReadConfigPanel
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * IDEA 设置面板入口
 */
class NovelConfigurable : Configurable {

    private var panel: ReadConfigPanel? = null

    override fun getDisplayName(): String = "Novel Reader"

    override fun createComponent(): JComponent {
        val state = NovelConfigState.getInstance()
        val config = state.toModel()
        panel = ReadConfigPanel(onConfigChange = { })
        panel?.setConfig(config)
        return panel!!
    }

    override fun isModified(): Boolean {
        return true
    }

    override fun apply() {
        panel?.let {
            val current = it.getCurrentConfig()
            NovelConfigState.getInstance().fromModel(current)
            cn.microvideo.novel.plugin.ui.ReaderPanelHolder.get()?.applyConfig(current)
        }
    }

    override fun reset() {
        panel?.setConfig(NovelConfigState.getInstance().toModel())
    }
}
