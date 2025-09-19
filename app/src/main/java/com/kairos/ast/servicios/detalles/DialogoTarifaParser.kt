package com.kairos.ast.servicios.detalles

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

// Modelo de datos para el resultado del parseo del diálogo
data class ResultadoDialogoTarifa(
    val valorTarifa: String?,
    val campoEditable: AccessibilityNodeInfo?,
    val btnOfertar: AccessibilityNodeInfo?,
    val btnCerrar: AccessibilityNodeInfo?
)

object DialogoTarifaParser {

    /**
     * Parsea el diálogo de tarifa y extrae el valor actual, el campo editable y los botones.
     */
    fun parsear(nodoRaiz: AccessibilityNodeInfo): ResultadoDialogoTarifa {
        var valorTarifa: String? = null
        var campoEditable: AccessibilityNodeInfo? = null
        var btnOferta: AccessibilityNodeInfo? = null
        var btnCerrar: AccessibilityNodeInfo? = null

        val cola = ArrayDeque<AccessibilityNodeInfo>().apply { add(nodoRaiz) }
        while (cola.isNotEmpty()) {
            val nodo = cola.removeFirst()
            val clase = nodo.className?.toString() ?: ""
            val texto = nodo.text?.toString() ?: ""
            val desc = nodo.contentDescription?.toString() ?: ""

            // Campo editable
            if (clase == "android.widget.EditText") {
                campoEditable = nodo
                valorTarifa = texto
            }
            // Botón Oferta
            if (clase == "android.widget.Button" && texto.equals("Oferta", ignoreCase = true)) {
                btnOferta = nodo
            }
            // Botón Cerrar
            if (clase == "android.widget.Button" && desc.equals("Cerrar", ignoreCase = true)) {
                btnCerrar = nodo
            }

            // Seguir recorriendo hijos si aún no hemos encontrado todo
            if (campoEditable == null || btnOferta == null || btnCerrar == null) {
                for (i in 0 until nodo.childCount) {
                    nodo.getChild(i)?.let { cola.addLast(it) }
                }
            }
        }
        return ResultadoDialogoTarifa(valorTarifa, campoEditable, btnOferta, btnCerrar)
    }
}