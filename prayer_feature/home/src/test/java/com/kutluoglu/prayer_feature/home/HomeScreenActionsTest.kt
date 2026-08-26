package com.kutluoglu.prayer_feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HomeScreenActionsTest {

    @Test
    fun `granted permission triggers use my location`() {
        var action: String? = null
        resolveUseMyLocationAction(
            allPermissionsGranted = true,
            permanentlyDenied = false,
            onUseMyLocation = { action = "use" },
            openSettings = { action = "settings" },
            requestPermission = { action = "request" }
        )
        assertThat(action).isEqualTo("use")
    }

    @Test
    fun `permanently denied opens settings`() {
        var action: String? = null
        resolveUseMyLocationAction(
            allPermissionsGranted = false,
            permanentlyDenied = true,
            onUseMyLocation = { action = "use" },
            openSettings = { action = "settings" },
            requestPermission = { action = "request" }
        )
        assertThat(action).isEqualTo("settings")
    }

    @Test
    fun `not granted and not denied requests permission`() {
        var action: String? = null
        resolveUseMyLocationAction(
            allPermissionsGranted = false,
            permanentlyDenied = false,
            onUseMyLocation = { action = "use" },
            openSettings = { action = "settings" },
            requestPermission = { action = "request" }
        )
        assertThat(action).isEqualTo("request")
    }
}
