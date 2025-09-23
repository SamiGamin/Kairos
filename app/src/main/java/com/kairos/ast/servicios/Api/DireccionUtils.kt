package com.kairos.ast.servicios.api

/**
 * Limpia y estandariza una dirección. Esta función podría necesitar ajustes
 * dependiendo de qué tan bien la API de Google maneje las direcciones originales.
 */
fun limpiarDireccion(direccion: String): String {
    return direccion
        // 1. Quitar texto entre paréntesis
        // .replace(Regex("""\s*\(.*?\)"""), "")
        // 2. Quitar sufijos de empresa
        .replace(Regex("""\s*S\.A\.S\.?|\s*S\.A\.?|\s*Ltda\.?""", RegexOption.IGNORE_CASE), "")
        // 3. Reemplazar abreviaturas comunes con punto o sin punto
        .replace("Cl\\.?", "Calle", ignoreCase = true)
        .replace("Cra\\.?", "Carrera", ignoreCase = true)
        .replace("Tv\\.?", "Transversal", ignoreCase = true)
        .replace("Dg\\.?", "Diagonal", ignoreCase = true)
        // 4. Eliminar los caracteres '#' y '-' que confunden al geocodificador.
        .replace("#", " ")
        .replace("-", " ")
        // 5. Dejar solo letras, números y espacios para eliminar cualquier otro símbolo.
        .replace(Regex("""[^a-zA-Z0-9\s,()]"""), "")
        // 6. Limpiar espacios múltiples que puedan haber quedado
        .replace(Regex("""\s+"""), " ")
        .trim()
        // 7. Asegurarse de que termine con "Bogota, Colombia" si no lo tiene.
        // Esto podría ser específico de GraphHopper y quizás no tan necesario para Google.
        // Considera si quieres mantenerlo o ajustarlo.
        .let { if (!it.contains("Bogota", true) && !it.contains("Colombia", true)) "$it, Bogota, Colombia" else it }

}
