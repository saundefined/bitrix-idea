package com.github.saundefined.bitrix_idea.validation

class ComponentSimpleCodeVerifier {
    fun verify(input: String): Boolean {
        return input.matches(Regex("^[a-zA-Z0-9_.]+[a-zA-Z0-9]+\$")) && input.isNotBlank()
    }
}