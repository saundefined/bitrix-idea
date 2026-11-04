package com.github.saundefined.bitrix_idea.settings

import org.junit.Assert.assertFalse
import org.junit.Test

class BitrixProjectSettingsTest {
    @Test
    fun bitrixFrameworkIsDisabledByDefault() {
        val settings = BitrixProjectSettings()

        assertFalse(settings.bitrixFrameworkEnabled)
        assertFalse(settings.bitrixDetectionNotificationShown)
    }
}
