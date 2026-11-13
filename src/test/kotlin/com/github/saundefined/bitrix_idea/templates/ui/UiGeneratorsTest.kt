package com.github.saundefined.bitrix_idea.templates.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class UiGeneratorsTest {
    @Test
    fun generatesGridFilterAndForm() {
        val fields = listOf("ID", "NAME", "ACTIVE")

        val grid = UiGenerators.generateGridConfig("Vendor\\Module", "Items", "ItemsTable", fields)
        val filter = UiGenerators.generateFilterConfig("Vendor\\Module", "Items", fields)
        val form = UiGenerators.generateFormConfig("Vendor\\Module", "Items", fields)

        assertTrue(grid.contains("namespace Vendor\\Module\\Ui;"))
        assertTrue(grid.contains("ItemsTable::getList"))
        assertTrue(grid.contains("'ID'"))
        assertTrue(filter.contains("'type' => 'string'"))
        assertTrue(form.contains("'label' => 'NAME'"))
    }

    @Test
    fun generatesAdminPages() {
        val list = UiGenerators.generateListPage("vendor.module", "Vendor\\Module", "Items", "Элементы")
        val edit = UiGenerators.generateEditPage("vendor.module", "Vendor\\Module", "Items", "Элемент")

        assertTrue(list.contains("CAdminUiList"))
        assertTrue(list.contains("ItemsGrid::getRows"))
        assertTrue(edit.contains("CAdminForm"))
        assertTrue(edit.contains("ItemsForm::getFields"))
    }
}
