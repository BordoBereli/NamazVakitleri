package com.kutluoglu.prayer_feature.settings.juristic

import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateJuristicMethodUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineRule::class)
class JuristicMethodViewModelTest {

    private val getSettings = mockk<GetSettingsUseCase>(relaxed = true)
    private val updateMethod = mockk<UpdateJuristicMethodUseCase>(relaxed = true)

    @Test
    fun `load exposes current method`() = runTest {
        coEvery { getSettings() } returns Settings(juristicMethod = "HANAFI")
        val vm = JuristicMethodViewModel(getSettings, updateMethod)
        vm.load()
        assertEquals("HANAFI", vm.currentMethod.value)
    }

    @Test
    fun `selecting method persists it`() = runTest {
        val vm = JuristicMethodViewModel(getSettings, updateMethod)
        vm.onEvent(JuristicMethodEvent.SelectMethod("HANAFI"))
        coVerify { updateMethod("HANAFI") }
    }
}
