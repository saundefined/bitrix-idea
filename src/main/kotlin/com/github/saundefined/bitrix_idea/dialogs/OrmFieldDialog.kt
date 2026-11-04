package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmArraySerialization
import com.github.saundefined.bitrix_idea.orm.OrmEntityValidator
import com.github.saundefined.bitrix_idea.orm.OrmField
import com.github.saundefined.bitrix_idea.orm.OrmFieldType
import com.github.saundefined.bitrix_idea.ui.OrmValidatorsTable
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
import java.util.Locale
import javax.swing.JComponent
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.SimpleListCellRenderer

class OrmFieldDialog(
    private val initialField: OrmField,
    private val existingNames: Set<String>,
) : DialogWrapper(true) {
    private val nameField = JBTextField(initialField.name)
    private val typeCombo = ComboBox(OrmFieldType.entries.toTypedArray())
    private val columnField = JBTextField(initialField.columnName.orEmpty())
    private val primaryCheck = JBCheckBox(message("create.orm.field.primary"), initialField.primary)
    private val autocompleteCheck = JBCheckBox(message("create.orm.field.autocomplete"), initialField.autocomplete)
    private val requiredCheck = JBCheckBox(message("create.orm.field.required"), initialField.required)
    private val nullableCheck = JBCheckBox(message("create.orm.field.nullable"), initialField.nullable)
    private val uniqueCheck = JBCheckBox(message("create.orm.field.unique"), initialField.unique)
    private val defaultField = JBTextField(initialField.defaultValue.orEmpty())
    private val defaultNowCheck = JBCheckBox(message("create.orm.field.default.now"), initialField.defaultNow)
    private val sizeField = JBTextField(initialField.size?.toString().orEmpty())
    private val lengthField = JBTextField(initialField.length?.toString().orEmpty())
    private val truncateField = JBTextField(initialField.truncateSize?.toString().orEmpty())
    private val precisionField = JBTextField(initialField.precision?.toString().orEmpty())
    private val scaleField = JBTextField(initialField.scale?.toString().orEmpty())
    private val valuesField = JBTextField(initialField.values.joinToString(", "))
    private val falseValueField = JBTextField(initialField.falseValue)
    private val trueValueField = JBTextField(initialField.trueValue)
    private val formatField = JBTextField(initialField.format.orEmpty())
    private val longCheck = JBCheckBox(message("create.orm.field.long"), initialField.long)
    private val timezoneCheck = JBCheckBox(message("create.orm.field.timezone"), initialField.useTimezone)
    private val serializationCombo = ComboBox(OrmArraySerialization.entries.toTypedArray())
    private val cryptoEnabledCheck = JBCheckBox(message("create.orm.field.crypto.enabled"), initialField.cryptoEnabled)
    private val cryptoKeyField = JBTextField(initialField.cryptoKey.orEmpty())
    private val secretLengthField = JBTextField(initialField.secretLength?.toString().orEmpty())
    private val expressionField = JBTextField(initialField.expression.orEmpty())
    private val expressionFieldsField = JBTextField(initialField.expressionFields.joinToString(", "))
    private val validatorsTable = OrmValidatorsTable(initialField.validators)

    private lateinit var flagsRow: Row
    private lateinit var primaryCell: Cell<JBCheckBox>
    private lateinit var autocompleteCell: Cell<JBCheckBox>
    private lateinit var requiredCell: Cell<JBCheckBox>
    private lateinit var nullableCell: Cell<JBCheckBox>
    private lateinit var uniqueCell: Cell<JBCheckBox>
    private lateinit var defaultRow: Row
    private lateinit var defaultNowRow: Row
    private lateinit var sizeRow: Row
    private lateinit var lengthRow: Row
    private lateinit var truncateRow: Row
    private lateinit var precisionRow: Row
    private lateinit var scaleRow: Row
    private lateinit var valuesRow: Row
    private lateinit var falseValueRow: Row
    private lateinit var trueValueRow: Row
    private lateinit var formatRow: Row
    private lateinit var longRow: Row
    private lateinit var timezoneRow: Row
    private lateinit var serializationRow: Row
    private lateinit var cryptoEnabledRow: Row
    private lateinit var cryptoKeyRow: Row
    private lateinit var secretLengthRow: Row
    private lateinit var expressionRow: Row
    private lateinit var expressionFieldsRow: Row
    private lateinit var validatorsRow: Row

    private val formPanel = panel {
        row(message("create.orm.field.name")) {
            cell(nameField)
                .align(AlignX.FILL)
        }
        row(message("create.orm.field.type")) {
            cell(typeCombo)
                .align(AlignX.FILL)
        }
        row(message("create.orm.field.column")) {
            cell(columnField)
                .align(AlignX.FILL)
        }

        flagsRow = row {
            primaryCell = cell(primaryCheck)
            autocompleteCell = cell(autocompleteCheck)
            requiredCell = cell(requiredCheck)
            nullableCell = cell(nullableCheck)
            uniqueCell = cell(uniqueCheck)
        }

        defaultRow = row(message("create.orm.field.default")) {
            cell(defaultField)
                .align(AlignX.FILL)
        }
        defaultNowRow = row {
            cell(defaultNowCheck)
        }
        sizeRow = row(message("create.orm.field.size")) {
            cell(sizeField)
                .align(AlignX.FILL)
        }
        lengthRow = row(message("create.orm.field.length")) {
            cell(lengthField)
                .align(AlignX.FILL)
        }
        truncateRow = row(message("create.orm.field.truncate")) {
            cell(truncateField)
                .align(AlignX.FILL)
        }
        precisionRow = row(message("create.orm.field.precision")) {
            cell(precisionField)
                .align(AlignX.FILL)
        }
        scaleRow = row(message("create.orm.field.scale")) {
            cell(scaleField)
                .align(AlignX.FILL)
        }
        valuesRow = row(message("create.orm.field.values")) {
            cell(valuesField)
                .align(AlignX.FILL)
        }
        falseValueRow = row(message("create.orm.field.boolean.false")) {
            cell(falseValueField)
                .align(AlignX.FILL)
        }
        trueValueRow = row(message("create.orm.field.boolean.true")) {
            cell(trueValueField)
                .align(AlignX.FILL)
        }
        formatRow = row(message("create.orm.field.format")) {
            cell(formatField)
                .align(AlignX.FILL)
        }
        longRow = row {
            cell(longCheck)
        }
        timezoneRow = row {
            cell(timezoneCheck)
        }
        serializationRow = row(message("create.orm.field.serialization")) {
            cell(serializationCombo)
                .align(AlignX.FILL)
        }
        cryptoEnabledRow = row {
            cell(cryptoEnabledCheck)
        }
        cryptoKeyRow = row(message("create.orm.field.crypto.key")) {
            cell(cryptoKeyField)
                .align(AlignX.FILL)
        }
        secretLengthRow = row(message("create.orm.field.secret.length")) {
            cell(secretLengthField)
                .align(AlignX.FILL)
        }
        expressionRow = row(message("create.orm.field.expression")) {
            cell(expressionField)
                .align(AlignX.FILL)
        }
        expressionFieldsRow = row(message("create.orm.field.expression.fields")) {
            cell(expressionFieldsField)
                .align(AlignX.FILL)
        }
        validatorsRow = row(message("create.orm.field.validators")) {
            cell(validatorsTable.component)
                .align(AlignX.FILL)
        }
    }

    private val contentPanel: JComponent = ScrollPaneFactory.createScrollPane(formPanel).apply {
        preferredSize = Dimension(620, 760)
    }

    private var result: OrmField? = null

    init {
        typeCombo.selectedItem = initialField.type
        serializationCombo.selectedItem = initialField.serialization
        typeCombo.renderer = SimpleListCellRenderer.create("") { value: OrmFieldType -> value.displayName }
        typeCombo.addActionListener { updateAvailability() }
        primaryCheck.addActionListener { updateAvailability() }
        requiredCheck.addActionListener {
            if (requiredCheck.isSelected) nullableCheck.isSelected = false
        }
        nullableCheck.addActionListener {
            if (nullableCheck.isSelected) requiredCheck.isSelected = false
        }
        defaultNowCheck.addActionListener {
            if (defaultNowCheck.isSelected) defaultField.text = ""
            updateAvailability()
        }

        init()
        title = message("create.orm.field.title")
        updateAvailability()
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        if (sizeField.isEnabled && sizeField.text.isNotBlank() && sizeField.text.trim().toIntOrNull() == null) {
            return ValidationInfo(message("create.orm.entity.validation.field.size"), sizeField)
        }
        if (lengthField.isEnabled && lengthField.text.isNotBlank() && lengthField.text.trim().toIntOrNull() == null) {
            return ValidationInfo(message("create.orm.entity.validation.field.length"), lengthField)
        }
        if (truncateField.isEnabled && truncateField.text.isNotBlank() && truncateField.text.trim().toIntOrNull() == null) {
            return ValidationInfo(message("create.orm.entity.validation.field.length"), truncateField)
        }
        if (precisionField.isEnabled && precisionField.text.isNotBlank() && precisionField.text.trim().toIntOrNull() == null) {
            return ValidationInfo(message("create.orm.entity.validation.field.precision"), precisionField)
        }
        if (scaleField.isEnabled && scaleField.text.isNotBlank() && scaleField.text.trim().toIntOrNull() == null) {
            return ValidationInfo(message("create.orm.entity.validation.field.precision"), scaleField)
        }
        if (secretLengthField.isEnabled && secretLengthField.text.isNotBlank() && secretLengthField.text.trim().toIntOrNull() == null) {
            return ValidationInfo(message("create.orm.entity.validation.field.secret"), secretLengthField)
        }

        val error = OrmEntityValidator.fieldError(buildField(), existingNames)
        return error?.let { ValidationInfo(message(it), nameField) }
    }

    override fun doOKAction() {
        result = buildField()
        super.doOKAction()
    }

    fun getField(): OrmField? = result

    private fun selectedType(): OrmFieldType =
        typeCombo.selectedItem as? OrmFieldType ?: OrmFieldType.STRING

    private fun updateAvailability() {
        val type = selectedType()

        primaryCheck.isEnabled = type.supportsPrimary
        autocompleteCheck.isEnabled = type.supportsAutocomplete && primaryCheck.isSelected
        requiredCheck.isEnabled = type.supportsRequired
        nullableCheck.isEnabled = type.supportsNullable
        uniqueCheck.isEnabled = type.supportsUnique
        defaultField.isEnabled = type.supportsDefault && !defaultNowCheck.isSelected
        defaultNowCheck.isEnabled = type.supportsDefaultNow
        sizeField.isEnabled = type.supportsSize
        lengthField.isEnabled = type.supportsLength
        truncateField.isEnabled = type.supportsTruncateSize
        precisionField.isEnabled = type.supportsPrecision
        scaleField.isEnabled = type.supportsPrecision
        valuesField.isEnabled = type.supportsValues
        falseValueField.isEnabled = type.supportsBooleanValues
        trueValueField.isEnabled = type.supportsBooleanValues
        formatField.isEnabled = type.supportsFormat
        longCheck.isEnabled = type.supportsLong
        timezoneCheck.isEnabled = type.supportsTimezone
        serializationCombo.isEnabled = type.supportsSerialization
        cryptoEnabledCheck.isEnabled = type.supportsCrypto
        cryptoKeyField.isEnabled = type.supportsCrypto
        secretLengthField.isEnabled = type.supportsSecret
        expressionField.isEnabled = type.supportsExpression
        expressionFieldsField.isEnabled = type.supportsExpression
        validatorsTable.component.isEnabled = !type.supportsExpression

        flagsRow.visible(type.supportsPrimary || type.supportsRequired || type.supportsNullable || type.supportsUnique)
        primaryCell.visible(type.supportsPrimary)
        autocompleteCell.visible(type.supportsAutocomplete)
        requiredCell.visible(type.supportsRequired)
        nullableCell.visible(type.supportsNullable)
        uniqueCell.visible(type.supportsUnique)
        defaultRow.visible(type.supportsDefault && !defaultNowCheck.isSelected)
        defaultNowRow.visible(type.supportsDefaultNow)
        sizeRow.visible(type.supportsSize)
        lengthRow.visible(type.supportsLength)
        truncateRow.visible(type.supportsTruncateSize)
        precisionRow.visible(type.supportsPrecision)
        scaleRow.visible(type.supportsPrecision)
        valuesRow.visible(type.supportsValues)
        falseValueRow.visible(type.supportsBooleanValues)
        trueValueRow.visible(type.supportsBooleanValues)
        formatRow.visible(type.supportsFormat)
        longRow.visible(type.supportsLong)
        timezoneRow.visible(type.supportsTimezone)
        serializationRow.visible(type.supportsSerialization)
        cryptoEnabledRow.visible(type.supportsCrypto)
        cryptoKeyRow.visible(type.supportsCrypto)
        secretLengthRow.visible(type.supportsSecret)
        expressionRow.visible(type.supportsExpression)
        expressionFieldsRow.visible(type.supportsExpression)
        validatorsRow.visible(!type.supportsExpression)

        if (!type.supportsPrimary) primaryCheck.isSelected = false
        if (!type.supportsAutocomplete) autocompleteCheck.isSelected = false
        if (!type.supportsRequired) requiredCheck.isSelected = false
        if (!type.supportsNullable) nullableCheck.isSelected = false
        if (!type.supportsUnique) uniqueCheck.isSelected = false
        if (!type.supportsDefaultNow) defaultNowCheck.isSelected = false
    }

    private fun buildField(): OrmField {
        val type = selectedType()
        return OrmField(
            name = nameField.text.trim().uppercase(Locale.ROOT),
            type = type,
            primary = primaryCheck.isSelected && type.supportsPrimary,
            autocomplete = autocompleteCheck.isSelected && type.supportsAutocomplete && primaryCheck.isSelected,
            required = requiredCheck.isSelected && type.supportsRequired,
            nullable = nullableCheck.isSelected && type.supportsNullable,
            unique = uniqueCheck.isSelected && type.supportsUnique,
            defaultValue = defaultField.text.trim().takeIf { it.isNotEmpty() && type.supportsDefault && !defaultNowCheck.isSelected },
            defaultNow = defaultNowCheck.isSelected && type.supportsDefaultNow,
            columnName = columnField.text.trim().takeIf { it.isNotEmpty() },
            size = sizeField.text.trim().toIntOrNull().takeIf { type.supportsSize },
            length = lengthField.text.trim().toIntOrNull().takeIf { type.supportsLength },
            truncateSize = truncateField.text.trim().toIntOrNull().takeIf { type.supportsTruncateSize },
            precision = precisionField.text.trim().toIntOrNull().takeIf { type.supportsPrecision },
            scale = scaleField.text.trim().toIntOrNull().takeIf { type.supportsPrecision },
            values = if (type.supportsValues) {
                valuesField.text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            },
            falseValue = falseValueField.text.trim().ifBlank { "N" },
            trueValue = trueValueField.text.trim().ifBlank { "Y" },
            format = formatField.text.trim().takeIf { it.isNotEmpty() && type.supportsFormat },
            long = longCheck.isSelected && type.supportsLong,
            useTimezone = timezoneCheck.isSelected,
            serialization = serializationCombo.selectedItem as? OrmArraySerialization ?: OrmArraySerialization.JSON,
            cryptoEnabled = cryptoEnabledCheck.isSelected,
            cryptoKey = cryptoKeyField.text.trim().takeIf { it.isNotEmpty() },
            secretLength = secretLengthField.text.trim().toIntOrNull()?.takeIf { type.supportsSecret }
                ?: 16.takeIf { type.supportsSecret },
            expression = expressionField.text.trim().takeIf { it.isNotEmpty() && type.supportsExpression },
            expressionFields = if (type.supportsExpression) {
                expressionFieldsField.text.split(',').map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            },
            validators = validatorsTable.items,
        )
    }
}
