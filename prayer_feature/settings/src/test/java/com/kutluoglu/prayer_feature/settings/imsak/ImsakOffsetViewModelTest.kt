package com.kutluoglu.prayer_feature.settings.imsak

import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateImsakOffsetUseCase
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
class ImsakOffsetViewModelTest {

    private val getSettings = mockk<GetSettingsUseCase>(relaxed = true)
    private val updateOffset = mockk<UpdateImsakOffsetUseCase>(relaxed = true)

    @Test
    fun `load exposes current offset`() = runTest {
        coEvery { getSettings() } returns Settings(imsakOffsetMinutes = 15)
        val vm = ImsakOffsetViewModel(getSettings, updateOffset)
        vm.load()
        assertEquals(15, vm.currentOffset.value)
    }

    @Test
    fun `confirm persists offset`() = runTest {
        val vm = ImsakOffsetViewModel(getSettings, updateOffset)
        vm.onEvent(ImsakOffsetEvent.OnOffsetChanged(20))
        vm.onEvent(ImsakOffsetEvent.OnConfirm)
        coVerify { updateOffset(20) }
    }
}
