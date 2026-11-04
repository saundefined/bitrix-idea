package com.github.saundefined.bitrix_idea.orm

import org.junit.Assert.assertEquals
import org.junit.Test

class OrmEntityContextResolverTest {
    @Test
    fun resolvesLocalModuleNamespace() {
        val context = OrmEntityContextResolver.resolvePath(
            "/project/local/modules/vendor.module/lib/Orm",
        )

        assertEquals("Vendor\\Module\\Orm", context.namespace)
        assertEquals("vendor_module_", context.tablePrefix)
    }

    @Test
    fun resolvesLocalModuleRootNamespace() {
        val context = OrmEntityContextResolver.resolvePath(
            "/project/local/modules/vendor.module/lib",
        )

        assertEquals("Vendor\\Module", context.namespace)
        assertEquals("vendor_module_", context.tablePrefix)
    }

    @Test
    fun resolvesSingleSegmentLocalModule() {
        val context = OrmEntityContextResolver.resolvePath(
            "/project/local/modules/vendor/lib",
        )

        assertEquals("Bitrix\\Vendor", context.namespace)
        assertEquals("vendor_", context.tablePrefix)
    }

    @Test
    fun resolvesBitrixModuleNamespaceUsingModuleCode() {
        val context = OrmEntityContextResolver.resolvePath(
            "/project/bitrix/modules/vendor.module/lib/Orm",
        )

        assertEquals("Vendor\\Module\\Orm", context.namespace)
        assertEquals("vendor_module_", context.tablePrefix)
    }

    @Test
    fun resolvesSingleSegmentBitrixModule() {
        val context = OrmEntityContextResolver.resolvePath(
            "/project/bitrix/modules/main/lib",
        )

        assertEquals("Bitrix\\Main", context.namespace)
        assertEquals("main_", context.tablePrefix)
    }

    @Test
    fun returnsEmptyContextOutsideModules() {
        val context = OrmEntityContextResolver.resolvePath(
            "/project/local/php_interface",
        )

        assertEquals("", context.namespace)
        assertEquals("", context.tablePrefix)
    }
}
