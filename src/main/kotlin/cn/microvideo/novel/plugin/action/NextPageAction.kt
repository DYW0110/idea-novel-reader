package cn.microvideo.novel.plugin.action

import cn.microvideo.novel.plugin.ui.ReaderPanelHolder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class NextPageAction : AnAction("Next Page") {

    override fun actionPerformed(e: AnActionEvent) {
        ReaderPanelHolder.get()?.nextPage()
    }
}
