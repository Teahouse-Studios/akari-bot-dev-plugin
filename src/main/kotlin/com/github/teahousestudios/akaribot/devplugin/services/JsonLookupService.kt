package com.github.teahousestudios.akaribot.devplugin.services

import com.github.teahousestudios.akaribot.devplugin.settings.LocaleSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class JsonLookupService(private val project: Project) {
    private val gson = Gson()
    @Volatile
    private var localeData : Map<String, String> = emptyMap()
    private var dirty: Boolean = false

    init {
        if (LocaleSettings.getInstance(project).isEnabled()) {
            load()
        } else {
            // ensure empty state when disabled
            localeData = emptyMap()
            dirty = false
        }
    }

    fun getLocaleData(): Map<String, String> {
        return if (LocaleSettings.getInstance(project).isEnabled()) localeData else emptyMap()
    }

    fun markDirty() {
        if (LocaleSettings.getInstance(project).isEnabled()) {
            dirty = true
        }
    }

    fun isDirty(): Boolean {
        return dirty
    }

    /**
     * 对外可调用的刷新接口，供监听器触发重载
     */
    fun reload() {
        if (LocaleSettings.getInstance(project).isEnabled()) {
            load()
        } else {
            localeData = emptyMap()
            dirty = false
        }
    }

    fun isWatchedPath(path: String): Boolean {
        if (!LocaleSettings.getInstance(project).isEnabled()) return false
        val relativePath = toRelativeProjectPath(path) ?: return false
        val segments = relativePath.split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (segments.isEmpty()) return false

        val localeFileName = LocaleSettings.getInstance(project).getLocaleFile()
        val whitelistFolders = LocaleSettings.getInstance(project).getWhitelistFolders()
        return whitelistFolders.any { rootFolder ->
            isWatchedLocaleFile(segments, rootFolder, localeFileName)
        }
    }

    private fun load() {
        try {
            val projectPath = project.basePath ?: run {
                localeData = emptyMap()
                dirty = false
                return
            }

            val whitelistFolders = LocaleSettings.getInstance(project).getWhitelistFolders()
            val localeFileName = LocaleSettings.getInstance(project).getLocaleFile()
            val mergedLocaleData = linkedMapOf<String, String>()
            val type = object : TypeToken<Map<String, String>>() {}.type

            for (folderName in whitelistFolders) {
                val rootDir = Paths.get(projectPath, folderName).toFile()
                if (!rootDir.exists() || !rootDir.isDirectory) continue

                collectLocaleFiles(rootDir, localeFileName, mergedLocaleData, type)
            }

            localeData = mergedLocaleData

            val group = NotificationGroupManager.getInstance().getNotificationGroup("Akaribot-Plugin")
            group?.createNotification("Locale JSON files were loaded from the configured whitelist folders.", NotificationType.INFORMATION)
                ?.notify(project)
            dirty = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun collectLocaleFiles(currentDir: File, localeFileName: String, mergedLocaleData: MutableMap<String, String>, type: java.lang.reflect.Type) {
        if (!currentDir.exists() || !currentDir.isDirectory) return

        if (currentDir.name.equals("locales", ignoreCase = true)) {
            val localeFile = File(currentDir, localeFileName)
            if (localeFile.exists() && localeFile.isFile) {
                try {
                    localeFile.inputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
                        val localeMap: Map<String, String> = gson.fromJson(reader, type) ?: emptyMap()
                        mergedLocaleData.putAll(localeMap)
                    }
                } catch (fileError: Exception) {
                    fileError.printStackTrace()
                }
            }
            return
        }

        currentDir.listFiles { file -> file.isDirectory }
            ?.sortedBy { it.name }
            ?.forEach { childDir -> collectLocaleFiles(childDir, localeFileName, mergedLocaleData, type) }
    }

    private fun isWatchedLocaleFile(segments: List<String>, rootFolder: String, localeFileName: String): Boolean {
        val normalizedRoot = rootFolder.trim().trim('/', '\\')
        if (normalizedRoot.isEmpty()) return false
        if (segments.firstOrNull() != normalizedRoot) return false

        return segments.drop(1).contains("locales") && segments.lastOrNull() == localeFileName
    }

    private fun toRelativeProjectPath(path: String): String? {
        val basePath = project.basePath?.replace('\\', '/')?.trimEnd('/') ?: return null
        val normalizedPath = path.replace('\\', '/')

        return when {
            normalizedPath == basePath -> ""
            normalizedPath.startsWith("$basePath/") -> normalizedPath.removePrefix("$basePath/").trimStart('/')
            else -> null
        }
    }

    companion object {
        fun getInstance(project: Project): JsonLookupService = project.getService(JsonLookupService::class.java)
    }
}