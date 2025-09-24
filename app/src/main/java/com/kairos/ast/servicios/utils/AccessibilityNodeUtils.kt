package com.kairos.ast.servicios.utils

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

private const val TAG_LOG = "AccessibilityNodeUtils"

/**
 * Contiene textos clave para identificar la pantalla de detalle de un viaje.
 */
val TEXTOS_CLAVE_PANTALLA_DETALLE = listOf("Ofrece tu tarifa", "Aceptar por COL$")

/**
 * Busca un nodo ancestro que sea clicable y realiza la acción de clic.
 * Sube por el árbol de nodos hasta encontrar un padre clicable.
 *
 * IMPORTANTE: Esta función maneja su propia lógica de reciclaje para los nodos padre que obtiene.
 *
 * @return `true` si el clic fue exitoso, `false` en caso contrario.
 * @receiver El nodo desde el que se inicia la búsqueda. No se recicla.
 */
fun AccessibilityNodeInfo.intentarClic(): Boolean {
    var nodoActual: AccessibilityNodeInfo? = this
    var esNodoOriginal = true

    while (nodoActual != null) {
        if (nodoActual.isClickable) {
            val resultado = nodoActual.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG_LOG, "[CLIC] performAction en '${nodoActual.text}' resultado: $resultado")
            if (!esNodoOriginal) {
                nodoActual.recycle()
            }
            return resultado
        }
        val padre = nodoActual.parent
        if (!esNodoOriginal) {
            nodoActual.recycle()
        }
        nodoActual = padre
        esNodoOriginal = false
    }
    Log.w(TAG_LOG, "No se encontró ningún ancestro clicable para el nodo original '${this.text}'.")
    return false
}

/**
 * Busca de forma no recursiva en el árbol de nodos si alguno contiene los textos clave.
 *
 * IMPORTANTE: El nodo raíz (this) no se recicla. Los nodos hijos obtenidos se reciclan.
 *
 * @return `true` si se encuentra algún texto clave, `false` en caso contrario.
 */
fun AccessibilityNodeInfo.esPantallaDeDetalle(): Boolean {
    val cola = ArrayDeque<AccessibilityNodeInfo>()
    cola.add(this)

    while (cola.isNotEmpty()) {
        val nodo = cola.removeFirst()

        // Comprobar el texto del nodo actual
        if (nodo.text != null && TEXTOS_CLAVE_PANTALLA_DETALLE.any { clave -> nodo.text.toString().contains(clave, ignoreCase = true) }) {
            // Encontrado. Limpiar la cola reciclando los nodos restantes.
            if (nodo != this) nodo.recycle()
            cola.forEach { it.recycle() }
            return true
        }

        // Añadir hijos a la cola
        for (i in 0 until nodo.childCount) {
            nodo.getChild(i)?.let { cola.addLast(it) }
        }

        // Reciclar el nodo procesado si no es el nodo raíz original
        if (nodo != this) {
            nodo.recycle()
        }
    }
    return false
}

/**
 * Busca todos los nodos que coinciden con un predicado.
 *
 * @param predicado La condición que debe cumplir un nodo.
 * @return Una lista de nodos que cumplen la condición. Es responsabilidad del llamador reciclarlos.
 */
fun AccessibilityNodeInfo.buscarNodos(predicado: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
    val resultados = mutableListOf<AccessibilityNodeInfo>()
    val cola = ArrayDeque<AccessibilityNodeInfo>()
    cola.add(this)

    while (cola.isNotEmpty()) {
        val nodo = cola.removeFirst()

        if (predicado(nodo)) {
            // Se crea una copia para el resultado, el llamador la debe reciclar.
            resultados.add(AccessibilityNodeInfo.obtain(nodo))
        }

        for (i in 0 until nodo.childCount) {
            nodo.getChild(i)?.let { cola.addLast(it) }
        }
        
        if (nodo != this) {
            nodo.recycle()
        }
    }
    return resultados
}
