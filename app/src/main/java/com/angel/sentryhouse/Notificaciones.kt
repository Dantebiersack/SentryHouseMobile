package com.angel.sentryhouse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.angel.sentryhouse.R

fun Context.enviarNotificacionAlerta(mensaje: String) {
    val canalId = "canal_gas"
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Crear canal si es necesario
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val canal = NotificationChannel(
            canalId,
            "Alertas de gas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones por posibles fugas de gas"
        }
        manager.createNotificationChannel(canal)
    }

    val notificacion = NotificationCompat.Builder(this, canalId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // O usa un ícono personalizado
        .setContentTitle("⚠️ Alerta de gas")
        .setContentText(mensaje)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(1001, notificacion)
}