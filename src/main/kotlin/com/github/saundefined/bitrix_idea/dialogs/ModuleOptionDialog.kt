package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionSpec
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionType
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import java.util.Locale
import javax.swing.JComponent

class ModuleOptionDialog(
    initial: ModuleOptionSpec,
    private val existingKeys: Set<String>,
) : DialogWrapper(true) {
    private val keyField = JBTextField(initial.key)
    private val nameField = JBTextField(initial.name)
    private val defaultField = JBTextField(initial.defaultValue)
    private val typeCombo = ComboBox(ModuleOptionType.entries.toTypedArray())
    private val initialKey = initial.key.trim().lowercase(Locale.ROOT)

    private val contentPanel = panel {
        row(message("create.options.key")) {
            cell(keyField)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.options.name")) {
            cell(nameField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.options.type")) {
            cell(typeCombo)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.options.default")) {
            cell(defaultField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
    }

    private var result: ModuleOptionSpec? = null

    init {
        typeCombo.selectedItem = initial.type
        typeCombo.renderer = SimpleListCellRenderer.create("") { value: ModuleOptionType -> value.displayName }
        init()
        title = message("create.options.edit.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        val normalized = keyField.text.trim().lowercase(Locale.ROOT)
        if (!normalized.matches(Regex("^[a-z][a-z0-9_]*$"))) {
            return ValidationInfo(message("create.options.key.validation"), keyField)
        }
        if (normalized in existingKeys && normalized != initialKey) {
            return ValidationInfo(message("create.options.key.duplicate"), keyField)
        }
        if (nameField.text.isBlank()) {
            return ValidationInfo(message("create.options.name.validation"), nameField)
        }
        return null
    }

    override fun doOKAction() {
        result = ModuleOptionSpec(
            key = keyField.text.trim().lowercase(Locale.ROOT),
            name = nameField.text.trim(),
            type = typeCombo.selectedItem as? ModuleOptionType ?: ModuleOptionType.TEXT,
            defaultValue = defaultField.text.trim(),
        )
        super.doOKAction()
    }

    fun getOption(): ModuleOptionSpec? = result
}
