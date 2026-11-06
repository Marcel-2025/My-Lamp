package dev.flondy.mylamp.core

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Sprachauswahl innerhalb der App.
 *
 * - Android 13+: offizielle App-Sprache des Systems (LocaleManager). Die Wahl erscheint auch unter
 *   Einstellungen → Apps → My Lamp → Sprache und überlebt Neuinstallationen per Backup.
 * - Android 7–12: Wahl wird lokal gespeichert und beim Start der Activity angewendet.
 *
 * Leerer Tag = Systemsprache.
 */
object AppLanguage {

    data class Option(val tag: String, val nativeName: String)

    /** Unterstützte Sprachen in ihrer eigenen Schreibweise (bewusst nicht übersetzt). */
    val options: List<Option> = listOf(
        Option("de", "Deutsch"),
        Option("en", "English"),
        Option("es", "Español"),
        Option("fr", "Français"),
        Option("it", "Italiano"),
        Option("nl", "Nederlands"),
        Option("pl", "Polski"),
        Option("pt", "Português"),
        Option("tr", "Türkçe"),
        Option("ru", "Русский"),
        Option("ja", "日本語"),
        Option("zh-CN", "简体中文"),
    )

    private const val PREFS = "app_language"
    private const val KEY = "tag"

    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (locales == null || locales.isEmpty) "" else matchOption(locales[0])
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        }

    /** Setzt die App-Sprache. Die Activity wird vom System bzw. hier neu aufgebaut. */
    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
            activity.recreate()
        }
    }

    /** Für Android 7–12 in Activity.attachBaseContext aufrufen. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    fun displayName(tag: String): String? = options.firstOrNull { it.tag == tag }?.nativeName

    private fun matchOption(locale: Locale): String {
        val full = locale.toLanguageTag()
        return options.firstOrNull { it.tag.equals(full, ignoreCase = true) }?.tag
            ?: options.firstOrNull { it.tag.substringBefore('-') == locale.language }?.tag
            ?: full
    }
}
