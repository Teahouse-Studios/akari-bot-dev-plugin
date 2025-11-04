// kotlin
package com.github.teahousestudios.akaribot.devplugin.folding

import com.github.teahousestudios.akaribot.devplugin.services.JsonLookupService
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStringLiteralExpression
import java.util.concurrent.ConcurrentHashMap


class LocaleStringFoldingBuilder : FoldingBuilderEx(), DumbAware {
    private val placeholders = ConcurrentHashMap<ASTNode, String>()

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        // clear previous placeholders to avoid memory growth / stale mappings
        placeholders.clear()

        if (root !is PsiFile || !root.name.endsWith(".py", ignoreCase = true)) return emptyArray()
        val project = root.project
        val localeData = JsonLookupService.getInstance(project).getLocaleData()
        val descriptors = ArrayList<FoldingDescriptor>()

        val strings = PsiTreeUtil.findChildrenOfType(root, PyStringLiteralExpression::class.java)

        // regex to find patterns like {I18N:key}
        val pattern = Regex("""\{I18N:([^}]+)}""")

        for (str in strings) {
            val original = str.stringValue

            // 1) if the whole string is a key in localeData, fold to that value (existing behavior)
            val wholeValue = localeData[original]
            if (wholeValue != null) {
                val node = str.node ?: continue
                val range = TextRange(str.textRange.startOffset, str.textRange.endOffset)
                placeholders[node] = "\"$wholeValue\""
                descriptors.add(FoldingDescriptor(node, range))
                continue
            }

            // 2) otherwise, check for occurrences of {I18N:key} inside the string
            var replaced = false
            val replacedText = pattern.replace(original) { matchResult ->
                val key = matchResult.groupValues[1]
                val value = localeData[key]
                if (value != null) {
                    replaced = true
                    value
                } else {
                    // leave original token if not found
                    matchResult.value
                }
            }

            if (replaced) {
                val node = str.node ?: continue
                val range = TextRange(str.textRange.startOffset, str.textRange.endOffset)
                // show the whole string but with replacements applied; keep the original quoting style by using double quotes in placeholder
                placeholders[node] = "\"$replacedText\""
                descriptors.add(FoldingDescriptor(node, range))
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return placeholders[node]
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = node.psi is PyStringLiteralExpression
}
