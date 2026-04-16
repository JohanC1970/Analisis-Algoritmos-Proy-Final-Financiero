import etl.ApiClient;
import etl.DataParser;
import model.RegistroFinanciero;
import ventana.AnalisisRiesgoServer;
import ventana.AnalisisRiesgoService;

import java.util.ArrayList;
import java.util.List;

/*
 * MainAnalisisRiesgo.java - Punto de entrada del Requerimiento 4:
 * Análisis de Frecuencia de Patrones y Clasificación de Riesgo.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * FLUJO DE EJECUCIÓN
 * ═══════════════════════════════════════════════════════════════════════
 *   1. Descarga los datos históricos de los 20 activos desde Yahoo Finance
 *      (idéntico al ETL de Main.java y MainSimilitud.java).
 *
 *   2. Construye AnalisisRiesgoService, que internamente:
 *      a. Agrupa los registros por ticker y los ordena por fecha.
 *      b. Calcula los retornos diarios de cada activo.
 *      c. Computa desviación estándar y volatilidad histórica anualizada.
 *      d. Ejecuta el analizador de ventana deslizante (20 días) con dos patrones:
 *           - Patrón 1: Racha Alcista de ≥3 días consecutivos.
 *           - Patrón 2: Compresión de Rango Intradiario ≥30%.
 *      e. Clasifica cada activo en CONSERVADOR / MODERADO / AGRESIVO.
 *      f. Ordena la lista de menor a mayor riesgo.
 *
 *   3. Levanta AnalisisRiesgoServer en el puerto 8080.
 *
 *   4. El proceso queda en ejecución esperando peticiones HTTP.
 *      La interfaz web analisis_riesgo.html consume la API directamente.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * INSTRUCCIONES DE USO
 * ═══════════════════════════════════════════════════════════════════════
 *
 *   Desde terminal (Maven):
 *     mvn compile exec:java -Dexec.mainClass="MainAnalisisRiesgo"
 *
 *   Desde el IDE:
 *     Ejecutar esta clase directamente (Run As > Java Application).
 *
 *   Para ver la interfaz web:
 *     Abrir python_viz/analisis_riesgo.html directamente en el navegador.
 *     (Funciona como archivo local porque la API tiene CORS habilitado.)
 *
 *   Para detener el servidor:
 *     Ctrl+C en la terminal.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * NOTA: NO ejecutar simultáneamente con MainSimilitud.java
 * si ambos usan el puerto 8080.
 * ═══════════════════════════════════════════════════════════════════════
 */
public class MainAnalisisRiesgo {

    // Los mismos 20 tickers del portafolio definido en Main.java y MainSimilitud.java.
    private static final String[] TICKERS = {
            "VOO", "AAPL", "MSFT", "GOOGL", "AMZN",
            "TSLA", "META", "NVDA", "SPY", "QQQ",
            "JPM", "V", "WMT", "JNJ", "PG",
            "MA", "UNH", "HD", "BAC", "DIS"
    };

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  Req. 4 — Análisis de Patrones y Clasificación de Riesgo ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // ─── PASO 1: ETL — Descarga y parseo de datos ─────────────────────────
        System.out.println("═══ FASE 1: Descarga de datos ══════════════════════════════");
        ApiClient cliente = new ApiClient();
        DataParser parser  = new DataParser();
        List<RegistroFinanciero> todos = new ArrayList<>();

        for (String ticker : TICKERS) {
            System.out.println("Descargando: " + ticker);
            String json = cliente.descargarDatosHistoricos(ticker);
            if (json != null) {
                todos.addAll(parser.parsearYahooJson(json, ticker));
            }
        }

        if (todos.isEmpty()) {
            System.err.println("\nERROR: No se obtuvieron datos. Verifica tu conexión a internet.");
            System.exit(1);
        }
        System.out.println("\nTotal de registros cargados: " + todos.size());

        // ─── PASO 2: Clasificación y análisis de patrones ─────────────────────
        System.out.println("\n═══ FASE 2: Análisis de riesgo y detección de patrones ════");
        System.out.println("Procesando " + TICKERS.length + " activos...");

        AnalisisRiesgoService servicio = new AnalisisRiesgoService(todos);

        // ─── PASO 3: Levantar el servidor HTTP ────────────────────────────────
        System.out.println("═══ FASE 3: Levantando servidor HTTP ═══════════════════════");
        try {
            AnalisisRiesgoServer servidor = new AnalisisRiesgoServer(servicio);
            servidor.iniciar();

            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  Servidor listo. Abre en tu navegador:                   ║");
            System.out.println("║  python_viz/analisis_riesgo.html                         ║");
            System.out.println("║  (o http://localhost:8000/riesgo para ver el JSON crudo) ║");
            System.out.println("║                                                          ║");
            System.out.println("║  Presiona Ctrl+C para detener el servidor.               ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            // Gancho de cierre: Ctrl+C detiene el servidor ordenadamente.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[MainAnalisisRiesgo] Deteniendo servidor...");
                servidor.detener();
            }));

            // Mantenemos el hilo principal vivo indefinidamente.
            // El servidor HTTP corre en su propio hilo interno de JDK.
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("ERROR al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
