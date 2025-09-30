package com.kairos.ast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Dispositivo arrancado. Iniciando servicio si es necesario...")
            // Aquí puedes añadir la lógica para iniciar tu servicio principal
            // Por ejemplo, podrías iniciar el ServicioAccesibilidad o un ForegroundService
        }
    }
}