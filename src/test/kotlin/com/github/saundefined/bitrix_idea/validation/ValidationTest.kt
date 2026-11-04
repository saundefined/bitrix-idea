package com.github.saundefined.bitrix_idea.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {
    @Test
    fun moduleCodeAcceptsVendorModuleNames() {
        assertTrue(ModuleCodeVerifier.verify("module"))
        assertTrue(ModuleCodeVerifier.verify("vendor.module"))
        assertTrue(ModuleCodeVerifier.verify("vendor.module2"))
    }

    @Test
    fun moduleCodeRejectsInvalidNames() {
        assertFalse(ModuleCodeVerifier.verify(""))
        assertFalse(ModuleCodeVerifier.verify(".module"))
        assertFalse(ModuleCodeVerifier.verify("1module"))
        assertFalse(ModuleCodeVerifier.verify("vendor..module"))
        assertFalse(ModuleCodeVerifier.verify("vendor-module"))
        assertFalse(ModuleCodeVerifier.verify("vendor.module_name"))
        assertFalse(ModuleCodeVerifier.verify("Vendor.Module"))
    }

    @Test
    fun templateCodeAcceptsLatinLettersDigitsAndSeparators() {
        assertTrue(TemplateCodeVerifier.verify("my-template_1"))
        assertFalse(TemplateCodeVerifier.verify(""))
        assertFalse(TemplateCodeVerifier.verify("my template"))
        assertFalse(TemplateCodeVerifier.verify("шаблон"))
    }

    @Test
    fun componentCodeRequiresLowercaseName() {
        assertTrue(ComponentSimpleCodeVerifier.verify("component1"))
        assertTrue(ComponentSimpleCodeVerifier.verify("vendor.component"))
        assertTrue(ComponentSimpleCodeVerifier.verify("component-name"))
        assertTrue(ComponentSimpleCodeVerifier.verify("component_name"))
        assertFalse(ComponentSimpleCodeVerifier.verify(""))
        assertFalse(ComponentSimpleCodeVerifier.verify("component."))
        assertFalse(ComponentSimpleCodeVerifier.verify("Component"))
    }

    @Test
    fun componentVendorRequiresLowercase() {
        assertTrue(ComponentVendorVerifier.verify("my"))
        assertTrue(ComponentVendorVerifier.verify("my_vendor"))
        assertFalse(ComponentVendorVerifier.verify(""))
        assertFalse(ComponentVendorVerifier.verify("My"))
        assertFalse(ComponentVendorVerifier.verify("my-vendor"))
    }

    @Test
    fun urlRequiresHttpSchemeAndNonEmptyValue() {
        assertTrue(UrlVerifier.verify("http://example.com"))
        assertTrue(UrlVerifier.verify("https://example.com/path"))
        assertFalse(UrlVerifier.verify("example.com"))
        assertFalse(UrlVerifier.verify("ftp://example.com"))
        assertFalse(UrlVerifier.verify("https://"))
    }
}
