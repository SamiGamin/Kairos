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

    private const val TAG = "DialogoTarifaParser"

    /**
     * Parsea el diálogo de tarifa y extrae el valor actual, el campo editable y los botones.
     * Utiliza un enfoque de búsqueda en profundidad (DFS) para recorrer los nodos, optimizando
     * la búsqueda para detenerse tan pronto como se encuentren todos los elementos necesarios.
     * Se ha añadido un sistema de logs para facilitar la depuración y el seguimiento del proceso.
     */
    fun parsear(nodoRaiz: AccessibilityNodeInfo): ResultadoDialogoTarifa {
        Log.d(TAG, "Iniciando parseo del diálogo de tarifa.")
        val resultado = encontrarNodos(nodoRaiz)
        Log.d(TAG, "Parseo finalizado. Tarifa: ${resultado.valorTarifa}, Campo editable: ${resultado.campoEditable != null}, Botón ofertar: ${resultado.btnOfertar != null}, Botón cerrar: ${resultado.btnCerrar != null}")
        return resultado
    }

    private fun encontrarNodos(nodoRaiz: AccessibilityNodeInfo): ResultadoDialogoTarifa {
        var valorTarifa: String? = null
        var campoEditable: AccessibilityNodeInfo? = null
        var btnOfertar: AccessibilityNodeInfo? = null
        var btnCerrar: AccessibilityNodeInfo? = null

        fun buscar(nodo: AccessibilityNodeInfo) {
            val clase = nodo.className?.toString() ?: ""
            val texto = nodo.text?.toString() ?: ""
            val desc = nodo.contentDescription?.toString() ?: ""

            Log.v(TAG, "Visitando nodo: Clase='$clase', Texto='$texto', Desc='$desc'")

            // Campo editable (android.widget.EditText)
            if (campoEditable == null && clase == "android.widget.EditText") {
                campoEditable = nodo
                valorTarifa = texto
                Log.d(TAG, "Campo editable encontrado. Valor: $valorTarifa")
            }

            // Botón "Oferta" (android.widget.Button)
            if (btnOfertar == null && clase == "android.widget.Button" && texto.equals("Oferta", ignoreCase = true)) {
                btnOfertar = nodo
                Log.d(TAG, "Botón 'Oferta' encontrado.")
            }

            // Botón "Cerrar" (android.widget.Button con content description)
            if (btnCerrar == null && clase == "android.widget.Button" && desc.equals("Cerrar", ignoreCase = true)) {
                btnCerrar = nodo
                Log.d(TAG, "Botón 'Cerrar' encontrado.")
            }

            // Si ya se encontraron todos los elementos, no es necesario seguir buscando en este subárbol.
            if (campoEditable != null && btnOfertar != null && btnCerrar != null) {
                return
            }

            // Continuar búsqueda en los hijos
            for (i in 0 until nodo.childCount) {
                nodo.getChild(i)?.let { hijo ->
                    buscar(hijo)
                    // Si después de buscar en un hijo ya completamos, podemos parar de buscar en los hermanos.
                    if (campoEditable != null && btnOfertar != null && btnCerrar != null) {
                        return
                    }
                }
            }
        }

        buscar(nodoRaiz)
        return ResultadoDialogoTarifa(valorTarifa, campoEditable, btnOfertar, btnCerrar)
    }
}