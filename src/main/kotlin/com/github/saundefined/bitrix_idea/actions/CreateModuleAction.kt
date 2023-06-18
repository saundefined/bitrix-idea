package com.github.saundefined.bitrix_idea.actions

import com.github.saundefined.bitrix_idea.BitrixIdeaBundle.message
import com.github.saundefined.bitrix_idea.validation.ModuleCodeVerifier
import com.github.saundefined.bitrix_idea.validation.UrlVerifier
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import icons.license.CheckLicense
import org.jetbrains.annotations.NotNull
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class CreateModuleAction : AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        if (CheckLicense.isLicensed == false) {
            CheckLicense.requestLicense(message("license.request"))
            return
        }

        val frame = JFrame()
        val panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.anchor = GridBagConstraints.WEST
        constraints.insets = JBUI.insets(10)

        val codeField = JBTextField(53)
        codeField.inputVerifier = ModuleCodeVerifier()
        
        val nameField = JBTextField(53)
        val descriptionField = JBTextField(53)

        val vendorField = JBTextField(20)
        val urlField = JBTextField(20)
        urlField.inputVerifier = UrlVerifier()

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(JBLabel(message("create.module.code")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(codeField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(JBLabel(message("create.module.name")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        panel.add(JBLabel(message("create.module.description")), constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        panel.add(descriptionField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        panel.add(JBLabel(message("create.module.vendor")), constraints);

        constraints.gridx = 1;
        panel.add(vendorField, constraints);

        constraints.gridx = 2;
        panel.add(JBLabel(message("create.module.url")), constraints);

        constraints.gridx = 3;
        panel.add(urlField, constraints);


        val result = JOptionPane.showConfirmDialog(
            frame,
            panel,
            "Please Enter the Following",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )
        if (result == JOptionPane.OK_OPTION) {
            val vendor = vendorField.text
            val name = nameField.text
            val description = descriptionField.text
            // Do something with the input data
        }

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}