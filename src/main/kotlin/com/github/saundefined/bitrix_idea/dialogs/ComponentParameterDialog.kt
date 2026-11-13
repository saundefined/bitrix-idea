package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.component.ComponentParameterSpec
import com.github.saundefined.bitrix_idea.templates.component.ComponentParameterType
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.util.Locale
import javax.swing.JComponent

/**
 * Редактирование одного параметра компонента.
 */
class ComponentParameterDialog(
    initial: ComponentParameterSpec,
    private val existingKeys: Set<String>,
) : DialogWrapper(true) {
    private var key: String = initial.key
    private var name: String = initial.name
    private var defaultValue: String = initial.defaultValue
    private var parent: String = initial.parent
    private var required: Boolean = initial.required
    private val keyField = JBTextField(key)
    private val nameField = JBTextField(name)
    private val defaultField = JBTextField(defaultValue)
    private val parentField = JBTextField(parent)
    private val typeCombo = ComboBox(ComponentParameterType.entries.toTypedArray())

    private val contentPanel = panel {
        row(message("create.parameters.key")) {
            cell(keyField)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.parameters.name")) {
            cell(nameField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.parameters.type")) {
            cell(typeCombo)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.parameters.default")) {
            cell(defaultField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.parameters.parent")) {
            cell(parentField)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row {
            checkBox(message("create.parameters.required"))
                .bindSelected(::required)
        }
    }

    private var result: ComponentParameterSpec? = null
    private val initialKey: String = initial.key.trim().uppercase(Locale.ROOT)

    init {
        typeCombo.selectedItem = initial.type
        typeCombo.renderer = SimpleListCellRenderer.create("") { value: ComponentParameterType -> value.displayName }
        init()
        title = message("create.parameters.edit.title")
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        key = keyField.text
        name = nameField.text
        defaultValue = defaultField.text
        parent = parentField.text
        val normalized = key.trim().uppercase(Locale.ROOT)
        if (!normalized.matches(Regex("^[A-Z][A-Z0-9_]*$"))) {
            return ValidationInfo(message("create.parameters.key.validation"), null)
        }
        if (normalized in existingKeys && normalized != initialKey) {
            return ValidationInfo(message("create.parameters.key.duplicate"), null)
        }
        if (name.isBlank()) {
            return ValidationInfo(message("create.parameters.name.validation"), null)
        }
        return null
    }

    override fun doOKAction() {
        key = keyField.text
        name = nameField.text
        defaultValue = defaultField.text
        parent = parentField.text
        result = ComponentParameterSpec(
            key = key.trim().uppercase(Locale.ROOT),
            name = name.trim(),
            type = typeCombo.selectedItem as? ComponentParameterType ?: ComponentParameterType.STRING,
            defaultValue = defaultValue.trim(),
            parent = parent.trim().ifBlank { "BASE" },
            required = required,
        )
        super.doOKAction()
    }

    fun getParameter(): ComponentParameterSpec? = result
}
