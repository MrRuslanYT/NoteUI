package com.pandacorp.noteui.presentation.utils.helpers

import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.pandacorp.noteui.presentation.di.app.App

val Fragment.sp: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(requireContext())

val Fragment.app get() = (requireActivity().application as App)

fun Toolbar.hideToolbarWhileScrollingUseCase(isHide: Boolean) {
    val layoutParams = layoutParams as AppBarLayout.LayoutParams
    if (isHide)
        layoutParams.scrollFlags =
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
    else layoutParams.scrollFlags = 0
    this.layoutParams = layoutParams
}

fun ExtendedFloatingActionButton.showFabIfHidden() {
    val layoutParams = layoutParams as CoordinatorLayout.LayoutParams
    val behavior = layoutParams.behavior as HideBottomViewOnScrollBehavior
    behavior.slideUp(this)
}

/**
 * A compatibility wrapper around PackageManager's `getPackageInfo()` method that allows
 * developers to use either the old flag-based API or the new enum-based API depending on the
 * version of Android running on the device.
 *
 * @param packageName The name of the package for which to retrieve package information.
 * @param flags Additional flags to control the behavior of the method.
 * @return A PackageInfo object containing information about the specified package.
 */
fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }

/**
 * A compatibility wrapper around Bundle's `getParcelableExtra()` method that allows
 * developers to get a Parcelable extra from an Bundle object regardless of the version of
 * Android running on the device.
 *
 * @param name The name of the extra to retrieve.
 * @param clazz The class of the extra to retrieve.
 * @return The Parcelable extra with the specified name and class, or null if it does not exist.
 */
inline fun <reified T : Parcelable> Bundle.getParcelableExtraSupport(name: String, clazz: Class<T>): T? {
    val extra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelable(name, clazz)
    else @Suppress("DEPRECATION") getParcelable(name) as? T
    if (extra is T) return extra
    return null
}