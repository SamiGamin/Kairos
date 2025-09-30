package com.kairos.ast.servicios.procesadores

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.kairos.ast.servicios.utils.buscarNodos
import com.kairos.ast.servicios.utils.intentarClic

private const val TAG_LOG = "ProcesadorRevelado"

/**
 * Objeto responsable de la lógica para iniciar una contraoferta personalizada.
 * En la UI de la app, esto corresponde a hacer clic en un ícono (un ImageView)
 * que está junto a las ofertas de precio predefinidas.
 */
object ProcesadorRevelado {

    /**
     * Busca y hace clic en el ícono de "editar oferta" (un ImageView).
     * La heurística localiza el HorizontalScrollView que contiene las ofertas
     * y luego busca un ImageView que esté alineado verticalmente con él.
     */
    fun revelarContenido(nodoRaiz: AccessibilityNodeInfo): Boolean {
        Log.d(TAG_LOG, "Buscando ícono de editar oferta...")

        // 1. Localizar el HorizontalScrollView que contiene los botones de oferta.
        val scrollNode = nodoRaiz.buscarNodos { it.className == "android.widget.HorizontalScrollView" }.firstOrNull()

        if (scrollNode != null) {
            val rectScroll = Rect()
            scrollNode.getBoundsInScreen(rectScroll)
            Log.d(TAG_LOG, "HorizontalScrollView encontrado con bounds: $rectScroll")

            // 2. Buscar todas las ImageView clicables.
            val imageViews = nodoRaiz.buscarNodos {
                it.className == "android.widget.ImageView" && it.isClickable
            }
            Log.d(TAG_LOG, "Encontradas ${imageViews.size} ImageView(s) clicables.")

            // 3. Encontrar la ImageView que está perfectamente alineada verticalmente con el scroll.
            val targetNode = imageViews.find { 
                val rectImage = Rect()
                it.getBoundsInScreen(rectImage)
                val isAligned = rectImage.top == rectScroll.top && rectImage.bottom == rectScroll.bottom
                if (isAligned) {
                    Log.d(TAG_LOG, "ImageView alineada encontrada con bounds: $rectImage")
                }
                isAligned
            }
            
            // Reciclar nodos no utilizados
            imageViews.forEach { if (it != targetNode) it.recycle() }
            scrollNode.recycle()

            if (targetNode != null) {
                Log.i(TAG_LOG, "Ícono de editar oferta encontrado. Intentando clic.")
                val exito = targetNode.intentarClic()
                targetNode.recycle()
                return exito
            } else {
                Log.w(TAG_LOG, "No se encontró un ImageView alineado con el HorizontalScrollView.")
                return false
            }

        } else {
            Log.w(TAG_LOG, "No se encontró el HorizontalScrollView de referencia.")
            return false
        }
    }
}