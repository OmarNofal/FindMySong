package com.omar.findmysong.visualizer

import org.jtransforms.fft.FloatFFT_1D
import timber.log.Timber
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
    private val magnitudeHistory = ArrayDeque<Float>()
    private val magnitudeThreshold = 1.2f
    private val historySizeChunks = 40
    private val bassMinFreq = 20
    private val bassMaxFreq = 300
    private val smoothing = 0.3f
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

//        magnitudeHistory.add(totalBassMagnitude)
//        if (magnitudeHistory.size > historySizeChunks)
//            magnitudeHistory.removeFirst()

        val canBeat = (time - lastBeatTime) > cooldownTime
        if (totalBassMagnitude > runningAverage * magnitudeThreshold && canBeat) {
            lastBeatTime = time
            Timber.tag("Beat").d("Found Bass Beat")
            return true
        }

        return false
    }


}
