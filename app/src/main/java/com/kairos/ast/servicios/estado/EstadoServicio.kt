package com.kairos.ast.servicios.estado

/**
 * Define los diferentes estados por los que puede pasar el servicio de accesibilidad.
 * Usar un sealed class permite asociar datos a estados específicos si fuera necesario en el futuro.
 */
sealed class EstadoServicio {
    /** El servicio está activamente buscando y analizando viajes en la lista principal. */
    object BuscandoEnLista : EstadoServicio()

    /** El servicio ha hecho clic en un viaje y está esperando que aparezca la pantalla de detalle. */
    object EsperandoAparicionDetalle : EstadoServicio()

    /** El servicio está en la pantalla de detalle y necesita hacer clic en un elemento para revelar toda la información. */
    object EnDetalleRevelando : EstadoServicio()

    /** El servicio está en la pantalla de detalle con toda la información visible, listo para analizarla. */
    object EnDetalleProcesando : EstadoServicio()

    /** El servicio ha decidido contraofertar y está esperando que aparezca el diálogo para ingresar la tarifa. */
    object EsperandoDialogoTarifa : EstadoServicio()

    /** El servicio está interactuando con el diálogo de contraoferta. */
    object EnDialogoTarifa : EstadoServicio()
}
