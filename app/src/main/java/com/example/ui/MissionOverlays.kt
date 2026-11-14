package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.GameHudUiState
import com.example.game.HullAlertType
import com.example.game.IntroStep
import com.example.game.TailGunnerEngine
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CockpitBg
import com.example.ui.theme.DimSilver
import com.example.ui.theme.MutedOlive
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PanelDark
import com.example.ui.theme.PanelPlate
import com.example.ui.theme.ParchmentInk
import com.example.ui.theme.SpecialEliteFamily
import com.example.ui.theme.StardosStencilFamily
import kotlin.math.sin

@Composable
fun TitleOverlay(
    highScore: Int,
    muted: Boolean,
    onStartMission: () -> Unit,
    onToggleMute: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBg)
    ) {
        // Cover image (user-provided coastal dogfight tail-gunner scene, NO demo fighters)
        Image(
            painter = painterResource(id = R.drawable.img_title_cover),
            contentDescription = "Torreta de cola B-17 sobre la costa europea",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Cinematic dark gradient scrim for crisp typography contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xCC0A0D0A),
                        0.45f to Color(0x880D100E),
                        1f to Color(0xEE080A08)
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp)
        ) {
            val isWide = maxWidth > 620.dp

            // Sound toggle top-right
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .minimumInteractiveComponentSize()
                    .background(PanelPlate, RoundedCornerShape(8.dp))
                    .border(1.dp, PanelBorder, RoundedCornerShape(8.dp))
                    .testTag("title_mute_button")
            ) {
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (muted) "Activar sonido" else "Silenciar sonido",
                    tint = AmberGold
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "B-17G · POSICIÓN DE COLA",
                    fontFamily = SpecialEliteFamily,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                    color = AmberGold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "TAIL GUNNER",
                    fontFamily = StardosStencilFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isWide) 56.sp else 42.sp,
                    letterSpacing = 5.sp,
                    color = ParchmentInk,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(3.dp)
                        .background(AmberGold)
                )

                Surface(
                    color = Color(0xD60D110E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PanelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_formation_attack),
                                contentDescription = "Formación de bombarderos B-17",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, PanelBorder, RoundedCornerShape(6.dp))
                            )
                            Image(
                                painter = painterResource(id = R.drawable.img_turret_cockpit),
                                contentDescription = "Artillero en los mandos",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, PanelBorder, RoundedCornerShape(6.dp))
                            )
                        }

                        Text(
                            text = "Eres el artillero de cola de una Fortaleza Volante.\nDesliza el dedo para apuntar y mantener fuego con las dos ametralladoras de 12,7 mm.\nDispara en ráfagas para evitar que se atasquen.",
                            fontFamily = SpecialEliteFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = DimSilver,
                            textAlign = TextAlign.Center
                        )

                        if (highScore > 0) {
                            Surface(
                                color = Color(0xFF1A201B),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "RÉCORD ACTUAL: ${highScore.toString().padStart(6, '0')}",
                                    fontFamily = StardosStencilFamily,
                                    fontSize = 16.sp,
                                    letterSpacing = 2.sp,
                                    color = AmberGold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onStartMission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = CockpitBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_mission_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlightTakeoff,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SUBIR A BORDO",
                        fontFamily = StardosStencilFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 3.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IntroCinematicOverlay(
    state: GameHudUiState,
    onSkipIntro: () -> Unit
) {
    val step = state.introStep
    val elapsed = state.introStepElapsed

    val bgRes = when (step) {
        IntroStep.FORMATION_ATTACK -> R.drawable.img_formation_attack
        IntroStep.BOMBER_BURNING -> R.drawable.img_bomber_burning
        IntroStep.RADIO_ALERT -> R.drawable.img_bomber_smoking
        IntroStep.HANDS_ON_CONTROLS -> R.drawable.img_hands_controls
        IntroStep.CROSSHAIR_LOCK -> R.drawable.img_crosshair_bandit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBg)
    ) {
        Image(
            painter = painterResource(id = bgRes),
            contentDescription = "Escena de introducción",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle dark film vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x88000000),
                        0.5f to Color(0x33000000),
                        1f to Color(0xE0080A08)
                    )
                )
        )

        // Red flicker ("con parpadeo rojo") during burning bomber step
        if (step == IntroStep.BOMBER_BURNING) {
            val flicker = (0.32f + 0.36f * sin(elapsed * 26f)).coerceIn(0.08f, 0.70f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD82018).copy(alpha = flicker))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp)
        ) {
            // Skip button top-right
            OutlinedButton(
                onClick = onSkipIntro,
                border = BorderStroke(1.dp, PanelBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PanelPlate,
                    contentColor = ParchmentInk
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .minimumInteractiveComponentSize()
                    .testTag("skip_intro_button")
            ) {
                Text(
                    text = "SALTAR",
                    fontFamily = StardosStencilFamily,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Saltar introducción",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bottom dramatic subtitle / radio panel
            Surface(
                color = Color(0xDF0B0E0B),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (step == IntroStep.BOMBER_BURNING || step == IntroStep.RADIO_ALERT) {
                        AlertRed.copy(alpha = 0.85f)
                    } else {
                        AmberGold.copy(alpha = 0.8f)
                    }
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (step) {
                        IntroStep.FORMATION_ATTACK -> {
                            Text(
                                text = "ATAQUE A LA FORMACIÓN DE BOMBARDEROS",
                                fontFamily = StardosStencilFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 2.sp,
                                color = AmberGold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Flak antiaéreo y cazas interceptores sobre la costa europea.",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 15.sp,
                                color = ParchmentInk,
                                textAlign = TextAlign.Center
                            )
                        }

                        IntroStep.BOMBER_BURNING -> {
                            Text(
                                text = "¡UN BOMBARDERO CAE EN LLAMAS!",
                                fontFamily = StardosStencilFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = 2.sp,
                                color = AlertCrimson,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Impacto directo en la formación · Mantengan sus puestos.",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 15.sp,
                                color = ParchmentInk,
                                textAlign = TextAlign.Center
                            )
                        }

                        IntroStep.RADIO_ALERT -> {
                            val fullEs = TailGunnerEngine.RADIO_MESSAGE_ES
                            val progress = (elapsed / 2.2f).coerceIn(0f, 1f)
                            val visibleChars = (fullEs.length * progress).toInt().coerceIn(0, fullEs.length)
                            val typedText = fullEs.substring(0, visibleChars)
                            val cursor = if (visibleChars < fullEs.length && ((elapsed * 8f).toInt() % 2 == 0)) "_" else ""

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Radio,
                                    contentDescription = "Mensaje de radio",
                                    tint = AlertCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "RADIO INTERCOM · ALERTA",
                                    fontFamily = SpecialEliteFamily,
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    color = AmberGold
                                )
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "“$typedText$cursor”",
                                fontFamily = StardosStencilFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 1.5.sp,
                                color = ParchmentInk,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "“${TailGunnerEngine.RADIO_MESSAGE_EN}”",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                color = DimSilver,
                                textAlign = TextAlign.Center
                            )
                        }

                        IntroStep.HANDS_ON_CONTROLS -> {
                            Text(
                                text = "¡ESTÉ LISTO!",
                                fontFamily = StardosStencilFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                letterSpacing = 3.sp,
                                color = AmberGold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "GET READY",
                                fontFamily = StardosStencilFamily,
                                fontSize = 18.sp,
                                letterSpacing = 3.sp,
                                color = ParchmentInk,
                                textAlign = TextAlign.Center
                            )
                        }

                        IntroStep.CROSSHAIR_LOCK -> {
                            Text(
                                text = "¡FUEGO A DISCRECIÓN!",
                                fontFamily = StardosStencilFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = 3.sp,
                                color = AlertCrimson,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "FIRE AT WILL · BLANCO EN LA MIRA",
                                fontFamily = SpecialEliteFamily,
                                fontSize = 14.sp,
                                letterSpacing = 1.5.sp,
                                color = ParchmentInk,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HullAlertCardOverlay(
    alertType: HullAlertType,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hull_alert_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (alertType == HullAlertType.WOUNDED_12) 420 else 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    AnimatedVisibility(
        visible = alertType != HullAlertType.NONE,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.94f),
        modifier = modifier
    ) {
        val isWounded = alertType == HullAlertType.WOUNDED_12
        val imageRes = if (isWounded) R.drawable.img_wounded_gunner else R.drawable.img_bomber_burning
        val title = if (isWounded) "¡ARTILLERO HERIDO!" else "¡CASCO EN ESTADO CRÍTICO!"
        val subtitle = if (isWounded) {
            "Casco < 12% · Mayor dispersión de disparos"
        } else {
            "Casco < 30% · Daños graves en el bombardero"
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xEB150B0A)),
            border = BorderStroke(2.dp, AlertRed.copy(alpha = borderAlpha)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .scale(if (isWounded) pulseScale else 1f)
                .testTag(if (isWounded) "wounded_gunner_card" else "critical_hull_card")
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 104.dp, height = 68.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, AlertRed.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = AlertCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = title,
                            fontFamily = StardosStencilFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            color = AlertCrimson
                        )
                    }
                    Text(
                        text = subtitle,
                        fontFamily = SpecialEliteFamily,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = ParchmentInk
                    )
                }
            }
        }
    }
}

@Composable
fun PauseOverlay(
    muted: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleMute: () -> Unit,
    onReturnTitle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3080A08))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PanelDark),
            border = BorderStroke(1.5.dp, AmberGold.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PAUSA",
                    fontFamily = StardosStencilFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = 6.sp,
                    color = ParchmentInk
                )
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = CockpitBg),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("resume_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("CONTINUAR", fontFamily = StardosStencilFamily, fontSize = 18.sp, letterSpacing = 2.sp)
                }
                OutlinedButton(
                    onClick = onRestart,
                    border = BorderStroke(1.dp, PanelBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = ParchmentInk)
                    Spacer(Modifier.width(8.dp))
                    Text("REINICIAR MISIÓN", fontFamily = StardosStencilFamily, fontSize = 15.sp, color = ParchmentInk)
                }
                OutlinedButton(
                    onClick = onToggleMute,
                    border = BorderStroke(1.dp, PanelBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = AmberGold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (muted) "SONIDO: APAGADO" else "SONIDO: ENCENDIDO",
                        fontFamily = StardosStencilFamily,
                        fontSize = 15.sp,
                        color = AmberGold
                    )
                }
                OutlinedButton(
                    onClick = onReturnTitle,
                    border = BorderStroke(1.dp, PanelBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("VOLVER A PORTADA", fontFamily = StardosStencilFamily, fontSize = 15.sp, color = MutedOlive)
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    state: GameHudUiState,
    onRetry: () -> Unit,
    onReturnTitle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8080A08))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "ABATIDO",
                fontFamily = StardosStencilFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                letterSpacing = 6.sp,
                color = AlertCrimson
            )
            Text(
                text = "El B-17 se pierde en las nubes.",
                fontFamily = SpecialEliteFamily,
                fontSize = 16.sp,
                color = DimSilver,
                textAlign = TextAlign.Center
            )

            // Dramatic dual image strip from the user's artwork
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_wounded_gunner),
                    contentDescription = "Artillero herido",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.65f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, AlertRed.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                )
                Image(
                    painter = painterResource(id = R.drawable.img_bomber_burning),
                    contentDescription = "Bombardero B-17 en llamas",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.65f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, AlertRed.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                )
            }

            Surface(
                color = PanelDark.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, PanelBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatRow("PUNTOS", state.score.toString())
                    StatRow("OLEADA ALCANZADA", state.wave.toString())
                    StatRow("CAZAS DERRIBADOS", state.kills.toString())
                    StatRow("RÉCORD", state.highScore.toString(), highlight = true)
                }
            }

            if (state.canRestartFromOver) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onReturnTitle,
                        border = BorderStroke(1.dp, PanelBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("title_return_button")
                    ) {
                        Text(
                            text = "PORTADA",
                            fontFamily = StardosStencilFamily,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp,
                            color = DimSilver
                        )
                    }
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = CockpitBg
                        ),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(52.dp)
                            .testTag("retry_mission_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "OTRA MISIÓN",
                            fontFamily = StardosStencilFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = SpecialEliteFamily,
            fontSize = 14.sp,
            letterSpacing = 1.5.sp,
            color = MutedOlive
        )
        Text(
            text = value,
            fontFamily = StardosStencilFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = 2.sp,
            color = if (highlight) AmberGold else ParchmentInk
        )
    }
}
