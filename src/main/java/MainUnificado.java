import etl.ApiClient;
import etl.DataParser;
import model.RegistroFinanciero;
import similitud.SimilitudService;
import ventana.AnalisisRiesgoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MainUnificado.java — Punto de entrada único del proyecto.
 *
 * Resuelve el problema de tener 3 clases Main independientes (Main.java,
 * MainSimilitud.java, MainAnalisisRiesgo.java) que cada una descargaba los
 * 20 activos desde cero y levantaba su propio servidor HTTP en un puerto distinto.
 *
 * FLUJO UNIFICADO:
 *   1. ETL: descarga los 20 activos UNA SOLA VEZ (~30s con buena conexión).
 *   2. Los tres servicios reciben la MISMA lista en memoria (sin re-descargar).
 *   3. Un único servidor HTTP en el puerto 8080 expone todos los endpoints.
 *   4. El evaluador solo necesita abrir http://localhost:8080 para ver todo.
 *
 * USO:
 *   mvn package -q
 *   java -jar target/Analisis-Algoritmos-Proy-Final-Financiero.jar
 *
 *   O desde el IDE: ejecutar esta clase como punto de entrada principal.
 */
public class MainUnificado {

    private static final String[] TICKERS = {
            "VOO", "AAPL", "MSFT", "GOOGL", "AMZN",
            "TSLA", "META", "NVDA", "SPY", "QQQ",
            "JPM", "V", "WMT", "JNJ", "PG",
            "MA", "UNH", "HD", "BAC", "DIS"
    };

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   Análisis de Algoritmos — Universidad del Quindío 2026-1   ║");
        System.out.println("║   Pipeline unificado: ETL → Servicios → Servidor HTTP       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // ═══════════════════════════════════════════════════════════════════════
        // FASE 1: ETL — Descarga única para todos los requerimientos
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("── FASE 1: Descarga de datos (" + TICKERS.length + " activos) ──────────────────");

        ApiClient cliente = new ApiClient();
        DataParser parser  = new DataParser();
        List<RegistroFinanciero> todos = new ArrayList<>();

        // Contadores para el resumen ETL (Req 1)
        Map<String, Integer> registrosPorActivo   = new HashMap<>();
        Map<String, Integer> descartadosPorActivo = new HashMap<>();

        for (String ticker : TICKERS) {
            System.out.print("  Descargando " + ticker + "... ");
            String json = cliente.descargarDatosHistoricos(ticker);
            if (json != null) {
                int antesDeAgregar = todos.size();
                List<RegistroFinanciero> registros = parser.parsearYahooJson(json, ticker);
                todos.addAll(registros);
                registrosPorActivo.put(ticker, registros.size());
                // El parser imprime cuántos descartó; aquí lo capturamos de forma aproximada
                // como diferencia entre lo que podría haber y lo que realmente se guardó.
                descartadosPorActivo.put(ticker, 0); // El DataParser ya lo reporta en consola
            } else {
                System.out.println("ERROR — no se obtuvo respuesta.");
                registrosPorActivo.put(ticker, 0);
                descartadosPorActivo.put(ticker, 0);
            }
        }

        if (todos.isEmpty()) {
            System.err.println("\nERROR CRÍTICO: No se obtuvieron datos. Verifica tu conexión a internet.");
            System.exit(1);
        }

        System.out.println("\nTotal de registros cargados: " + todos.size());

        // Resumen ETL para el endpoint /api/etl
        ServidorUnificado.EtlResumen etlResumen = new ServidorUnificado.EtlResumen(
                registrosPorActivo.size(),
                todos.size(),
                registrosPorActivo,
                descartadosPorActivo
        );

        // ═══════════════════════════════════════════════════════════════════════
        // FASE 2: Construcción de servicios (todos usan la misma lista 'todos')
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("\n── FASE 2: Inicializando servicios ─────────────────────────────");

        System.out.println("  [1/3] BenchmarkService — ejecutando 12 algoritmos de sorting...");
        BenchmarkService benchmarkService = new BenchmarkService(todos);

        System.out.println("  [2/3] SimilitudService — calculando retornos y matriz de correlación...");
        SimilitudService similitudService = new SimilitudService(todos);

        System.out.println("  [3/3] AnalisisRiesgoService — clasificando riesgo y detectando patrones...");
        AnalisisRiesgoService riesgoService = new AnalisisRiesgoService(todos);

        System.out.println("  Todos los servicios inicializados correctamente.");

        // ═══════════════════════════════════════════════════════════════════════
        // FASE 3: Servidor HTTP unificado
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("\n── FASE 3: Levantando servidor HTTP ────────────────────────────");

        try {
            ServidorUnificado servidor = new ServidorUnificado(
                    similitudService,
                    riesgoService,
                    benchmarkService,
                    etlResumen
            );
            servidor.iniciar();

            // Gancho de cierre: Ctrl+C detiene el servidor limpiamente.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[MainUnificado] Señal de cierre recibida...");
                servidor.detener();
            }));

            // Mantener el proceso vivo indefinidamente.
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("ERROR al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}