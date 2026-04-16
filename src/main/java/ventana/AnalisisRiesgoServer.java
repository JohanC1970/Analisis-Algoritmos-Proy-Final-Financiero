package ventana;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/*
 * AnalisisRiesgoServer.java - Servidor HTTP embebido para el Req. 4.
 *
 * Usa com.sun.net.httpserver.HttpServer (JDK estándar, sin dependencias externas),
 * igual que SimilitudServer.java del Req. 3.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * ENDPOINTS EXPUESTOS
 * ═══════════════════════════════════════════════════════════════════════
 *
 *   GET /health
 *     → { "status": "ok", "activos": N }
 *     Verifica que el servidor está listo y retorna cuántos activos hay cargados.
 *
 *   GET /riesgo
 *     → { "activos": [...], "resumen": {...} }
 *     Lista completa de activos con métricas de riesgo y resultados de patrones,
 *     ordenada de menor a mayor riesgo (el ranking completo del Req. 4).
 *
 *   GET /ticker?t=AAPL
 *     → MetricasActivo completo del ticker solicitado.
 *     Útil para ver el detalle de un activo específico.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * CORS
 * ═══════════════════════════════════════════════════════════════════════
 *   Todas las respuestas incluyen Access-Control-Allow-Origin: *
 *   Esto permite que analisis_riesgo.html (servido como file:// o desde
 *   cualquier servidor estático) pueda llamar a esta API sin restricciones.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PUERTO
 * ═══════════════════════════════════════════════════════════════════════
 *   8080. Si SimilitudServer ya está corriendo en 8080, ejecutar solo
 *   uno de los dos a la vez (MainSimilitud XOR MainAnalisisRiesgo).
 */
public class AnalisisRiesgoServer {

    private static final int PUERTO = 8000;

    private final HttpServer servidor;
    private final AnalisisRiesgoService servicio;
    private final Gson gson;

    /**
     * Crea e inicializa el servidor HTTP con el servicio de análisis de riesgo.
     *
     * @param servicio El AnalisisRiesgoService ya construido con los datos del ETL.
     * @throws IOException Si el puerto 8080 ya está en uso.
     */
    public AnalisisRiesgoServer(AnalisisRiesgoService servicio) throws IOException {
        this.servicio = servicio;
        // serializeSpecialFloatingPointValues incluye NaN e Infinity en el JSON,
        // aunque en la práctica no deberíamos tener esos valores en métricas válidas.
        this.gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

        this.servidor = HttpServer.create(new InetSocketAddress(PUERTO), 10);

        servidor.createContext("/health", this::handleHealth);
        servidor.createContext("/riesgo", this::handleRiesgo);
        servidor.createContext("/ticker", this::handleTicker);

        servidor.setExecutor(null); // Usa el hilo actual del servidor.
    }

    /** Inicia el servidor. Bloqueará el proceso hasta que se llame detener(). */
    public void iniciar() {
        servidor.start();
        System.out.println("[AnalisisRiesgoServer] Servidor HTTP iniciado en http://localhost:" + PUERTO);
        System.out.println("  GET http://localhost:" + PUERTO + "/health");
        System.out.println("  GET http://localhost:" + PUERTO + "/riesgo");
        System.out.println("  GET http://localhost:" + PUERTO + "/ticker?t=TICKER");
    }

    /** Detiene el servidor ordenadamente con un tiempo de gracia de 1 segundo. */
    public void detener() {
        servidor.stop(1);
        System.out.println("[AnalisisRiesgoServer] Servidor detenido.");
    }

    // ─── HANDLERS ────────────────────────────────────────────────────────────

    /** GET /health → { "status": "ok", "activos": N } */
    private void handleHealth(HttpExchange ex) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("activos", servicio.getActivosOrdenados().size());
        enviarJson(ex, 200, gson.toJson(resp));
    }

    /**
     * GET /riesgo → JSON con la lista completa de activos (ordenada por riesgo)
     * y el resumen agregado del portafolio.
     */
    private void handleRiesgo(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            enviarJson(ex, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("activos",  servicio.getActivosOrdenados());
        resp.put("resumen",  servicio.getResumen());
        enviarJson(ex, 200, gson.toJson(resp));
    }

    /**
     * GET /ticker?t=TICKER → JSON con las MetricasActivo del ticker solicitado.
     * Devuelve 400 si falta el parámetro y 404 si el ticker no existe.
     */
    private void handleTicker(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            enviarJson(ex, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }

        // Parseo manual del query string: ?t=TICKER
        URI uri   = ex.getRequestURI();
        String qs = uri.getQuery(); // Ej: "t=AAPL"
        String ticker = null;

        if (qs != null) {
            for (String par : qs.split("&")) {
                String[] kv = par.split("=", 2);
                if (kv.length == 2 && "t".equals(kv[0])) {
                    ticker = kv[1].toUpperCase().trim();
                }
            }
        }

        if (ticker == null || ticker.isEmpty()) {
            enviarJson(ex, 400, "{\"error\":\"Parámetro 't' requerido. Ej: /ticker?t=AAPL\"}");
            return;
        }

        MetricasActivo metricas = servicio.buscarTicker(ticker);
        if (metricas == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ticker no encontrado: " + ticker);
            enviarJson(ex, 404, gson.toJson(error));
            return;
        }

        System.out.println("[AnalisisRiesgoServer] Detalle solicitado: " + ticker);
        enviarJson(ex, 200, gson.toJson(metricas));
    }

    // ─── UTILIDADES HTTP ─────────────────────────────────────────────────────

    /**
     * Envía una respuesta JSON con las cabeceras correctas y headers CORS.
     *
     * @param ex     El intercambio HTTP activo.
     * @param codigo Código de estado HTTP.
     * @param json   Cuerpo de la respuesta como String JSON.
     */
    private void enviarJson(HttpExchange ex, int codigo, String json) throws IOException {
        byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type",                  "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",   "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods",  "GET, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers",  "Content-Type");

        ex.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(cuerpo);
        }
    }
}
