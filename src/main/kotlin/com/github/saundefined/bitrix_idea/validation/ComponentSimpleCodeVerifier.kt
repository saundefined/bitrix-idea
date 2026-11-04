package com.github.saundefined.bitrix_idea.validation

object ComponentSimpleCodeVerifier {
    fun verify(input: String): Boolean {
        return input.matches(Regex("^[a-z][a-z0-9]*([._-][a-z0-9]+)*$"))
    }
}
