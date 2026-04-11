import etl.ApiClient;
import etl.DataParser;
import model.RegistroFinanciero;
import similitud.SimilitudServer;
import similitud.SimilitudService;

import java.util.ArrayList;
import java.util.List;

/*
 * MainSimilitud.java - Punto de entrada del servidor de similitud (Requerimiento 3).
 *
 * Este main es independiente del Main.java original (que hacía el benchmark de sorting).
 * Puedes ejecutar ambos en paralelo: Main.java hace el ETL + benchmark y guarda los CSV,
 * MainSimilitud.java carga los mismos datos y levanta el servidor HTTP para la UI.
 *
 * FLUJO DE EJECUCIÓN:
 *   1. Descarga los datos de los 20 activos desde Yahoo Finance (igual que Main.java).
 *   2. Construye el SimilitudService (que calcula los retornos diarios internamente).
 *   3. Levanta el SimilitudServer en el puerto 8080.
 *   4. El proceso queda en ejecución esperando peticiones HTTP.
 *
 * PARA USAR:
 *   mvn compile exec:java -Dexec.mainClass="MainSimilitud"
 *   O desde el IDE: ejecutar esta clase directamente.
 *   Luego en otra terminal: python python_viz/app.py
 *   Abrir: http://localhost:5000
 *
 * NOTA SOBRE RENDIMIENTO:
 *   La descarga de 20 tickers tarda ~20-30 segundos con buena conexión.
 *   El cálculo de retornos es inmediato (O(n) por ticker).
 *   El DTW para una comparación tarda ~1-3 segundos (O(n²) con banda).
 */
public class MainSimilitud {

    private static final String[] TICKERS = {
            "VOO", "AAPL", "MSFT", "GOOGL", "AMZN",
            "TSLA", "META", "NVDA", "SPY", "QQQ",
            "JPM", "V", "WMT", "JNJ", "PG",
            "MA", "UNH", "HD", "BAC", "DIS"
    };

    public static void main(String[] args) {
        System.out.println("=== Iniciando servidor de Análisis de Similitud (Req. 3) ===\n");

        // ─── PASO 1: ETL - Descarga y parseo de datos ─────────────────────────────
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
            System.err.println("ERROR: No se obtuvieron datos. Verifica tu conexión a internet.");
            System.exit(1);
        }

        System.out.println("\nTotal registros cargados: " + todos.size());

        // ─── PASO 2: Construir el servicio de similitud ───────────────────────────
        System.out.println("\nCalculando retornos diarios por activo...");
        SimilitudService servicio = new SimilitudService(todos);

        // ─── PASO 3: Levantar el servidor HTTP ────────────────────────────────────
        try {
            SimilitudServer servidor = new SimilitudServer(servicio);
            servidor.iniciar();

            System.out.println("\n=== Servidor listo. Abre http://localhost:5000 en tu navegador ===");
            System.out.println("Presiona Ctrl+C para detener el servidor.\n");

            // Gancho de cierre: cuando el usuario presiona Ctrl+C, detenemos el servidor limpiamente.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[MainSimilitud] Deteniendo servidor...");
                servidor.detener();
            }));

            // Mantenemos el hilo principal vivo indefinidamente.
            // El servidor HTTP corre en su propio hilo interno.
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("ERROR al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
