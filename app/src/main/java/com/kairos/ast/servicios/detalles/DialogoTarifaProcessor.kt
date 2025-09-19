package com.kairos.ast.servicios.detalles

import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object DialogoTarifaProcessor {
    private const val TAG_LOG = "DialogoTarifaProcessor"

    fun ofertarNuevaTarifa(nodoRaiz: AccessibilityNodeInfo, tarifa: String): Boolean {
        var editText: AccessibilityNodeInfo? = null
        var botonOfertar: AccessibilityNodeInfo? = null
        var exito = false

        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }
        while (cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            val clase = nodo.className?.toString()
            val texto = nodo.text?.toString()

            when {
                clase == "android.widget.EditText" -> editText = nodo
                clase == "android.widget.Button" && texto?.equals("Oferta", true) == true -> botonOfertar = nodo
            }

            if (editText != null && botonOfertar != null) break

            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { cola.addLast(it) }
            }
        }

        // 1. Escribir la tarifa en el EditText
        if (editText != null) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, tarifa)
            exito = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Log.i(TAG_LOG, "Tarifa escrita en EditText: $exito")
        }

        // 2. Pulsar el botón de ofertar
        if (exito && botonOfertar != null) {
            exito = botonOfertar.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.i(TAG_LOG, "Botón Oferta pulsado: $exito")
        }

        // Limpieza
        editText?.recycle()
        botonOfertar?.recycle()

        return exito
    }
}
