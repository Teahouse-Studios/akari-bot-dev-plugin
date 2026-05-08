package com.github.teahousestudios.akaribot.devplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "AkaribotLocaleSettings", storages = [Storage("akaribot_settings.xml")])
@Service(Service.Level.PROJECT)
class LocaleSettings : PersistentStateComponent<LocaleSettings.State> {
    data class State(
        var localeFile: String = "zh_cn.json",
        var whitelistFolders: MutableList<String> = mutableListOf("core", "modules"),
        var enabled: Boolean = false
    )

    private var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.myState)
    }

    fun getLocaleFile(): String = myState.localeFile

    fun setLocaleFile(fileName: String) {
        myState.localeFile = fileName
    }

    fun isEnabled(): Boolean = myState.enabled

    fun setEnabled(value: Boolean) {
        myState.enabled = value
    }

    fun getWhitelistFolders(): List<String> = normalizeWhitelist(myState.whitelistFolders)

    fun setWhitelistFolders(folders: Collection<String>) {
        myState.whitelistFolders = normalizeWhitelist(folders).toMutableList()
    }

    private fun normalizeWhitelist(folders: Collection<String>?): List<String> {
        val normalized = folders
            ?.map { it.trim().trim('/', '\\') }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()

        return if (normalized.isEmpty()) {
            listOf("core", "modules")
        } else {
            normalized
        }
    }

    companion object {
        fun getInstance(project: Project): LocaleSettings = project.getService(LocaleSettings::class.java)
    }
}
