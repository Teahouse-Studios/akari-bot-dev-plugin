package com.github.teahousestudios.akaribot.devplugin.settings

import com.github.teahousestudios.akaribot.devplugin.services.JsonLookupService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import javax.swing.*
import java.awt.BorderLayout

class LocaleConfigurable(private val project: Project) : Configurable {
    private var panel: JPanel? = null
    private var combo: ComboBox<String>? = null
    private var reloadButton: JButton? = null

    private val options = arrayOf("zh_cn.json", "zh_tw.json", "en_us.json", "ja_jp.json")

    override fun createComponent(): JComponent? {
        if (panel == null) {
            panel = JPanel(BorderLayout())
            combo = ComboBox(options)
            reloadButton = JButton("Reload Now")
            val top = JPanel()
            top.add(JLabel("Locale JSON:"))
            top.add(combo)
            top.add(reloadButton)
            panel!!.add(top, BorderLayout.NORTH)

            reloadButton!!.addActionListener {
                // Trigger reload
                JsonLookupService.Companion.getInstance(project).reload()
            }
        }
        return panel
    }

    override fun isModified(): Boolean {
        val settings = LocaleSettings.getInstance(project)
        val selected = combo?.selectedItem as? String ?: settings.getLocaleFile()
        return selected != settings.getLocaleFile()
    }

    override fun apply() {
        val settings = LocaleSettings.getInstance(project)
        val selected = combo?.selectedItem as? String ?: "zh_cn.json"
        settings.setLocaleFile(selected)
        // Reload after changing
        JsonLookupService.Companion.getInstance(project).reload()
    }

    override fun getDisplayName(): String = "Akaribot Locale"

    override fun reset() {
        val settings = LocaleSettings.getInstance(project)
        combo?.selectedItem = settings.getLocaleFile()
    }

    override fun disposeUIResources() {
        panel = null
        combo = null
        reloadButton = null
    }
}
