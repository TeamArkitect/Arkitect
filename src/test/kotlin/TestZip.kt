import com.github.devlaq.arkitect.i18n.I18N
import com.github.devlaq.arkitect.util.console.Logging
import java.io.File
import java.util.*
import java.util.zip.ZipFile
import kotlin.test.Test

class TestZip {
    @Test
    fun testZip() {

    }
}

fun main() {
    I18N.loadBundles(Locale.KOREAN, Locale.ENGLISH)
    Logging.init()

    fun useTranslation(text: String, vararg args: Any) {
        val (prefix, suffix) = "<%" to "%>"

        val translated = text.split(prefix).drop(1).map {
            val key = it.substringBefore(suffix)
            if(key == it) return@map it
            I18N.translate(key) + it.substringAfter(suffix)
        }.joinToString("")

        val result = I18N.format(translated, *args)

        println(result)
    }
    useTranslation("<%module.load%>")
}
