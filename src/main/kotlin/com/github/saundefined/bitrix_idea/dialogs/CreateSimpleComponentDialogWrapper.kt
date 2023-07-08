package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.settings.AppSettingsState
import com.github.saundefined.bitrix_idea.validation.ComponentSimpleCodeVerifier
import com.intellij.ide.IdeView
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

class CreateSimpleComponentDialogWrapper : DialogWrapper(true) {
    val settings: AppSettingsState
        get() = AppSettingsState().getInstance()
    
    var code: String = ""
    var name: String = ""
    var description: String = ""
    var languages: List<String> = settings.languages

    lateinit var view: IdeView
    lateinit var project: Project
    lateinit var directory: PsiDirectory

    init {
        init()
        title = message("create.component.simple.form.title")
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

                this@CreateSimpleComponentDialogWrapper.pack()
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

                this@CreateSimpleComponentDialogWrapper.pack()
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
                label(message("create.component.simple.code"))
                textField()
                    .bindText(::code)
                    .focused()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            ComponentSimpleCodeVerifier().verify(value)
                                .not() -> error(message("create.component.simple.code.validation.fail"))

                            else -> null
                        }
                    }

                label(message("create.component.simple.name"))
                textField()
                    .bindText(::name)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.component.simple.name.validation.fail"))
                            else -> null
                        }
                    }

            }.layout(RowLayout.PARENT_GRID)

            row(message("create.component.simple.description")) {
                textField()
                    .bindText(::description)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        val value = it.text
                        when {
                            value === null -> error(message("create.component.simple.description.validation.fail"))
                            else -> null
                        }
                    }
            }.layout(RowLayout.PARENT_GRID)

            row {
                label(message("localization.table.row.name"))
                    .verticalAlign(VerticalAlign.TOP)
                cell(wrapperPanel)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
                .layout(RowLayout.PARENT_GRID)
        }
    }

    override fun doOKAction() {
        super.applyFields()

        val templateManager = FileTemplateManager.getInstance(project)

        val properties: Properties = FileTemplateManager.getInstance(project).defaultProperties

        properties.setProperty("UPPERCASE_COMPONENT_NAME", code.replace(".", "_").uppercase(Locale.getDefault()))
        properties.setProperty(
            "CAMEL_COMPONENT_NAME",
            code.replace(".", "_").split("_").joinToString("") { it ->
                it.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }
        )
        properties.setProperty("COMPONENT_NAME", name)
        properties.setProperty("COMPONENT_DESCRIPTION", description)

        val componentDirectory = directory.createSubdirectory(code)

        try {
            val rootFileNames = listOf(".description.php", ".parameters.php", "class.php")
            rootFileNames.forEach { fileName ->
                val template = templateManager.getJ2eeTemplate("Bitrix Simple Component $fileName")
                FileTemplateUtil.createFromTemplate(template, fileName, properties, componentDirectory)
            }

            val templatesDirectory = componentDirectory.createSubdirectory("templates");
            val defaultTemplateDirectory = templatesDirectory.createSubdirectory(".default");

            val defaultTemplateDirectoryTemplate =
                templateManager.getJ2eeTemplate("Bitrix Simple Component templates default template.php")
            FileTemplateUtil.createFromTemplate(
                defaultTemplateDirectoryTemplate,
                "template.php",
                properties,
                defaultTemplateDirectory
            )

            val langDirectory = componentDirectory.createSubdirectory("lang")
            languages.forEach { language ->
                val langInstallDirectory = langDirectory.createSubdirectory(language)
                rootFileNames
                    .filter { fileName -> fileName !== "class.php" }
                    .forEach { fileName ->
                        val template = templateManager.getJ2eeTemplate("Bitrix Simple Component language $fileName")
                        FileTemplateUtil.createFromTemplate(template, fileName, properties, langInstallDirectory)
                    }

                val languageTemplatesDirectory =
                    langInstallDirectory.createSubdirectory("templates");
                val languageDefaultTemplateDirectory =
                    languageTemplatesDirectory.createSubdirectory(".default");

                val languageDefaultTemplateDirectoryTemplate =
                    templateManager.getJ2eeTemplate("Bitrix Simple Component language templates default template.php")
                FileTemplateUtil.createFromTemplate(
                    languageDefaultTemplateDirectoryTemplate,
                    "template.php",
                    properties,
                    languageDefaultTemplateDirectory
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.doOKAction()
    }
}