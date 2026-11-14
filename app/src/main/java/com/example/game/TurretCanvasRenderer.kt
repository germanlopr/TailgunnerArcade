package com.example.game

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class TurretSpriteCache {
    val clouds: List<ImageBitmap> = List(TailGunnerEngine.CLOUD_VARIANTS) { buildCloudBitmap() }
    val fire: ImageBitmap = buildRadialBitmap(
        128,
        intArrayOf(
            android.graphics.Color.argb(255, 255, 250, 210),
            android.graphics.Color.argb(242, 255, 200, 90),
            android.graphics.Color.argb(153, 235, 100, 25),
            android.graphics.Color.argb(0, 80, 20, 0)
        ),
        floatArrayOf(0f, 0.25f, 0.55f, 1f)
    )
    val smoke: ImageBitmap = buildRadialBitmap(
        128,
        intArrayOf(
            android.graphics.Color.argb(216, 70, 72, 74),
            android.graphics.Color.argb(115, 50, 52, 54),
            android.graphics.Color.argb(0, 40, 40, 42)
        ),
        floatArrayOf(0f, 0.60f, 1f)
    )
    val glow: ImageBitmap = buildRadialBitmap(
        128,
        intArrayOf(
            android.graphics.Color.argb(255, 255, 248, 220),
            android.graphics.Color.argb(178, 255, 214, 120),
            android.graphics.Color.argb(0, 255, 170, 60)
        ),
        floatArrayOf(0f, 0.30f, 1f)
    )

    private fun buildRadialBitmap(size: Int, colors: IntArray, stops: FloatArray): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val r = size / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(r, r, r, colors, stops, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        return bmp.asImageBitmap()
    }

    private fun buildCloudBitmap(): ImageBitmap {
        val w = 360
        val h = 216
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = Random.nextInt(17, 25)
        data class Puff(val x: Float, val y: Float, val r: Float)
        val puffs = ArrayList<Puff>(count)
        for (i in 0 until count) {
            val a = Random.nextFloat() * (PI * 2f).toFloat()
            val rad = sqrt(Random.nextFloat())
            puffs.add(
                Puff(
                    x = w / 2f + cos(a) * rad * w * 0.34f,
                    y = h * 0.56f + sin(a) * rad * h * 0.22f,
                    r = (0.11f + Random.nextFloat() * 0.11f) * h * 1.25f
                )
            )
        }
        // Underside shadow
        for (pf in puffs) {
            val sy = pf.y + pf.r * 0.34f
            p.shader = RadialGradient(
                pf.x, sy, pf.r * 1.12f,
                intArrayOf(
                    android.graphics.Color.argb(184, 72, 91, 116),
                    android.graphics.Color.argb(107, 112, 132, 157),
                    android.graphics.Color.argb(0, 112, 132, 157)
                ),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP
            )
            c.drawCircle(pf.x, sy, pf.r * 1.12f, p)
        }
        // Sunlit tops
        for (pf in puffs) {
            val sx = pf.x - pf.r * 0.18f
            val sy = pf.y - pf.r * 0.28f
            p.shader = RadialGradient(
                sx - pf.r * 0.18f, sy - pf.r * 0.22f, pf.r,
                intArrayOf(
                    android.graphics.Color.argb(252, 255, 255, 255),
                    android.graphics.Color.argb(237, 246, 250, 255),
                    android.graphics.Color.argb(184, 218, 230, 243),
                    android.graphics.Color.argb(0, 190, 207, 226)
                ),
                floatArrayOf(0f, 0.42f, 0.76f, 1f),
                Shader.TileMode.CLAMP
            )
            c.drawCircle(sx, sy, pf.r, p)
        }
        return bmp.asImageBitmap()
    }
}

fun DrawScope.renderTurretScene(
    engine: TailGunnerEngine,
    sprites: TurretSpriteCache
) {
    val w = size.width
    val h = size.height
    val u = engine.unit
    val cx = engine.cx
    val cy = engine.cy

    val shakeX = if (engine.shake > 0f) {
        val k = engine.shake * 13f * u
        (Random.nextFloat() * 2f - 1f) * k
    } else 0f
    val shakeY = if (engine.shake > 0f) {
        val k = engine.shake * 13f * u
        (Random.nextFloat() * 2f - 1f) * k
    } else 0f

    translate(shakeX, shakeY) {
        drawSky(w, h, cy)
        drawClouds(engine, sprites, w, h, cx, cy)
        drawFormation(engine, w, h, u)
        if (engine.mode != GameMode.TITLE) {
            drawEnemies(engine, w)
        }
        drawBullets(engine, u)
        drawParticles(engine, sprites, u)
        drawTailStructure(w, h, u, cx)
        drawCracks(engine, u)
        drawWindowFrame(w, h, u)
        drawGuns(engine, sprites, w, h, u, cx)
    }

    if (engine.mode == GameMode.PLAY || engine.mode == GameMode.PAUSED) {
        drawSight(engine, u)
    }
    drawFilmAndVignette(engine, w, h, cx, cy)
}

private fun DrawScope.drawSky(w: Float, h: Float, cy: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF4B91CE),
            0.48f to Color(0xFF83B9E7),
            1f to Color(0xFFD5E8F8),
            startY = -60f,
            endY = cy
        ),
        topLeft = Offset(-60f, -60f),
        size = Size(w + 120f, cy + 60f)
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFC9DCED),
            0.30f to Color(0xFF7898B7),
            1f to Color(0xFF344D68),
            startY = cy,
            endY = h + 60f
        ),
        topLeft = Offset(-60f, cy),
        size = Size(w + 120f, h - cy + 60f)
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x85FFF5DC), Color(0x00FFEEC8)),
            center = Offset(w * 0.22f, h * 0.12f),
            radius = max(w, h) * 0.55f
        ),
        topLeft = Offset(-60f, -60f),
        size = Size(w + 120f, h + 120f)
    )
}

private fun DrawScope.drawClouds(
    engine: TailGunnerEngine,
    sprites: TurretSpriteCache,
    w: Float,
    h: Float,
    cx: Float,
    cy: Float
) {
    val sorted = engine.clouds.sortedByDescending { it.z }
    val f = engine.focal
    for (c in sorted) {
        if (c.z < 150f) continue
        val sw = c.size * f / c.z
        val sh = sw * 216f / 360f
        val sx = cx + c.x * f / c.z
        val sy = cy + c.y * f / c.z
        if (sx + sw < -20f || sx - sw > w + 20f || sy + sh < -20f || sy - sh > h + 20f) continue
        val alpha = (((c.z - 420f) / 520f).coerceIn(0f, 1f) *
            ((3700f - c.z) / 700f).coerceIn(0f, 1f) * 0.80f)
        if (alpha <= 0.01f) continue
        val img = sprites.clouds[c.variant]
        translate(sx, sy) {
            scale(scaleX = c.flip, scaleY = 1f, pivot = Offset.Zero) {
                drawImage(
                    image = img,
                    dstOffset = IntOffset((-sw / 2f).roundToInt(), (-sh / 2f).roundToInt()),
                    dstSize = IntSize(max(1, sw.roundToInt()), max(1, sh.roundToInt())),
                    alpha = alpha
                )
            }
        }
    }
}

private fun DrawScope.drawFormation(engine: TailGunnerEngine, w: Float, h: Float, u: Float) {
    val t = engine.totalTime
    val m = min(w, h)
    drawB17Silhouette(
        x = w * 0.15f,
        y = h * 0.30f + sin(t * 0.6f) * 3f * u,
        width = m * 0.20f,
        bank = sin(t * 0.4f) * 0.03f,
        alpha = 0.82f
    )
    drawB17Silhouette(
        x = w * 0.85f,
        y = h * 0.36f + sin(t * 0.5f + 2f) * 3f * u,
        width = m * 0.16f,
        bank = -0.02f + sin(t * 0.35f + 1f) * 0.025f,
        alpha = 0.70f
    )
}

private fun DrawScope.drawB17Silhouette(
    x: Float,
    y: Float,
    width: Float,
    bank: Float,
    alpha: Float
) {
    val s = width / 100f
    translate(x, y) {
        rotateRad(bank, Offset.Zero) {
            scale(s, s, Offset.Zero) {
                val wingPath = Path().apply {
                    moveTo(-50f, 0f)
                    lineTo(-6f, -2f)
                    lineTo(6f, -2f)
                    lineTo(50f, 0f)
                    lineTo(50f, 3f)
                    lineTo(6f, 5f)
                    lineTo(-6f, 5f)
                    lineTo(-50f, 3f)
                    close()
                }
                drawPath(
                    path = wingPath,
                    brush = Brush.verticalGradient(
                        0f to Color(0xFFDFE6EE),
                        1f to Color(0xFF8E99A6),
                        startY = -24f,
                        endY = 8f
                    ),
                    alpha = alpha
                )
                for (ex in listOf(-32f, -14f, 14f, 32f)) {
                    drawOval(
                        color = Color(0xFF6D7886),
                        topLeft = Offset(ex - 3.2f, 1.5f - 4f),
                        size = Size(6.4f, 8f),
                        alpha = alpha
                    )
                    drawCircle(
                        color = Color(0x38EBF0F5),
                        radius = 7f,
                        center = Offset(ex, 1.5f),
                        alpha = alpha
                    )
                }
                drawOval(
                    color = Color(0xFFB4BFC9),
                    topLeft = Offset(-4f, 3f - 7f),
                    size = Size(8f, 14f),
                    alpha = alpha
                )
                drawRect(
                    color = Color(0xFFB4BFC9),
                    topLeft = Offset(-16f, -1.5f),
                    size = Size(32f, 3f),
                    alpha = alpha
                )
                val finPath = Path().apply {
                    moveTo(-2.6f, 2f)
                    lineTo(-1.4f, -24f)
                    lineTo(1.4f, -24f)
                    lineTo(2.6f, 2f)
                    close()
                }
                drawPath(finPath, Color(0xFFAAB4C0), alpha = alpha)
            }
        }
    }
}

private fun DrawScope.drawEnemies(engine: TailGunnerEngine, w: Float) {
    val active = engine.enemies.filter { it.state != EnemyState.GONE }.sortedByDescending { it.z }
    for (e in active) {
        if (e.sx < -e.size || e.sx > w + e.size) continue
        drawFighter(e)
    }
}

private fun DrawScope.drawFighter(e: Enemy) {
    val T = e.type
    val s = e.size / 100f
    val a = (1.18f - e.z / 3400f).coerceIn(0.4f, 1f)

    translate(e.sx, e.sy) {
        rotateRad(e.bank, Offset.Zero) {
            scale(s, s, Offset.Zero) {
                // Tail surfaces
                drawRect(
                    color = T.darkColor,
                    topLeft = Offset(-16f, -3f),
                    size = Size(32f, 2.6f),
                    alpha = a
                )
                if (T.isTwin) {
                    drawRect(T.darkColor, Offset(-17f, -16f), Size(2.6f, 14f), alpha = a)
                    drawRect(T.darkColor, Offset(14.4f, -16f), Size(2.6f, 14f), alpha = a)
                } else {
                    drawRect(T.darkColor, Offset(-1.4f, -20f), Size(2.8f, 18f), alpha = a)
                }

                // Wings
                val wingDrop = if (T.isTwin) 1f else 0f
                val leftWing = Path().apply {
                    moveTo(-6f, 4f)
                    lineTo(-50f, 1f + wingDrop)
                    lineTo(-50f, 4.5f)
                    lineTo(-6f, 9.5f)
                    close()
                }
                val rightWing = Path().apply {
                    moveTo(6f, 4f)
                    lineTo(50f, 1f + wingDrop)
                    lineTo(50f, 4.5f)
                    lineTo(6f, 9.5f)
                    close()
                }
                drawPath(leftWing, T.bodyColor, alpha = a)
                drawPath(rightWing, T.bodyColor, alpha = a)
                drawRect(Color(0x1FFFFFFF), Offset(-50f, 1.4f), Size(100f, 0.9f), alpha = a)

                // Wing gun ports
                for (gx in listOf(-22f, 22f)) {
                    drawCircle(Color(0xFF14181A), radius = 1.9f, center = Offset(gx, 5.5f), alpha = a)
                }

                // Fuselage & canopy
                drawOval(
                    color = T.darkColor,
                    topLeft = Offset(if (T.isTwin) -5f else -7.5f, if (T.isTwin) -6f else -7f),
                    size = Size(if (T.isTwin) 10f else 15f, if (T.isTwin) 16f else 18f),
                    alpha = a
                )
                drawOval(
                    color = Color(0xFF8FA0AD),
                    topLeft = Offset(if (T.isTwin) -3.6f else -4.4f, -8.6f),
                    size = Size(if (T.isTwin) 7.2f else 8.8f, 7.2f),
                    alpha = a * 0.55f
                )

                // Engine cowlings & spinning props
                val engines = if (T.isTwin) listOf(-20f, 20f) else listOf(0f)
                val cowlingR = if (T.isTwin) 6f else 8f
                val propR = if (T.isTwin) 12.5f else 16f
                val rotBase = e.t * 38f
                for (ex in engines) {
                    drawCircle(T.darkColor, radius = cowlingR, center = Offset(ex, 4f), alpha = a)
                    drawCircle(
                        color = T.cowlColor ?: T.noseColor,
                        radius = cowlingR,
                        center = Offset(ex, 4f),
                        alpha = a,
                        style = Stroke(width = if (T.cowlColor != null) 2.2f else 1.6f)
                    )
                    drawCircle(Color(0x2BD7DEE4), radius = propR, center = Offset(ex, 4f), alpha = a)
                    for (i in 0 until 3) {
                        val an = rotBase + i * (TailGunnerEngine.TAU / 3f)
                        drawLine(
                            color = Color(0x73EBF0F5),
                            start = Offset(ex, 4f),
                            end = Offset(ex + cos(an) * (propR - 0.5f), 4f + sin(an) * (propR - 0.5f)),
                            strokeWidth = 1f,
                            alpha = a
                        )
                    }
                    drawCircle(Color(0xFFC4CCD2), radius = 3f, center = Offset(ex, 4f), alpha = a)
                    drawCircle(
                        color = if (T.cowlColor != null) Color(0xFF444444) else T.noseColor,
                        radius = 1.6f,
                        center = Offset(ex, 4f),
                        alpha = a
                    )
                }

                // Enemy muzzle flashes
                if (e.flashTimer > 0f) {
                    for (gx in listOf(-22f, 22f)) {
                        drawCircle(
                            color = Color(0xF2FFDC78),
                            radius = 4.2f,
                            center = Offset(gx, 5.5f),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
                // Hit flash
                if (e.hitTimer > 0f) {
                    drawOval(
                        color = Color(0x47FFFFFF),
                        topLeft = Offset(-50f, -6f),
                        size = Size(100f, 20f)
                    )
                }
            }
        }
    }
}

private fun pe(qRaw: Float): Float {
    val q = qRaw.coerceIn(0f, 1f)
    return 1f - (1f - q) * (1f - q)
}

private fun DrawScope.drawBullets(engine: TailGunnerEngine, u: Float) {
    for (b in engine.playerBullets) {
        val p = b.t / b.duration
        val h = pe(p)
        val t = pe(p - 0.2f)
        val hx = lerp(b.x0, b.x1, h)
        val hy = lerp(b.y0, b.y1, h)
        val tx = lerp(b.x0, b.x1, t)
        val ty = lerp(b.y0, b.y1, t)
        val w = (2.6f * (1f - p) + 0.9f) * u
        drawLine(
            color = Color(0x47FFAA3C),
            start = Offset(tx, ty),
            end = Offset(hx, hy),
            strokeWidth = w * 3.2f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
        drawLine(
            color = Color(0xF2FFECAA),
            start = Offset(tx, ty),
            end = Offset(hx, hy),
            strokeWidth = w,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
    }

    for (b in engine.enemyBullets) {
        val p = b.t / b.duration
        val q = p * p
        val q2 = max(0f, p - 0.14f) * max(0f, p - 0.14f)
        val hx = lerp(b.x0, b.x1, q)
        val hy = lerp(b.y0, b.y1, q)
        val tx = lerp(b.x0, b.x1, q2)
        val ty = lerp(b.y0, b.y1, q2)
        val w = (1.2f + 4.5f * p) * u
        drawLine(
            color = Color(0x4DFF461E),
            start = Offset(tx, ty),
            end = Offset(hx, hy),
            strokeWidth = w * 3f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
        drawLine(
            color = Color(0xF2FFC478),
            start = Offset(tx, ty),
            end = Offset(hx, hy),
            strokeWidth = w,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
    }
}

private fun DrawScope.drawParticles(
    engine: TailGunnerEngine,
    sprites: TurretSpriteCache,
    u: Float
) {
    for (p in engine.particles) {
        if (p.kind != ParticleKind.SMOKE) continue
        val f = (p.life / p.maxLife).coerceIn(0f, 1f)
        val d = max(2, (p.radius * 2f).roundToInt())
        drawImage(
            image = sprites.smoke,
            dstOffset = IntOffset((p.x - p.radius).roundToInt(), (p.y - p.radius).roundToInt()),
            dstSize = IntSize(d, d),
            alpha = f * p.alpha
        )
    }

    for (p in engine.particles) {
        val f = (p.life / p.maxLife).coerceIn(0f, 1f)
        when (p.kind) {
            ParticleKind.FIRE -> {
                val d = max(2, (p.radius * 2f).roundToInt())
                drawImage(
                    image = sprites.fire,
                    dstOffset = IntOffset((p.x - p.radius).roundToInt(), (p.y - p.radius).roundToInt()),
                    dstSize = IntSize(d, d),
                    alpha = min(1f, f * 1.4f) * p.alpha,
                    blendMode = BlendMode.Plus
                )
            }
            ParticleKind.GLOW -> {
                val d = max(2, (p.radius * 2f).roundToInt())
                drawImage(
                    image = sprites.glow,
                    dstOffset = IntOffset((p.x - p.radius).roundToInt(), (p.y - p.radius).roundToInt()),
                    dstSize = IntSize(d, d),
                    alpha = f * p.alpha,
                    blendMode = BlendMode.Plus
                )
            }
            ParticleKind.SPARK -> {
                drawLine(
                    color = Color(0xFFFFBE5A),
                    start = Offset(p.x, p.y),
                    end = Offset(p.x - p.vx * 0.04f, p.y - p.vy * 0.04f),
                    strokeWidth = 1.8f * u,
                    alpha = f,
                    blendMode = BlendMode.Plus
                )
            }
            else -> Unit
        }
    }

    for (p in engine.particles) {
        if (p.kind != ParticleKind.SHARD) continue
        val f = (p.life / p.maxLife).coerceIn(0f, 1f)
        val a = min(1f, f * 2f)
        translate(p.x, p.y) {
            rotateRad(p.rot, Offset.Zero) {
                val shardPath = Path().apply {
                    moveTo(-p.radius, -p.radius * 0.4f)
                    lineTo(p.radius * 1.1f, 0f)
                    lineTo(-p.radius * 0.3f, p.radius * 0.7f)
                    close()
                }
                drawPath(shardPath, p.color, alpha = a)
                drawPath(
                    shardPath,
                    color = Color(0xFFFF8C28).copy(alpha = f * 0.8f),
                    style = Stroke(width = 1.2f * u)
                )
            }
        }
    }
}

private fun DrawScope.drawTailStructure(w: Float, h: Float, u: Float, cx: Float) {
    val m = min(w, h) * 0.05f
    for (s in listOf(-1f, 1f)) {
        val x0 = if (s < 0f) m else w - m
        val xE = cx + s * w * 0.27f
        val stabPath = Path().apply {
            moveTo(x0, h * 0.73f)
            lineTo(xE, h * 0.87f)
            lineTo(xE - s * w * 0.03f, h - m)
            lineTo(x0, h - m)
            close()
        }
        drawPath(
            path = stabPath,
            brush = Brush.verticalGradient(
                0f to Color(0xFFAAB3BD),
                0.5f to Color(0xFF778390),
                1f to Color(0xFF3D4651),
                startY = h * 0.74f,
                endY = h
            )
        )
        drawLine(
            color = Color(0x8CF0F6FC),
            start = Offset(x0, h * 0.73f),
            end = Offset(xE, h * 0.87f),
            strokeWidth = 2f * u
        )
        for (i in 1 until 5) {
            val t = i / 5f
            drawLine(
                color = Color(0x73141A20),
                start = Offset(lerp(x0, xE, t), lerp(h * 0.73f, h * 0.87f, t) + 4f * u),
                end = Offset(lerp(x0, xE - s * w * 0.03f, t), h - m),
                strokeWidth = 1f * u
            )
        }
        for (i in 0 until 9) {
            val t = (i + 0.5f) / 9f
            drawCircle(
                color = Color(0xB31E242A),
                radius = 1.8f * u,
                center = Offset(lerp(x0, xE, t), lerp(h * 0.73f, h * 0.87f, t) + 9f * u)
            )
        }
    }

    val finPath = Path().apply {
        moveTo(cx - w * 0.034f, m)
        lineTo(cx + w * 0.034f, m)
        lineTo(cx + w * 0.022f, h * 0.11f)
        lineTo(cx - w * 0.022f, h * 0.11f)
        close()
    }
    drawPath(
        path = finPath,
        brush = Brush.horizontalGradient(
            0f to Color(0xFF5C6671),
            0.5f to Color(0xFFAEB8C3),
            1f to Color(0xFF59636E),
            startX = cx - w * 0.03f,
            endX = cx + w * 0.03f
        )
    )
    drawLine(
        color = Color(0x80141A20),
        start = Offset(cx, m),
        end = Offset(cx, h * 0.11f),
        strokeWidth = 1f * u
    )
}

private fun DrawScope.drawCracks(engine: TailGunnerEngine, u: Float) {
    for (c in engine.cracks) {
        translate(c.x, c.y) {
            drawCircle(Color(0xD9080A0C), radius = 3.2f * u, center = Offset.Zero)
            for (pass in 0..1) {
                val color = if (pass == 1) Color(0x99EEF4FA) else Color(0x660A0C0E)
                val sw = (if (pass == 1) 1f else 2f) * u
                for (line in c.lines) {
                    val path = Path().apply {
                        moveTo(line[0].first, line[0].second)
                        for (i in 1 until line.size) {
                            lineTo(line[i].first, line[i].second)
                        }
                    }
                    drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round))
                }
            }
        }
    }
}

private fun DrawScope.drawWindowFrame(w: Float, h: Float, u: Float) {
    val m = min(w, h) * 0.05f
    val r = min(w, h) * 0.12f
    val framePath = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(-60f, -60f, w + 60f, h + 60f))
        addRoundRect(RoundRect(m, m, w - m, h - m, CornerRadius(r, r)))
    }
    drawPath(
        path = framePath,
        brush = Brush.linearGradient(
            0f to Color(0xFF232820),
            0.5f to Color(0xFF151913),
            1f to Color(0xFF1D221B),
            start = Offset.Zero,
            end = Offset(w, h)
        )
    )

    // Corner struts
    val a = r * 1.25f
    val leg = a + r * 0.35f
    val corners = listOf(
        floatArrayOf(m, m, 1f, 1f),
        floatArrayOf(w - m, m, -1f, 1f),
        floatArrayOf(m, h - m, 1f, -1f),
        floatArrayOf(w - m, h - m, -1f, -1f)
    )
    for (c in corners) {
        val cx0 = c[0]
        val cy0 = c[1]
        val sx = c[2]
        val sy = c[3]
        val tri = Path().apply {
            moveTo(cx0, cy0)
            lineTo(cx0 + sx * leg, cy0)
            lineTo(cx0, cy0 + sy * leg)
            close()
        }
        drawPath(tri, Color(0xFF161A14))
        drawLine(
            color = Color(0x47A5AF96),
            start = Offset(cx0 + sx * leg, cy0),
            end = Offset(cx0, cy0 + sy * leg),
            strokeWidth = 1.6f * u
        )
    }

    drawRoundRect(
        color = Color(0xE6080A07),
        topLeft = Offset(m, m),
        size = Size(w - 2 * m, h - 2 * m),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 3f * u)
    )
    drawRoundRect(
        color = Color(0x4DBEC8B4),
        topLeft = Offset(m + 2f * u, m + 2f * u),
        size = Size(w - 2 * m - 4f * u, h - 2 * m - 4f * u),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 1.4f * u)
    )

    // Rivets
    val step = 46f * u
    val rivetR = 2.1f * u
    var x = m * 1.5f
    while (x < w - m) {
        drawCircle(Color(0xFF596055), rivetR, Offset(x, m * 0.5f))
        drawCircle(Color(0xFF596055), rivetR, Offset(x, h - m * 0.5f))
        x += step
    }
    var y = m * 1.5f
    while (y < h - m) {
        drawCircle(Color(0xFF596055), rivetR, Offset(m * 0.5f, y))
        drawCircle(Color(0xFF596055), rivetR, Offset(w - m * 0.5f, y))
        y += step
    }

    // Subtle glass sheen
    drawRoundRect(
        brush = Brush.linearGradient(
            0f to Color(0x12FFFFFF),
            0.35f to Color(0x00FFFFFF),
            start = Offset(w * 0.1f, 0f),
            end = Offset(w * 0.5f, h)
        ),
        topLeft = Offset(m, m),
        size = Size(w - 2 * m, h - 2 * m),
        cornerRadius = CornerRadius(r, r)
    )
}

private fun DrawScope.drawGuns(
    engine: TailGunnerEngine,
    sprites: TurretSpriteCache,
    w: Float,
    h: Float,
    u: Float,
    cx: Float
) {
    val geo = engine.gunGeom()
    val hot = engine.heat / 100f
    val yo = (engine.aimX - cx) * 0.05f

    // Gun yoke base
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF3A413A),
            1f to Color(0xFF12150F),
            startY = h - h * 0.08f,
            endY = h
        ),
        topLeft = Offset(cx - w * 0.22f + yo, h - h * 0.07f),
        size = Size(w * 0.44f, h * 0.10f),
        cornerRadius = CornerRadius(6f * u, 6f * u)
    )
    drawLine(
        color = Color(0x40BEC8B4),
        start = Offset(cx - w * 0.22f + yo + 6f * u, h - h * 0.07f),
        end = Offset(cx + w * 0.22f + yo - 6f * u, h - h * 0.07f),
        strokeWidth = 1.5f * u
    )

    geo.forEachIndexed { idx, g ->
        val dx = g.tx - g.bx
        val dy = g.ty - g.by
        val len = max(1f, hypot(dx, dy))
        val ux = dx / len
        val uy = dy / len
        val nx = -uy
        val ny = ux
        val w0 = w * 0.034f
        val w1 = w * 0.012f
        fun at(t: Float, o: Float): Offset {
            val wt = lerp(w0, w1, t) * o
            return Offset(g.bx + dx * t + nx * wt / 2f, g.by + dy * t + ny * wt / 2f)
        }

        val barrelPath = Path().apply {
            val p0 = at(0f, 1f)
            val p1 = at(1f, 1f)
            val p2 = at(1f, -1f)
            val p3 = at(0f, -1f)
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawPath(
            path = barrelPath,
            brush = Brush.linearGradient(
                0f to Color(0xFF101316),
                0.35f to Color(0xFF58626B),
                0.52f to Color(0xFFA3AEB7),
                0.78f to Color(0xFF39414A),
                1f to Color(0xFF0E1114),
                start = Offset(g.bx - nx * w0 / 2f, g.by - ny * w0 / 2f),
                end = Offset(g.bx + nx * w0 / 2f, g.by + ny * w0 / 2f)
            )
        )
        if (hot > 0.45f) {
            drawPath(barrelPath, Color(0xFFFF6E1E).copy(alpha = (hot - 0.45f) * 0.85f))
        }

        // Cooling jacket perforations
        val angleDeg = (atan2(uy, ux) * 180f / PI).toFloat()
        for (j in 0 until 7) {
            val t = 0.30f + j * 0.085f
            val wt = lerp(w0, w1, t)
            for (o in listOf(-0.22f, 0.22f)) {
                val px = g.bx + dx * t + nx * wt * o
                val py = g.by + dy * t + ny * wt * o
                translate(px, py) {
                    rotateRad(angleDeg * (PI.toFloat() / 180f), Offset.Zero) {
                        drawOval(
                            color = Color(0xFF05070A),
                            topLeft = Offset(-wt * 0.085f, -wt * 0.20f),
                            size = Size(wt * 0.17f, wt * 0.40f)
                        )
                    }
                }
            }
        }

        for (t in listOf(0.22f, 0.90f)) {
            drawLine(
                color = Color(0x8C000000),
                start = at(t, 1f),
                end = at(t, -1f),
                strokeWidth = 2f * u
            )
        }
        drawCircle(Color(0xFF050608), radius = w1 * 0.55f, center = Offset(g.tx, g.ty))
        drawCircle(
            color = Color(0xFF808B94),
            radius = w1 * 0.55f,
            center = Offset(g.tx, g.ty),
            style = Stroke(width = 1.2f * u)
        )

        if (engine.muzzleFlash[idx] > 0f) {
            val r = (70f + Random.nextFloat() * 26f) * u
            val d = max(2, (r * 2f).roundToInt())
            drawImage(
                image = sprites.glow,
                dstOffset = IntOffset((g.tx - r).roundToInt(), (g.ty - r).roundToInt()),
                dstSize = IntSize(d, d),
                alpha = 0.95f,
                blendMode = BlendMode.Plus
            )
            for (k in 0 until 4) {
                val a = Random.nextFloat() * TailGunnerEngine.TAU
                drawLine(
                    color = Color(0xE6FFE6A0),
                    start = Offset(g.tx, g.ty),
                    end = Offset(g.tx + cos(a) * r * 0.9f, g.ty + sin(a) * r * 0.9f),
                    strokeWidth = 2f * u,
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}

private fun DrawScope.drawSight(engine: TailGunnerEngine, u: Float) {
    val hot = engine.heat / 100f
    val wounded = engine.hull < 12f
    val col = when {
        engine.jammed -> Color(0xF2FF5A3C)
        wounded -> Color(0xF2FF8A4C)
        else -> Color(0xFFFFD682).copy(alpha = (0.85f - hot * 0.2f).coerceIn(0.45f, 0.95f))
    }

    // Subtle heartbeat wobble on sight when wounded (<12% hull)
    val jitterX = if (wounded) sin(engine.totalTime * 11f) * 4f * u else 0f
    val jitterY = if (wounded) cos(engine.totalTime * 9f) * 4f * u else 0f

    translate(engine.aimX + jitterX, engine.aimY + jitterY) {
        val outerR = (if (wounded) 56f else 46f) * u
        val dashEffect = if (engine.jammed || wounded) {
            PathEffect.dashPathEffect(floatArrayOf(6f * u, 6f * u), 0f)
        } else null

        drawCircle(
            color = col,
            radius = outerR,
            center = Offset.Zero,
            style = Stroke(width = 2.2f * u, pathEffect = dashEffect)
        )
        drawCircle(
            color = col,
            radius = 19f * u,
            center = Offset.Zero,
            style = Stroke(width = 1.6f * u)
        )
        for (i in 0 until 4) {
            val a = i * TailGunnerEngine.TAU / 4f + (PI / 4.0).toFloat()
            drawLine(
                color = col,
                start = Offset(cos(a) * outerR, sin(a) * outerR),
                end = Offset(cos(a) * (outerR + 14f * u), sin(a) * (outerR + 14f * u)),
                strokeWidth = 2f * u
            )
        }
        for (i in -4..4) {
            if (i == 0) continue
            drawLine(
                color = col,
                start = Offset(i * 5.5f * u, -3f * u),
                end = Offset(i * 5.5f * u, 3f * u),
                strokeWidth = 1.2f * u
            )
        }
        drawCircle(color = col, radius = 2.2f * u, center = Offset.Zero)
    }
}

private fun DrawScope.drawFilmAndVignette(
    engine: TailGunnerEngine,
    w: Float,
    h: Float,
    cx: Float,
    cy: Float
) {
    // Base cockpit vignette
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x99000000)),
            center = Offset(cx, h * 0.5f),
            radius = max(w, h) * 0.72f
        ),
        topLeft = Offset.Zero,
        size = Size(w, h)
    )

    // Impact / Game over flash
    if (engine.flash > 0f) {
        val baseColor = if (engine.mode == GameMode.OVER) Color.White else Color(0xFFC82814)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, baseColor.copy(alpha = (engine.flash * 0.6f).coerceIn(0f, 1f))),
                center = Offset(cx, cy),
                radius = max(w, h) * 0.75f
            ),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )
    }

    // Pulsing red critical vignette when hull < 30% ("viñeta roja que pulsa")
    if (engine.mode == GameMode.PLAY && engine.hull < 30f) {
        val pulseFreq = if (engine.hull < 12f) 6.5f else 4.2f
        val pulse = 0.45f + 0.55f * (0.5f + 0.5f * sin(engine.totalTime * pulseFreq))
        val intensity = if (engine.activeHullAlert != HullAlertType.NONE) 0.72f else 0.48f
        drawRect(
            brush = Brush.radialGradient(
                0.35f to Color.Transparent,
                0.78f to Color(0xFFD82418).copy(alpha = pulse * intensity * 0.55f),
                1f to Color(0xFFE61919).copy(alpha = pulse * intensity),
                center = Offset(cx, h * 0.5f),
                radius = max(w, h) * 0.74f
            ),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
