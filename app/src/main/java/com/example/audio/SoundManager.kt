package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopHomeMusic()
            }
        }
    var isHapticEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var homeMusicJob: Job? = null

    // Rich Pentatonic Chime Scale for Combos (C5, D5, E5, G5, A5, C6, D6, E6, G6, A6, C7...)
    private val pentatonicNotes = doubleArrayOf(
        523.25, 587.33, 659.25, 783.99, 880.00,
        1046.50, 1174.66, 1318.51, 1567.98, 1760.00,
        2093.00, 2349.32
    )

    // Catchy upbeat casual game theme song (BGM for home page)
    private val homeSongNotes = listOf(
        Pair(523.25, 240), // C5
        Pair(659.25, 240), // E5
        Pair(783.99, 240), // G5
        Pair(880.00, 340), // A5
        Pair(783.99, 240), // G5
        Pair(659.25, 340), // E5
        Pair(587.33, 240), // D5
        Pair(523.25, 460), // C5
        Pair(659.25, 240), // E5
        Pair(783.99, 240), // G5
        Pair(880.00, 240), // A5
        Pair(1046.50, 340),// C6
        Pair(987.77, 240), // B5
        Pair(783.99, 240), // G5
        Pair(880.00, 460), // A5
        Pair(698.46, 240), // F5
        Pair(880.00, 240), // A5
        Pair(1046.50, 240),// C6
        Pair(1174.66, 340),// D6
        Pair(1046.50, 240),// C6
        Pair(880.00, 240), // A5
        Pair(783.99, 460), // G5
        Pair(783.99, 240), // G5
        Pair(987.77, 240), // B5
        Pair(1174.66, 240),// D6
        Pair(1318.51, 340),// E6
        Pair(1174.66, 240),// D6
        Pair(987.77, 240), // B5
        Pair(1046.50, 520) // C6
    )

    fun startHomeMusic() {
        if (!isSoundEnabled || homeMusicJob?.isActive == true) return
        homeMusicJob = scope.launch(Dispatchers.IO) {
            while (isActive && isSoundEnabled) {
                try {
                    for ((index, note) in homeSongNotes.withIndex()) {
                        if (!isActive || !isSoundEnabled) break
                        val hasKick = (index % 2 == 0) // Punchy kick bass on every downbeat!
                        generatePunchyBeatChime(
                            frequency = note.first,
                            hasKick = hasKick,
                            durationMs = note.second,
                            volume = 0.28f
                        )
                        delay(note.second.toLong() + 25L)
                    }
                    delay(400)
                } catch (_: Throwable) {
                    delay(1000)
                }
            }
        }
    }

    fun stopHomeMusic() {
        homeMusicJob?.cancel()
        homeMusicJob = null
    }

    private val sundarTapBuffer: ShortArray by lazy {
        val sampleRate = 22050
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            // High-end sweet crystal glass chime: 1250 Hz rising to 1550 Hz with harmonics
            val currentFreq = 1250.0 + 300.0 * (1.0 - progress)
            val overtone = currentFreq * 2.0
            val sub = currentFreq * 0.5
            val env = exp(-9.5 * progress)
            val wave = (
                sin(2.0 * PI * currentFreq * t) * 0.65 +
                sin(2.0 * PI * overtone * t) * 0.25 +
                sin(2.0 * PI * sub * t) * 0.10
            ) * env * 0.40f
            buffer[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buffer
    }

    fun playSundarClick() {
        if (!isSoundEnabled) return
        playRawBuffer(sundarTapBuffer, 22050)
        performHaptic(12)
    }

    fun playButtonClick() {
        playSundarClick()
    }

    fun playPickUp() {
        if (!isSoundEnabled) return
        scope.launch {
            // Crisp bubble pop
            generateFrequencySlide(startFreq = 480.0, endFreq = 650.0, durationMs = 35, volume = 0.35f)
        }
        performHaptic(12)
    }

    fun playDrop() {
        if (!isSoundEnabled) return
        scope.launch {
            // Satisfying resonant wooden block snap (low wood thud + crisp click)
            generateWoodBlockClick(volume = 0.45f)
        }
        performHaptic(20)
    }

    fun playLineClear(lines: Int, combo: Int) {
        if (isSoundEnabled) {
            scope.launch {
                val noteIdx = (combo.coerceAtLeast(1) - 1).coerceAtMost(pentatonicNotes.size - 3)
                val f1 = pentatonicNotes[noteIdx]
                val f2 = pentatonicNotes[noteIdx + 1]
                val f3 = pentatonicNotes[noteIdx + 2]
                // Crystal bell arpeggio flourish with warm harmonic overtones
                generateLushChime(frequency = f1, durationMs = 240, volume = 0.65f)
                delay(40)
                generateLushChime(frequency = f2, durationMs = 240, volume = 0.70f)
                delay(40)
                generateLushChime(frequency = f3, durationMs = 320, volume = 0.75f)
            }
        }
        val intensity = (30 + lines * 25 + combo * 12).coerceAtMost(255)
        performHaptic(intensity.toLong())
    }

    fun playLevelWin() {
        if (!isSoundEnabled) return
        scope.launch {
            // Triumphant orchestral chime fanfare: C5, E5, G5, high C6 + sparkle
            val fanfare = listOf(523.25, 659.25, 783.99, 1046.50)
            for (freq in fanfare) {
                generateLushChime(freq, 180, 0.65f)
                delay(90)
            }
            delay(50)
            generateLushChime(1318.51, 350, 0.75f)
        }
        performHaptic(75)
    }

    fun playGameOver() {
        if (!isSoundEnabled) return
        scope.launch {
            generateTone(330.0, 160, 0.35f)
            delay(110)
            generateTone(246.94, 250, 0.4f)
        }
        performHaptic(50)
    }

    fun playBombExplosion() {
        if (!isSoundEnabled) return
        scope.launch {
            generateExplosion(durationMs = 260, volume = 0.8f)
        }
        performHaptic(90)
    }

    fun playLineLaser() {
        if (!isSoundEnabled) return
        scope.launch {
            generateFrequencySlide(startFreq = 880.0, endFreq = 1400.0, durationMs = 120, volume = 0.6f)
        }
        performHaptic(40)
    }

    fun playCrossBlast() {
        if (!isSoundEnabled) return
        scope.launch {
            generateLushChime(587.33, 160, 0.6f)
            generateLushChime(880.00, 220, 0.65f)
        }
        performHaptic(60)
    }

    fun playUndoSound() {
        if (!isSoundEnabled) return
        scope.launch {
            generateFrequencySlide(startFreq = 580.0, endFreq = 420.0, durationMs = 70, volume = 0.4f)
        }
        performHaptic(20)
    }

    fun playReviveSound() {
        if (!isSoundEnabled) return
        scope.launch {
            val arpeggio = listOf(440.0, 554.37, 659.25, 880.0, 1108.73)
            for (f in arpeggio) {
                generateLushChime(f, 140, 0.6f)
                delay(60)
            }
        }
        performHaptic(60)
    }

    private fun performHaptic(durationMs: Long) {
        if (!isHapticEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    /**
     * Compound synthesizer producing warm chime melody layered with a punchy kick bass pulse!
     */
    private fun generatePunchyBeatChime(frequency: Double, hasKick: Boolean, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Chime melody wave
            val chimeEnv = exp(-3.2 * progress)
            val melodyWave = (sin(2.0 * PI * frequency * t) * 0.65 +
                    sin(2.0 * PI * frequency * 2.0 * t) * 0.25 +
                    sin(2.0 * PI * frequency * 3.0 * t) * 0.10) * chimeEnv
            // Punchy bass kick on beat
            val kickWave = if (hasKick) {
                val kickEnv = exp(-9.5 * progress)
                val kickFreq = 65.0 + 35.0 * (1.0 - progress)
                sin(2.0 * PI * kickFreq * t) * kickEnv * 0.90
            } else 0.0

            val combined = (melodyWave * 0.70 + kickWave * 0.30) * volume
            buffer[i] = (combined * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playRawBuffer(buffer, sampleRate)
    }

    /**
     * Lush chime with warm acoustic harmonics (fundamental + 2nd + 3rd harmonic) and exponential decay.
     */
    private fun generateLushChime(frequency: Double, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Natural bell/chime exponential decay envelope
            val env = exp(-3.8 * (i.toDouble() / numSamples))
            val wave = (sin(2.0 * PI * frequency * t) * 0.70 +
                    sin(2.0 * PI * frequency * 2.0 * t) * 0.20 +
                    sin(2.0 * PI * frequency * 3.0 * t) * 0.10) * env * volume
            buffer[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playRawBuffer(buffer, sampleRate)
    }

    private fun generateWoodBlockClick(volume: Float) {
        val sampleRate = 22050
        val durationMs = 50
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-12.0 * (i.toDouble() / numSamples))
            val wave = (sin(2.0 * PI * 240.0 * t) * 0.65 + sin(2.0 * PI * 720.0 * t) * 0.35) * env * volume
            buffer[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playRawBuffer(buffer, sampleRate)
    }

    private fun generateFrequencySlide(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val t = i.toDouble() / sampleRate
            val env = 1.0 - progress
            val wave = sin(2.0 * PI * currentFreq * t) * env * volume
            buffer[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playRawBuffer(buffer, sampleRate)
    }

    private fun generateExplosion(durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val random = java.util.Random()
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val env = exp(-4.5 * progress)
            // Low rumble + filtered noise
            val lowSine = sin(2.0 * PI * 75.0 * (i.toDouble() / sampleRate))
            val noise = (random.nextFloat() * 2f - 1f) * 0.5f
            val sample = (lowSine * 0.6 + noise * 0.4) * env * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playRawBuffer(buffer, sampleRate)
    }

    private fun generateTone(frequency: Double, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val envelope = 1.0 - (i.toDouble() / numSamples)
            val sample = sin(2.0 * PI * frequency * time) * envelope * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playRawBuffer(buffer, sampleRate)
    }

    private fun playRawBuffer(buffer: ShortArray, sampleRate: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val audioTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay((buffer.size * 1000L / sampleRate) + 30)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Throwable) {}
            } catch (_: Throwable) {}
        }
    }
}
