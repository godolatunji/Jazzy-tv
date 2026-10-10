package com.nexient.jazzy

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.nexient.jazzy.core.designsystem.theme.FormFactor

object FormFactors {
    fun isTv(context: Context): Boolean {
        // Debug builds only: `adb shell settings put global jazzy_force_tv 1` previews the TV UI on a phone/emulator.
        if (BuildConfig.DEBUG && android.provider.Settings.Global.getInt(context.contentResolver, "jazzy_force_tv", 0) == 1) return true
        val uiMode = context.getSystemService(UiModeManager::class.java)?.currentModeType
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    fun current(context: Context, widthDp: Int): FormFactor = when {
        isTv(context) -> FormFactor.TV
        widthDp >= 600 -> FormFactor.TABLET
        else -> FormFactor.PHONE
    }
}
