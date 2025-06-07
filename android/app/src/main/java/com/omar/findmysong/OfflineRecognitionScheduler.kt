package com.omar.findmysong

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.omar.findmysong.worker.OfflineRecognizerWorker
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class OfflineRecognitionScheduler(private val dir: File, private val workManager: WorkManager) {

    private val buffer = ByteArrayOutputStream()

    fun appendBuffer(array: ByteArray) {
        buffer.write(array)
    }

    fun cancel() {
        buffer.reset()
    }

    fun schedule() {
        val tempFile = File(dir, "temp")
        val fOs = FileOutputStream(tempFile, false).buffered()
        fOs.use { it ->
            it.write(buffer.toByteArray())
        }

        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OfflineRecognizerWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueue(workRequest)
        Timber.tag("Worker").d("Worker enqueued")

        buffer.reset()
    }

}