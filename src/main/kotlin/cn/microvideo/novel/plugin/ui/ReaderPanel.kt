package cn.microvideo.novel.plugin.ui

import cn.microvideo.novel.plugin.model.Book
import cn.microvideo.novel.plugin.model.Chapter
import cn.microvideo.novel.plugin.model.NovelConfig
import cn.microvideo.novel.plugin.model.ThemePreset
import cn.microvideo.novel.plugin.state.NovelConfigState
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.datatransfer.DataFlavor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.AdjustmentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

import javax.swing.ScrollPaneConstants

/**
 * 核心正文阅读区
 */
class ReaderPanel : JPanel(BorderLayout()) {

    private val textPane = object : JTextPane() {
        override fun getScrollableTracksViewportWidth(): Boolean {
            return true
        }
    }
    private val contentWrapper = JPanel(BorderLayout())
    private val scrollPane: JBScrollPane
    private val emptyPanel = JPanel(GridBagLayout())
    private val bottomBar = JPanel(FlowLayout(FlowLayout.CENTER, 12, 12))

    private var book: Book? = null
    private var currentChapterIndex: Int = 0
    private var config: NovelConfig = NovelConfig()

    var onChapterChange: ((Int) -> Unit)? = null
    var onProgressChange: ((chapterTitle: String, percent: Float) -> Unit)? = null
    var onOpenBookRequested: (() -> Unit)? = null
    var onFileDropped: ((File) -> Unit)? = null

    init {
        textPane.isEditable = false
        textPane.isFocusable = true

        // 底部翻章引导栏
        setupBottomBar()

        contentWrapper.add(textPane, BorderLayout.CENTER)
        contentWrapper.add(bottomBar, BorderLayout.SOUTH)

        scrollPane = JBScrollPane(contentWrapper).apply {
            border = BorderFactory.createEmptyBorder()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = 28
        }

        // 初始化空状态引导面板
        setupEmptyPanel()

        // 默认显示空状态
        add(emptyPanel, BorderLayout.CENTER)

        // 文件拖拽支持 (Drag and Drop)
        setupDragAndDrop()

        // 键盘快捷翻页监听
        textPane.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_SPACE -> {
                        scrollPage(1)
                        e.consume()
                    }
                    KeyEvent.VK_PAGE_UP -> {
                        scrollPage(-1)
                        e.consume()
                    }
                    KeyEvent.VK_UP -> {
                        if (e.isAltDown) {
                            previousChapter()
                            e.consume()
                        }
                    }
                    KeyEvent.VK_DOWN -> {
                        if (e.isAltDown) {
                            nextChapter()
                            e.consume()
                        }
                    }
                }
            }
        })

        // 滚动条进度保存监听
        scrollPane.verticalScrollBar.addAdjustmentListener { e: AdjustmentEvent ->
            if (!e.valueIsAdjusting) {
                updateProgressInfo()
            }
        }

        // 窗口缩放自适应正文内边距
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent?) {
                updateTextMargins()
            }
        })

        refreshStyles()
    }

    private fun setupBottomBar() {
        bottomBar.isOpaque = false
        bottomBar.removeAll()

        val prevBtn = JButton("< 上一章").apply {
            font = Font("Microsoft YaHei", Font.PLAIN, 12)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { previousChapter() }
        }

        val topBtn = JButton("回到顶部").apply {
            font = Font("Microsoft YaHei", Font.PLAIN, 12)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener {
                scrollPane.verticalScrollBar.value = 0
            }
        }

        val nextBtn = JButton("进入下一章 >").apply {
            font = Font("Microsoft YaHei", Font.PLAIN, 12)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { nextChapter() }
        }

        bottomBar.add(prevBtn)
        bottomBar.add(topBtn)
        bottomBar.add(nextBtn)
    }

    private fun setupEmptyPanel() {
        emptyPanel.removeAll()
        val c = GridBagConstraints().apply {
            gridx = 0
            gridy = GridBagConstraints.RELATIVE
            insets = Insets(8, 8, 8, 8)
            anchor = GridBagConstraints.CENTER
        }

        val titleLabel = JLabel("开始你的阅读之旅").apply {
            font = Font("Microsoft YaHei", Font.BOLD, 18)
            foreground = Color(0xDD, 0xDD, 0xDD)
            horizontalAlignment = SwingConstants.CENTER
        }

        val tipLabel = JLabel("支持 TXT 格式，可点击下方按钮或直接将文件拖拽至此处").apply {
            font = Font("Microsoft YaHei", Font.PLAIN, 13)
            foreground = Color(0x88, 0x88, 0x88)
            horizontalAlignment = SwingConstants.CENTER
        }

        val openBtn = JButton("打开本地小说文件").apply {
            font = Font("Microsoft YaHei", Font.PLAIN, 14)
            preferredSize = Dimension(200, 38)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { onOpenBookRequested?.invoke() }
        }

        emptyPanel.add(titleLabel, c)
        emptyPanel.add(tipLabel, c)
        emptyPanel.add(Box.createVerticalStrut(10), c)
        emptyPanel.add(openBtn, c)
    }

    private fun setupDragAndDrop() {
        val dropTargetListener = object : DropTargetAdapter() {
            override fun drop(dtde: DropTargetDropEvent) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    val transferable = dtde.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                        val target = files?.firstOrNull { it.extension.equals("txt", ignoreCase = true) }
                        if (target != null) {
                            onFileDropped?.invoke(target)
                            dtde.dropComplete(true)
                            return
                        }
                    }
                    dtde.dropComplete(false)
                } catch (e: Exception) {
                    dtde.dropComplete(false)
                }
            }
        }
        DropTarget(this, dropTargetListener)
        DropTarget(textPane, dropTargetListener)
        DropTarget(contentWrapper, dropTargetListener)
        DropTarget(emptyPanel, dropTargetListener)
    }

    fun nextPage() {
        scrollPage(1)
    }

    fun previousPage() {
        scrollPage(-1)
    }

    private fun scrollPage(direction: Int) {
        val scrollBar = scrollPane.verticalScrollBar
        val pageSize = (scrollBar.visibleAmount * 0.9).toInt()
        val newValue = scrollBar.value + direction * pageSize

        if (newValue >= scrollBar.maximum - scrollBar.visibleAmount && direction > 0) {
            nextChapter()
        } else if (newValue <= 0 && direction < 0) {
            previousChapter(toBottom = true)
        } else {
            scrollBar.value = newValue
        }
    }

    fun applyConfig(config: NovelConfig) {
        this.config = config
        refreshStyles()
        updateTextMargins()
        renderCurrentChapter()
    }

    private fun updateTextMargins() {
        val containerWidth = width.coerceAtLeast(100)
        val maxContentWidth = config.maxWidth.coerceIn(400, 1400)
        val sidePadding = if (containerWidth > maxContentWidth) {
            ((containerWidth - maxContentWidth) / 2).coerceAtLeast(20)
        } else {
            18
        }
        val topBottomPadding = if (config.camouflageMode) 8 else 20
        textPane.border = BorderFactory.createEmptyBorder(topBottomPadding, sidePadding, topBottomPadding, sidePadding)
        textPane.margin = Insets(0, 0, 0, 0)
        textPane.revalidate()
    }

    private fun refreshStyles() {
        val colors = resolveColors()
        val fg = colors.first
        val bg = colors.second

        background = bg
        emptyPanel.background = bg
        contentWrapper.background = bg
        scrollPane.background = bg
        scrollPane.viewport.background = bg
        bottomBar.background = bg

        textPane.background = bg
        textPane.foreground = fg
        textPane.caretColor = fg
        textPane.font = if (config.camouflageMode) {
            Font("JetBrains Mono", Font.PLAIN, 13)
        } else {
            config.getFont()
        }

        bottomBar.isVisible = !config.camouflageMode
    }

    private fun resolveColors(): Pair<Color, Color> {
        if (config.camouflageMode) {
            return Pair(Color(0xA9, 0xB7, 0xC6), Color(0x2B, 0x2B, 0x2B))
        }

        val preset = ThemePreset.fromName(config.themePreset)
        return when (preset) {
            ThemePreset.PARCHMENT -> Pair(Color.decode(ThemePreset.PARCHMENT.fgHex), Color.decode(ThemePreset.PARCHMENT.bgHex))
            ThemePreset.DARK_CODE -> Pair(Color.decode(ThemePreset.DARK_CODE.fgHex), Color.decode(ThemePreset.DARK_CODE.bgHex))
            ThemePreset.PURE_BLACK -> Pair(Color.decode(ThemePreset.PURE_BLACK.fgHex), Color.decode(ThemePreset.PURE_BLACK.bgHex))
            ThemePreset.CUSTOM -> {
                val fg = parseColor(config.fontColor, Color(0xC7, 0xC7, 0xC7))
                val bg = parseColor(config.backgroundColor, Color(0x2B, 0x2B, 0x2B))
                Pair(fg, bg)
            }
            ThemePreset.FOLLOW_IDE -> {
                try {
                    val scheme = EditorColorsManager.getInstance().globalScheme
                    val bg = scheme.defaultBackground
                    val fg = scheme.defaultForeground
                    Pair(fg, bg)
                } catch (e: Exception) {
                    Pair(Color(0xDF, 0xE1, 0xE5), Color(0x1E, 0x1F, 0x22))
                }
            }
        }
    }

    fun hasBook(): Boolean = book != null

    fun loadBook(book: Book, chapterIndex: Int = 0, offset: Int = 0) {
        this.book = book
        this.currentChapterIndex = chapterIndex.coerceIn(0, (book.chapters.size - 1).coerceAtLeast(0))

        removeAll()
        add(scrollPane, BorderLayout.CENTER)
        revalidate()
        repaint()

        renderCurrentChapter()
        if (offset > 0) {
            SwingUtilities.invokeLater {
                val bar = scrollPane.verticalScrollBar
                val max = bar.maximum - bar.visibleAmount
                bar.value = offset.coerceIn(0, max.coerceAtLeast(0))
            }
        }
        notifyProgress()
    }

    private fun renderCurrentChapter(toBottom: Boolean = false) {
        val chapter = currentChapter() ?: return
        val rawText = chapter.text

        val formattedText = if (config.camouflageMode) {
            buildCamouflageContent(rawText, chapter.title)
        } else {
            buildNormalContent(rawText, chapter.title)
        }

        textPane.text = formattedText
        applyDocParagraphStyle()

        SwingUtilities.invokeLater {
            val bar = scrollPane.verticalScrollBar
            bar.value = if (toBottom) bar.maximum else 0
            updateProgressInfo()
        }
    }

    private fun buildNormalContent(text: String, title: String): String {
        val sb = java.lang.StringBuilder()
        sb.append(title.trim()).append("\n\n")

        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (config.indentFirstLine) {
                sb.append("　　")
            }
            sb.append(trimmed).append("\n\n")
        }
        return sb.toString()
    }

    private fun buildCamouflageContent(text: String, title: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val lines = text.lines().filter { it.isNotBlank() }

        return if (config.camouflageType == "COMMENT") {
            val commentBody = lines.joinToString("\n * ") { it.trim() }
            """
            /**
             * [Chapter: $title]
             * $commentBody
             */
            """.trimIndent()
        } else {
            // 系统日志格式
            val sb = java.lang.StringBuilder()
            val nowStr = dateFormat.format(Date())
            sb.append("[$nowStr] [main] INFO  c.m.n.service.NovelWorker - Initializing batch execution for chapter: $title\n")
            lines.forEachIndexed { i, line ->
                val time = dateFormat.format(Date(System.currentTimeMillis() + i * 100))
                val thread = "worker-thread-${(i % 4) + 1}"
                sb.append("[$time] [$thread] INFO  c.m.n.service.NovelWorker - ${line.trim()}\n")
            }
            sb.append("[$nowStr] [main] INFO  c.m.n.service.NovelWorker - Batch execution completed successfully.\n")
            sb.toString()
        }
    }

    private fun applyDocParagraphStyle() {
        if (config.camouflageMode) return

        val doc = textPane.styledDocument
        val set = SimpleAttributeSet()
        val lineSpacing = ((config.lineSpacingMultiplier - 1.0f).coerceAtLeast(0f) * 0.8f)
        val paragraphSpacing = (config.paragraphSpacingMultiplier * config.fontSize * 0.6f)

        StyleConstants.setLineSpacing(set, lineSpacing)
        StyleConstants.setSpaceAbove(set, paragraphSpacing / 2f)
        StyleConstants.setSpaceBelow(set, paragraphSpacing / 2f)
        doc.setParagraphAttributes(0, doc.length, set, false)
    }

    fun nextChapter() {
        val book = this.book ?: return
        if (currentChapterIndex < book.totalChapters() - 1) {
            currentChapterIndex++
            renderCurrentChapter()
            notifyProgress()
            onChapterChange?.invoke(currentChapterIndex)
        }
    }

    fun previousChapter(toBottom: Boolean = false) {
        if (currentChapterIndex > 0) {
            currentChapterIndex--
            renderCurrentChapter(toBottom)
            notifyProgress()
            onChapterChange?.invoke(currentChapterIndex)
        }
    }

    fun jumpToChapter(index: Int) {
        val book = this.book ?: return
        if (index in 0 until book.totalChapters()) {
            currentChapterIndex = index
            renderCurrentChapter()
            notifyProgress()
            onChapterChange?.invoke(currentChapterIndex)
        }
    }

    fun toggleCamouflage(): Boolean {
        config.camouflageMode = !config.camouflageMode
        refreshStyles()
        updateTextMargins()
        renderCurrentChapter()
        return config.camouflageMode
    }

    fun isCamouflaged(): Boolean = config.camouflageMode

    fun currentChapter(): Chapter? {
        return book?.chapters?.getOrNull(currentChapterIndex)
    }

    fun getCurrentChapterIndex(): Int = currentChapterIndex

    private fun updateProgressInfo() {
        val book = this.book ?: return
        val bar = scrollPane.verticalScrollBar
        val max = (bar.maximum - bar.visibleAmount).coerceAtLeast(1)
        val scrollRatio = (bar.value.toFloat() / max).coerceIn(0f, 1f)

        val totalChapters = book.totalChapters().coerceAtLeast(1)
        val totalPercent = ((currentChapterIndex + scrollRatio) / totalChapters) * 100f

        val chapter = currentChapter()
        val chapterName = chapter?.title ?: "第 ${currentChapterIndex + 1} 章"
        onProgressChange?.invoke(chapterName, totalPercent)

        // 保存持久化进度
        val state = NovelConfigState.getInstance()
        state.setLastBookPath(book.path)
        state.setLastProgress(currentChapterIndex, bar.value)
    }

    private fun notifyProgress() {
        updateProgressInfo()
    }

    private fun parseColor(hex: String?, fallback: Color): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.decode(hex.trim())
        } catch (e: Exception) {
            fallback
        }
    }
}
