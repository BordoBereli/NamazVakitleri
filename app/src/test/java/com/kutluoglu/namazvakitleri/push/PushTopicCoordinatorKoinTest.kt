package com.kutluoglu.namazvakitleri.push

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.namazvakitleri.appModule
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_notifications.push.TopicSubscriptionManager
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class PushTopicCoordinatorKoinTest {

    @Test
    fun `PushTopicCoordinator resolves from the app module`() {
        val koin = koinApplication {
            modules(
                appModule,
                module {
                    single<TopicSubscriptionManager> { mockk(relaxed = true) }
                    single<LocationsCoordinator> { mockk(relaxed = true) }
                }
            )
        }.koin

        assertThat(koin.get<PushTopicCoordinator>()).isNotNull()
    }
}
