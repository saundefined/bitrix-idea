package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionSpec
import com.github.saundefined.bitrix_idea.templates.module.ModuleOptionType
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.util.Locale
import javax.swing.JComponent

class ModuleOptionDialog(
    initial: ModuleOptionSpec,
    private val existingKeys: Set<String>,
) : DialogWrapper(true) {
    private var key: String = initial.key
    private var name: String = initial.name
    private var defaultValue: String = initial.defaultValue
    private val typeCombo = ComboBox(ModuleOptionType.entries.toTypedArray())
    private val initialKey = initial.key.trim().lowercase(Locale.ROOT)

    private val contentPanel = panel {
        row(message("create.options.key")) {
            textField()
                .bindText(::key)
                .focused()
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.options.name")) {
            textField()
                .bindText(::name)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.options.type")) {
            cell(typeCombo)
                .align(AlignX.FILL)
        }.layout(RowLayout.PARENT_GRID)
        row(message("create.options.default")) {
            textField()
                .bindText(::defaultValue)
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
        val normalized = key.trim().lowercase(Locale.ROOT)
        if (!normalized.matches(Regex("^[a-z][a-z0-9_]*$"))) {
            return ValidationInfo(message("create.options.key.validation"), null)
        }
        if (normalized in existingKeys && normalized != initialKey) {
            return ValidationInfo(message("create.options.key.duplicate"), null)
        }
        if (name.isBlank()) return ValidationInfo(message("create.options.name.validation"), null)
        return null
    }

    override fun doOKAction() {
        result = ModuleOptionSpec(
            key = key.trim().lowercase(Locale.ROOT),
            name = name.trim(),
            type = typeCombo.selectedItem as? ModuleOptionType ?: ModuleOptionType.TEXT,
            defaultValue = defaultValue.trim(),
        )
        super.doOKAction()
    }

    fun getOption(): ModuleOptionSpec? = result
}
