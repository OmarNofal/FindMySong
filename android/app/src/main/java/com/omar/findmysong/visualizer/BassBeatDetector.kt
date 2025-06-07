package com.omar.findmysong.visualizer

import org.jtransforms.fft.FloatFFT_1D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor


/**
 * This class detects bass and drum beats
 */
class BassBeatDetector(
    val sampleRate: Int,
    val cooldownTime: Long
) {

    private var lastBeatTime = 0L
    private val magnitudeThreshold = 1.1f
    private val bassMinFreq = 20
    private val bassMaxFreq = 300
    private val smoothing = 0.6f
    private var runningAverage = 0f

    fun processFrame(chunk: FloatArray, time: Long): Boolean {

        val numSamples = chunk.size
        val fft = FloatFFT_1D(numSamples.toLong())
        fft.realForward(chunk)

        val binWidth = sampleRate / chunk.size.toFloat()

        val minBin = ceil(bassMinFreq / binWidth).toInt()
        val maxBin = floor(bassMaxFreq / binWidth).toInt()

        val fftOut = chunk

        var totalBassMagnitude = 0f
        for (k in minBin..maxBin) {

            val real = fftOut[2 * k]
            val imag = fftOut[2 * k + 1]

            val binMagnitude = real * real + imag * imag
            totalBassMagnitude += binMagnitude
        }

        runningAverage = (1 - smoothing) * runningAverage + smoothing * totalBassMagnitude

        val canBeat = (time - lastBeatTime) > cooldownTime
        if (totalBassMagnitude > runningAverage * magnitudeThreshold && canBeat) {
            lastBeatTime = time
            return true
        }

        return false
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
