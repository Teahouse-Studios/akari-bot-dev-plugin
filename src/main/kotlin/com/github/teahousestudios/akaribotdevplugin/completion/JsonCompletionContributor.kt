package com.github.teahousestudios.akaribotdevplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.jetbrains.python.psi.PyStringLiteralExpression

class JsonCompletionContributor : CompletionContributor() {
    init {
        // Trigger only inside Python string literals
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withParent(PyStringLiteralExpression::class.java),
            JsonCompletionProvider()
        )
    }
}