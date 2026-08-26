package com.kutluoglu.app_update.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.annotation.Factory

@Factory
class UpdateUrlOpener(
    private val context: Context,
) {

    fun open(url: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolved = context.packageManager.resolveActivity(intent, 0)
            if (resolved != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
