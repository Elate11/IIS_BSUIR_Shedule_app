package com.example.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NoteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra("SUBJECT") ?: "Событие"
        val text = intent.getStringExtra("TEXT") ?: "Напоминание о событии"
        val isEvent = intent.getBooleanExtra("IS_EVENT", true)
        
        val channelId = "note_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания о событиях и заметках",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления и напоминания о запланированных событиях"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val title = if (isEvent) "Событие: $subject" else "Заметка: $subject"
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
