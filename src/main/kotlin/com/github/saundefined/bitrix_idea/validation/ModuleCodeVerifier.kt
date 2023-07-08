package com.github.saundefined.bitrix_idea.validation

class ModuleCodeVerifier {
    fun verify(input: String): Boolean {
        return input.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.?[a-zA-Z0-9_]+)*$")) && input.isNotBlank()
    }
}