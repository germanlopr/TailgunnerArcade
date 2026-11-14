package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(context: Context) : TextToSpeech.OnInitListener {

    private val sampleRate = 22050
    private val running = AtomicBoolean(false)
    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    var muted: Boolean = false
        private set

    @Volatile
    var engineActive: Boolean = false

    private val voices = CopyOnWriteArrayList<ActiveVoice>()

    private var tts: TextToSpeech? = null
    @Volatile
    private var ttsReady = false

    private val spanishLocale = Locale.forLanguageTag("es-ES")
    private val englishLocale = Locale.US

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {
            tts = null
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.setSpeechRate(1.02f)
            tts?.setPitch(0.94f)
        }
    }

    fun start() {
        if (running.getAndSet(true)) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            running.set(false)
            return
        }

        audioTrack = track
        try {
            track.play()
        } catch (_: Exception) {
            running.set(false)
            return
        }

        audioThread = Thread {
            val chunkSamples = 512
            val pcm = ShortArray(chunkSamples)
            var p1 = 0.0
            var p2 = 0.0
            var p3 = 0.0
            var p4 = 0.0
            var lpEngine = 0.0
            var rngState = 0x12345678L

            while (running.get()) {
                val engineGain = if (!muted && engineActive) 0.14 else 0.0
                for (i in 0 until chunkSamples) {
                    // Fast xorshift noise
                    rngState = rngState xor (rngState shl 13)
                    rngState = rngState xor (rngState ushr 17)
                    rngState = rngState xor (rngState shl 5)
                    val white = ((rngState and 0xFFFF).toDouble() / 32768.0) - 1.0

                    var sample = 0.0
                    if (engineGain > 0.0) {
                        p1 = (p1 + 72.0 / sampleRate) % 1.0
                        p2 = (p2 + 73.4 / sampleRate) % 1.0
                        p3 = (p3 + 109.0 / sampleRate) % 1.0
                        p4 = (p4 + 110.7 / sampleRate) % 1.0
                        val saw1 = p1 * 2.0 - 1.0
                        val saw2 = p2 * 2.0 - 1.0
                        val tri3 = 1.0 - kotlin.math.abs(p3 * 4.0 - 2.0)
                        val tri4 = 1.0 - kotlin.math.abs(p4 * 4.0 - 2.0)
                        val rawEng = (saw1 + saw2) * 0.35 + (tri3 + tri4) * 0.25 + white * 0.14
                        lpEngine += 0.075 * (rawEng - lpEngine)
                        sample += lpEngine * engineGain
                    }

                    if (!muted && voices.isNotEmpty()) {
                        val iter = voices.iterator()
                        while (iter.hasNext()) {
                            val v = iter.next()
                            val s = v.nextSample(sampleRate, white)
                            sample += s
                            if (v.done) {
                                voices.remove(v)
                            }
                        }
                    }

                    val clamped = (sample * 0.72).coerceIn(-0.98, 0.98)
                    pcm[i] = (clamped * 32767.0).toInt().toShort()
                }

                try {
                    track.write(pcm, 0, chunkSamples)
                } catch (_: Exception) {
                    break
                }
            }
        }.apply {
            name = "TailGunnerAudio"
            isDaemon = true
            start()
        }
    }

    fun setMuted(mute: Boolean) {
        muted = mute
        if (mute) {
            voices.clear()
            try {
                tts?.stop()
            } catch (_: Exception) {
            }
        }
    }

    fun toggleMuted(): Boolean {
        setMuted(!muted)
        return muted
    }

    fun pause() {
        engineActive = false
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun release() {
        running.set(false)
        voices.clear()
        try {
            audioThread?.join(250)
        } catch (_: Exception) {
        }
        audioThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }

    private fun addVoice(v: ActiveVoice) {
        if (muted || voices.size > 28) return
        voices.add(v)
    }

    fun playShot() {
        addVoice(ShotVoice())
    }

    fun playBoom(big: Boolean) {
        addVoice(BoomVoice(big))
    }

    fun playHit() {
        addVoice(HitVoice())
    }

    fun playJam() {
        addVoice(JamVoice())
    }

    fun playWhiz() {
        addVoice(WhizVoice())
    }

    fun playSpark() {
        addVoice(SparkVoice())
    }

    fun playFlakBoom() {
        addVoice(BoomVoice(big = false, volume = 0.22))
    }

    fun playRadioStaticAndClick() {
        addVoice(RadioStaticVoice())
    }

    fun playCriticalAlarm() {
        addVoice(CriticalAlarmVoice())
    }

    fun playHeartbeat() {
        addVoice(HeartbeatVoice())
    }

    fun speakRadioBandits() {
        if (muted || !ttsReady) return
        speakWithLocale(
            text = "Bandidos a las seis, nueve, diez y una en punto.",
            locale = spanishLocale,
            queueMode = TextToSpeech.QUEUE_FLUSH,
            utteranceId = "radio_bandits"
        )
    }

    fun speakGetReady() {
        if (muted || !ttsReady) return
        speakWithLocale(
            text = "¡Esté listo!",
            locale = spanishLocale,
            queueMode = TextToSpeech.QUEUE_FLUSH,
            utteranceId = "ready_es"
        )
        speakWithLocale(
            text = "Get ready!",
            locale = englishLocale,
            queueMode = TextToSpeech.QUEUE_ADD,
            utteranceId = "ready_en"
        )
    }

    fun speakFireAtWill() {
        if (muted || !ttsReady) return
        speakWithLocale(
            text = "¡Fuego a discreción!",
            locale = spanishLocale,
            queueMode = TextToSpeech.QUEUE_FLUSH,
            utteranceId = "fire_at_will"
        )
    }

    private fun speakWithLocale(
        text: String,
        locale: Locale,
        queueMode: Int,
        utteranceId: String
    ) {
        val engine = tts ?: return
        try {
            val res = engine.isLanguageAvailable(locale)
            if (res >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
            } else if (locale.language == "es") {
                engine.language = Locale.forLanguageTag("es")
            }
            engine.speak(text, queueMode, null, utteranceId)
        } catch (_: Exception) {
        }
    }

    private abstract class ActiveVoice(val durationSec: Double) {
        var elapsed = 0.0
        var phase = 0.0
        val done: Boolean get() = elapsed >= durationSec

        fun nextSample(sr: Int, white: Double): Double {
            val dt = 1.0 / sr
            val out = sample(dt, white)
            elapsed += dt
            return out
        }

        protected abstract fun sample(dt: Double, white: Double): Double
    }

    private class ShotVoice : ActiveVoice(0.085) {
        private var bp = 0.0
        private var prev = 0.0
        override fun sample(dt: Double, white: Double): Double {
            val t = elapsed / durationSec
            val env = exp(-t * 6.5)
            val freq = 135.0 * exp(-t * 0.8)
            phase = (phase + freq * dt) % 1.0
            val sq = if (phase < 0.5) 1.0 else -1.0
            val diff = white - prev
            prev = white
            bp += 0.35 * (diff - bp)
            return (bp * 0.42 + sq * 0.14) * env
        }
    }

    private class BoomVoice(big: Boolean, private val volume: Double = if (big) 0.75 else 0.55) :
        ActiveVoice(if (big) 1.15 else 0.72) {
        private var lp = 0.0
        override fun sample(dt: Double, white: Double): Double {
            val t = elapsed / durationSec
            val env = exp(-t * 4.2)
            val f = 72.0 * exp(-t * 0.9)
            phase = (phase + f * dt) % 1.0
            val sub = sin(phase * 2.0 * PI)
            val cutoff = 0.04 + 0.18 * (1.0 - t)
            lp += cutoff * (white - lp)
            return (lp * 0.8 + sub * 0.45) * env * volume
        }
    }

    private class HitVoice : ActiveVoice(0.22) {
        private var lp = 0.0
        override fun sample(dt: Double, white: Double): Double {
            val t = elapsed / durationSec
            val env = exp(-t * 5.0)
            val f = 230.0 * exp(-t * 0.85)
            phase = (phase + f * dt) % 1.0
            val tri = 1.0 - kotlin.math.abs(phase * 4.0 - 2.0)
            lp += 0.25 * (white - lp)
            return (lp * 0.55 + tri * 0.3) * env
        }
    }

    private class JamVoice : ActiveVoice(0.32) {
        override fun sample(dt: Double, white: Double): Double {
            val t = elapsed / durationSec
            val env = exp(-t * 3.5)
            val f = 95.0 * exp(-t * 0.7)
            phase = (phase + f * dt) % 1.0
            val saw = phase * 2.0 - 1.0
            return (saw * 0.22 + white * 0.08) * env
        }
    }

    private class WhizVoice : ActiveVoice(0.20) {
        private var bp = 0.0
        override fun sample(dt: Double, white: Double): Double {
            val t = elapsed / durationSec
            val env = sin(t * PI) * 0.16
            val f = 2200.0 * exp(-t * 1.1)
            phase = (phase + f * dt) % 1.0
            bp += 0.22 * (white - bp)
            return (bp * 0.6 + sin(phase * 2.0 * PI) * 0.25) * env
        }
    }

    private class SparkVoice : ActiveVoice(0.06) {
        override fun sample(dt: Double, white: Double): Double {
            val t = elapsed / durationSec
            val f = 1500.0 - 600.0 * t
            phase = (phase + f * dt) % 1.0
            return sin(phase * 2.0 * PI) * exp(-t * 6.0) * 0.08
        }
    }

    private class RadioStaticVoice : ActiveVoice(0.85) {
        private var bp = 0.0
        private var prev = 0.0
        override fun sample(dt: Double, white: Double): Double {
            // Initial PTT click (0..0.03s), crackly radio static (0.03..0.75s), tail squelch click (0.75..0.82s)
            val click1 = if (elapsed < 0.025) {
                val u = elapsed / 0.025
                sin(2.0 * PI * 1850.0 * elapsed) * exp(-u * 5.0) * 0.45
            } else 0.0
            val click2 = if (elapsed in 0.76..0.80) {
                val u = (elapsed - 0.76) / 0.04
                sin(2.0 * PI * 1450.0 * (elapsed - 0.76)) * exp(-u * 6.0) * 0.35
            } else 0.0

            val diff = white - prev
            prev = white
            bp += 0.30 * (diff - bp)
            val crackle = if (Random.nextFloat() < 0.02f) white * 0.5 else 0.0
            val staticEnv = when {
                elapsed < 0.05 -> elapsed / 0.05
                elapsed > 0.70 -> ((0.85 - elapsed) / 0.15).coerceAtLeast(0.0)
                else -> 1.0
            }
            return click1 + click2 + (bp * 0.24 + crackle * 0.18) * staticEnv
        }
    }

    private class CriticalAlarmVoice : ActiveVoice(1.80) {
        override fun sample(dt: Double, white: Double): Double {
            // 3 urgent klaxon blasts
            val cycle = elapsed % 0.60
            if (cycle > 0.36) return 0.0
            val freq = if (cycle < 0.18) 760.0 else 580.0
            phase = (phase + freq * dt) % 1.0
            val horn = (if (phase < 0.5) 1.0 else -1.0) * 0.35 + sin(phase * 2.0 * PI) * 0.65
            val env = sin((cycle / 0.36) * PI)
            return horn * env * 0.28
        }
    }

    private class HeartbeatVoice : ActiveVoice(1.30) {
        override fun sample(dt: Double, white: Double): Double {
            // Two lub-dub cycles
            val local = elapsed % 0.65
            val pulse = when {
                local < 0.14 -> {
                    val u = local / 0.14
                    val f = 58.0 - 22.0 * u
                    sin(2.0 * PI * f * local) * exp(-u * 4.5) * 0.68
                }
                local in 0.17..0.30 -> {
                    val t2 = local - 0.17
                    val u = t2 / 0.13
                    val f = 66.0 - 24.0 * u
                    sin(2.0 * PI * f * t2) * exp(-u * 5.0) * 0.55
                }
                else -> 0.0
            }
            return pulse
        }
    }
}
