package com.github.teahousestudios.akaribotdevplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns

class JsonCompletionContributor : CompletionContributor() {
    init {
        // 简单地在所有位置提供补全，生产中可以限制 psiElement() 的具体 language / context
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), JsonCompletionProvider())
    }
}