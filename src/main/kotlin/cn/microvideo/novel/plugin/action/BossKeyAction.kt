package cn.microvideo.novel.plugin.action

import cn.microvideo.novel.plugin.ui.ReaderPanelHolder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class BossKeyAction : AnAction("Boss Key") {

    override fun actionPerformed(e: AnActionEvent) {
        ReaderPanelHolder.toggleCamouflage()
    }
}
