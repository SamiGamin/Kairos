# Kairos - Asistente Inteligente para Conductores 🚗💸

![Versión](https://img.shields.io/github/v/release/SamiGamin/kairos?include_prereleases&label=versi%C3%B3n&color=blue)
![Commits](https://img.shields.io/github/commit-activity/m/SamiGamin/kairos?label=commits)
![Licencia](https://img.shields.io/badge/licencia-MIT-green)

**Toma el control de tus ganancias. Kairos te ayuda a filtrar y elegir solo los viajes más rentables, analizando las solicitudes de viaje en tiempo real.**

---

## 📖 Acerca del Proyecto

Como conductor de aplicaciones, es común recibir un gran volumen de solicitudes de viaje, muchas de las cuales no son rentables. Analizar cada una manualmente es estresante e ineficiente.

**Kairos** nace como una solución a este problema. Es una aplicación Android que funciona como un copiloto inteligente, utilizando el Servicio de Accesibilidad para "leer" las solicitudes de viaje y compararlas con un conjunto de reglas personalizadas que tú mismo defines.

### ✨ Características Principales

* **Filtro Basado en Reglas:** Configura tus propios criterios para que Kairos decida por ti.
    * 💰 **Ganancia Mínima por Viaje:** Establece un monto mínimo que estás dispuesto a aceptar.
    * 📐 **Tarifa por Kilómetro:** Define tu precio ideal por cada kilómetro recorrido.
    * 📍 **Filtros de Distancia:** Controla la distancia máxima de recogida y la distancia total del viaje.
* **Análisis en Tiempo Real:** El asistente se activa automáticamente cuando usas tus aplicaciones de conducción.
* **Backend con Supabase:** Sincronización de configuraciones y preparación para futuras funcionalidades en la nube.
* **Integración con Google Maps API:** Cálculos de distancia y visualización de rutas precisas.

---

## 🛠️ Tecnologías Utilizadas

Este proyecto fue construido utilizando un stack moderno de desarrollo Android:

* **Lenguaje:** [Kotlin](https://kotlinlang.org/)
* **UI:** [xml](https://developer.android.com/xml) 
* **Backend:** [Supabase](https://supabase.io/)
* **CI/CD:** GitHub Actions

---

## 🚀 Instalación y Uso

Para empezar a usar Kairos, sigue estos pasos:

1.  **Descargar la App:**
    * Ve a la sección de [**Releases**](https://github.com/SamiGamin/kairos/releases) en este repositorio.
    * Descarga el archivo `.apk` de la última versión.

2.  **Instalar en tu Dispositivo:**
    * Abre el archivo `.apk` en tu teléfono Android.
    * Es posible que necesites habilitar la opción de "Instalar desde fuentes desconocidas" en los ajustes de tu teléfono.

3.  **Configuración Inicial:**
    * Abre Kairos. La aplicación te pedirá que actives el **Permiso de Accesibilidad**.
    * Sigue las instrucciones para ir a los ajustes de tu teléfono y activar el servicio para Kairos. Este paso es **crucial** para que la app funcione.

4.  **Define tus Reglas:**
    * Una vez concedido el permiso, vuelve a la app y establece tus reglas de ganancia (tarifa mínima, precio por km, etc.).
    * ¡Y listo! El asistente ya está activo.

---

## 📸 Capturas de Pantalla

*(Aquí es un lugar excelente para agregar GIFs o capturas de pantalla de la app en acción. Muestra la pantalla de configuración y cómo interactúa con otra app)*

| Configuración de Reglas | Análisis en Tiempo Real |
| :---------------------: | :-----------------------: |
| *(Tu imagen aquí)* | *(Tu imagen aquí)* |

---

## 🗺️ Roadmap (Planes a Futuro)

Kairos está en desarrollo activo. Algunas de las funcionalidades planeadas son:

* [ ] 📈 **Dashboard de Rentabilidad:** Estadísticas detalladas de tus ganancias.
* [ ] 🏠 **Modo Destino:** Filtro para aceptar solo viajes que te acerquen a una dirección específica.
* [ ] ⚡️ **Sugerencias de Tarifa Dinámica:** Recomendaciones de precios en horas de alta demanda.
* [ ] ☁️ **Sincronización en la Nube:** Guarda y restaura tus configuraciones desde tu cuenta.

---

## 🤝 Cómo Contribuir

Por el momento, el proyecto no está abierto a contribuciones externas, pero agradecemos enormemente el feedback y los reportes de errores. Si encuentras un problema, por favor, crea un [**Issue**](https://github.com/SamiGamin/kairos/issues) detallando el error.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.

---

## 📫 Contacto

**SamiGamin** - [GitHub @SamiGamin](https://github.com/SamiGamin)
