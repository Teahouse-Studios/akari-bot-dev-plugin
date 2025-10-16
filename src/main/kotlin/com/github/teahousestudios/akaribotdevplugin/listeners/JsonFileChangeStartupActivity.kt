// kotlin
package com.github.teahousestudios.akaribotdevplugin.listeners

import com.github.teahousestudios.akaribotdevplugin.services.JsonLookupService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent

class JsonFileChangeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val connection = project.messageBus.connect()

        // VFS 改动只标记为 dirty，不立即重载
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                if (events.isEmpty()) return

                val shouldMark = events.any { event ->
                    val path = event.path.replace('\\', '/')
                    path.endsWith("/core/locales/zh_cn.json") ||
                            Regex(".*/modules/[^/]+/locales/zh_cn.json$").containsMatchIn(path)
                }

                if (shouldMark) {
                    JsonLookupService.getInstance(project).markDirty()
                }
            }
        })

        // 编辑器切换时，如果有 dirty，就执行重载
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                val service = JsonLookupService.getInstance(project)
                if (service.isDirty()) {
                    service.reload()
                }
            }
        })
    }
}
