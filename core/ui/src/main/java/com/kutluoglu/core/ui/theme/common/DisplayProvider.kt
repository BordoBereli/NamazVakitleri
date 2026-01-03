package com.kutluoglu.core.ui.theme.common

/**
 * Created by F.K. on 2.01.2026.
 *
 */

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Single // Factory yerine Single kullanacağız
import java.lang.ref.WeakReference

/**
 * Provides the current Activity's Display object.
 * This is a Singleton that holds a WeakReference to the current Activity
 * to prevent memory leaks. Its context is updated via ActivityLifecycleCallbacks.
 */
@Single
class DisplayProvider {
    private var currentActivity: WeakReference<Activity>? = null

    /**
     * This method is called from the Application class to set the current foreground Activity.
     */
    fun setCurrentActivity(activity: Activity) {
        this.currentActivity = WeakReference(activity)
    }

    /**
     * Provides the Display object from the current Activity.
     * Throws an exception if the Activity is not available.
     */
    fun display(): Display {
        val activity = currentActivity?.get()
            ?: throw IllegalStateException("Activity is not available. Ensure setCurrentActivity is called.")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            (activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }
    }
}
