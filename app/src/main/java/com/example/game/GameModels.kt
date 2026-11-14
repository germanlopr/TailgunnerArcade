package com.example.game

import androidx.compose.ui.graphics.Color

enum class GameMode {
    TITLE,
    INTRO,
    PLAY,
    PAUSED,
    OVER
}

enum class IntroStep(val durationSec: Float) {
    FORMATION_ATTACK(2.5f),
    BOMBER_BURNING(2.4f),
    RADIO_ALERT(4.3f),
    HANDS_ON_CONTROLS(2.7f),
    CROSSHAIR_LOCK(2.1f)
}

enum class HullAlertType {
    NONE,
    CRITICAL_30,
    WOUNDED_12
}

enum class EnemyType(
    val displayName: String,
    val maxHp: Float,
    val points: Int,
    val wingSpan: Float,
    val speed: Float,
    val damage: Float,
    val hitChance: Float,
    val bodyColor: Color,
    val darkColor: Color,
    val noseColor: Color,
    val cowlColor: Color?,
    val isTwin: Boolean
) {
    BF_109(
        displayName = "Bf 109",
        maxHp = 14f,
        points = 100,
        wingSpan = 120f,
        speed = 560f,
        damage = 1.1f,
        hitChance = 0.50f,
        bodyColor = Color(0xFF4B5547),
        darkColor = Color(0xFF2F372D),
        noseColor = Color(0xFFD6BF3E),
        cowlColor = null,
        isTwin = false
    ),
    FW_190(
        displayName = "Fw 190",
        maxHp = 24f,
        points = 200,
        wingSpan = 128f,
        speed = 620f,
        damage = 1.6f,
        hitChance = 0.55f,
        bodyColor = Color(0xFF3F4852),
        darkColor = Color(0xFF262D35),
        noseColor = Color(0xFF1B1B1B),
        cowlColor = Color(0xFFDCDCDC),
        isTwin = false
    ),
    ME_410(
        displayName = "Me 410",
        maxHp = 46f,
        points = 400,
        wingSpan = 170f,
        speed = 480f,
        damage = 2.2f,
        hitChance = 0.60f,
        bodyColor = Color(0xFF4C5442),
        darkColor = Color(0xFF2D3328),
        noseColor = Color(0xFF9AA28F),
        cowlColor = null,
        isTwin = true
    )
}

enum class EnemyState {
    APPROACH,
    ATTACK,
    BREAK,
    GONE
}

class Enemy(
    val type: EnemyType,
    var hp: Float,
    val hpMax: Float,
    var z: Float,
    var state: EnemyState,
    var t: Float,
    var phase: Float,
    var xc: Float,
    var yc: Float,
    var ax: Float,
    var ay: Float,
    var x: Float,
    var y: Float,
    var bank: Float = 0f,
    var stateTimer: Float = 0f,
    var zAtk: Float,
    var burstTimer: Float,
    var burstCount: Int = 0,
    var shotTimer: Float = 0f,
    var flashTimer: Float = 0f,
    var smokeTimer: Float = 0f,
    var dirX: Float,
    var dirY: Float,
    var reformTimer: Float = 0f,
    var sx: Float = 0f,
    var sy: Float = 0f,
    var size: Float = 10f,
    var hitTimer: Float = 0f
)

class Bullet(
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    var t: Float,
    val duration: Float,
    val isEnemy: Boolean,
    val willHit: Boolean = false,
    val enemyType: EnemyType? = null,
    var whizzed: Boolean = false
)

enum class ParticleKind {
    SMOKE,
    FIRE,
    GLOW,
    SPARK,
    SHARD
}

class Particle(
    val kind: ParticleKind,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    var radius: Float = 4f,
    val growth: Float = 0f,
    val alpha: Float = 1f,
    var rot: Float = 0f,
    val vRot: Float = 0f,
    val color: Color = Color.Gray
)

class GlassCrack(
    val x: Float,
    val y: Float,
    val lines: List<List<Pair<Float, Float>>>
)

class Cloud(
    var z: Float,
    var x: Float,
    var y: Float,
    var size: Float,
    var variant: Int,
    var flip: Float
)

data class BannerMessage(
    val title: String,
    val subtitle: String,
    var remaining: Float,
    val maxDuration: Float
)

data class GameHudUiState(
    val mode: GameMode = GameMode.TITLE,
    val introStep: IntroStep = IntroStep.FORMATION_ATTACK,
    val introStepElapsed: Float = 0f,
    val introStepDuration: Float = 2.5f,
    val score: Int = 0,
    val highScore: Int = 0,
    val hull: Float = 100f,
    val heat: Float = 0f,
    val jammed: Boolean = false,
    val wave: Int = 0,
    val enemiesRemaining: Int = 0,
    val kills: Int = 0,
    val multiplier: Int = 1,
    val banner: BannerMessage? = null,
    val activeHullAlert: HullAlertType = HullAlertType.NONE,
    val isCriticalHull: Boolean = false,
    val isWounded: Boolean = false,
    val muted: Boolean = false,
    val canRestartFromOver: Boolean = false
)
