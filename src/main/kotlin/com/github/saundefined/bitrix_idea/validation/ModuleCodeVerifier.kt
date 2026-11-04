package com.github.saundefined.bitrix_idea.validation

object ModuleCodeVerifier {
    fun verify(input: String): Boolean {
        return input.matches(Regex("^[a-z][a-z0-9]*(\\.[a-z0-9]+)*$"))
    }
}
