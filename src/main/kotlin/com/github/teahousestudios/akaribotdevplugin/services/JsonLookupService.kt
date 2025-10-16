package com.github.teahousestudios.akaribotdevplugin.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class JsonLookupService(private val project: Project) {
    private val gson = Gson()
    @Volatile
    private var localeData : Map<String, String> = emptyMap()
    private var dirty: Boolean = false

    init {
        load()
    }

    fun getLocaleData(): Map<String, String> {
        return localeData
    }

    fun markDirty() {
        dirty = true
    }

    fun isDirty(): Boolean {
        return dirty
    }

    /**
     * 对外可调用的刷新接口，供监听器触发重载
     */
    fun reload() {
        load()
    }

    private fun load() {
        try {
            val projectPath = project.basePath

            // 读取core/locales/zh_cn.json
            val baseLocaleFile = projectPath?.let { Paths.get(it, "core", "locales", "zh_cn.json").toFile() }
            val reader = when {
                baseLocaleFile != null && baseLocaleFile.exists() ->
                    baseLocaleFile.inputStream().bufferedReader(StandardCharsets.UTF_8)
                else -> null
            } ?: return
            reader.use {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val localeMap: Map<String, String> = gson.fromJson(it, type) ?: emptyMap()
                localeData = localeMap
            }

            // 读取modules目录下的所有插件的locales/zh_cn.json
            val modulesDir = projectPath?.let { Paths.get(it, "modules").toFile() }
            if (modulesDir != null && modulesDir.exists() && modulesDir.isDirectory) {
                val moduleDirs = modulesDir.listFiles { file -> file.isDirectory } ?: emptyArray()
                for (moduleDir in moduleDirs) {
                    val localeFile = File(moduleDir, "locales/zh_cn.json")
                    if (localeFile.exists()) {
                        FileReader(localeFile, StandardCharsets.UTF_8).use { fileReader ->
                            val type = object : TypeToken<Map<String, String>>() {}.type
                            val moduleLocaleMap: Map<String, String> = gson.fromJson(fileReader, type) ?: emptyMap()
                            // 合并到总的 localeData 中，模块的键值对会覆盖基础的键值对
                            localeData = localeData + moduleLocaleMap
                        }
                    }
                }
            }
            val group = NotificationGroupManager.getInstance().getNotificationGroup("Akaribot-Plugin")
            group?.createNotification("本地化文件已加载", NotificationType.INFORMATION)
                ?.notify(project)
            dirty = false
        } catch (e: Exception) {
            // 简单容错，避免监听触发时抛出未捕获异常
            e.printStackTrace()
        }
    }

    companion object {
        fun getInstance(project: Project): JsonLookupService = project.getService(JsonLookupService::class.java)
    }
}