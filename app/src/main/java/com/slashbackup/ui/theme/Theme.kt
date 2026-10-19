package com.slashbackup.ui.theme

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object ObsidianTheme {
    val BgPrimary = Color.parseColor("#090D16")
    val BgCard = Color.parseColor("#111827")
    val BgElevated = Color.parseColor("#1E293B")
    val BgTerminal = Color.parseColor("#05080F")
    val BorderSubtle = Color.parseColor("#1F2937")
    val BorderEmerald = Color.parseColor("#059669")

    val EmeraldPrimary = Color.parseColor("#10B981")
    val EmeraldDark = Color.parseColor("#064E3B")
    val CyanAccent = Color.parseColor("#38BDF8")
    val CyanDark = Color.parseColor("#0C4A6E")
    val AmberWarning = Color.parseColor("#F59E0B")
    val AmberDark = Color.parseColor("#451A03")
    val CoralDanger = Color.parseColor("#EF4444")

    val TextPrimary = Color.parseColor("#F8FAFC")
    val TextSecondary = Color.parseColor("#94A3B8")
    val TextMuted = Color.parseColor("#64748B")

    fun dp(context: Context, value: Int): Int {
        val density = context.resources.displayMetrics.density
        return (value * density + 0.5f).toInt()
    }

    fun roundedBox(
        context: Context,
        fillColor: Int,
        strokeColor: Int = Color.TRANSPARENT,
        strokeWidthDp: Int = 1,
        cornerRadiusDp: Int = 12
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = dp(context, cornerRadiusDp).toFloat()
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(context, strokeWidthDp), strokeColor)
            }
        }
    }
}
