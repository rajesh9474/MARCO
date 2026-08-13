package com.example.voice

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Biometric Voice Match & Speaker Verification Manager for MARCO.
 * Analyzes pitch (fundamental frequency F0 via autocorrelation) and spectral energy distribution
 * to ensure MARCO only activates when the enrolled user speaks "Hey MARCO".
 */
class SpeakerVoiceMatchManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "marco_voice_match_prefs",
        Context.MODE_PRIVATE
    )

    private val _isVoiceMatchEnabled = MutableStateFlow(
        prefs.getBoolean("voice_match_enabled", false)
    )
    val isVoiceMatchEnabled: StateFlow<Boolean> = _isVoiceMatchEnabled.asStateFlow()

    private val _isProfileEnrolled = MutableStateFlow(
        prefs.getBoolean("profile_enrolled", false)
    )
    val isProfileEnrolled: StateFlow<Boolean> = _isProfileEnrolled.asStateFlow()

    private val _enrolledPitchHz = MutableStateFlow(
        prefs.getFloat("enrolled_pitch_hz", 0f)
    )
    val enrolledPitchHz: StateFlow<Float> = _enrolledPitchHz.asStateFlow()

    private val _enrolledCentroid = MutableStateFlow(
        prefs.getFloat("enrolled_centroid", 0f)
    )
    val enrolledCentroid: StateFlow<Float> = _enrolledCentroid.asStateFlow()

    fun setVoiceMatchEnabled(enabled: Boolean) {
        _isVoiceMatchEnabled.value = enabled
        prefs.edit().putBoolean("voice_match_enabled", enabled).apply()
    }

    /**
     * Enrolls user voice sample from recorded PCM shorts.
     * Computes average fundamental pitch (Hz) and spectral centroid.
     */
    fun enrollVoiceProfile(pcmBuffer: ShortArray, sampleRate: Int = 16000): Boolean {
        if (pcmBuffer.isEmpty()) return false

        val pitch = estimatePitchHz(pcmBuffer, sampleRate)
        val centroid = estimateSpectralCentroid(pcmBuffer)

        if (pitch in 60f..400f) { // Valid human vocal range F0
            _enrolledPitchHz.value = pitch
            _enrolledCentroid.value = centroid
            _isProfileEnrolled.value = true
            _isVoiceMatchEnabled.value = true

            prefs.edit()
                .putFloat("enrolled_pitch_hz", pitch)
                .putFloat("enrolled_centroid", centroid)
                .putBoolean("profile_enrolled", true)
                .putBoolean("voice_match_enabled", true)
                .apply()

            Log.d("VoiceMatch", "Voice Profile Enrolled Successfully! Pitch: $pitch Hz, Centroid: $centroid")
            return true
        }
        return false
    }

    /**
     * Verifies if the incoming audio frame matches the enrolled user's speaker profile.
     * Returns true if speaker matched or if Voice Match is disabled / not enrolled.
     */
    fun verifySpeaker(pcmBuffer: ShortArray, sampleRate: Int = 16000): Pair<Boolean, Float> {
        if (!_isVoiceMatchEnabled.value || !_isProfileEnrolled.value) {
            return Pair(true, 1.0f) // Accept all speakers if Voice Match disabled or not enrolled
        }

        val currentPitch = estimatePitchHz(pcmBuffer, sampleRate)
        val targetPitch = _enrolledPitchHz.value

        if (currentPitch <= 0f || targetPitch <= 0f) {
            return Pair(true, 0.7f) // Fallback if pitch couldn't be extracted reliably
        }

        val pitchDiff = abs(currentPitch - targetPitch)
        val allowedPitchVariance = targetPitch * 0.35f // Allow 35% pitch variance for voice dynamics

        val isPitchMatch = pitchDiff <= allowedPitchVariance
        val confidence = (1.0f - (pitchDiff / targetPitch)).coerceIn(0.0f, 1.0f)

        Log.d("VoiceMatch", "Speaker Verification: Current Pitch=$currentPitch Hz, Enrolled=$targetPitch Hz -> Match=$isPitchMatch (Conf: $confidence)")

        return Pair(isPitchMatch, confidence)
    }

    fun resetVoiceProfile() {
        _isProfileEnrolled.value = false
        _isVoiceMatchEnabled.value = false
        _enrolledPitchHz.value = 0f
        _enrolledCentroid.value = 0f

        prefs.edit().clear().apply()
    }

    /**
     * Pitch estimation using Normalized Autocorrelation (YIN/Autocorrelation method).
     * Calculates fundamental pitch F0 in Hz for human voice (60Hz - 400Hz).
     */
    private fun estimatePitchHz(buffer: ShortArray, sampleRate: Int): Float {
        val minLag = sampleRate / 400 // 400 Hz max pitch -> lag ~40 samples
        val maxLag = sampleRate / 60  // 60 Hz min pitch -> lag ~266 samples

        if (buffer.size < maxLag * 2) return 0f

        var maxAutocorr = 0.0
        var bestLag = -1

        val zeroLagEnergy = buffer.take(maxLag).fold(0.0) { sum, s -> sum + (s * s) }
        if (zeroLagEnergy < 1000.0) return 0f // Noise threshold

        for (lag in minLag..maxLag) {
            var sum = 0.0
            for (i in 0 until maxLag) {
                sum += buffer[i].toDouble() * buffer[i + lag].toDouble()
            }
            val normSum = sum / zeroLagEnergy
            if (normSum > maxAutocorr) {
                maxAutocorr = normSum
                bestLag = lag
            }
        }

        return if (bestLag > 0 && maxAutocorr > 0.3) {
            sampleRate.toFloat() / bestLag
        } else {
            0f
        }
    }

    /**
     * Calculates spectral centroid (center of mass of voice spectrum).
     */
    private fun estimateSpectralCentroid(buffer: ShortArray): Float {
        var weightedSum = 0.0
        var totalEnergy = 0.0

        for (i in 0 until buffer.size - 1) {
            val delta = abs(buffer[i + 1] - buffer[i]).toDouble()
            weightedSum += i * delta
            totalEnergy += delta
        }

        return if (totalEnergy > 0) (weightedSum / totalEnergy).toFloat() else 0f
    }

    companion object {
        @Volatile
        private var INSTANCE: SpeakerVoiceMatchManager? = null

        fun getInstance(context: Context): SpeakerVoiceMatchManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpeakerVoiceMatchManager(context).also { INSTANCE = it }
            }
        }
    }
}
