package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityValidator
import com.github.saundefined.bitrix_idea.orm.OrmValidator
import com.github.saundefined.bitrix_idea.orm.OrmValidatorType
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.util.Locale
import javax.swing.JComponent
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.SimpleListCellRenderer

class OrmValidatorDialog(private val initial: OrmValidator) : DialogWrapper(true) {
    private val typeCombo = ComboBox(OrmValidatorType.entries.toTypedArray())
    private val minField = JBTextField(initial.min.orEmpty())
    private val maxField = JBTextField(initial.max.orEmpty())
    private val patternField = JBTextField(initial.pattern.orEmpty())
    private val targetClassField = JBTextField(initial.targetClass.orEmpty())
    private val targetField = JBTextField(initial.targetField.orEmpty())
    private var result: OrmValidator? = null

    private val contentPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(message("create.orm.validator.type"), typeCombo)
        .addLabeledComponent(message("create.orm.validator.min"), minField)
        .addLabeledComponent(message("create.orm.validator.max"), maxField)
        .addLabeledComponent(message("create.orm.validator.pattern"), patternField)
        .addLabeledComponent(message("create.orm.validator.target.class"), targetClassField)
        .addLabeledComponent(message("create.orm.validator.target.field"), targetField)
        .panel
        .apply { preferredSize = Dimension(520, 300) }

    init {
        typeCombo.selectedItem = initial.type
        typeCombo.renderer = SimpleListCellRenderer.create("") { value: OrmValidatorType -> value.displayName }
        typeCombo.addActionListener { updateAvailability() }
        init()
        title = message("create.orm.validator.title")
        updateAvailability()
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        val error = OrmEntityValidator.validatorError(buildValidator())
        return error?.let { ValidationInfo(message(it), typeCombo) }
    }

    override fun doOKAction() {
        result = buildValidator()
        super.doOKAction()
    }

    fun getValidator(): OrmValidator? = result

    private fun selectedType(): OrmValidatorType =
        typeCombo.selectedItem as? OrmValidatorType ?: OrmValidatorType.LENGTH

    private fun updateAvailability() {
        val type = selectedType()
        minField.isEnabled = type == OrmValidatorType.LENGTH || type == OrmValidatorType.RANGE
        maxField.isEnabled = type == OrmValidatorType.LENGTH || type == OrmValidatorType.RANGE
        patternField.isEnabled = type == OrmValidatorType.REGEXP
        targetClassField.isEnabled = type == OrmValidatorType.FOREIGN
        targetField.isEnabled = type == OrmValidatorType.FOREIGN
    }

    private fun buildValidator(): OrmValidator {
        val type = selectedType()
        return OrmValidator(
            type = type,
            min = minField.text.trim().takeIf { it.isNotEmpty() },
            max = maxField.text.trim().takeIf { it.isNotEmpty() },
            pattern = patternField.text.trim().takeIf { it.isNotEmpty() },
            targetClass = targetClassField.text.trim().takeIf { it.isNotEmpty() },
            targetField = targetField.text.trim().uppercase(Locale.ROOT).takeIf { it.isNotEmpty() },
        )
    }
}
