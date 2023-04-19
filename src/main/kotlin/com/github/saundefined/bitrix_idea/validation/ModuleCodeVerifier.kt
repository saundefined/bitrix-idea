package com.github.saundefined.bitrix_idea.validation

import com.intellij.ui.JBColor
import javax.swing.*


class ModuleCodeVerifier : InputVerifier() {
    override fun verify(input: JComponent): Boolean {
        val text = (input as JTextField).text
        val result = text.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]+$"))

        if (!result) {
            JOptionPane.showMessageDialog(
                input,
                "Код модуля должен состоять только из латинских букв, цифр, точки и нижнего подчеркивания и не начинаться с цифры",
                "Ошибка валидации",
                JOptionPane.ERROR_MESSAGE
            )
            return false
        }

        return true
    }
}