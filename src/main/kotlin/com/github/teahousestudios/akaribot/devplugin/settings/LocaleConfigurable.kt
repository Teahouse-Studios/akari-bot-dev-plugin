package com.github.teahousestudios.akaribot.devplugin.settings

import com.github.teahousestudios.akaribot.devplugin.services.JsonLookupService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import java.awt.BorderLayout
import javax.swing.*

class LocaleConfigurable(private val project: Project) : Configurable {
    private var panel: JPanel? = null
    private var localeCombo: ComboBox<String>? = null
    private var enabledCheck: JCheckBox? = null
    private var whitelistArea: JTextArea? = null
    private var reloadButton: JButton? = null

    private val localeOptions = arrayOf("zh_cn.json", "zh_tw.json", "en_us.json", "ja_jp.json", "ko_kr.json")

    override fun createComponent(): JComponent? {
        if (panel == null) {
            panel = JPanel(BorderLayout(0, 8))
            // Enabled checkbox on top
            enabledCheck = JCheckBox("Enable I18N string folding for this project")
            enabledCheck!!.isSelected = LocaleSettings.getInstance(project).isEnabled()
            panel!!.add(enabledCheck!!, BorderLayout.NORTH)

            localeCombo = ComboBox(localeOptions)
            localeCombo!!.selectedItem = LocaleSettings.getInstance(project).getLocaleFile()

            val localePanel = JPanel(BorderLayout(0, 4))
            localePanel.add(JLabel("Locale JSON:"), BorderLayout.NORTH)
            localePanel.add(localeCombo!!, BorderLayout.CENTER)

            whitelistArea = JTextArea(6, 28)
            whitelistArea!!.lineWrap = false
            whitelistArea!!.text = LocaleSettings.getInstance(project).getWhitelistFolders().joinToString("\n")

            val whitelistPanel = JPanel(BorderLayout(0, 4))
            whitelistPanel.add(JLabel("Whitelist folders (one per line):"), BorderLayout.NORTH)
            whitelistPanel.add(JScrollPane(whitelistArea), BorderLayout.CENTER)

            reloadButton = JButton("Reload Now")
            reloadButton!!.addActionListener {
                JsonLookupService.getInstance(project).reload()
            }

            val centerPanel = JPanel(BorderLayout(0, 8))
            centerPanel.add(localePanel, BorderLayout.NORTH)
            centerPanel.add(whitelistPanel, BorderLayout.CENTER)

            panel!!.add(centerPanel, BorderLayout.CENTER)
            panel!!.add(reloadButton!!, BorderLayout.SOUTH)
        }
        return panel
    }

    override fun isModified(): Boolean {
        val settings = LocaleSettings.getInstance(project)
        val selectedLocale = localeCombo?.selectedItem as? String ?: settings.getLocaleFile()
        val enabledUi = enabledCheck?.isSelected ?: settings.isEnabled()
        return enabledUi != settings.isEnabled() || selectedLocale != settings.getLocaleFile() || readWhitelistFromUi() != settings.getWhitelistFolders()
    }

    override fun apply() {
        val settings = LocaleSettings.getInstance(project)
        settings.setEnabled(enabledCheck?.isSelected ?: settings.isEnabled())
        settings.setLocaleFile(localeCombo?.selectedItem as? String ?: settings.getLocaleFile())
        settings.setWhitelistFolders(readWhitelistFromUi())
        JsonLookupService.getInstance(project).reload()
    }

    override fun getDisplayName(): String = "Akaribot Locale"

    override fun reset() {
        val settings = LocaleSettings.getInstance(project)
        enabledCheck?.isSelected = settings.isEnabled()
        localeCombo?.selectedItem = settings.getLocaleFile()
        whitelistArea?.text = settings.getWhitelistFolders().joinToString("\n")
    }

    override fun disposeUIResources() {
        panel = null
        localeCombo = null
        enabledCheck = null
        whitelistArea = null
        reloadButton = null
    }

    private fun readWhitelistFromUi(): List<String> {
        return whitelistArea?.text
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.toList()
            ?: LocaleSettings.getInstance(project).getWhitelistFolders()
    }
}
