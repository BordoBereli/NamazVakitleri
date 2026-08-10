package com.kutluoglu.core.designsystem.utils

import android.app.Activity
import android.content.Context
import android.view.Display
import android.view.WindowManager
import org.koin.core.annotation.Single
import java.lang.ref.WeakReference

@Single
class DisplayProvider(private val context: Context) {
    private var activityRef: WeakReference<Activity>? = null

    fun display(): Display {
        val activity = activityRef?.get()
        return if (activity != null) {
            activity.windowManager.defaultDisplay
        } else {
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }
    }

    fun setCurrentActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun getCurrentActivity(): Activity? = activityRef?.get()
}
