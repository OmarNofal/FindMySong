package com.omar.findmysong.visualizer

import org.jtransforms.fft.FloatFFT_1D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sqrt


class BeatDetector(
    val sampleRate: Int,
    val chunkSize: Int = 2048,  // FFT window size
    val hopSize: Int = 1024,    // 50% overlap
    val minFreq: Float = 20f,
    val maxFreq: Float = 300f,
    val historySize: Int = 60   // ~2 seconds of history at 1024 hop (1024*43 / 44100 ~ 1s)
) {

    private val fft = FloatFFT_1D(chunkSize.toLong())
    private val buffer = FloatArray(chunkSize) { 0f }  // sliding window buffer
    private var bufferFill = 0

    // Circular buffer for energy history
    private val energyHistory = FloatArray(historySize) { 0f }
    private var energyIndex = 0

    // Frequency bin width
    private val binWidth = sampleRate.toFloat() / chunkSize

    private val startBin = (minFreq / binWidth).toInt()
    private val endBin = (maxFreq / binWidth).toInt()

    /**
     * Feed incoming samples, call this repeatedly with chunks of samples (can be any size)
     * Returns true if a beat is detected in this update
     */
    fun processSamples(inputSamples: FloatArray): Boolean {
        var beatDetected = false
        var inputIndex = 0

        while (inputIndex < inputSamples.size) {
            val remaining = chunkSize - bufferFill
            val toCopy = minOf(remaining, inputSamples.size - inputIndex)

            // Copy samples into the sliding window buffer
            System.arraycopy(inputSamples, inputIndex, buffer, bufferFill, toCopy)
            bufferFill += toCopy
            inputIndex += toCopy

            if (bufferFill == chunkSize) {
                // Process the buffer for beat detection
                if (detectBeat(buffer)) {
                    beatDetected = true
                }
                // Slide window by hopSize samples (50% overlap)
                System.arraycopy(buffer, hopSize, buffer, 0, chunkSize - hopSize)
                bufferFill = chunkSize - hopSize
            }
        }

        return beatDetected
    }

    private fun detectBeat(window: FloatArray): Boolean {
        // Copy window to avoid modifying original buffer during FFT
        val fftInput = window.copyOf()

        fft.realForward(fftInput)

        // Sum magnitudes in frequency band
        var magnitudeSum = 0f
        for (k in startBin..endBin) {
            val real = fftInput[k * 2]
            val imag = if (k == 0 || k == chunkSize / 2) 0f else fftInput[k * 2 + 1]
            magnitudeSum += sqrt(real * real + imag * imag)
        }

        // Update energy history
        energyHistory[energyIndex] = magnitudeSum
        energyIndex = (energyIndex + 1) % historySize

        // Compute moving average energy
        val avgEnergy = energyHistory.average().toFloat()

        // Beat if current energy is significantly above average (adjust sensitivity here)
        val sensitivity = 1.3f
        return magnitudeSum > avgEnergy * sensitivity && magnitudeSum > 10f
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


