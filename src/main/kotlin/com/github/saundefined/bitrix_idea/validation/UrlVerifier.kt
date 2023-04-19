package com.github.saundefined.bitrix_idea.validation

import com.intellij.ui.JBColor
import javax.swing.*


class UrlVerifier : InputVerifier() {
    override fun verify(input: JComponent): Boolean {
        val text = (input as JTextField).text
        val result = text.matches(Regex("^https?://\\S*\$")) || text.isEmpty()

        if (!result) {
            JOptionPane.showMessageDialog(
                input,
                "Укажите валидный URL",
                "Ошибка валидации",
                JOptionPane.ERROR_MESSAGE
            )
            return false
        }

        return true
    }
}