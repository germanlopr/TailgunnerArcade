package com.example.game

import com.example.audio.SoundEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class TailGunnerEngine(
    private val soundEngine: SoundEngine,
    private val onNewHighScore: (Int) -> Unit
) {
    companion object {
        const val TAU = (PI * 2.0).toFloat()
        const val CLOUD_VARIANTS = 6
        const val RADIO_MESSAGE_ES = "BANDIDOS A LAS 6, 9, 10 Y 1 EN PUNTO"
        const val RADIO_MESSAGE_EN = "BANDITS AT 6, 9, 10 AND 1 O'CLOCK"
    }

    var width = 1080f
        private set
    var height = 1920f
        private set
    var unit = 1f
        private set
    var cx = 540f
        private set
    var cy = 883f
        private set
    var focal = 600f
        private set

    var mode = GameMode.TITLE
        private set
    var totalTime = 0f
        private set

    var introStep = IntroStep.FORMATION_ATTACK
        private set
    var introStepTime = 0f
        private set

    var score = 0
        private set
    var highScore = 0
        private set
    var hull = 100f
        private set
    var heat = 0f
        private set
    var jammed = false
        private set
    var wave = 0
        private set
    var kills = 0
        private set
    var streak = 0
        private set
    var mult = 1
        private set
    var lastKillTime = -99f
        private set

    var shake = 0f
        private set
    var flash = 0f
        private set
    var kick = 0f
        private set
    var side = 0
    val muzzleFlash = floatArrayOf(0f, 0f)
    var overAt = 0f
        private set

    var aimX = 540f
    var aimY = 880f
    var isFiring = false

    var banner: BannerMessage? = null
        private set

    private val toSpawn = ArrayDeque<EnemyType>()
    private var spawnTimer = 0f
    private var interWaveTimer = 0f
    private var maxActive = 2
    private var fireTimer = 0f
    private var flakTimer = 2.4f

    // Hull threshold card triggers that re-arm after wave repair
    private var armedCritical30 = true
    private var armedWounded12 = true
    var activeHullAlert = HullAlertType.NONE
        private set
    private var hullAlertTimer = 0f
    private var heartbeatLoopTimer = 0f
    private var alarmLoopTimer = 0f

    val clouds = ArrayList<Cloud>(48)
    val enemies = ArrayList<Enemy>()
    val playerBullets = ArrayList<Bullet>()
    val enemyBullets = ArrayList<Bullet>()
    val particles = ArrayList<Particle>()
    val cracks = ArrayList<GlassCrack>()

    private var radioVoiceTriggered = false

    fun setStoredHighScore(value: Int) {
        if (value > highScore) {
            highScore = value
        }
    }

    fun resize(w: Float, h: Float) {
        val nw = max(280f, w)
        val nh = max(200f, h)
        if (nw == width && nh == height && clouds.isNotEmpty()) return
        val firstInit = clouds.isEmpty()
        width = nw
        height = nh
        unit = sqrt(width * height) / sqrt(1280f * 720f)
        cx = width / 2f
        cy = height * 0.46f
        focal = min(height * 0.95f, width * 0.62f)
        if (firstInit) {
            aimX = cx
            aimY = cy
            for (i in 0 until 48) {
                clouds.add(makeCloud(initial = true))
            }
        } else {
            aimX = aimX.coerceIn(0f, width)
            aimY = aimY.coerceIn(0f, height)
        }
    }

    fun startMissionWithIntro() {
        resetWorldForMission()
        mode = GameMode.INTRO
        soundEngine.engineActive = true
        enterIntroStep(IntroStep.FORMATION_ATTACK)
    }

    fun skipIntroToPlay() {
        if (mode != GameMode.INTRO) return
        beginActivePlay()
    }

    fun restartImmediate() {
        resetWorldForMission()
        beginActivePlay()
    }

    fun returnToTitle() {
        enemies.clear()
        playerBullets.clear()
        enemyBullets.clear()
        particles.clear()
        cracks.clear()
        banner = null
        activeHullAlert = HullAlertType.NONE
        isFiring = false
        mode = GameMode.TITLE
        soundEngine.engineActive = false
    }

    fun togglePause() {
        when (mode) {
            GameMode.PLAY -> {
                mode = GameMode.PAUSED
                isFiring = false
                soundEngine.engineActive = false
            }
            GameMode.PAUSED -> {
                mode = GameMode.PLAY
                soundEngine.engineActive = true
            }
            else -> Unit
        }
    }

    fun pauseIfPlaying() {
        if (mode == GameMode.PLAY) {
            mode = GameMode.PAUSED
            isFiring = false
        }
        soundEngine.pause()
    }

    fun resumeFromLifecycle() {
        if (mode == GameMode.PLAY || mode == GameMode.INTRO) {
            soundEngine.engineActive = true
        }
    }

    private fun resetWorldForMission() {
        enemies.clear()
        playerBullets.clear()
        enemyBullets.clear()
        particles.clear()
        cracks.clear()
        toSpawn.clear()
        score = 0
        hull = 100f
        heat = 0f
        jammed = false
        wave = 0
        kills = 0
        streak = 0
        mult = 1
        lastKillTime = -99f
        shake = 0f
        flash = 0f
        kick = 0f
        muzzleFlash[0] = 0f
        muzzleFlash[1] = 0f
        banner = null
        spawnTimer = 0f
        interWaveTimer = 0.7f
        fireTimer = 0f
        aimX = cx
        aimY = cy
        isFiring = false
        armedCritical30 = true
        armedWounded12 = true
        activeHullAlert = HullAlertType.NONE
        hullAlertTimer = 0f
        heartbeatLoopTimer = 0f
        alarmLoopTimer = 0f
    }

    private fun beginActivePlay() {
        mode = GameMode.PLAY
        interWaveTimer = 0.65f
        soundEngine.engineActive = true
    }

    private fun enterIntroStep(step: IntroStep) {
        introStep = step
        introStepTime = 0f
        radioVoiceTriggered = false
        when (step) {
            IntroStep.FORMATION_ATTACK -> {
                soundEngine.playFlakBoom()
            }
            IntroStep.BOMBER_BURNING -> {
                soundEngine.playBoom(big = true)
            }
            IntroStep.RADIO_ALERT -> {
                soundEngine.playRadioStaticAndClick()
            }
            IntroStep.HANDS_ON_CONTROLS -> {
                soundEngine.speakGetReady()
            }
            IntroStep.CROSSHAIR_LOCK -> {
                soundEngine.speakFireAtWill()
            }
        }
    }

    fun update(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0f, 0.05f)
        totalTime += dt
        updateClouds(dt)

        if (mode == GameMode.PAUSED) return

        if (mode == GameMode.INTRO) {
            updateIntro(dt)
            return
        }

        // Note: on TITLE screen, NO demo fighters are spawned ("Quité los cazas de demostración de la portada")
        if (mode != GameMode.TITLE) {
            updateEnemies(dt)
        }
        updateBullets(dt)
        updateParticles(dt)

        if (mode == GameMode.PLAY) {
            updateGuns(dt)
            updateWaves(dt)
            updateHullAlerts(dt)
            if (totalTime - lastKillTime > 5f) {
                mult = 1
            }
        } else {
            kick = max(0f, kick - dt * 9f)
        }

        shake = max(0f, shake - dt * 2.2f)
        flash = max(0f, flash - dt * 1.8f)

        banner?.let { b ->
            b.remaining -= dt
            if (b.remaining <= 0f) banner = null
        }
    }

    private fun updateIntro(dt: Float) {
        introStepTime += dt
        if (introStep == IntroStep.RADIO_ALERT && !radioVoiceTriggered && introStepTime >= 0.30f) {
            radioVoiceTriggered = true
            soundEngine.speakRadioBandits()
        }
        if (introStepTime >= introStep.durationSec) {
            when (introStep) {
                IntroStep.FORMATION_ATTACK -> enterIntroStep(IntroStep.BOMBER_BURNING)
                IntroStep.BOMBER_BURNING -> enterIntroStep(IntroStep.RADIO_ALERT)
                IntroStep.RADIO_ALERT -> enterIntroStep(IntroStep.HANDS_ON_CONTROLS)
                IntroStep.HANDS_ON_CONTROLS -> enterIntroStep(IntroStep.CROSSHAIR_LOCK)
                IntroStep.CROSSHAIR_LOCK -> beginActivePlay()
            }
        }
    }

    private fun updateHullAlerts(dt: Float) {
        if (hullAlertTimer > 0f) {
            hullAlertTimer -= dt
            if (hullAlertTimer <= 0f) {
                activeHullAlert = HullAlertType.NONE
            }
        }

        if (hull < 12f) {
            heartbeatLoopTimer -= dt
            if (heartbeatLoopTimer <= 0f) {
                soundEngine.playHeartbeat()
                heartbeatLoopTimer = 1.9f
            }
        } else if (hull < 30f) {
            alarmLoopTimer -= dt
            if (alarmLoopTimer <= 0f && activeHullAlert == HullAlertType.CRITICAL_30) {
                soundEngine.playCriticalAlarm()
                alarmLoopTimer = 2.0f
            }
        }
    }

    fun toHudUiState(): GameHudUiState {
        return GameHudUiState(
            mode = mode,
            introStep = introStep,
            introStepElapsed = introStepTime,
            introStepDuration = introStep.durationSec,
            score = score,
            highScore = max(highScore, score),
            hull = hull,
            heat = heat,
            jammed = jammed,
            wave = wave,
            enemiesRemaining = toSpawn.size + enemies.size,
            kills = kills,
            multiplier = mult,
            banner = banner?.copy(),
            activeHullAlert = activeHullAlert,
            isCriticalHull = mode == GameMode.PLAY && hull < 30f,
            isWounded = mode == GameMode.PLAY && hull < 12f,
            muted = soundEngine.muted,
            canRestartFromOver = mode == GameMode.OVER && (totalTime - overAt > 1.1f)
        )
    }

    // ========== Clouds & Flak ==========

    private fun halfW(z: Float) = width / 2f * z / focal
    private fun halfH(z: Float) = height / 2f * z / focal

    private fun makeCloud(initial: Boolean): Cloud {
        val z = if (initial) rnd(150f, 3600f) else rnd(120f, 260f)
        return Cloud(
            z = z,
            x = rnd(-1.5f, 1.5f) * halfW(z),
            y = rnd(-0.8f, 1.5f) * halfH(z),
            size = rnd(450f, 1150f),
            variant = Random.nextInt(CLOUD_VARIANTS),
            flip = if (Random.nextBoolean()) -1f else 1f
        )
    }

    private fun updateClouds(dt: Float) {
        for (i in clouds.indices) {
            val c = clouds[i]
            c.z += 420f * dt
            if (c.z > 3700f) {
                clouds[i] = makeCloud(initial = false)
            }
        }
        if (mode == GameMode.PLAY) {
            flakTimer -= dt
            if (flakTimer <= 0f) {
                flakTimer = rnd(2.1f, 4.8f)
                val x = rnd(width * 0.12f, width * 0.88f)
                val y = rnd(height * 0.12f, height * 0.48f)
                val r = rnd(16f, 30f) * unit
                particles.add(
                    Particle(
                        kind = ParticleKind.GLOW,
                        x = x, y = y, vx = 0f, vy = 0f,
                        life = 0.16f, maxLife = 0.16f, radius = r * 1.8f, growth = 2f, alpha = 0.8f
                    )
                )
                for (j in 0 until 7) {
                    val a = rnd(0f, TAU)
                    val v = rnd(12f, 65f) * unit
                    particles.add(
                        Particle(
                            kind = ParticleKind.SMOKE,
                            x = x + cos(a) * r * 0.35f,
                            y = y + sin(a) * r * 0.35f,
                            vx = cos(a) * v,
                            vy = sin(a) * v - 12f * unit,
                            life = rnd(0.9f, 1.7f),
                            maxLife = 1.7f,
                            radius = r * rnd(0.45f, 0.85f),
                            growth = 0.3f,
                            alpha = 0.65f
                        )
                    )
                }
                for (j in 0 until 5) {
                    val a = rnd(0f, TAU)
                    val v = rnd(35f, 100f) * unit
                    particles.add(
                        Particle(
                            kind = ParticleKind.SPARK,
                            x = x, y = y, vx = cos(a) * v, vy = sin(a) * v,
                            life = 0.28f, maxLife = 0.28f, alpha = 0.8f
                        )
                    )
                }
            }
        }
    }

    // ========== Enemies ==========

    private fun spawnEnemy(type: EnemyType) {
        val z = rnd(2300f, 2800f)
        val xc = rnd(-0.75f, 0.75f) * halfW(620f)
        val yc = rnd(-0.50f, 0.25f) * halfH(620f)
        val e = Enemy(
            type = type,
            hp = type.maxHp,
            hpMax = type.maxHp,
            z = z,
            state = EnemyState.APPROACH,
            t = rnd(0f, 9f),
            phase = rnd(0f, TAU),
            xc = xc,
            yc = yc,
            ax = rnd(0.10f, 0.30f) * halfW(620f),
            ay = rnd(0.05f, 0.18f) * halfH(620f),
            x = xc,
            y = yc,
            zAtk = rnd(520f, 720f),
            burstTimer = rnd(0.4f, 1.0f),
            dirX = if (Random.nextBoolean()) -1f else 1f,
            dirY = rnd(-0.4f, 0.4f),
            sx = cx,
            sy = cy
        )
        enemies.add(e)
    }

    private fun reformEnemy(e: Enemy) {
        e.state = EnemyState.APPROACH
        e.z = rnd(2300f, 2700f)
        e.phase = rnd(0f, TAU)
        e.xc = rnd(-0.75f, 0.75f) * halfW(620f)
        e.yc = rnd(-0.50f, 0.25f) * halfH(620f)
        e.zAtk = rnd(520f, 720f)
        e.dirX = if (Random.nextBoolean()) -1f else 1f
        e.dirY = rnd(-0.4f, 0.4f)
        e.burstCount = 0
        e.bank = 0f
    }

    private fun updateEnemies(dt: Float) {
        for (e in enemies) {
            e.t += dt
            e.hitTimer = max(0f, e.hitTimer - dt)
            e.flashTimer = max(0f, e.flashTimer - dt)
            if (e.state == EnemyState.GONE) {
                e.reformTimer -= dt
                if (e.reformTimer <= 0f) reformEnemy(e)
                continue
            }
            var bankTarget = cos(e.phase + e.t * 0.9f) * 0.35f
            if (e.state == EnemyState.APPROACH || e.state == EnemyState.ATTACK) {
                e.x = e.xc + e.ax * sin(e.phase + e.t * 0.9f)
                e.y = e.yc + e.ay * sin(e.phase * 1.7f + e.t * 1.3f)
            }
            when (e.state) {
                EnemyState.APPROACH -> {
                    e.z -= e.type.speed * dt
                    if (e.z <= e.zAtk) {
                        e.state = EnemyState.ATTACK
                        e.stateTimer = rnd(2.6f, 3.8f)
                    }
                }
                EnemyState.ATTACK -> {
                    e.z = max(e.zAtk * 0.6f, e.z - 45f * dt)
                    e.stateTimer -= dt
                    e.burstTimer -= dt
                    if (e.burstCount > 0) {
                        e.shotTimer -= dt
                        if (e.shotTimer <= 0f) {
                            enemyShoot(e)
                            e.burstCount--
                            e.shotTimer = 0.085f
                        }
                    } else if (e.burstTimer <= 0f) {
                        e.burstCount = Random.nextInt(4, 8)
                        e.burstTimer = rnd(0.5f, 1.0f) + e.burstCount * 0.085f
                        e.shotTimer = 0f
                    }
                    if (e.stateTimer <= 0f) {
                        e.state = EnemyState.BREAK
                    }
                }
                EnemyState.BREAK -> {
                    e.z -= 720f * dt
                    e.x += e.dirX * 340f * dt
                    e.y += e.dirY * 140f * dt
                    bankTarget = e.dirX * 1.1f
                    if (e.z < 70f) {
                        e.state = EnemyState.GONE
                        e.reformTimer = rnd(2.5f, 4.5f)
                        continue
                    }
                }
                EnemyState.GONE -> Unit
            }

            e.bank = lerp(e.bank, bankTarget, min(1f, dt * 3f))
            val zz = max(e.z, 60f)
            e.sx = cx + e.x * focal / zz
            e.sy = cy + e.y * focal / zz
            e.size = e.type.wingSpan * focal / zz

            val hf = e.hp / e.hpMax
            if (hf < 0.55f) {
                e.smokeTimer -= dt
                if (e.smokeTimer <= 0f) {
                    e.smokeTimer = lerp(0.03f, 0.09f, hf)
                    spawnTrail(e, fire = hf < 0.30f)
                }
            }
        }
    }

    private fun spawnTrail(e: Enemy, fire: Boolean) {
        val x = e.sx + rnd(-0.08f, 0.08f) * e.size
        val y = e.sy + rnd(-0.03f, 0.05f) * e.size
        particles.add(
            Particle(
                kind = ParticleKind.SMOKE,
                x = x, y = y,
                vx = (cx - x) * 0.9f,
                vy = (cy - y) * 0.9f - 4f * unit,
                life = 1.1f, maxLife = 1.1f,
                radius = e.size * 0.20f, growth = -0.12f, alpha = 0.7f
            )
        )
        if (fire) {
            particles.add(
                Particle(
                    kind = ParticleKind.FIRE,
                    x = x, y = y,
                    vx = (cx - x) * 0.6f,
                    vy = (cy - y) * 0.6f,
                    life = 0.35f, maxLife = 0.35f,
                    radius = e.size * 0.13f, growth = -0.2f, alpha = 0.9f
                )
            )
        }
    }

    private fun enemyShoot(e: Enemy) {
        val sSide = if (Random.nextBoolean()) -1f else 1f
        val x0 = e.sx + sSide * e.size * 0.22f * cos(e.bank)
        val y0 = e.sy + e.size * 0.05f + sSide * e.size * 0.22f * sin(e.bank)
        val hit = Random.nextFloat() < e.type.hitChance
        val tx: Float
        val ty: Float
        if (hit) {
            tx = cx + rnd(-0.30f, 0.30f) * width
            ty = cy + rnd(-0.28f, 0.30f) * height
        } else {
            val dir = if (Random.nextBoolean()) -1f else 1f
            tx = cx + dir * rnd(0.55f, 0.95f) * width
            ty = rnd(0.05f, 0.95f) * height
        }
        enemyBullets.add(
            Bullet(
                x0 = x0, y0 = y0, x1 = tx, y1 = ty,
                t = 0f, duration = rnd(0.32f, 0.45f),
                isEnemy = true, willHit = hit, enemyType = e.type
            )
        )
        e.flashTimer = 0.06f
    }

    // ========== Player Guns & Damage ==========

    data class GunTip(val bx: Float, val by: Float, val tx: Float, val ty: Float)

    fun gunGeom(): Array<GunTip> {
        val f = 0.42f - 0.05f * kick
        fun tip(s: Int): GunTip {
            val sign = if (s == 1) 1f else -1f
            val bx = cx + sign * 0.12f * width + (aimX - cx) * 0.05f
            val by = height + 10f * unit
            return GunTip(bx, by, lerp(bx, aimX, f), lerp(by, aimY, f))
        }
        return arrayOf(tip(0), tip(1))
    }

    private fun fireBullet() {
        val g = gunGeom()[side]
        val wounded = hull < 12f
        // When wounded (<12% hull), bullets disperse significantly more ("los disparos se dispersan más")
        val spread = if (wounded) {
            (26f + heat * 0.28f) * unit
        } else {
            (5f + heat * 0.10f) * unit
        }
        val targetX = (aimX + rnd(-spread, spread)).coerceIn(0f, width)
        val targetY = (aimY + rnd(-spread, spread)).coerceIn(0f, height)

        playerBullets.add(
            Bullet(
                x0 = g.tx, y0 = g.ty,
                x1 = targetX, y1 = targetY,
                t = 0f, duration = 0.20f,
                isEnemy = false
            )
        )
        muzzleFlash[side] = 0.05f
        side = side xor 1
        kick = 1f
        shake = max(shake, if (wounded) 0.16f else 0.10f)
        soundEngine.playShot()
    }

    private fun hitTest(x: Float, y: Float): Enemy? {
        var best: Enemy? = null
        for (e in enemies) {
            if (e.state == EnemyState.GONE || e.z < 90f) continue
            val rx = e.size * 0.5f * 0.9f + 8f * unit
            val ry = e.size * 0.5f * 0.42f + 8f * unit
            val dx = (x - e.sx) / rx
            val dy = (y - e.sy) / ry
            if (dx * dx + dy * dy <= 1f && (best == null || e.z < best.z)) {
                best = e
            }
        }
        return best
    }

    private fun damageEnemy(e: Enemy, x: Float, y: Float) {
        e.hp -= 1f
        e.hitTimer = 0.08f
        for (i in 0 until 3) {
            val a = rnd(0f, TAU)
            val v = rnd(80f, 260f) * unit
            particles.add(
                Particle(
                    kind = ParticleKind.SPARK,
                    x = x, y = y, vx = cos(a) * v, vy = sin(a) * v,
                    life = 0.25f, maxLife = 0.25f, alpha = 1f
                )
            )
        }
        if (Random.nextFloat() < 0.2f) soundEngine.playSpark()
        if (e.hp <= 0f) killEnemy(e)
    }

    private fun killEnemy(e: Enemy) {
        boom(e.sx, e.sy, min(e.size, 420f * unit), e.type)
        enemies.remove(e)
        soundEngine.playBoom(e.size > 150f * unit)
        if (mode != GameMode.PLAY) return
        kills++
        streak = if (totalTime - lastKillTime < 5f) streak + 1 else 1
        lastKillTime = totalTime
        mult = min(4, 1 + (streak - 1) / 2)
        score += e.type.points * mult
        if (score > highScore) {
            highScore = score
            onNewHighScore(highScore)
        }
    }

    private fun boom(x: Float, y: Float, size: Float, type: EnemyType?) {
        val s = max(size, 40f * unit)
        particles.add(
            Particle(
                kind = ParticleKind.GLOW,
                x = x, y = y, vx = 0f, vy = 0f,
                life = 0.20f, maxLife = 0.20f, radius = s * 1.3f, growth = 1.5f, alpha = 1f
            )
        )
        for (i in 0 until 14) {
            val a = rnd(0f, TAU)
            val v = rnd(0.2f, 1.1f) * s
            particles.add(
                Particle(
                    kind = ParticleKind.FIRE,
                    x = x + cos(a) * s * 0.1f, y = y + sin(a) * s * 0.1f,
                    vx = cos(a) * v, vy = sin(a) * v,
                    life = rnd(0.5f, 0.9f), maxLife = 0.9f,
                    radius = s * rnd(0.25f, 0.5f), growth = 0.35f, alpha = 1f
                )
            )
        }
        for (i in 0 until 12) {
            val a = rnd(0f, TAU)
            val v = rnd(0.1f, 0.6f) * s
            particles.add(
                Particle(
                    kind = ParticleKind.SMOKE,
                    x = x, y = y,
                    vx = cos(a) * v + (cx - x) * 0.3f,
                    vy = sin(a) * v + (cy - y) * 0.3f,
                    life = rnd(1.2f, 2.0f), maxLife = 2.0f,
                    radius = s * rnd(0.25f, 0.45f), growth = 0.25f, alpha = 0.85f
                )
            )
        }
        for (i in 0 until 9) {
            val a = rnd(0f, TAU)
            val v = rnd(0.3f, 1.2f) * s
            particles.add(
                Particle(
                    kind = ParticleKind.SHARD,
                    x = x, y = y,
                    vx = cos(a) * v, vy = sin(a) * v - s * 0.2f,
                    life = rnd(1.0f, 1.6f), maxLife = 1.6f,
                    radius = s * rnd(0.06f, 0.15f),
                    rot = rnd(0f, TAU), vRot = rnd(-8f, 8f),
                    color = type?.bodyColor ?: androidx.compose.ui.graphics.Color(0xFF444444)
                )
            )
        }
        for (i in 0 until 22) {
            val a = rnd(0f, TAU)
            val v = rnd(0.4f, 1.6f) * s * 1.2f
            particles.add(
                Particle(
                    kind = ParticleKind.SPARK,
                    x = x, y = y, vx = cos(a) * v, vy = sin(a) * v,
                    life = rnd(0.3f, 0.7f), maxLife = 0.7f, alpha = 1f
                )
            )
        }
        shake = max(shake, min(0.9f, 0.25f + s / 700f))
    }

    private fun addCrack(x: Float, y: Float) {
        val lines = ArrayList<List<Pair<Float, Float>>>()
        val n = Random.nextInt(4, 7)
        for (i in 0 until n) {
            val a = i.toFloat() / n * TAU + rnd(-0.3f, 0.3f)
            val len = rnd(16f, 48f) * unit
            val pts = ArrayList<Pair<Float, Float>>(4)
            pts.add(0f to 0f)
            for (j in 1..3) {
                val d = len * j / 3f
                val ja = a + rnd(-0.25f, 0.25f)
                pts.add(cos(ja) * d to sin(ja) * d)
            }
            lines.add(pts)
        }
        cracks.add(GlassCrack(x, y, lines))
        if (cracks.size > 8) cracks.removeAt(0)
    }

    private fun hitHull(b: Bullet) {
        val dmg = (b.enemyType?.damage ?: 1.2f) * rnd(0.9f, 1.1f)
        hull -= dmg
        shake = max(shake, 0.55f)
        flash = max(flash, 0.50f)
        addCrack(b.x1, b.y1)
        soundEngine.playHit()

        for (i in 0 until 8) {
            val a = rnd(0f, TAU)
            val v = rnd(80f, 320f) * unit
            particles.add(
                Particle(
                    kind = ParticleKind.SPARK,
                    x = b.x1, y = b.y1, vx = cos(a) * v, vy = sin(a) * v,
                    life = 0.30f, maxLife = 0.30f, alpha = 1f
                )
            )
        }

        if (hull <= 0f) {
            hull = 0f
            gameOver()
            return
        }

        // Check hull alert thresholds (12% wounded takes priority if crossed directly, otherwise 30% critical)
        if (hull < 12f && armedWounded12) {
            armedWounded12 = false
            activeHullAlert = HullAlertType.WOUNDED_12
            hullAlertTimer = 4.2f
            heartbeatLoopTimer = 1.6f
            soundEngine.playHeartbeat()
        } else if (hull < 30f && armedCritical30) {
            armedCritical30 = false
            activeHullAlert = HullAlertType.CRITICAL_30
            hullAlertTimer = 4.0f
            alarmLoopTimer = 2.0f
            soundEngine.playCriticalAlarm()
        }
    }

    private fun updateBullets(dt: Float) {
        for (i in playerBullets.indices.reversed()) {
            val b = playerBullets[i]
            b.t += dt
            if (b.t >= b.duration) {
                val e = hitTest(b.x1, b.y1)
                if (e != null) damageEnemy(e, b.x1, b.y1)
                playerBullets.removeAt(i)
            }
        }
        for (i in enemyBullets.indices.reversed()) {
            val b = enemyBullets[i]
            b.t += dt
            if (!b.willHit && !b.whizzed && b.t > b.duration * 0.7f) {
                b.whizzed = true
                if (Random.nextFloat() < 0.5f) soundEngine.playWhiz()
            }
            if (b.t >= b.duration) {
                if (b.willHit && mode == GameMode.PLAY) {
                    hitHull(b)
                }
                enemyBullets.removeAt(i)
            }
        }
    }

    private fun updateParticles(dt: Float) {
        for (i in particles.indices.reversed()) {
            val p = particles[i]
            p.life -= dt
            if (p.life <= 0f) {
                particles.removeAt(i)
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.kind == ParticleKind.SHARD) {
                p.vy += 420f * unit * dt
                p.rot += p.vRot * dt
            }
            if (p.growth != 0f) {
                p.radius = max(1f, p.radius * (1f + p.growth * dt))
            }
        }
        if (particles.size > 650) {
            particles.subList(0, particles.size - 650).clear()
        }
    }

    private fun updateGuns(dt: Float) {
        kick = max(0f, kick - dt * 9f)
        muzzleFlash[0] = max(0f, muzzleFlash[0] - dt)
        muzzleFlash[1] = max(0f, muzzleFlash[1] - dt)

        if (jammed) {
            heat -= 30f * dt
            if (heat <= 38f) {
                jammed = false
            }
            return
        }

        if (isFiring) {
            fireTimer -= dt
            while (fireTimer <= 0f) {
                fireTimer += 1f / 15f
                fireBullet()
                heat += 0.95f
                if (heat >= 100f) {
                    heat = 100f
                    jammed = true
                    soundEngine.playJam()
                    banner = BannerMessage(
                        title = "CAÑONES SOBRECALENTADOS",
                        subtitle = "Suelta el gatillo para enfriar",
                        remaining = 2.2f,
                        maxDuration = 2.2f
                    )
                    break
                }
            }
        } else {
            heat = max(0f, heat - 22f * dt)
            fireTimer = 0f
        }
    }

    private fun updateWaves(dt: Float) {
        if (interWaveTimer > 0f) {
            interWaveTimer -= dt
            if (interWaveTimer <= 0f) startWave()
            return
        }
        spawnTimer -= dt
        if (toSpawn.isNotEmpty() && enemies.size < maxActive && spawnTimer <= 0f) {
            spawnEnemy(toSpawn.removeFirst())
            spawnTimer = rnd(1.2f, 2.2f)
        }
        if (toSpawn.isEmpty() && enemies.isEmpty()) {
            val bonus = wave * 150
            score += bonus
            if (score > highScore) {
                highScore = score
                onNewHighScore(highScore)
            }
            hull = min(100f, hull + 15f)
            cracks.clear()

            // Re-arm alert cards if hull repaired past their thresholds!
            if (hull >= 30f) armedCritical30 = true
            if (hull >= 12f) armedWounded12 = true
            if (hull >= 30f && activeHullAlert == HullAlertType.CRITICAL_30) {
                activeHullAlert = HullAlertType.NONE
            }
            if (hull >= 12f && activeHullAlert == HullAlertType.WOUNDED_12) {
                activeHullAlert = HullAlertType.NONE
            }

            banner = BannerMessage(
                title = "OLEADA SUPERADA",
                subtitle = "+$bonus puntos · casco reparado +15%",
                remaining = 3.4f,
                maxDuration = 3.4f
            )
            interWaveTimer = 3.8f
        }
    }

    private fun startWave() {
        wave++
        val count = min(2 + wave, 10)
        toSpawn.clear()
        for (i in 0 until count) {
            val r = Random.nextFloat()
            val type = when {
                wave >= 3 && r < min(0.28f, 0.06f * wave) -> EnemyType.ME_410
                wave >= 2 && r < min(0.55f, 0.12f * wave) -> EnemyType.FW_190
                else -> EnemyType.BF_109
            }
            toSpawn.addLast(type)
        }
        spawnTimer = 1.2f
        maxActive = min(2 + floor(wave / 2f).toInt(), 5)
        banner = BannerMessage(
            title = "OLEADA $wave",
            subtitle = "$count cazas enemigos a las seis en punto",
            remaining = 2.8f,
            maxDuration = 2.8f
        )
    }

    private fun gameOver() {
        mode = GameMode.OVER
        overAt = totalTime
        flash = 1f
        shake = 1f
        isFiring = false
        banner = null
        activeHullAlert = HullAlertType.NONE
        boom(cx, cy, 260f * unit, null)
        soundEngine.playBoom(big = true)
        soundEngine.engineActive = false
        if (score >= highScore && score > 0) {
            onNewHighScore(score)
        }
    }

    private fun rnd(a: Float, b: Float) = a + Random.nextFloat() * (b - a)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
