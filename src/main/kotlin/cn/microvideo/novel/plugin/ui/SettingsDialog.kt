package cn.microvideo.novel.plugin.ui

import cn.microvideo.novel.plugin.model.NovelConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

/**
 * 悬浮设置弹窗
 */
class SettingsDialog(
    project: Project?,
    private val initialConfig: NovelConfig,
    private val onSave: (NovelConfig) -> Unit
) : DialogWrapper(project, true) {

    private lateinit var panel: ReadConfigPanel

    init {
        title = "阅读设置"
        setOKButtonText("保存并应用")
        setCancelButtonText("取消")
        init()
    }

    override fun createCenterPanel(): JComponent {
        panel = ReadConfigPanel(
            onConfigChange = { cfg ->
                onSave(cfg)
            },
            onClose = {
                close(OK_EXIT_CODE)
            }
        )
        panel.setConfig(initialConfig)
        return panel
    }

    override fun doOKAction() {
        val cfg = panel.getCurrentConfig()
        onSave(cfg)
        super.doOKAction()
    }
}
