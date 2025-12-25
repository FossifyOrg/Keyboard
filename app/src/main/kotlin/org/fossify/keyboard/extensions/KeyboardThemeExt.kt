package org.fossify.keyboard.extensions

import android.content.Context
import android.graphics.Color
import org.fossify.commons.extensions.darkenColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.isSystemInDarkMode
import org.fossify.commons.extensions.lightenColor
import org.fossify.keyboard.R

fun Context.getKeyboardBackgroundColor(): Int {
    val color = if (isDynamicTheme()) {
        resources.getColor(R.color.you_keyboard_background_color, theme)
    } else {
        getProperBackgroundColor().darkenColor(2)
    }

    // use darker background color when key borders are enabled
    if (config.showKeyBorders) {
        val darkerColor = color.darkenColor(2)
        return if (darkerColor == Color.WHITE) {
            resources.getColor(R.color.md_grey_200, theme)
        } else {
            darkerColor
        }
    }

    return color
}

fun Context.getStrokeColor(): Int {
    return if (isDynamicTheme()) {
        if (isSystemInDarkMode()) {
            resources.getColor(R.color.md_grey_800, theme)
        } else {
            resources.getColor(R.color.md_grey_400, theme)
        }
    } else {
        val lighterColor = safeStorageContext.getProperBackgroundColor().lightenColor()
        if (lighterColor == Color.WHITE || lighterColor == Color.BLACK) {
            resources.getColor(R.color.divider_grey, theme)
        } else {
            lighterColor
        }
    }
}
