package com.github.saundefined.bitrix_idea.orm

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

object OrmFileWriter {
    fun writeEntityList(moduleRoot: VirtualFile, model: OrmEntityModel) {
        val file = ormFile(moduleRoot, "install", "orm.php")
        val marker = "// ORM_ENTITY: ${model.namespace}\\${model.className}"
        val existing = read(file)
        if (existing.contains(marker)) return

        val entries = OrmInstallerGenerator.entityEntries(model)
            .joinToString("\n") { "    $it," }
        val text = if (existing.isBlank()) {
            "<?php\n\nreturn [\n$entries\n];\n"
        } else {
            val index = existing.lastIndexOf("];")
            if (index < 0) {
                existing.trimEnd() + "\n\n$marker\n$entries\n"
            } else {
                existing.substring(0, index) + "$marker\n$entries\n" + existing.substring(index)
            }
        }
        VfsUtil.saveText(file, text)
    }

    fun writeAnnotations(moduleRoot: VirtualFile, model: OrmEntityModel, relativePath: String) {
        val file = ormFile(moduleRoot, "meta", "orm.php")
        val marker = "/* ORMENTITYANNOTATION:${model.namespace}\\${model.className}:$relativePath */"
        val existing = read(file)
        if (existing.contains(marker)) return
        val block = OrmInstallerGenerator.annotationBlock(model, relativePath)
        val text = if (existing.isBlank()) "<?php\n\n$block\n" else existing.trimEnd() + "\n\n$block\n"
        VfsUtil.saveText(file, text)
    }

    private fun ormFile(root: VirtualFile, directory: String, fileName: String): VirtualFile {
        val dir = root.findChild(directory) ?: root.createChildDirectory(this, directory)
        return dir.findChild(fileName) ?: dir.createChildData(this, fileName)
    }

    private fun read(file: VirtualFile): String {
        return runCatching { VfsUtilCore.loadText(file) }.getOrDefault("")
    }
}
