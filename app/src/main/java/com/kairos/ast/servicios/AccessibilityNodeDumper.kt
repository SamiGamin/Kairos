package com.kairos.ast.servicios


import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Clase de depuración. Su única función es recorrer recursivamente todos los nodos
 * de la pantalla a partir de un nodo raíz y mostrar sus propiedades más importantes
 * en el Logcat. Esto nos ayuda a "ver" lo que el servicio de accesibilidad ve.
 */
object AccessibilityNodeDumper {

    private const val TAG_LOG = "NodeDumper"

    /**
     * Inicia el proceso de volcado de información de los nodos.
     */
    fun dumpNodes(nodoRaiz: AccessibilityNodeInfo) {
        Log.d(TAG_LOG, "================ INICIO DEL DUMP DE NODOS ================")
        recorrerYMostrar(nodoRaiz, "")
        Log.d(TAG_LOG, "================= FIN DEL DUMP DE NODOS =================")
    }

    private fun recorrerYMostrar(nodo: AccessibilityNodeInfo, prefijo: String) {
        // Construimos la línea de log para el nodo actual
        val infoNodo = buildString {
            append("Clase: ${nodo.className}")
            if (nodo.text != null) append(", Texto: \"${nodo.text}\"")
            if (nodo.contentDescription != null) append(", Desc: \"${nodo.contentDescription}\"")
            if (nodo.viewIdResourceName != null) append(", ID: \"${nodo.viewIdResourceName}\"")
            append(", Clicable: ${nodo.isClickable}")
        }

        Log.d(TAG_LOG, "$prefijo $infoNodo")

        // Llamada recursiva para cada hijo, aumentando la indentación
        for (i in 0 until nodo.childCount) {
            val nodoHijo = nodo.getChild(i)
            if (nodoHijo != null) {
                recorrerYMostrar(nodoHijo, "$prefijo  ") // Añade dos espacios para la jerarquía
                // Importante: Reciclamos el hijo después de procesarlo
                nodoHijo.recycle()
            }
        }
    }
}