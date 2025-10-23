package com.github.teahousestudios.akaribotdevplugin.completion

import com.github.teahousestudios.akaribotdevplugin.services.JsonLookupService
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class JsonCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val items = JsonLookupService.getInstance(project).getLocaleData()

        for (item in items) {
            result.addElement(LookupElementBuilder.create(item.key).withTypeText(item.value))
            result.addElement(LookupElementBuilder.create("{I18N:" + item.key + "}").withTypeText(item.value))
        }
    }
}
