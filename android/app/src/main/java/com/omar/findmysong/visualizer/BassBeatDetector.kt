package com.omar.findmysong.visualizer

import org.jtransforms.fft.FloatFFT_1D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


/**
 * Detects beats in a specific frequency band
 */
class FrequencyBandBeatDetector(
    private val sampleRate: Int,
    private val minFreq: Int,
    private val maxFreq: Int,
    private val cooldownTime: Long,
    private val magnitudeThreshold: Float = 1.2f,
    private val smoothing: Float = 0.4f
) {
    private var lastBeatTime = 0L
    private var runningAverage = 10f

    fun processFrame(fftData: FloatArray, time: Long): Boolean {
        val numSamples = fftData.size
        val binWidth = sampleRate / (numSamples.toFloat())

        val minBin = ceil(minFreq / binWidth).toInt()
        val maxBin = floor(maxFreq / binWidth).toInt()

        var totalMagnitude = 0f
        for (k in minBin..maxBin) {
            val real = fftData[2 * k]
            val imag = fftData[2 * k + 1]
            val binMagnitude = real * real + imag * imag
            totalMagnitude += binMagnitude
        }

        runningAverage = (1 - smoothing) * runningAverage + smoothing * totalMagnitude

        val canBeat = (time - lastBeatTime) > cooldownTime
        return if (totalMagnitude > runningAverage * magnitudeThreshold && canBeat) {
            lastBeatTime = time
            true
        } else {
            false
        }
    }
}

class BeatDetector(
    private val sampleRate: Int,
    private val cooldownTime: Long
) {

    private val bassBeatDetector = FrequencyBandBeatDetector(
        sampleRate,
        20,
        300,
        cooldownTime,
        magnitudeThreshold = 1.2f,
        smoothing = 0.3f
    )
    private val snareBeatDetector = FrequencyBandBeatDetector(
        sampleRate,
        500,
        2500,
        cooldownTime * 2,
        magnitudeThreshold = 2.0f,
        smoothing = 0.7f
    )
    private val kickBeatDetector = FrequencyBandBeatDetector(
        sampleRate,
        50,
        150,
        cooldownTime,
        magnitudeThreshold = 1.5f,
        smoothing = 0.5f
    )
    private val tempoTracker = TempoTracker()

    fun processFrame(chunk: FloatArray, time: Long): BeatWithData {
        val numSamples = chunk.size.toLong()

        val fftInput = chunk.copyOf()

        val fft = FloatFFT_1D(numSamples)
        fft.realForward(fftInput)

        val bass = bassBeatDetector.processFrame(fftInput, time)
        val snare = snareBeatDetector.processFrame(fftInput, time)
        val kick = kickBeatDetector.processFrame(fftInput, time)

        if (bass or kick) tempoTracker.addBeat(time)

        return BeatWithData(
            isSnare = snare,
            isBass = bass,
            isKick = kick,
            tempo = tempoTracker.getTempoBPM().roundToInt()
        )
    }

    companion object {
        fun byteToFloatArray(byteArray: ByteArray): FloatArray {
            val floatCount = byteArray.size / 4
            val floatArray = FloatArray(floatCount)
            val byteBuffer = ByteBuffer.wrap(byteArray)
                .order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until floatCount) {
                floatArray[i] = byteBuffer.float
            }
            return floatArray
        }
    }
}

data class BeatWithData(
    val isSnare: Boolean = false,
    val isBass: Boolean = false,
    val isKick: Boolean = false,
    val tempo: Int = 0
)

class TempoTracker {
    private val beatTimestamps = mutableListOf<Long>()
    private val maxHistory = 20

    fun addBeat(time: Long) {
        beatTimestamps.add(time)
        if (beatTimestamps.size > maxHistory) {
            beatTimestamps.removeAt(0)
        }
    }

    fun getTempoBPM(): Float {
        if (beatTimestamps.size < 2) return 120f
        val intervals = beatTimestamps.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        return if (avgInterval > 0) 60000f / avgInterval.toFloat() else 0f
    }
}