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
