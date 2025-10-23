// kotlin
package com.github.teahousestudios.akaribotdevplugin.folding

import com.github.teahousestudios.akaribotdevplugin.services.JsonLookupService
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.diagnostic.thisLogger
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
        if (root !is PsiFile || !root.name.endsWith(".py", ignoreCase = true)) return emptyArray()
        val project = root.project
        val localeData = JsonLookupService.getInstance(project).getLocaleData()
        val descriptors = ArrayList<FoldingDescriptor>()

        val strings = PsiTreeUtil.findChildrenOfType(root, PyStringLiteralExpression::class.java)

        for (str in strings) {
            val key = str.stringValue ?: continue
            val value = localeData[key] ?: continue
            val node = str.node ?: continue
            // 折叠整个字符串字面量范围
            val range = TextRange(str.textRange.startOffset, str.textRange.endOffset)
            placeholders[node] = "\"$value\"" // 占位显示带引号的值
            descriptors.add(FoldingDescriptor(node, range))
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return placeholders[node]
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
}
