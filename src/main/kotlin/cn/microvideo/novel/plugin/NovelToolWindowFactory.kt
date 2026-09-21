package cn.microvideo.novel.plugin

import cn.microvideo.novel.plugin.model.Book
import cn.microvideo.novel.plugin.service.BookService
import cn.microvideo.novel.plugin.state.NovelConfigState
import cn.microvideo.novel.plugin.ui.DrawerCatalogPanel
import cn.microvideo.novel.plugin.ui.ReaderPanel
import cn.microvideo.novel.plugin.ui.ReaderPanelHolder
import cn.microvideo.novel.plugin.ui.SettingsDialog
import cn.microvideo.novel.plugin.ui.TopNavigationBar
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 小说阅读工具窗口工厂
 */
class NovelToolWindowFactory : ToolWindowFactory {

    private val bookService = BookService()
    private lateinit var readerPanel: ReaderPanel
    private lateinit var drawerCatalogPanel: DrawerCatalogPanel
    private lateinit var topBar: TopNavigationBar
    private lateinit var mainContainer: JPanel

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        try {
            val rootPanel = createMainPanel(project)
            val content = ContentFactory.getInstance().createContent(rootPanel, "", false)
            toolWindow.contentManager.addContent(content)
        } catch (e: Throwable) {
            val errorPanel = JPanel(BorderLayout()).apply {
                add(javax.swing.JLabel("Novel Reader 加载失败: ${e.message}", javax.swing.SwingConstants.CENTER), BorderLayout.CENTER)
            }
            val content = ContentFactory.getInstance().createContent(errorPanel, "", false)
            toolWindow.contentManager.addContent(content)
        }
    }

    private fun createMainPanel(project: Project): JComponent {
        mainContainer = JPanel(BorderLayout())

        readerPanel = ReaderPanel()
        val configState = NovelConfigState.getInstance()
        val currentConfig = configState.toModel()
        readerPanel.applyConfig(currentConfig)

        drawerCatalogPanel = DrawerCatalogPanel(
            onChapterSelected = { index ->
                readerPanel.jumpToChapter(index)
                drawerCatalogPanel.isVisible = false
                mainContainer.revalidate()
                mainContainer.repaint()
            },
            onClose = {
                drawerCatalogPanel.isVisible = false
                mainContainer.revalidate()
                mainContainer.repaint()
            }
        ).apply {
            isVisible = false
        }

        topBar = TopNavigationBar(
            onOpenBook = { openBook(project) },
            onToggleCatalog = { toggleCatalog() },
            onPrevChapter = { readerPanel.previousChapter() },
            onNextChapter = { readerPanel.nextChapter() },
            onOpenSettings = { openSettingsDialog(project) },
            onToggleCamouflage = { toggleCamouflage() }
        )

        // 注册全局动作分发
        ReaderPanelHolder.register(
            panel = readerPanel,
            onToggleToolbar = { },
            onToggleCatalog = { toggleCatalog() },
            onToggleCamouflage = { toggleCamouflage() },
            onOpenBook = { openBook(project) }
        )

        // 联动与事件回调
        readerPanel.onChapterChange = { index ->
            drawerCatalogPanel.selectChapter(index)
        }

        readerPanel.onProgressChange = { chapterTitle, percent ->
            topBar.updateProgress(chapterTitle, percent)
        }

        readerPanel.onOpenBookRequested = {
            openBook(project)
        }

        readerPanel.onFileDropped = { file ->
            parseAndLoadBook(project, file)
        }

        // 布局装配：顶部导航栏，中间内容区（左侧可折叠目录 + 右侧正文阅读区）
        val centerPanel = JPanel(BorderLayout())
        centerPanel.add(drawerCatalogPanel, BorderLayout.WEST)
        centerPanel.add(readerPanel, BorderLayout.CENTER)

        mainContainer.add(topBar, BorderLayout.NORTH)
        mainContainer.add(centerPanel, BorderLayout.CENTER)

        loadLastBook(project)
        return mainContainer
    }

    private fun toggleCatalog() {
        val show = !drawerCatalogPanel.isVisible
        drawerCatalogPanel.isVisible = show
        if (show) {
            drawerCatalogPanel.focusSearch()
        }
        mainContainer.revalidate()
        mainContainer.repaint()
    }

    private fun toggleCamouflage() {
        val isCamouflaged = readerPanel.toggleCamouflage()
        topBar.setCamouflaged(isCamouflaged)
    }

    private fun openSettingsDialog(project: Project) {
        val state = NovelConfigState.getInstance()
        val dialog = SettingsDialog(project, state.toModel()) { newConfig ->
            state.fromModel(newConfig)
            readerPanel.applyConfig(newConfig)
        }
        dialog.show()
    }

    private fun openBook(project: Project) {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("选择小说文件")
            .withFileFilter { file -> file.extension?.lowercase() in listOf("txt") }

        FileChooser.chooseFile(descriptor, project, null)?.let { virtualFile ->
            val file = File(virtualFile.path)
            parseAndLoadBook(project, file)
        }
    }

    private fun parseAndLoadBook(project: Project, file: File) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在解析小说...", false) {
            private var parsedBook: Book? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "正在读取并解析: ${file.name}"
                parsedBook = bookService.openBook(file)
            }

            override fun onSuccess() {
                val book = parsedBook
                if (book != null) {
                    drawerCatalogPanel.setBook(book, 0)
                    readerPanel.loadBook(book, 0, 0)
                } else {
                    Messages.showErrorDialog(project, "无法解析该小说文件，请检查文件格式。", "错误")
                }
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(project, "解析小说失败: ${error.message}", "错误")
            }
        })
    }

    private fun loadLastBook(project: Project) {
        val path = NovelConfigState.getInstance().getLastBookPath()
        if (path.isNotBlank()) {
            val file = File(path)
            if (file.exists()) {
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在加载上次阅读...", false) {
                    private var parsedBook: Book? = null
                    private var chapter = 0
                    private var offset = 0

                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true
                        indicator.text = "正在恢复: ${file.name}"
                        parsedBook = bookService.openBook(file)
                        val progress = NovelConfigState.getInstance().getLastProgress()
                        chapter = progress.first
                        offset = progress.second
                    }

                    override fun onSuccess() {
                        val book = parsedBook
                        if (book != null) {
                            drawerCatalogPanel.setBook(book, chapter)
                            readerPanel.loadBook(book, chapter, offset)
                        }
                    }
                })
            }
        }
    }
}
