package cn.microvideo.novel.plugin.ui

import cn.microvideo.novel.plugin.model.Book
import cn.microvideo.novel.plugin.model.Chapter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * 悬浮抽屉式目录面板
 */
class DrawerCatalogPanel(
    private val onChapterSelected: (Int) -> Unit,
    private val onClose: () -> Unit
) : JPanel(BorderLayout()) {

    data class ChapterItem(val index: Int, val title: String) {
        override fun toString(): String = title
    }

    private val searchField = com.intellij.ui.components.JBTextField()
    private val listModel = DefaultListModel<ChapterItem>()
    private val chapterList = JBList(listModel)
    private var allChapters: List<ChapterItem> = emptyList()
    private var currentChapterIndex: Int = 0
    private var isUpdatingSelection = false

    init {
        preferredSize = Dimension(320, 0)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, Color(0x50, 0x50, 0x50)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )

        // 顶部搜索栏与关闭按钮
        val headerPanel = JPanel(BorderLayout(6, 0)).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(0, 0, 8, 0)

            val titleLabel = JLabel("章节目录").apply {
                font = font.deriveFont(java.awt.Font.BOLD, 14f)
            }
            val closeBtn = JButton("✕").apply {
                isBorderPainted = false
                isContentAreaFilled = false
                isFocusPainted = false
                toolTipText = "关闭目录 (Esc / Alt+L)"
                addActionListener { onClose() }
            }

            val topRow = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(titleLabel, BorderLayout.WEST)
                add(closeBtn, BorderLayout.EAST)
            }

            searchField.emptyText.text = "搜索章节..."
            searchField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = filter()
                override fun removeUpdate(e: DocumentEvent?) = filter()
                override fun changedUpdate(e: DocumentEvent?) = filter()
            })

            add(topRow, BorderLayout.NORTH)
            add(searchField, BorderLayout.CENTER)
        }

        chapterList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chapterList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): java.awt.Component {
                val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is ChapterItem && value.index == currentChapterIndex && !isSelected) {
                    foreground = Color(0x4A, 0x90, 0xE2)
                }
                return comp
            }
        }

        chapterList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount >= 1) {
                    val item = chapterList.selectedValue
                    if (item != null && !isUpdatingSelection) {
                        onChapterSelected(item.index)
                        onClose()
                    }
                }
            }
        })

        chapterList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    val item = chapterList.selectedValue
                    if (item != null) {
                        onChapterSelected(item.index)
                        onClose()
                    }
                } else if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    onClose()
                }
            }
        })

        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DOWN) {
                    chapterList.requestFocusInWindow()
                    if (chapterList.selectedIndex < 0 && listModel.size() > 0) {
                        chapterList.selectedIndex = 0
                    }
                } else if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    onClose()
                }
            }
        })

        add(headerPanel, BorderLayout.NORTH)
        add(JBScrollPane(chapterList).apply {
            border = BorderFactory.createEmptyBorder()
        }, BorderLayout.CENTER)
    }

    private fun filter() {
        val keyword = searchField.text.trim().lowercase()
        listModel.clear()
        val filtered = if (keyword.isEmpty()) {
            allChapters
        } else {
            allChapters.filter { it.title.lowercase().contains(keyword) || (it.index + 1).toString().contains(keyword) }
        }
        filtered.forEach { listModel.addElement(it) }

        // 高亮当前章节
        val curIndex = filtered.indexOfFirst { it.index == currentChapterIndex }
        if (curIndex >= 0) {
            isUpdatingSelection = true
            chapterList.selectedIndex = curIndex
            chapterList.ensureIndexIsVisible(curIndex)
            isUpdatingSelection = false
        }
    }

    fun setBook(book: Book, initialChapterIndex: Int = 0) {
        allChapters = book.chapters.mapIndexed { idx, ch -> ChapterItem(idx, ch.title) }
        currentChapterIndex = initialChapterIndex
        searchField.text = ""
        filter()
    }

    fun selectChapter(index: Int) {
        currentChapterIndex = index
        val pos = (0 until listModel.size()).firstOrNull { listModel.get(it).index == index }
        if (pos != null && chapterList.selectedIndex != pos) {
            isUpdatingSelection = true
            chapterList.selectedIndex = pos
            chapterList.ensureIndexIsVisible(pos)
            isUpdatingSelection = false
        }
        chapterList.repaint()
    }

    fun focusSearch() {
        searchField.requestFocusInWindow()
        searchField.selectAll()
    }
}
