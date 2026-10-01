package com.kutluoglu.namazvakitleri

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class AgpVersionTest {

    @Test
    fun `AGP version is 9 or higher`() {
        val toml = File(System.getProperty("user.dir")).let { dir ->
            generateSequence(dir) { it.parentFile }
                .map { File(it, "gradle/libs.versions.toml") }
                .firstOrNull { it.exists() }
        }
        assertThat(toml).isNotNull()
        val agpVersion = toml!!.readLines()
            .firstOrNull { it.trimStart().startsWith("agp =") }
            ?.substringAfter("=")?.trim()?.trim('"')
        assertThat(agpVersion).isNotNull()
        assertThat(agpVersion).matches("9\\..*")
    }
}
