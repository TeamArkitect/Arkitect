package com.github.devlaq.arkitect.i18n

import com.github.devlaq.arkitect.util.console.Logger
import java.io.Reader
import java.text.MessageFormat
import java.util.*

class BundleManager {

    private val bundles = mutableMapOf<Locale, I18NBundle>()

    var localeProvider = {
        Locale.getDefault()
    }

    fun availableLocales(): List<Locale> {
        return bundles.keys.toList()
    }

    fun addBundle(bundle: I18NBundle) {
        bundles[bundle.locale] = bundle
    }

    fun getBundle(locale: Locale): I18NBundle? {
        return bundles[locale]
    }

    fun getDefaultBundle(): I18NBundle {
        return bundles[Locale.ENGLISH]!!
    }

    fun loadBundle(locale: Locale, path: String) {
        addBundle(I18NBundleLoader.loadClasspath(path, locale))
    }

    fun loadBundles(locales: List<Locale>) {
        locales.forEach {
            loadBundle(it, "/translations/bundle_${it.toLanguageTag()}.properties")
        }
    }

    fun loadBundles(vararg locales: Locale) = loadBundles(locales.toList())

    fun format(string: String, vararg args: Any): String {
        return try {
            MessageFormat.format(string, *args)
        } catch (e: Exception) {
            "${string}\n(Failed to format translation)"
        }
    }

    fun translate(locale: Locale, key: String, vararg args: Any?): String {
        val bundle = bundles[locale] ?: getDefaultBundle()
        return bundle.translate(key, *args)
    }

    fun translate(key: String, vararg args: Any?): String {
        return translate(localeProvider(), key, *args)
    }
}

class I18NBundle(
    val parent: I18NBundle?,
    val locale: Locale,
    private val properties: Map<String, String>
) {

    companion object {
        fun createEmptyBundle(): I18NBundle {
            return I18NBundle(null, Locale.getDefault(), emptyMap())
        }
    }

    fun has(key: String): Boolean {
        return properties.containsKey(key) || (parent?.has(key) ?: false)
    }

    fun get(key: String): String? {
        return properties[key] ?: parent?.get(key)
    }

    fun translate(key: String, vararg args: Any?): String {
        return MessageFormat.format(get(key) ?: "???${key}???", *args.map { it.toString() }.toTypedArray())
    }

}

object I18NBundleLoader {

    private val logger = Logger("Arkitect/I18NBundleLoader")

    fun loadProperties(reader: Reader): Map<String, String> {
        val properties = Properties()
        properties.load(reader)
        return mapOf(*properties.stringPropertyNames().map { it to properties.getProperty(it) }.toTypedArray())
    }

    fun load(reader: Reader, locale: Locale, parent: I18NBundle? = null): I18NBundle {
        val properties = loadProperties(reader)
        return I18NBundle(
            parent = parent,
            locale = locale,
            properties = properties
        )
    }

    fun loadClasspath(path: String, locale: Locale, parent: I18NBundle? = null): I18NBundle {
        val reader = javaClass.getResourceAsStream(path)?.reader()
        if(reader == null) {
            logger.errorln("Bundle file $path not found in classpath!")
            return I18NBundle.createEmptyBundle()
        }
        return load(reader, locale, parent)
    }

}
