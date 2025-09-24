package com.kairos.ast.servicios.procesadores

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.utils.buscarNodos
import com.kairos.ast.servicios.utils.intentarClic

private const val TAG_LOG = "ProcesadorRevelado"

/**
 * Objeto responsable de la lógica para "revelar" el contenido oculto en la pantalla de detalle.
 * En algunas versiones de la app, es necesario hacer clic en una imagen para que aparezca
 * toda la información del viaje.
 */
object ProcesadorRevelado {

    /**
     * Busca y hace clic en el elemento correcto para revelar el contenido.
     * La heurística se basa en encontrar el botón "Cancelar" y luego buscar la imagen
     * clicable más baja que esté por encima de dicho botón.
     *
     * @param nodoRaiz El nodo raíz de la pantalla de detalle.
     * @return `true` si se intentó hacer clic (con éxito o no), `false` si no se encontró un objetivo válido.
     */
    fun revelarContenido(nodoRaiz: AccessibilityNodeInfo): Boolean {
        Log.d(TAG_LOG, "Buscando imagen clicable para revelar contenido...")

        // Usamos la utilidad para encontrar todos los botones e imágenes clicables
        val todosLosBotones = nodoRaiz.buscarNodos { it.className == "android.widget.Button" }
        val imagenesClicables = nodoRaiz.buscarNodos { it.className == "android.widget.ImageView" && it.isClickable }

        // Paso 1: Encontrar el botón de cancelar.
        val botonCancelar = todosLosBotones.find { it.text?.toString()?.contains("cancelar", true) == true }
        todosLosBotones.forEach { if (it != botonCancelar) it.recycle() } // Reciclar los no usados

        // Paso 2: Si no hay botón de cancelar, no podemos continuar con esta lógica.
        val rectBotonCancelar = Rect()
        if (botonCancelar != null) {
            botonCancelar.getBoundsInScreen(rectBotonCancelar)
            botonCancelar.recycle() // Ya tenemos las coordenadas, lo reciclamos.
        } else {
            Log.w(TAG_LOG, "No se encontró el botón 'Cancelar'. No se puede determinar qué imagen clicar.")
            imagenesClicables.forEach { it.recycle() }
            return false // No se encontró, no se hizo nada.
        }

        // Paso 3: Encontrar la imagen clicable más baja que esté por encima del botón de cancelar.
        var imagenObjetivo: AccessibilityNodeInfo? = null
        var maxTop = -1

        imagenesClicables.forEach { imagen ->
            val rectImagen = Rect()
            imagen.getBoundsInScreen(rectImagen)
            // La imagen debe estar por encima del botón
            if (rectImagen.bottom < rectBotonCancelar.top) {
                if (rectImagen.top > maxTop) {
                    maxTop = rectImagen.top
                    imagenObjetivo?.recycle() // Reciclar el candidato anterior
                    imagenObjetivo = imagen // Guardar el nuevo mejor candidato (no se obtiene copia)
                } else {
                    imagen.recycle() // No es el mejor candidato, reciclar.
                }
            } else {
                imagen.recycle() // No está por encima del botón, reciclar.
            }
        }

        // Paso 4: Hacer clic en la imagen objetivo si se encontró.
        if (imagenObjetivo != null) {
            Log.i(TAG_LOG, "Imagen objetivo encontrada. Intentando clic.")
            imagenObjetivo.intentarClic()
            imagenObjetivo.recycle()
            return true // Se encontró y se intentó el clic.
        } else {
            Log.w(TAG_LOG, "No se encontró una imagen clicable adecuada por encima del botón Cancelar.")
            return false // No se encontró, no se hizo nada.
        }
    }
}
