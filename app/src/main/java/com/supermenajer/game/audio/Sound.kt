package com.supermenajer.game.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.supermenajer.game.R

/**
 * Oyunun sesleri: kısa efektler SoundPool'dan, tribün uğultusu ve menü müziği döngüsel MediaPlayer ile
 * (uzun kayıtlar SoundPool'un ~1 MB sınırına takılmasın). Kaynaklar: docs/SES_KAYNAKLARI.md. [init] çağrılmadan (testler) her şey sessizce yok sayılır.
 * Ayarlar (efekt, müzik, ses düzeyi) cihazda saklanır.
 */
object Sound {
    enum class Fx(val res: Int) {
        CLICK(R.raw.sfx_click), NOTIFY(R.raw.sfx_notify), KICKOFF(R.raw.sfx_whistle_kickoff),
        HALF(R.raw.sfx_whistle_half), FULL(R.raw.sfx_whistle_full), GOAL(R.raw.sfx_goal),
        OOH(R.raw.sfx_ooh),
    }

    var sfxOn by mutableStateOf(true)
        private set
    var musicOn by mutableStateOf(true)
        private set
    var volume by mutableFloatStateOf(0.8f)
        private set

    private var ctx: Context? = null
    private var prefs: SharedPreferences? = null
    private var pool: SoundPool? = null
    private val ids = HashMap<Fx, Int>()
    private val loaded = HashSet<Int>()
    private var crowdPlayer: MediaPlayer? = null
    private var crowdLevel = 1f
    private var music: MediaPlayer? = null
    private var wantMusic = false
    private var wantCrowd = false
    private var paused = false

    fun init(context: Context) {
        if (pool != null) return
        ctx = context.applicationContext
        prefs = context.getSharedPreferences("sound", Context.MODE_PRIVATE).also {
            sfxOn = it.getBoolean("sfx", true)
            musicOn = it.getBoolean("music", true)
            volume = it.getFloat("volume", 0.8f)
        }
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build().also { p ->
            p.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
            Fx.entries.forEach { ids[it] = p.load(context, it.res, 1) }
        }
    }

    /** Kısa efekt çalar; [vol] 0..1, [rate] 0.5..2 (perde). */
    fun play(fx: Fx, vol: Float = 1f, rate: Float = 1f) {
        val p = pool ?: return
        if (!sfxOn || paused) return
        val id = ids[fx] ?: return
        if (id !in loaded) return
        val v = (vol * volume).coerceIn(0f, 1f)
        p.play(id, v, v, 1, 0, rate)
    }

    fun click() = play(Fx.CLICK, 0.45f)

    /** Menü müziği istenir/bırakılır (ayar kapalıysa çalmaz). */
    fun music(on: Boolean) { wantMusic = on; refresh() }

    /** Maç boyunca tribün uğultusu. */
    fun crowd(on: Boolean) { wantCrowd = on; refresh() }

    /** Uğultu şiddeti (gerilimli anlarda yükselir), 0..1.5. */
    fun crowdLevel(level: Float) {
        crowdLevel = level
        crowdPlayer?.setVolume(crowdVol(), crowdVol())
    }

    fun pause() { paused = true; refresh() }
    fun resume() { paused = false; refresh() }

    fun changeSfx(on: Boolean) { sfxOn = on; prefs?.edit()?.putBoolean("sfx", on)?.apply(); refresh() }
    fun changeMusic(on: Boolean) { musicOn = on; prefs?.edit()?.putBoolean("music", on)?.apply(); refresh() }
    fun changeVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        prefs?.edit()?.putFloat("volume", volume)?.apply()
        music?.setVolume(musicVol(), musicVol())
        crowdLevel(crowdLevel)
    }

    private fun musicVol() = 0.45f * volume
    private fun crowdVol() = (0.55f * crowdLevel * volume).coerceIn(0f, 1f)

    private fun refresh() {
        val c = ctx ?: return
        // Müzik
        if (wantMusic && musicOn && !paused) {
            val m = music ?: runCatching { MediaPlayer.create(c, R.raw.music_menu) }.getOrNull()?.also {
                it.isLooping = true
                music = it
            }
            m?.setVolume(musicVol(), musicVol())
            if (m != null && !m.isPlaying) m.start()
        } else {
            music?.let { if (it.isPlaying) it.pause() }
        }
        // Tribün
        val crowdOk = wantCrowd && sfxOn && !paused
        if (crowdOk) {
            val cp = crowdPlayer ?: runCatching { MediaPlayer.create(c, R.raw.sfx_crowd_loop) }.getOrNull()?.also {
                it.isLooping = true
                crowdPlayer = it
            }
            cp?.setVolume(crowdVol(), crowdVol())
            if (cp != null && !cp.isPlaying) cp.start()
        } else {
            crowdPlayer?.let { if (it.isPlaying) it.pause() }
        }
    }
}
