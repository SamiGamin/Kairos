package com.kairos.ast.servicios.procesadores

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.utils.buscarNodos
import com.kairos.ast.servicios.utils.intentarClic

private const val TAG_LOG = "ProcesadorRevelado"

/**
 * Objeto responsable de la lógica para "revelar" el contenido oculto en la pantalla de detalle.
 */
object ProcesadorRevelado {

    /**
     * Busca y hace clic en el elemento correcto para revelar el contenido usando una heurística flexible.
     */
    fun revelarContenido(nodoRaiz: AccessibilityNodeInfo): Boolean {
        Log.d(TAG_LOG, "Buscando imagen clicable para revelar contenido con heurística flexible...")

        // 1. Buscar todos los nodos candidatos una sola vez.
        val scrollContraofertas = nodoRaiz.buscarNodos { it.className == "android.widget.HorizontalScrollView" }.firstOrNull()
        val botonCerrar = nodoRaiz.buscarNodos { it.className == "android.widget.Button" && it.text?.toString()?.contains("Cerrar", true) == true }.firstOrNull()
        val imagenesClicables = nodoRaiz.buscarNodos { it.className == "android.widget.ImageView" && it.isClickable && it.text == null && it.contentDescription == null }

        var imagenObjetivo: AccessibilityNodeInfo? = null

        // Heurística 1: Lógica posicional estricta (entre el scroll y el botón cerrar).
        if (scrollContraofertas != null && botonCerrar != null) {
            Log.d(TAG_LOG, "Aplicando heurística estricta (entre scroll y botón cerrar).")
            val rectScroll = Rect()
            scrollContraofertas.getBoundsInScreen(rectScroll)
            val rectBotonCerrar = Rect()
            botonCerrar.getBoundsInScreen(rectBotonCerrar)

            imagenObjetivo = imagenesClicables.find { imagen ->
                val rectImagen = Rect()
                imagen.getBoundsInScreen(rectImagen)
                // La imagen debe estar después del scroll Y antes del botón
                rectImagen.top >= rectScroll.bottom && rectImagen.bottom <= rectBotonCerrar.top
            }
        }

        // Heurística 2 (Plan B): Si la primera falla, buscar la imagen más baja que esté por encima del botón "Cerrar".
        if (imagenObjetivo == null && botonCerrar != null) {
            Log.d(TAG_LOG, "Aplicando heurística de fallback (imagen sobre el botón cerrar).")
            val rectBotonCerrar = Rect()
            botonCerrar.getBoundsInScreen(rectBotonCerrar)
            
            imagenObjetivo = imagenesClicables
                .filter { imagen ->
                    val rectImagen = Rect()
                    imagen.getBoundsInScreen(rectImagen)
                    rectImagen.bottom <= rectBotonCerrar.top // Debe estar estrictamente por encima
                }
                .maxByOrNull { imagen ->
                    val rectImagen = Rect()
                    imagen.getBoundsInScreen(rectImagen)
                    rectImagen.top // maxByOrNull encontrará la que tenga el 'top' más grande (la más baja)
                }
        }
        
        // Limpieza de nodos que no se van a usar.
        scrollContraofertas?.recycle()
        botonCerrar?.recycle()
        imagenesClicables.forEach { if(it != imagenObjetivo) it.recycle() }

        // Clic final si se encontró un objetivo.
        if (imagenObjetivo != null) {
            Log.i(TAG_LOG, "Imagen objetivo encontrada con heurística. Intentando clic.")
            val exito = imagenObjetivo.intentarClic()
            imagenObjetivo.recycle() // Reciclamos el objetivo después de usarlo.
            return exito
        } else {
            Log.w(TAG_LOG, "No se encontró ninguna ImageView que cumpla las heurísticas.")
            return false
        }
    }
}