package com.supermenajer.game.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Oyun dili: Türkçe ya da İngilizce. Metinler kaynakta L("türkçe", "english") olarak durur;
 * seçim değişince arayüz yeniden çizilir. Kayda yazılmış eski haberler yazıldıkları dilde kalır.
 */
object Lang {
    var en by mutableStateOf(false)
        private set
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        prefs = context.getSharedPreferences("fx", android.content.Context.MODE_PRIVATE)
        val p = prefs!!
        en = if (p.contains("lang")) p.getString("lang", "tr") == "en"
        else java.util.Locale.getDefault().language != "tr"
    }

    fun set(english: Boolean) {
        en = english
        prefs?.edit()?.putString("lang", if (english) "en" else "tr")?.apply()
    }
}

/** Dile göre metin. */
fun L(tr: String, en: String): String = if (Lang.en) en else tr

/**
 * Dile göre bir kez kurulan değer: her dil için ayrı önbellek. Nesne kimliği sabit kalır
 * (harita anahtarı olarak kullanılabilir), dil değişince diğer dilin kopyası döner.
 */
class LangCached<T>(private val build: () -> T) {
    private var tr: T? = null
    private var en: T? = null
    fun get(): T = if (Lang.en) en ?: build().also { en = it } else tr ?: build().also { tr = it }
}
