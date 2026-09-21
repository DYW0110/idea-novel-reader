package cn.microvideo.novel.plugin.ui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * 顶部导航栏（两行布局：第一行章节进度，第二行功能按钮）
 */
class TopNavigationBar(
    private val onOpenBook: () -> Unit,
    private val onToggleCatalog: () -> Unit,
    private val onPrevChapter: () -> Unit,
    private val onNextChapter: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onToggleCamouflage: () -> Unit
) : JPanel(BorderLayout()) {

    private val titleLabel = JLabel("未打开书籍", SwingConstants.CENTER)
    private var isCamouflaged = false
    private val camouflageBtn: JButton
    private val mainContainer: JPanel
    private val miniHandleBar: JPanel
    private var isCollapsed = false

    init {
        preferredSize = Dimension(0, 64)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color(55, 58, 64)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        )
        background = Color(38, 40, 46)

        mainContainer = JPanel(BorderLayout(0, 3)).apply {
            isOpaque = false
        }

        // 第一行：章节信息与进度（居中展示）
        val infoRow = JPanel(BorderLayout()).apply {
            isOpaque = false
            titleLabel.font = Font("Microsoft YaHei", Font.BOLD, 12)
            titleLabel.foreground = Color(0xDD, 0xDD, 0xDD)
            add(titleLabel, BorderLayout.CENTER)
        }

        // 第二行：操作按钮行（居中或两端对齐）
        val buttonRow = JPanel(FlowLayout(FlowLayout.CENTER, 6, 0)).apply {
            isOpaque = false
        }

        val openBtn = createToolButton("打开书籍", "打开本地小说文件 (Ctrl+Alt+O)") { onOpenBook() }
        val catalogBtn = createToolButton("章节目录", "展开章节目录 (F7 / Ctrl+Alt+L)") { onToggleCatalog() }
        val prevBtn = createToolButton("< 上一章", "切换上一章 (Alt+↑)") { onPrevChapter() }
        val nextBtn = createToolButton("下一章 >", "切换下一章 (Alt+↓)") { onNextChapter() }
        camouflageBtn = createToolButton("代码伪装", "一键伪装为代码/日志 (Ctrl+Alt+B)") { onToggleCamouflage() }
        val settingsBtn = createToolButton("阅读设置", "字号/排版/主题设置") { onOpenSettings() }
        val collapseBtn = createToolButton("收起工具栏", "隐藏顶部工具栏") { toggleCollapse() }

        buttonRow.add(openBtn)
        buttonRow.add(catalogBtn)
        buttonRow.add(prevBtn)
        buttonRow.add(nextBtn)
        buttonRow.add(camouflageBtn)
        buttonRow.add(settingsBtn)
        buttonRow.add(collapseBtn)

        mainContainer.add(infoRow, BorderLayout.NORTH)
        mainContainer.add(buttonRow, BorderLayout.CENTER)

        // 折叠收起后的极简手柄条
        miniHandleBar = JPanel(BorderLayout()).apply {
            isOpaque = false
            isVisible = false
            preferredSize = Dimension(0, 18)
            val expandBtn = JButton("▼ 展开工具栏").apply {
                isFocusPainted = false
                isBorderPainted = false
                isContentAreaFilled = false
                font = Font("Microsoft YaHei", Font.PLAIN, 11)
                foreground = Color(0x99, 0x99, 0x99)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addActionListener { toggleCollapse() }
            }
            add(expandBtn, BorderLayout.CENTER)
        }

        add(mainContainer, BorderLayout.CENTER)
        add(miniHandleBar, BorderLayout.SOUTH)
    }

    private fun toggleCollapse() {
        isCollapsed = !isCollapsed
        if (isCollapsed) {
            mainContainer.isVisible = false
            miniHandleBar.isVisible = true
            preferredSize = Dimension(0, 20)
        } else {
            mainContainer.isVisible = true
            miniHandleBar.isVisible = false
            preferredSize = Dimension(0, 64)
        }
        revalidate()
        repaint()
    }

    private fun createToolButton(text: String, tip: String, onClick: () -> Unit): JButton {
        return JButton(text).apply {
            toolTipText = tip
            isFocusPainted = false
            isBorderPainted = true
            isContentAreaFilled = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(70, 75, 85), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
            )
            font = Font("Microsoft YaHei", Font.PLAIN, 11)
            foreground = Color(0xD0, 0xD0, 0xD0)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { onClick() }
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    foreground = Color.WHITE
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color(0x4A, 0x90, 0xE2), 1),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)
                    )
                }

                override fun mouseExited(e: MouseEvent) {
                    foreground = Color(0xD0, 0xD0, 0xD0)
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color(70, 75, 85), 1),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)
                    )
                }
            })
        }
    }

    fun updateProgress(chapterTitle: String, percent: Float) {
        val displayPercent = String.format("%.1f%%", percent.coerceIn(0f, 100f))
        titleLabel.text = if (chapterTitle.isBlank()) "未打开书籍" else "$chapterTitle ($displayPercent)"
    }

    fun setCamouflaged(camouflaged: Boolean) {
        this.isCamouflaged = camouflaged
        camouflageBtn.text = if (camouflaged) "恢复正文" else "代码伪装"
    }
}
