package com.github.saundefined.bitrix_idea.dialogs

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.orm.OrmEntityValidator
import com.github.saundefined.bitrix_idea.orm.OrmRelation
import com.github.saundefined.bitrix_idea.orm.OrmRelationType
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

class OrmRelationDialog(
    private val initialRelation: OrmRelation,
    private val existingNames: Set<String>,
    private val scalarFieldNames: Set<String>,
) : DialogWrapper(true) {
    private val nameField = JBTextField(initialRelation.name)
    private val typeCombo = ComboBox(OrmRelationType.entries.toTypedArray())
    private val targetField = JBTextField(initialRelation.targetEntity)
    private val localField = JBTextField(initialRelation.localField)
    private val remoteField = JBTextField(initialRelation.remoteField)
    private val referenceField = JBTextField(initialRelation.referenceField)
    private val mediatorField = JBTextField(initialRelation.mediatorTable)
    private val joinTypeCombo = ComboBox(arrayOf("left", "inner", "right"))
    private val localPrimaryField = JBTextField(initialRelation.localPrimary)
    private val localPrimaryColumnField = JBTextField(initialRelation.localPrimaryColumn)
    private val localReferenceField = JBTextField(initialRelation.localReference)
    private val remotePrimaryField = JBTextField(initialRelation.remotePrimary)
    private val remotePrimaryColumnField = JBTextField(initialRelation.remotePrimaryColumn)
    private val remoteReferenceField = JBTextField(initialRelation.remoteReference)

    private val contentPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(message("create.orm.relation.name"), nameField)
        .addLabeledComponent(message("create.orm.relation.type"), typeCombo)
        .addLabeledComponent(message("create.orm.relation.target"), targetField)
        .addLabeledComponent(message("create.orm.relation.local"), localField)
        .addLabeledComponent(message("create.orm.relation.remote"), remoteField)
        .addLabeledComponent(message("create.orm.relation.reference"), referenceField)
        .addLabeledComponent(message("create.orm.relation.mediator"), mediatorField)
        .addLabeledComponent(message("create.orm.relation.join"), joinTypeCombo)
        .addLabeledComponent(message("create.orm.relation.local.primary"), localPrimaryField)
        .addLabeledComponent(message("create.orm.relation.local.primary.column"), localPrimaryColumnField)
        .addLabeledComponent(message("create.orm.relation.local.reference"), localReferenceField)
        .addLabeledComponent(message("create.orm.relation.remote.primary"), remotePrimaryField)
        .addLabeledComponent(message("create.orm.relation.remote.primary.column"), remotePrimaryColumnField)
        .addLabeledComponent(message("create.orm.relation.remote.reference"), remoteReferenceField)
        .panel
        .apply { preferredSize = Dimension(560, 470) }

    private var result: OrmRelation? = null

    init {
        typeCombo.selectedItem = initialRelation.type
        joinTypeCombo.selectedItem = initialRelation.joinType
        typeCombo.renderer = SimpleListCellRenderer.create("") { value: OrmRelationType -> value.displayName }
        typeCombo.addActionListener { updateAvailability() }

        init()
        title = message("create.orm.relation.title")
        updateAvailability()
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        val error = OrmEntityValidator.relationError(
            buildRelation(),
            existingNames,
            scalarFieldNames,
        )
        return error?.let { ValidationInfo(message(it), nameField) }
    }

    override fun doOKAction() {
        result = buildRelation()
        super.doOKAction()
    }

    fun getRelation(): OrmRelation? = result

    private fun selectedType(): OrmRelationType =
        typeCombo.selectedItem as? OrmRelationType ?: OrmRelationType.REFERENCE

    private fun updateAvailability() {
        val type = selectedType()
        val reference = type == OrmRelationType.REFERENCE
        val oneToMany = type == OrmRelationType.ONE_TO_MANY
        val manyToMany = type == OrmRelationType.MANY_TO_MANY

        localField.isEnabled = reference
        remoteField.isEnabled = reference
        referenceField.isEnabled = oneToMany
        mediatorField.isEnabled = manyToMany
        joinTypeCombo.isEnabled = true
        localPrimaryField.isEnabled = manyToMany
        localPrimaryColumnField.isEnabled = manyToMany
        localReferenceField.isEnabled = manyToMany
        remotePrimaryField.isEnabled = manyToMany
        remotePrimaryColumnField.isEnabled = manyToMany
        remoteReferenceField.isEnabled = manyToMany
    }

    private fun buildRelation(): OrmRelation {
        val type = selectedType()
        return OrmRelation(
            name = nameField.text.trim().uppercase(Locale.ROOT),
            type = type,
            targetEntity = targetField.text.trim(),
            localField = if (type == OrmRelationType.REFERENCE) {
                localField.text.trim().uppercase(Locale.ROOT)
            } else {
                ""
            },
            remoteField = if (type == OrmRelationType.REFERENCE) {
                remoteField.text.trim().uppercase(Locale.ROOT).ifBlank { "ID" }
            } else {
                "ID"
            },
            referenceField = if (type == OrmRelationType.ONE_TO_MANY) {
                referenceField.text.trim().uppercase(Locale.ROOT)
            } else {
                ""
            },
            mediatorTable = if (type == OrmRelationType.MANY_TO_MANY) {
                mediatorField.text.trim()
            } else {
                ""
            },
            joinType = joinTypeCombo.selectedItem?.toString() ?: "left",
            localPrimary = if (type == OrmRelationType.MANY_TO_MANY) {
                localPrimaryField.text.trim().uppercase(Locale.ROOT).ifBlank { "ID" }
            } else {
                "ID"
            },
            localPrimaryColumn = if (type == OrmRelationType.MANY_TO_MANY) {
                localPrimaryColumnField.text.trim().uppercase(Locale.ROOT)
            } else {
                ""
            },
            localReference = if (type == OrmRelationType.MANY_TO_MANY) {
                localReferenceField.text.trim().uppercase(Locale.ROOT)
            } else {
                ""
            },
            remotePrimary = if (type == OrmRelationType.MANY_TO_MANY) {
                remotePrimaryField.text.trim().uppercase(Locale.ROOT).ifBlank { "ID" }
            } else {
                "ID"
            },
            remotePrimaryColumn = if (type == OrmRelationType.MANY_TO_MANY) {
                remotePrimaryColumnField.text.trim().uppercase(Locale.ROOT)
            } else {
                ""
            },
            remoteReference = if (type == OrmRelationType.MANY_TO_MANY) {
                remoteReferenceField.text.trim().uppercase(Locale.ROOT)
            } else {
                ""
            },
        )
    }
}
