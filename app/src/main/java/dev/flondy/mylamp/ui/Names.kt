package dev.flondy.mylamp.ui

import androidx.annotation.StringRes
import dev.flondy.mylamp.R
import dev.flondy.mylamp.data.ScreenEffect
import dev.flondy.mylamp.data.ScreenLightColor
import dev.flondy.mylamp.torch.LightEffect

@StringRes
fun effectName(e: LightEffect): Int = when (e) {
    LightEffect.BLINK -> R.string.effect_blink
    LightEffect.HEARTBEAT -> R.string.effect_heartbeat
    LightEffect.BEACON -> R.string.effect_beacon
    LightEffect.SIGNAL -> R.string.effect_signal
}

@StringRes
fun effectDescription(e: LightEffect): Int = when (e) {
    LightEffect.BLINK -> R.string.effect_blink_desc
    LightEffect.HEARTBEAT -> R.string.effect_heartbeat_desc
    LightEffect.BEACON -> R.string.effect_beacon_desc
    LightEffect.SIGNAL -> R.string.effect_signal_desc
}

@StringRes
fun screenColorName(c: ScreenLightColor): Int = when (c) {
    ScreenLightColor.NEUTRAL -> R.string.color_neutral
    ScreenLightColor.WARM -> R.string.color_warm
    ScreenLightColor.COOL -> R.string.color_cool
    ScreenLightColor.AMBER -> R.string.color_amber
    ScreenLightColor.RED -> R.string.color_red
    ScreenLightColor.GREEN -> R.string.color_green
    ScreenLightColor.VIOLET -> R.string.color_violet
    ScreenLightColor.CYAN -> R.string.color_cyan
    ScreenLightColor.PINK -> R.string.color_pink
    ScreenLightColor.CUSTOM -> R.string.color_custom
}

@StringRes
fun screenEffectName(e: ScreenEffect): Int = when (e) {
    ScreenEffect.NONE -> R.string.screen_effect_none
    ScreenEffect.RAINBOW -> R.string.screen_effect_rainbow
    ScreenEffect.CANDLE -> R.string.screen_effect_candle
    ScreenEffect.PARTY -> R.string.screen_effect_party
    ScreenEffect.WARNING -> R.string.screen_effect_warning
}
