package com.github.saundefined.bitrix_idea.templates.d7

import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceGeneratorTest {
    @Test
    fun generatesInterfaceAndImplementation() {
        val interfaceCode = ServiceGenerator.generateInterface("Vendor\\Module", "PaymentInterface")
        val classCode = ServiceGenerator.generateImplementation("Vendor\\Module", "PaymentInterface", "PaymentService")

        assertTrue(interfaceCode.contains("namespace Vendor\\Module;"))
        assertTrue(interfaceCode.contains("interface PaymentInterface"))
        assertTrue(classCode.contains("class PaymentService implements PaymentInterface"))
    }
}
