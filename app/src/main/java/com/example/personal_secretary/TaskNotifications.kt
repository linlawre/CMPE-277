/**
 * Sets up the worker necessary for creating notifications
 */

package com.example.personal_secretary

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Sets up the Worker to be ready to create notifications
 */
class TaskNotifications(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskDescription = inputData.getString("task_description") ?: return Result.failure()

        val notification = NotificationCompat.Builder(applicationContext, "tasks_channel")
            .setContentTitle("Personal Secretary")
            .setContentText(taskDescription)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)

        return Result.success()
    }
}
