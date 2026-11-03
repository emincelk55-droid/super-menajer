package com.supermenajer.game

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.supermenajer.game.game.GameRepo
import java.io.File

/**
 * Çökme raporu: beklenmedik bir hata uygulamayı kapatırsa ayrıntısı cihaza yazılır; bir sonraki
 * açılışta kullanıcıya gösterilir ve isterse paylaşabilir (WhatsApp, Telegram, e-posta). İnternete
 * kendiliğinden hiçbir şey gönderilmez.
 */
object CrashGuard {
    private var file: File? = null

    /** Önceki oturumdan kalan rapor (gösterildikten sonra silinir). */
    var pending by mutableStateOf<String?>(null)

    fun install(context: Context) {
        val f = File(context.filesDir, "crash.txt")
        file = f
        if (f.exists()) pending = runCatching { f.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { f.writeText(report(context, thread, e)) }
            previous?.uncaughtException(thread, e)
        }
    }

    fun report(context: Context, thread: Thread, e: Throwable): String {
        val version = runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pi)})"
        }.getOrDefault("?")
        val g = GameRepo.game
        val state = if (g != null) "sezon ${g.season}, gün ${g.date}, takım ${g.userTeam.short}, ekran ${GameRepo.screen}, sekme ${GameRepo.tab}" else "ekran ${GameRepo.screen}"
        return buildString {
            appendLine("Süper Menajer çökme raporu")
            appendLine("Zaman: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine("Sürüm: $version · Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT}) · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Oyun: $state")
            appendLine("İş parçacığı: ${thread.name}")
            appendLine()
            append(e.stackTraceToString().take(12_000))
        }
    }

    fun dismiss() {
        pending = null
        file?.delete()
    }

    fun share(context: Context) {
        val text = pending ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Süper Menajer çökme raporu")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Raporu paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        dismiss()
    }
}
