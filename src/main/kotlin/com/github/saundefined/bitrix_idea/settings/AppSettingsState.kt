package com.github.saundefined.bitrix_idea.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


@State(
    name = "com.github.saundefined.bitrix_idea.settings.AppSettingsState",
    storages = [Storage("TestBitrixIdeaSettingsPlugin.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {
    var vendorCode: String = ""
    var vendorName: String = ""
    var vendorWebsite: String = ""
    var languages: List<String> = listOf("ru")

    @Nullable
    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(@NotNull state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getInstance(): AppSettingsState {
        return ApplicationManager.getApplication()
            .getService(AppSettingsState::class.java)
    }
}