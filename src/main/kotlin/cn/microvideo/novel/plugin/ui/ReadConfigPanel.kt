package cn.microvideo.novel.plugin.ui

import cn.microvideo.novel.plugin.model.NovelConfig
import cn.microvideo.novel.plugin.model.ThemePreset
import com.intellij.ui.ColorPicker
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JSlider
import javax.swing.JTextField

/**
 * 设置面板
 */
class ReadConfigPanel(
    private val onConfigChange: (NovelConfig) -> Unit,
    private val onClose: (() -> Unit)? = null
) : JPanel(GridBagLayout()) {

    private val fontCombo = JComboBox(arrayOf("JetBrains Mono", "Microsoft YaHei", "SimSun", "Consolas", "Courier New", "KaiTi"))
    private val fontSizeSlider = JSlider(12, 36, 16).apply {
        majorTickSpacing = 4
        minorTickSpacing = 1
        paintTicks = false
    }
    private val fontSizeLabel = JLabel("16 px")

    private val lineSpacingSlider = JSlider(10, 30, 16).apply { // 1.0x ~ 3.0x
        majorTickSpacing = 5
        minorTickSpacing = 1
    }
    private val lineSpacingLabel = JLabel("1.6x")

    private val paragraphSpacingSlider = JSlider(5, 30, 10).apply { // 0.5x ~ 3.0x
        majorTickSpacing = 5
        minorTickSpacing = 1
    }
    private val paragraphSpacingLabel = JLabel("1.0em")

    private val maxWidthSlider = JSlider(500, 1400, 800).apply {
        majorTickSpacing = 100
        minorTickSpacing = 50
    }
    private val maxWidthLabel = JLabel("800 px")

    private val indentCheck = JCheckBox("首行缩进 2 字符", true)

    private val themeGroup = ButtonGroup()
    private val followIdeRadio = JRadioButton("跟随 IDE", true)
    private val parchmentRadio = JRadioButton("羊皮纸")
    private val darkCodeRadio = JRadioButton("深色代码")
    private val pureBlackRadio = JRadioButton("纯黑极简")
    private val customRadio = JRadioButton("自定义")

    private val fgColorField = JTextField("#A9B7C6", 7)
    private val bgColorField = JTextField("#2B2B2B", 7)
    private val pickFgBtn = JButton("取色")
    private val pickBgBtn = JButton("取色")

    private val camouflageCombo = JComboBox(arrayOf("系统日志模式 (Console Log)", "代码注释模式 (Javadoc)"))

    private var currentConfig = NovelConfig()

    init {
        border = BorderFactory.createEmptyBorder(12, 16, 12, 16)

        themeGroup.add(followIdeRadio)
        themeGroup.add(parchmentRadio)
        themeGroup.add(darkCodeRadio)
        themeGroup.add(pureBlackRadio)
        themeGroup.add(customRadio)

        val themePanel = JPanel().apply {
            add(followIdeRadio)
            add(parchmentRadio)
            add(darkCodeRadio)
            add(pureBlackRadio)
            add(customRadio)
        }

        followIdeRadio.addActionListener { applyPreset(ThemePreset.FOLLOW_IDE) }
        parchmentRadio.addActionListener { applyPreset(ThemePreset.PARCHMENT) }
        darkCodeRadio.addActionListener { applyPreset(ThemePreset.DARK_CODE) }
        pureBlackRadio.addActionListener { applyPreset(ThemePreset.PURE_BLACK) }
        customRadio.addActionListener { applyPreset(ThemePreset.CUSTOM) }

        fontSizeSlider.addChangeListener {
            fontSizeLabel.text = "${fontSizeSlider.value} px"
        }
        lineSpacingSlider.addChangeListener {
            lineSpacingLabel.text = String.format("%.1fx", lineSpacingSlider.value / 10.0)
        }
        paragraphSpacingSlider.addChangeListener {
            paragraphSpacingLabel.text = String.format("%.1fem", paragraphSpacingSlider.value / 10.0)
        }
        maxWidthSlider.addChangeListener {
            maxWidthLabel.text = "${maxWidthSlider.value} px"
        }

        pickFgBtn.addActionListener {
            val chosen = ColorPicker.showDialog(this, "选择文字颜色", parseColor(fgColorField.text, Color(0xA9, 0xB7, 0xC6)), false, emptyList(), true)
            if (chosen != null) {
                fgColorField.text = NovelConfig.colorToHex(chosen)
                customRadio.isSelected = true
            }
        }

        pickBgBtn.addActionListener {
            val chosen = ColorPicker.showDialog(this, "选择背景颜色", parseColor(bgColorField.text, Color(0x2B, 0x2B, 0x2B)), false, emptyList(), true)
            if (chosen != null) {
                bgColorField.text = NovelConfig.colorToHex(chosen)
                customRadio.isSelected = true
            }
        }

        val c = GridBagConstraints().apply {
            insets = Insets(6, 6, 6, 6)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
        }

        var row = 0

        // 1. 配色主题
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("配色主题:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 2; add(themePanel, c); row++

        // 2. 自定义颜色 (若选自定义)
        val colorPanel = JPanel().apply {
            add(JLabel("文字:"))
            add(fgColorField)
            add(pickFgBtn)
            add(JLabel("  背景:"))
            add(bgColorField)
            add(pickBgBtn)
        }
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("自定义颜色:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 2; add(colorPanel, c); row++

        // 3. 字体
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("正文字体:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 2; add(fontCombo, c); row++

        // 4. 字号
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("字体大小:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 1; add(fontSizeSlider, c)
        c.gridx = 2; c.gridy = row; c.gridwidth = 1; add(fontSizeLabel, c); row++

        // 5. 行间距
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("行间距:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 1; add(lineSpacingSlider, c)
        c.gridx = 2; c.gridy = row; c.gridwidth = 1; add(lineSpacingLabel, c); row++

        // 6. 段间距
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("段间距:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 1; add(paragraphSpacingSlider, c)
        c.gridx = 2; c.gridy = row; c.gridwidth = 1; add(paragraphSpacingLabel, c); row++

        // 7. 最大正文宽度
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("正文最大宽度:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 1; add(maxWidthSlider, c)
        c.gridx = 2; c.gridy = row; c.gridwidth = 1; add(maxWidthLabel, c); row++

        // 8. 缩进
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("排版选项:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 2; add(indentCheck, c); row++

        // 9. 伪装样式
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; add(JLabel("伪装模式样式:"), c)
        c.gridx = 1; c.gridy = row; c.gridwidth = 2; add(camouflageCombo, c); row++

        // 10. 底部按钮
        val btnPanel = JPanel().apply {
            val applyBtn = JButton("应用并保存").apply {
                addActionListener {
                    val cfg = getCurrentConfig()
                    onConfigChange(cfg)
                }
            }
            val hideBtn = JButton("🙈 隐藏设置").apply {
                addActionListener {
                    val cfg = getCurrentConfig()
                    onConfigChange(cfg)
                    onClose?.invoke()
                }
            }
            val resetBtn = JButton("恢复默认").apply {
                addActionListener {
                    setConfig(NovelConfig())
                    onConfigChange(NovelConfig())
                }
            }
            add(applyBtn)
            add(hideBtn)
            add(resetBtn)
        }
        c.gridx = 0; c.gridy = row; c.gridwidth = 3
        c.anchor = GridBagConstraints.CENTER
        add(btnPanel, c)
    }

    private fun applyPreset(preset: ThemePreset) {
        when (preset) {
            ThemePreset.PARCHMENT -> {
                fgColorField.text = preset.fgHex
                bgColorField.text = preset.bgHex
            }
            ThemePreset.DARK_CODE -> {
                fgColorField.text = preset.fgHex
                bgColorField.text = preset.bgHex
            }
            ThemePreset.PURE_BLACK -> {
                fgColorField.text = preset.fgHex
                bgColorField.text = preset.bgHex
            }
            ThemePreset.FOLLOW_IDE, ThemePreset.CUSTOM -> {}
        }
    }

    fun setConfig(config: NovelConfig) {
        currentConfig = config.copy()

        when (config.themePreset) {
            ThemePreset.PARCHMENT.name -> parchmentRadio.isSelected = true
            ThemePreset.DARK_CODE.name -> darkCodeRadio.isSelected = true
            ThemePreset.PURE_BLACK.name -> pureBlackRadio.isSelected = true
            ThemePreset.CUSTOM.name -> customRadio.isSelected = true
            else -> followIdeRadio.isSelected = true
        }

        fontCombo.selectedItem = config.fontName
        fontSizeSlider.value = config.fontSize
        fontSizeLabel.text = "${config.fontSize} px"

        lineSpacingSlider.value = (config.lineSpacingMultiplier * 10).toInt().coerceIn(10, 30)
        lineSpacingLabel.text = String.format("%.1fx", config.lineSpacingMultiplier)

        paragraphSpacingSlider.value = (config.paragraphSpacingMultiplier * 10).toInt().coerceIn(5, 30)
        paragraphSpacingLabel.text = String.format("%.1fem", config.paragraphSpacingMultiplier)

        maxWidthSlider.value = config.maxWidth.coerceIn(500, 1400)
        maxWidthLabel.text = "${config.maxWidth} px"

        indentCheck.isSelected = config.indentFirstLine
        fgColorField.text = config.fontColor
        bgColorField.text = config.backgroundColor

        camouflageCombo.selectedIndex = if (config.camouflageType == "COMMENT") 1 else 0
    }

    fun getCurrentConfig(): NovelConfig {
        val preset = when {
            parchmentRadio.isSelected -> ThemePreset.PARCHMENT.name
            darkCodeRadio.isSelected -> ThemePreset.DARK_CODE.name
            pureBlackRadio.isSelected -> ThemePreset.PURE_BLACK.name
            customRadio.isSelected -> ThemePreset.CUSTOM.name
            else -> ThemePreset.FOLLOW_IDE.name
        }

        return currentConfig.copy(
            themePreset = preset,
            fontName = fontCombo.selectedItem?.toString() ?: "Microsoft YaHei",
            fontSize = fontSizeSlider.value,
            lineSpacingMultiplier = lineSpacingSlider.value / 10.0f,
            paragraphSpacingMultiplier = paragraphSpacingSlider.value / 10.0f,
            maxWidth = maxWidthSlider.value,
            indentFirstLine = indentCheck.isSelected,
            fontColor = fgColorField.text.trim(),
            backgroundColor = bgColorField.text.trim(),
            camouflageType = if (camouflageCombo.selectedIndex == 1) "COMMENT" else "CODE_LOG"
        )
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
