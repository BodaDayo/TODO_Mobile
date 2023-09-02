package com.rgbstudios.todomobile.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.rgbstudios.todomobile.data.repository.TodoRepository

class SyncWorkerFactory(private val repository: TodoRepository) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SyncWorker::class.java.name -> SyncWorker(appContext, workerParameters, repository)
            else -> null
        }
    }
}
