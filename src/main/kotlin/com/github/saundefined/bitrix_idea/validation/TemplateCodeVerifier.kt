package com.github.saundefined.bitrix_idea.validation

class TemplateCodeVerifier {
    fun verify(input: String): Boolean {
        return input.matches(Regex("^[a-zA-Z0-9_]+$")) && input.isNotBlank()
    }
}