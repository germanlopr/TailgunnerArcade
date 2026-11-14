package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.SoundEngine
import com.example.data.HighScoreStore
import com.example.game.GameHudUiState
import com.example.game.GameMode
import com.example.game.TailGunnerEngine
import com.example.game.TurretSpriteCache
import com.example.game.renderTurretScene
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DimSilver
import com.example.ui.theme.GunmetalSteel
import com.example.ui.theme.HullGreen
import com.example.ui.theme.MutedOlive
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PanelPlate
import com.example.ui.theme.ParchmentInk
import com.example.ui.theme.SpecialEliteFamily
import com.example.ui.theme.StardosStencilFamily
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun TailGunnerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val highScoreStore = remember { HighScoreStore(context) }
    val storedHighScore by highScoreStore.highScoreFlow.collectAsState(initial = 0)

    val soundEngine = remember { SoundEngine(context).also { it.start() } }
    val spriteCache = remember { TurretSpriteCache() }
    val engine = remember {
        TailGunnerEngine(
            soundEngine = soundEngine,
            onNewHighScore = { best ->
                scope.launch { highScoreStore.saveHighScoreIfBetter(best) }
            }
        )
    }

    LaunchedEffect(storedHighScore) {
        engine.setStoredHighScore(storedHighScore)
    }

    var hudState by remember { mutableStateOf(engine.toHudUiState()) }
    var renderTick by remember { mutableIntStateOf(0) }

    val keysDown = remember { mutableSetOf<Key>() }

    BackHandler(enabled = hudState.mode != GameMode.TITLE) {
        when (engine.mode) {
            GameMode.PLAY, GameMode.PAUSED -> engine.togglePause()
            GameMode.INTRO -> engine.skipIntroToPlay()
            GameMode.OVER -> engine.returnToTitle()
            GameMode.TITLE -> Unit
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> engine.pauseIfPlaying()
                Lifecycle.Event.ON_RESUME -> engine.resumeFromLifecycle()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            soundEngine.release()
        }
    }

    // 60fps simulation loop
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                val dt = if (lastNanos == 0L) 0f else (nanos - lastNanos) / 1_000_000_000f
                lastNanos = nanos

                // Keyboard aiming support
                if (engine.mode == GameMode.PLAY) {
                    val kx = (if (Key.DirectionRight in keysDown || Key.D in keysDown) 1f else 0f) -
                        (if (Key.DirectionLeft in keysDown || Key.A in keysDown) 1f else 0f)
                    val ky = (if (Key.DirectionDown in keysDown || Key.S in keysDown) 1f else 0f) -
                        (if (Key.DirectionUp in keysDown || Key.W in keysDown) 1f else 0f)
                    if (kx != 0f || ky != 0f) {
                        engine.aimX = (engine.aimX + kx * 720f * engine.unit * dt).coerceIn(0f, engine.width)
                        engine.aimY = (engine.aimY + ky * 720f * engine.unit * dt).coerceIn(0f, engine.height)
                    }
                }

                engine.update(dt)
                renderTick++
                hudState = engine.toHudUiState()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .onKeyEvent { event ->
                val k = event.key
                if (event.type == KeyEventType.KeyDown) {
                    keysDown.add(k)
                    when (k) {
                        Key.Spacebar -> {
                            when (engine.mode) {
                                GameMode.TITLE -> engine.startMissionWithIntro()
                                GameMode.INTRO -> engine.skipIntroToPlay()
                                GameMode.PAUSED -> engine.togglePause()
                                GameMode.OVER -> if (hudState.canRestartFromOver) engine.restartImmediate()
                                GameMode.PLAY -> engine.isFiring = true
                            }
                            true
                        }
                        Key.P, Key.Escape -> {
                            if (engine.mode == GameMode.PLAY || engine.mode == GameMode.PAUSED) {
                                engine.togglePause()
                                true
                            } else false
                        }
                        Key.M -> {
                            soundEngine.toggleMuted()
                            true
                        }
                        else -> false
                    }
                } else if (event.type == KeyEventType.KeyUp) {
                    keysDown.remove(k)
                    if (k == Key.Spacebar && engine.mode == GameMode.PLAY) {
                        engine.isFiring = false
                    }
                    false
                } else {
                    false
                }
            }
    ) {
        // Real-time 60fps cockpit & sky canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    engine.resize(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (engine.mode == GameMode.PLAY) {
                                val pressed = event.changes.firstOrNull { it.pressed }
                                if (pressed != null) {
                                    val touchOffsetY = -56f * engine.unit
                                    engine.aimX = pressed.position.x.coerceIn(0f, engine.width)
                                    engine.aimY = (pressed.position.y + touchOffsetY).coerceIn(0f, engine.height)
                                    engine.isFiring = true
                                } else {
                                    engine.isFiring = false
                                }
                            } else {
                                engine.isFiring = false
                            }
                        }
                    }
                }
                .testTag("turret_canvas")
        ) {
            // Read renderTick to trigger redraw every frame
            @Suppress("UNUSED_EXPRESSION")
            renderTick
            renderTurretScene(engine = engine, sprites = spriteCache)
        }

        // Gameplay & GameOver HUD
        if (hudState.mode == GameMode.PLAY || hudState.mode == GameMode.PAUSED || hudState.mode == GameMode.OVER) {
            TurretHudOverlay(
                state = hudState,
                onPauseClick = { engine.togglePause() },
                onToggleMute = { soundEngine.toggleMuted() }
            )
        }

        // Non-blocking tactical Hull Alert Cards (<30% Critical Bomber & <12% Wounded Gunner)
        if (hudState.mode == GameMode.PLAY) {
            HullAlertCardOverlay(
                alertType = hudState.activeHullAlert,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }

        // Mode-specific screens
        when (hudState.mode) {
            GameMode.TITLE -> {
                TitleOverlay(
                    highScore = hudState.highScore,
                    muted = hudState.muted,
                    onStartMission = { engine.startMissionWithIntro() },
                    onToggleMute = { soundEngine.toggleMuted() }
                )
            }
            GameMode.INTRO -> {
                IntroCinematicOverlay(
                    state = hudState,
                    onSkipIntro = { engine.skipIntroToPlay() }
                )
            }
            GameMode.PAUSED -> {
                PauseOverlay(
                    muted = hudState.muted,
                    onResume = { engine.togglePause() },
                    onRestart = { engine.restartImmediate() },
                    onToggleMute = { soundEngine.toggleMuted() },
                    onReturnTitle = { engine.returnToTitle() }
                )
            }
            GameMode.OVER -> {
                GameOverOverlay(
                    state = hudState,
                    onRetry = { engine.restartImmediate() },
                    onReturnTitle = { engine.returnToTitle() }
                )
            }
            GameMode.PLAY -> Unit
        }
    }
}

@Composable
private fun TurretHudOverlay(
    state: GameHudUiState,
    onPauseClick: () -> Unit,
    onToggleMute: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val narrow = maxWidth < 600.dp

        // Top bar: Score plate + (Center wave on wide) + Hull/Heat plate + Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Score & Record/Wave plate
            Surface(
                color = PanelPlate,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, PanelBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        text = "PUNTOS",
                        fontFamily = SpecialEliteFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = MutedOlive
                    )
                    Text(
                        text = state.score.toString().padStart(6, '0'),
                        fontFamily = StardosStencilFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (narrow) 24.sp else 28.sp,
                        letterSpacing = 1.sp,
                        color = ParchmentInk
                    )
                    Text(
                        text = if (narrow && state.wave > 0) {
                            "OLEADA ${state.wave} · ${state.enemiesRemaining} CAZAS"
                        } else {
                            "RÉCORD ${state.highScore.toString().padStart(6, '0')}"
                        },
                        fontFamily = SpecialEliteFamily,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        color = MutedOlive
                    )
                }
            }

            // Center Wave indicator on wider screens
            if (!narrow && state.wave > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "OLEADA ${state.wave}",
                        fontFamily = StardosStencilFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 3.sp,
                        color = ParchmentInk
                    )
                    if (state.enemiesRemaining > 0) {
                        Text(
                            text = "CAZAS RESTANTES  ${state.enemiesRemaining}",
                            fontFamily = SpecialEliteFamily,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                            color = MutedOlive
                        )
                    }
                }
            }

            // Right: Hull & Gun Heat + quick Pause/Mute buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = PanelPlate,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (state.isCriticalHull) AlertRed.copy(alpha = 0.9f) else PanelBorder
                    ),
                    modifier = Modifier.width(if (narrow) 154.dp else 196.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CASCO",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                color = if (state.isCriticalHull) AlertCrimson else MutedOlive
                            )
                            Text(
                                text = "${ceil(state.hull).toInt()}%",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 11.sp,
                                color = if (state.isCriticalHull) AlertCrimson else DimSilver
                            )
                        }
                        SegmentedBar(
                            fraction = state.hull / 100f,
                            colorAt = {
                                when {
                                    state.hull > 55f -> HullGreen
                                    state.hull > 30f -> AmberGold
                                    else -> AlertRed
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CAÑONES",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                color = MutedOlive
                            )
                            Text(
                                text = if (state.jammed) "ATASCADOS" else "${state.heat.roundToInt()}%",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 10.sp,
                                color = if (state.jammed) AlertCrimson else DimSilver
                            )
                        }
                        SegmentedBar(
                            fraction = state.heat / 100f,
                            colorAt = { t ->
                                when {
                                    t < 0.5f -> GunmetalSteel
                                    t < 0.8f -> AmberGold
                                    else -> AlertRed
                                }
                            }
                        )
                    }
                }

                // Controls row (Mute & Pause)
                if (state.mode == GameMode.PLAY) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(48.dp)
                                .background(PanelPlate, RoundedCornerShape(6.dp))
                                .border(1.dp, PanelBorder, RoundedCornerShape(6.dp))
                                .testTag("hud_mute_button")
                        ) {
                            Icon(
                                imageVector = if (state.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (state.muted) "Activar sonido" else "Silenciar sonido",
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onPauseClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(48.dp)
                                .background(PanelPlate, RoundedCornerShape(6.dp))
                                .border(1.dp, PanelBorder, RoundedCornerShape(6.dp))
                                .testTag("hud_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Pausar partida",
                                tint = ParchmentInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Status chips (Streak / Wounded dispersion warning)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (narrow) 108.dp else 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state.multiplier > 1) {
                Text(
                    text = "RACHA ×${state.multiplier}",
                    fontFamily = StardosStencilFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 3.sp,
                    color = AmberGold
                )
            }
            if (state.isWounded) {
                Surface(
                    color = Color(0xCC220B09),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.85f))
                ) {
                    Text(
                        text = "ARTILLERO HERIDO · DISPERSIÓN ALTA",
                        fontFamily = SpecialEliteFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = AlertCrimson,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Wave / Overheat Banner
        state.banner?.let { b ->
            val fade = min(
                1f,
                min(b.remaining / 0.4f, (b.maxDuration - b.remaining) / 0.25f + 0.001f)
            ).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (narrow) 152.dp else 120.dp)
                    .widthIn(max = 520.dp)
            ) {
                Text(
                    text = b.title,
                    fontFamily = StardosStencilFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (narrow) 28.sp else 38.sp,
                    letterSpacing = 4.sp,
                    color = ParchmentInk.copy(alpha = fade),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = b.subtitle,
                    fontFamily = SpecialEliteFamily,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = AmberGold.copy(alpha = fade),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SegmentedBar(
    fraction: Float,
    colorAt: (Float) -> Color
) {
    val segments = 16
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until segments) {
            val pos = (i + 0.5f) / segments
            val active = pos <= fraction.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(
                        if (active) colorAt(i.toFloat() / segments) else Color(0x33788273),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
