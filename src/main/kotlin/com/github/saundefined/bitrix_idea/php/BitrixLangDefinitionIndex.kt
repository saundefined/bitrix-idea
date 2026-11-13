package com.github.saundefined.bitrix_idea.php

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType

/**
 * Индекс lang-ключей: позволяет быстро находить файлы по ключу без полного PSI-сканирования.
 */
class BitrixLangDefinitionIndex : FileBasedIndexExtension<String, String>() {
    override fun getName(): ID<String, String> = NAME

    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return DataIndexer { inputData ->
            val text = inputData.contentAsText
            val result = HashMap<String, String>()
            MESS.findAll(text).forEach { match ->
                result[match.groupValues[1]] = ""
            }
            result
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): com.intellij.util.io.DataExternalizer<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return object : DefaultFileTypeSpecificInputFilter(PhpFileType.INSTANCE) {
            override fun acceptInput(file: VirtualFile): Boolean = file.path.contains("/lang/")
        }
    }

    override fun getVersion(): Int = 1

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        val NAME: ID<String, String> = ID.create("bitrix.lang.definitions")
        private val MESS = Regex("""\${'$'}MESS\s*\[\s*['"]([^'"]+)['"]\s*]""")

        fun findFiles(project: Project, key: String): List<VirtualFile> {
            return FileBasedIndex.getInstance()
                .getContainingFiles(NAME, key, GlobalSearchScope.projectScope(project))
                .toList()
        }
    }
}
