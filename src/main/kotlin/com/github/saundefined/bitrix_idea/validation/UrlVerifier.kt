package com.github.saundefined.bitrix_idea.validation

class UrlVerifier {
    fun verify(input: String): Boolean {
        return input.matches(Regex("^https?://\\S*\$")) || input.isEmpty()
    }
}