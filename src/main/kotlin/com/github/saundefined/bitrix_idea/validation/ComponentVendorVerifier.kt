package com.github.saundefined.bitrix_idea.validation

object ComponentVendorVerifier {
    fun verify(input: String): Boolean {
        return input.matches(Regex("^[a-z][a-z0-9_]*$"))
    }
}
