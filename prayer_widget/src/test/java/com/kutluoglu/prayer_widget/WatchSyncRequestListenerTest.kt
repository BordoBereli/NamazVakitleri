package com.kutluoglu.prayer_widget

import com.google.android.gms.wearable.MessageEvent
import com.kutluoglu.wear.shared.data.WatchTileDataCodec
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WatchSyncRequestListenerTest {

    @Test
    fun `sync request path triggers watch data sync`() = runTest {
        val syncer = mockk<WatchDataSyncer>(relaxed = true)
        startKoin { modules(module { single { syncer } }) }
        try {
            val listener = WatchSyncRequestListener(
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            )
            val event = mockk<MessageEvent> {
                every { path } returns WatchTileDataCodec.SYNC_REQUEST_PATH
            }

            listener.onMessageReceived(event)

            coVerify { syncer.sync() }
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `other paths are ignored`() = runTest {
        val syncer = mockk<WatchDataSyncer>(relaxed = true)
        startKoin { modules(module { single { syncer } }) }
        try {
            val listener = WatchSyncRequestListener(
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            )
            val event = mockk<MessageEvent> {
                every { path } returns "/some/other/path"
            }

            listener.onMessageReceived(event)

            coVerify(exactly = 0) { syncer.sync() }
        } finally {
            stopKoin()
        }
    }
}
