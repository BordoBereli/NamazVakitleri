package com.kutluoglu.namazvakitleri.locale

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.namazvakitleri.appModule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get

class LocaleManagerKoinTest : KoinTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `LocaleManager is resolvable from app module`() {
        startKoin { modules(appModule) }
        val manager = get<LocaleManager>()
        assertThat(manager).isNotNull()
    }
}
