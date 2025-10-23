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

            // I18N form with InsertHandler to avoid duplicate '{' when user already typed it
            val i18n = "{I18N:${item.key}}"
            val i18nElement = LookupElementBuilder.create(i18n).withTypeText(item.value)
                .withInsertHandler { ctx, _ ->
                    val doc = ctx.document
                    val start = ctx.startOffset
                    if (start > 0) {
                        try {
                            // look backwards skipping whitespace to find a previous non-space character
                            var i = start - 1
                            val seq = doc.charsSequence
                            while (i >= 0 && seq[i].isWhitespace()) i--
                            if (i >= 0 && seq[i] == '{') {
                                // delete from the '{' up to the insertion start (removes brace and any spaces)
                                doc.deleteString(i, start)
                            }
                        } catch (_: Exception) {
                            // ignore
                        }
                    }
                }

            result.addElement(i18nElement)
        }
    }
}
