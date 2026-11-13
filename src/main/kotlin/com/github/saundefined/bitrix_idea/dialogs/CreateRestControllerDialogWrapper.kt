package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityContextResolver
import com.github.saundefined.bitrix_idea.structure.BitrixModuleNames
import com.github.saundefined.bitrix_idea.templates.d7.D7ClassType
import com.github.saundefined.bitrix_idea.templates.d7.D7SettingsRegistrar
import com.github.saundefined.bitrix_idea.templates.d7.RestControllerGenerator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * REST-контроллер с методами-действиями и заготовкой `install/routes.php`.
 */
class CreateRestControllerDialogWrapper(
    private val project: Project,
    private val directory: PsiDirectory,
) : DialogWrapper(project, true) {
    private val moduleRootPath = OrmEntityContextResolver.resolveModuleRoot(directory.virtualFile.path)
    private val moduleId = OrmEntityContextResolver.resolveModuleId(directory.virtualFile.path).orEmpty()
    private val moduleNamespace = BitrixModuleNames.namespaceFor(moduleId)

    private var className: String = "Items"
    private var actions: String = "list:GET, add:POST, update:POST, delete:POST"
    private var register: Boolean = true
    private var createRoutes: Boolean = true

    private val contentPanel = panel {
        row(message("create.rest.module")) {
            label(moduleId.ifBlank { "—" })
        }
        row(message("create.rest.class")) {
            textField()
                .bindText(::className)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.rest.actions")) {
            textField()
                .bindText(::actions)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            comment(message("create.rest.actions.hint"))
        }
        row {
            checkBox(message("create.rest.register"))
                .bindSelected(::register)
            checkBox(message("create.rest.routes"))
                .bindSelected(::createRoutes)
        }
    }

    init {
        init()
        title = message("create.rest.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        contentPanel.apply()
        if (!className.matches(Regex("^[A-Z][A-Za-z0-9]*$"))) {
            return ValidationInfo(message("create.rest.class.validation"), null)
        }
        if (parseActions().isEmpty()) return ValidationInfo(message("create.rest.actions.validation"), null)
        return null
    }

    override fun doOKAction() {
        val root = moduleRootPath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        if (root == null || moduleId.isBlank()) {
            Messages.showErrorDialog(project, message("create.admin.module.not.found"), title)
            return
        }
        val actionSpecs = parseActions()
        val namespace = "$moduleNamespace\\Controller"
        val controllerText = RestControllerGenerator.generate(namespace, className, actionSpecs)
        val routesText = RestControllerGenerator.generateRoutes(moduleId, namespace, className, actionSpecs)

        val preview = buildList {
            add(GeneratorPreviewFile("lib/Controller/$className.php", controllerText))
            if (createRoutes) add(GeneratorPreviewFile("install/routes.php", routesText))
        }
        if (!GeneratorPreviewDialogWrapper(project, preview).showAndGet()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val lib = root.findChild("lib") ?: root.createChildDirectory(this, "lib")
            val controllerDir = lib.findChild("Controller") ?: lib.createChildDirectory(this, "Controller")
            val controllerFile = controllerDir.findChild("$className.php")
                ?: controllerDir.createChildData(this, "$className.php")
            VfsUtil.saveText(controllerFile, controllerText)

            if (createRoutes) {
                val install = root.findChild("install") ?: root.createChildDirectory(this, "install")
                val routes = install.findChild("routes.php") ?: install.createChildData(this, "routes.php")
                VfsUtil.saveText(routes, routesText)
            }

            if (register) {
                D7SettingsRegistrar.register(
                    moduleRoot = root,
                    type = D7ClassType.CONTROLLER,
                    namespace = moduleNamespace,
                    className = className,
                )
            }
        }
        super.doOKAction()
    }

    private fun parseActions(): List<RestControllerGenerator.ActionSpec> {
        return actions.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { item ->
                val name = item.substringBefore(':').trim()
                if (!name.matches(Regex("^[a-zA-Z_][A-Za-z0-9_]*$"))) return@mapNotNull null
                val method = item.substringAfter(':', "GET").trim().uppercase()
                if (method !in setOf("GET", "POST", "PUT", "DELETE", "PATCH")) return@mapNotNull null
                RestControllerGenerator.ActionSpec(name, method)
            }
    }
}
