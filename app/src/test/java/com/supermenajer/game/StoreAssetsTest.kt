package com.supermenajer.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.supermenajer.game.game.GameRepo
import com.supermenajer.game.game.Generator
import com.supermenajer.game.game.Screen
import com.supermenajer.game.game.Season
import com.supermenajer.game.ui.AppTheme
import com.supermenajer.game.ui.Bg
import com.supermenajer.game.ui.GameCrest
import com.supermenajer.game.ui.Green
import com.supermenajer.game.ui.StadiumBackdrop
import com.supermenajer.game.ui.TextMain
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import kotlin.random.Random

/**
 * Play Store görselleri: 512 px ikon, 1024x500 tanıtım görseli ve 1920x1080 (16:9) ekran görüntüleri.
 * Çıktılar app/src/test/snapshots/images altında; tools/store_assets.sh ile docs/store'a kopyalanır.
 */
class StoreAssetsTest {
    init { com.supermenajer.game.ui.Fx.enabled = false }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 1920, screenHeight = 1080, density = Density.XHIGH, orientation = ScreenOrientation.LANDSCAPE),
        // Mağaza tam çözünürlük ister: varsayılan kayıt 1000 px'e küçültür, bu yüzden kareler doğrudan yazılır
        snapshotHandler = FullResHandler(java.io.File("build/store")),
        useDeviceResolution = true,
    )

    /**
     * Kareyi küçültmeden app/build/store/<ad>.png olarak yazar. Birim testleri android.jar ile derlendiği için
     * java.awt görünmez; kare işleyici yansıma (Proxy) ile kurulur.
     */
    private class FullResHandler(private val dir: java.io.File) : app.cash.paparazzi.SnapshotHandler {
        override fun newFrameHandler(snapshot: app.cash.paparazzi.Snapshot, frameCount: Int, fps: Int): app.cash.paparazzi.SnapshotHandler.FrameHandler {
            val type = app.cash.paparazzi.SnapshotHandler.FrameHandler::class.java
            return java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, args ->
                if (m.name == "handle") {
                    dir.mkdirs()
                    val rendered = Class.forName("java.awt.image.RenderedImage")
                    Class.forName("javax.imageio.ImageIO").getMethod("write", rendered, String::class.java, java.io.File::class.java)
                        .invoke(null, args[0], "PNG", java.io.File(dir, "${snapshot.name}.png"))
                }
                null
            } as app.cash.paparazzi.SnapshotHandler.FrameHandler
        }
        override fun close() {}
    }

    private fun shot(name: String) {
        paparazzi.snapshot(name) { AppTheme { Surface(Modifier.fillMaxSize(), color = Bg) { AppRoot() } } }
    }

    @Test
    fun icon512() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenWidth = 512, screenHeight = 512, density = Density.XHIGH, orientation = ScreenOrientation.PORTRAIT))
        paparazzi.snapshot("store_icon_512") {
            Box(Modifier.fillMaxSize().background(Color(0xFF101538)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_launcher_fg), null, Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun featureGraphic() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(screenWidth = 1024, screenHeight = 500, density = Density.XHIGH, orientation = ScreenOrientation.LANDSCAPE))
        paparazzi.snapshot("store_feature_1024x500") {
            AppTheme {
                Box(Modifier.fillMaxSize()) {
                    StadiumBackdrop(Color(0xFF1DF0B0), Color(0xFF8B6CFF), Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE6070A1C), Color(0x99070A1C), Color.Transparent))))
                    Row(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                        GameCrest(110.dp)
                        Spacer(Modifier.width(18.dp))
                        Column {
                            Text("SÜPER", fontSize = 36.sp, fontWeight = FontWeight.Black, color = TextMain, letterSpacing = 6.sp, lineHeight = 36.sp)
                            Text("MENAJER", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Green, letterSpacing = 6.sp, lineHeight = 38.sp)
                            Spacer(Modifier.size(6.dp))
                            Text("Kulübünü kur, taktiğini çiz, efsaneni yaz.", fontSize = 13.sp, color = TextMain.copy(alpha = 0.85f))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun screenshots() {
        GameRepo.init(Files.createTempDirectory("store").toFile())
        GameRepo.preparedWorld = Generator.newWorld(Random(4)).also { Season.computeExpectations(it) }
        GameRepo.screen = Screen.TITLE
        shot("store_01_menu")
        val world = Generator.newWorld(Random(4)).also { Season.computeExpectations(it) }
        val team = world.teams.first { it.division == 10 && it.reputation >= 85 }
        GameRepo.newCareer(world, "Emin Hoca", team.id)
        GameRepo.game!!.introDone = true
        GameRepo.screen = Screen.MAIN
        GameRepo.tab = GameRepo.TAB_HOME
        shot("store_02_home")
        GameRepo.tab = GameRepo.TAB_TACTICS
        shot("store_03_tactics")
        GameRepo.tab = GameRepo.TAB_LEAGUE
        shot("store_04_league")
        GameRepo.tab = GameRepo.TAB_TRANSFER
        shot("store_05_transfer")
        GameRepo.tab = GameRepo.TAB_HOME
        GameRepo.game!!.introDone = true
        if (GameRepo.game!!.todayUserFixture() == null) GameRepo.skipToNextMatch()
        GameRepo.playMatch(live = true)
        val sim = GameRepo.match!!
        val an = GameRepo.pitchFor(sim)
        repeat(30) {
            val b = sim.events.size
            sim.step()
            for (e in sim.events.subList(b, sim.events.size).toList()) {
                val o = an.outcomes
                an.play(e)
                var w = 0
                while (e.buildUp.isNotEmpty() && an.outcomes == o && w++ < 300) an.update(1f / 30f)
            }
            repeat(20) { an.update(1f / 30f) }
        }
        shot("store_06_match")
    }
}
