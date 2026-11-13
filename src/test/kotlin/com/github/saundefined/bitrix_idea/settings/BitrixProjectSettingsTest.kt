package com.github.saundefined.bitrix_idea.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitrixProjectSettingsTest {
    @Test
    fun bitrixFrameworkIsDisabledByDefault() {
        val settings = BitrixProjectSettings()

        assertFalse(settings.bitrixFrameworkEnabled)
        assertFalse(settings.bitrixDetectionNotificationShown)
        assertEquals(BitrixCoreSource.BUNDLED, settings.coreSource)
        assertTrue(settings.excludedComponentIds.isEmpty())
        assertTrue(settings.excludedSiteTemplates.isEmpty())
    }
}
