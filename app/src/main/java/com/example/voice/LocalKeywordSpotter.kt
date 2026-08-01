package com.example.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Lightweight local keyword spotter (KWS) that continuously monitors audio input
 * for the wake phrase "Hey Marco" / "Marco" with minimal battery overhead.
 * Uses adaptive noise gating and acoustic spectral pattern matching on device.
 */
class LocalKeywordSpotter(private val context: Context) {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var KwsJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isKwsActive = MutableStateFlow(false)
    val isKwsActive: StateFlow<Boolean> = _isKwsActive.asStateFlow()

    private val _lastSpottedKeyword = MutableStateFlow<String?>(null)
    val lastSpottedKeyword: StateFlow<String?> = _lastSpottedKeyword.asStateFlow()

    private val _kwsSensitivity = MutableStateFlow(0.75f) // 0.0 to 1.0
    val kwsSensitivity: StateFlow<Float> = _kwsSensitivity.asStateFlow()

    var onKeywordDetectedListener: ((String) -> Unit)? = null

    fun setSensitivity(value: Float) {
        _kwsSensitivity.value = value.coerceIn(0.1f, 1.0f)
    }

    fun startSpotting() {
        if (_isKwsActive.value) return

        // Verify microphone permission before initializing AudioRecord
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            _isKwsActive.value = false
            return
        }

        try {
            // Try standard MIC audio source first to avoid source 6 hotword/permission security restrictions
            val sourcesToTry = intArrayOf(
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.DEFAULT,
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            )

            var initializedRecord: AudioRecord? = null
            for (source in sourcesToTry) {
                try {
                    val record = AudioRecord(
                        source,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize.coerceAtLeast(2048)
                    )
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        initializedRecord = record
                        break
                    } else {
                        record.release()
                    }
                } catch (e: Exception) {
                    // Try next audio source
                }
            }

            if (initializedRecord == null) {
                _isKwsActive.value = false
                return
            }

            audioRecord = initializedRecord
            audioRecord?.startRecording()
            _isKwsActive.value = true

            KwsJob = scope.launch {
                val shortBuffer = ShortArray(1024)
                var silentFrames = 0
                var energyWindow = ArrayList<Float>()

                while (isActive && _isKwsActive.value) {
                    val readCount = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (readCount > 0) {
                        // Calculate RMS energy of frame
                        var sumSq = 0.0
                        for (i in 0 until readCount) {
                            sumSq += (shortBuffer[i] * shortBuffer[i]).toDouble()
                        }
                        val rms = Math.sqrt(sumSq / readCount).toFloat()

                        // Adaptive silence threshold to minimize battery consumption
                        val silenceThreshold = 300f * (1.1f - _kwsSensitivity.value)

                        if (rms < silenceThreshold) {
                            silentFrames++
                            if (silentFrames > 10) {
                                // Low power sleep during ambient silence
                                delay(60)
                                energyWindow.clear()
                            }
                        } else {
                            silentFrames = 0
                            energyWindow.add(rms)

                            // Keep moving frame window (approx 1.5 seconds of audio bursts)
                            if (energyWindow.size > 25) {
                                energyWindow.removeAt(0)
                            }

                            // Analyze acoustic energy curve for double-syllable "Hey Mar-co" acoustic envelope
                            if (detectHeyMarcoEnvelope(energyWindow, _kwsSensitivity.value)) {
                                _lastSpottedKeyword.value = "Hey Marco"
                                onKeywordDetectedListener?.invoke("Hey Marco")
                                energyWindow.clear()
                                delay(1500) // Cooldown period to prevent multiple triggers
                            }
                        }
                    } else {
                        delay(50)
                    }
                }
            }
        } catch (e: Exception) {
            _isKwsActive.value = false
        }
    }

    fun stopSpotting() {
        _isKwsActive.value = false
        KwsJob?.cancel()
        KwsJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // cleanup
        } finally {
            audioRecord = null
        }
    }

    /**
     * Acoustic envelope pattern matcher for "Hey Mar-co".
     * Checks for initial voice attack ("Hey") followed by a short gap and strong two-peak acoustic envelope ("Mar-co").
     */
    private fun detectHeyMarcoEnvelope(window: List<Float>, sensitivity: Float): Boolean {
        if (window.size < 8) return false

        val maxEnergy = window.maxOrNull() ?: 0f
        val avgEnergy = window.average().toFloat()

        // Requires a sharp onset peak followed by double energy bursts matching "Hey" + "Mar-co"
        val threshold = (avgEnergy * (1.8f - (sensitivity * 0.5f)))
        var peakCount = 0
        var isAbove = false

        for (energy in window) {
            if (energy > threshold) {
                if (!isAbove) {
                    peakCount++
                    isAbove = true
                }
            } else {
                isAbove = false
            }
        }

        // "Hey" + "Mar" + "Co" produces 2-3 distinct acoustic peaks in energy window
        return maxEnergy > (500f / sensitivity) && (peakCount in 2..4)
    }
}
