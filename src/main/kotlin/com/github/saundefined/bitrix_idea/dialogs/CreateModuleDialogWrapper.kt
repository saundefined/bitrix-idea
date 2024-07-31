package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.validation.ModuleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.UrlVerifier
import com.intellij.ide.IdeView
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.table.JBTable
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

class CreateModuleDialogWrapper : DialogWrapper(true) {
    val settings: AppSettingsState
        get() = AppSettingsState().getInstance()

    var code: String = ""
    var name: String = ""
    var description: String = ""
    var vendor: String = ""
    var url: String = ""
    var languages: List<String> = settings.languages

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.module.form.title")
    }

    override fun createCenterPanel(): JComponent {
        val tableModel = object : DefaultTableModel(
            languages.map { arrayOf(it) }.toTypedArray(),
            arrayOf(message("localization.table.column.name"))
        ) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }

        val table = JBTable(tableModel)
            .apply {
                tableHeader = JTableHeader(this.columnModel).apply {
                    resizingAllowed = false
                    reorderingAllowed = false
                }
            }

        val buttonDimension = Dimension(24, 24)

        val addButton = JButton("+").apply {
            preferredSize = buttonDimension
            minimumSize = buttonDimension
            maximumSize = buttonDimension

            addActionListener {
                val result =
                    JOptionPane.showInputDialog(
                        null,
                        message("localization.add"),
                        message("localization.add.title"),
                        JOptionPane.QUESTION_MESSAGE,
                    )

                if (!result.isNullOrEmpty()) {
                    tableModel.addRow(arrayOf(result))
                    languages = tableModel.dataVector.map { (it as Vector<*>)[0].toString() }
                }

                this@CreateModuleDialogWrapper.pack()
            }
        }

        val removeButton = JButton("-").apply {
            preferredSize = buttonDimension
            minimumSize = buttonDimension
            maximumSize = buttonDimension

            addActionListener {
                if (table.selectedRow != -1 && tableModel.rowCount > 1) {
                    tableModel.removeRow(table.selectedRow)
                    languages = tableModel.dataVector.map { (it as Vector<*>)[0].toString() }
                }

                this@CreateModuleDialogWrapper.pack()
            }
        }

        val btnPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(addButton)
            add(removeButton)
        }

        val wrapperPanel = JPanel(BorderLayout())
        wrapperPanel.add(btnPanel, BorderLayout.LINE_END)
        wrapperPanel.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER)

        return panel {
            row {
                label(message("create.module.code"))
                textField()
                    .bindText(::code)
                    .focused()
                    .text(settings.vendorCode + ".")
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            ModuleCodeVerifier().verify(value)
                                .not() -> error(message("create.module.code.validation.fail"))

                            else -> null
                        }
                    }

                label(message("create.module.name"))
                textField()
                    .bindText(::name)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.module.name.validation.fail"))
                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row(message("create.module.description")) {
                textField()
                    .bindText(::description)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.module.description.validation.fail"))
                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                label(message("create.module.vendor"))
                textField()
                    .bindText(::vendor)
                    .text(settings.vendorName)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.module.vendor.validation.fail"))
                            else -> null
                        }
                    }

                label(message("create.module.url"))
                textField()
                    .bindText(::url)
                    .text(settings.vendorWebsite)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            UrlVerifier().verify(value)
                                .not() -> error(message("create.module.url.validation.fail"))

                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                label(message("localization.table.row.name"))
                    .align(AlignY.TOP)
                cell(wrapperPanel)
                    .align(AlignX.FILL)
            }
                .layout(RowLayout.PARENT_GRID)
        }
    }

    override fun doOKAction() {
        super.applyFields()

        val templateManager = FileTemplateManager.getInstance(project)

        val properties: Properties = FileTemplateManager.getInstance(project).defaultProperties
        properties.setProperty("DEFAULT_MODULE_NAME", code)
        properties.setProperty("SNACK_CASED_MODULE_NAME", code.replace(".", "_"))
        properties.setProperty("UPPER_CASED_MODULE_NAME", code.replace(".", "_").uppercase(Locale.getDefault()))

        properties.setProperty("MODULE_NAME", name)
        properties.setProperty("MODULE_DESCRIPTION", description)
        properties.setProperty("PARTNER_NAME", vendor)
        properties.setProperty("PARTNER_URI", url)

        ApplicationManager.getApplication().runWriteAction {
            try {
                val moduleDirectory = directory.createSubdirectory(code)

                val rootFileNames =
                    listOf("prolog.php", "options.php", "include.php", "default_option.php", ".settings.php")

                rootFileNames.forEach { fileName ->
                    val template = templateManager.getJ2eeTemplate("Bitrix Module $fileName")
                    FileTemplateUtil.createFromTemplate(template, fileName, properties, moduleDirectory)
                }

                val installDirectory = moduleDirectory.createSubdirectory("install")

                val installFileNames = listOf("index.php", "version.php")
                installFileNames.forEach { fileName ->
                    val template = templateManager.getJ2eeTemplate("Bitrix Module install $fileName")
                    FileTemplateUtil.createFromTemplate(template, fileName, properties, installDirectory)
                }

                val langDirectory = moduleDirectory.createSubdirectory("lang")
                languages.forEach { language ->
                    val langInstallDirectory = langDirectory.createSubdirectory(language).createSubdirectory("install")
                    installFileNames.filter { fileName -> fileName !== "version.php" }.forEach { fileName ->
                        val template = templateManager.getJ2eeTemplate("Bitrix Module language install $fileName")
                        FileTemplateUtil.createFromTemplate(template, fileName, properties, langInstallDirectory)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        super.doOKAction()
    }
}