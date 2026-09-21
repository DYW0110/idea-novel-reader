package cn.microvideo.novel.plugin.action

import cn.microvideo.novel.plugin.ui.ReaderPanelHolder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ToggleCatalogAction : AnAction("Toggle Novel Catalog") {

    override fun actionPerformed(e: AnActionEvent) {
        ReaderPanelHolder.toggleCatalog()
    }
}
