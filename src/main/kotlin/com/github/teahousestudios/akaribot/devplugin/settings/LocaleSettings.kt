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
    data class State(var localeFile: String = "zh_cn.json")

    private var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.myState)
    }

    fun getLocaleFile(): String = myState.localeFile

    fun setLocaleFile(fileName: String) {
        myState.localeFile = fileName
    }

    companion object {
        fun getInstance(project: Project): LocaleSettings = project.getService(LocaleSettings::class.java)
    }
}
