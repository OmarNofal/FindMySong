package com.omar.findmysong.visualizer

import org.jtransforms.fft.FloatFFT_1D

class SpectralFluxDetector(val sampleRate: Int, val chunkSize: Int) {
    private var prevMagnitudes = FloatArray(chunkSize / 2) { 0f }
    private val history = ArrayDeque<Float>()
    private val thresholdMultiplier = 1.3f

    fun processFrame(chunk: FloatArray): Boolean {
        val fft = FloatFFT_1D(chunk.size.toLong())
        fft.realForward(chunk)

        val currMagnitudes = FloatArray(chunk.size / 2)
        for (k in 1 until currMagnitudes.size) {
            val real = chunk[2 * k]
            val imag = chunk[2 * k + 1]
            val magnitude = real * real + imag * imag
            currMagnitudes[k] = magnitude
        }

        // Spectral flux calculation
        var flux = 0f
        for (k in currMagnitudes.indices) {
            val diff = currMagnitudes[k] - prevMagnitudes[k]
            if (diff > 0) flux += diff
        }

        prevMagnitudes = currMagnitudes

        history.add(flux)
        if (history.size > 30) history.removeFirst()

        val average = history.average().toFloat()
        return flux > average * thresholdMultiplier
    }
}
