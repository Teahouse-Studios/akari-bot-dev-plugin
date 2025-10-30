package com.github.teahousestudios.akaribot.devplugin.completion

import com.github.teahousestudios.akaribot.devplugin.services.JsonLookupService
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import kotlin.collections.iterator

class JsonCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val items = JsonLookupService.getInstance(project).getLocaleData()

        // find the string literal element ancestor to compute a prefix relative to the string start
        val stringElem = findStringLiteralElement(parameters)
        val doc = parameters.editor.document
        val caret = parameters.offset
        val prefix = if (stringElem != null) {
            try {
                val start = stringElem.textRange.startOffset
                val raw = doc.getText(TextRange(start, caret))
                // take substring after last space within the string literal
                val lastSpace = raw.lastIndexOf(' ')
                val token = if (lastSpace >= 0) raw.substring(lastSpace + 1) else raw
                // strip leading quote characters and non-alphanumeric chars so matching focuses on the token
                val cleaned = token.trimStart { ch -> ch == '\'' || ch == '"' || !ch.isLetterOrDigit() }
                cleaned
            } catch (_: Exception) {
                ""
            }
        } else {
            ""
        }

        val localResult = result.withPrefixMatcher(prefix)

        for (item in items) {
            localResult.addElement(LookupElementBuilder.create(item.key).withTypeText(item.value))

            // I18N form with InsertHandler to avoid duplicate '{' when user already typed it
            val i18n = "{I18N:${item.key}}"
            val i18nElement = LookupElementBuilder.create(i18n).withTypeText(item.value)
                .withInsertHandler { ctx, _ ->
                    val d = ctx.document
                    val startOff = ctx.startOffset
                    if (startOff > 0) {
                        try {
                            var i = startOff - 1
                            val seq = d.charsSequence
                            // skip whitespace backwards
                            while (i >= 0 && seq[i].isWhitespace()) i--
                            if (i >= 0 && seq[i] == '{') {
                                d.deleteString(i, startOff)
                            }
                        } catch (_: Exception) {
                            // ignore
                        }
                    }

                    // If a '}' already exists immediately after the inserted text, remove it
                    try {
                        val tailOff = ctx.tailOffset
                        val seq = d.charsSequence
                        if (tailOff < d.textLength && seq[tailOff] == '}') {
                            // delete the existing trailing '}' (the one that was already typed) so we don't end up with '}}'
                            d.deleteString(tailOff, tailOff + 1)
                        }
                    } catch (_: Exception) {
                        // ignore
                    }
                }

            localResult.addElement(i18nElement)
        }
    }

    private fun findStringLiteralElement(parameters: CompletionParameters): PsiElement? {
        var element: PsiElement? = parameters.position
        var depth = 0
        while (element != null && depth < 40) {
            val name = element::class.java.simpleName
            if (name.contains("PyStringLiteralExpression") || name.contains("StringTemplateExpression") || name.contains("LiteralExpression") || name.contains("StringLiteralExpression") || name.contains("KtLiteralStringTemplateEntry")) {
                return element
            }
            element = element.parent
            depth++
        }
        return null
    }
}
