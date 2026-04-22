import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import similitud.SimilitudService;
import ventana.AnalisisRiesgoService;
import ventana.MetricasActivo;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ServidorUnificado.java — Servidor HTTP único que centraliza todos los endpoints.
 *
 * Reemplaza a SimilitudServer (puerto 8080) y AnalisisRiesgoServer (puerto 8000).
 * Todos los requerimientos se sirven desde un único proceso en el puerto 8080.
 *
 * ENDPOINTS:
 *   GET /                          → index.html (SPA con navegación entre requerimientos)
 *   GET /health                    → { status, activos, benchmarkListo }
 *
 *   ── Req 1: ETL ──────────────────────────────────────────────────────────────
 *   GET /api/etl                   → resumen de la descarga (activos, registros, limpiezas)
 *
 *   ── Req 2: Sorting (Seguimiento) ────────────────────────────────────────────
 *   GET /api/benchmark             → tiempos de los 12 algoritmos + top-15 volumen
 *
 *   ── Req 3: Similitud (Proyecto) ─────────────────────────────────────────────
 *   GET /api/tickers               → lista de tickers disponibles
 *   GET /api/similitud?a=X&b=Y     → 4 algoritmos de similitud entre dos activos
 *
 *   ── Req 4: Dashboard ────────────────────────────────────────────────────────
 *   GET /api/correlacion           → matriz de correlación NxN
 *   GET /api/ohlc?ticker=X&dias=N  → datos OHLC para candlestick
 *   GET /api/riesgo                → ranking de riesgo + patrones por activo
 *
 *   ── Archivos estáticos ──────────────────────────────────────────────────────
 *   GET /static/*                  → sirve archivos desde python_viz/static/
 */
public class ServidorUnificado {

    private static final int PUERTO = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080")
    );

    private final HttpServer         servidor;
    private final SimilitudService   similitudService;
    private final AnalisisRiesgoService riesgoService;
    private final BenchmarkService   benchmarkService;
    private final EtlResumen         etlResumen;
    private final Gson               gson;

    // ─── Modelo para el resumen ETL ──────────────────────────────────────────
    public static class EtlResumen {
        public final int    totalActivos;
        public final int    totalRegistros;
        public final Map<String, Integer> registrosPorActivo;
        public final Map<String, Integer> descartadosPorActivo;

        public EtlResumen(int totalActivos, int totalRegistros,
                          Map<String, Integer> registrosPorActivo,
                          Map<String, Integer> descartadosPorActivo) {
            this.totalActivos          = totalActivos;
            this.totalRegistros        = totalRegistros;
            this.registrosPorActivo    = registrosPorActivo;
            this.descartadosPorActivo  = descartadosPorActivo;
        }
    }

    /**
     * Constructor: recibe los tres servicios ya inicializados con el dataset compartido.
     *
     * @param similitudService  Servicio de similitud y correlación (Req 3 y 4).
     * @param riesgoService     Servicio de clasificación de riesgo y patrones (Req 4).
     * @param benchmarkService  Servicio de benchmark de sorting (Req 2 - Seguimiento).
     * @param etlResumen        Resumen estadístico del proceso ETL (Req 1).
     */
    public ServidorUnificado(SimilitudService similitudService,
                             AnalisisRiesgoService riesgoService,
                             BenchmarkService benchmarkService,
                             EtlResumen etlResumen) throws IOException {
        this.similitudService  = similitudService;
        this.riesgoService     = riesgoService;
        this.benchmarkService  = benchmarkService;
        this.etlResumen        = etlResumen;
        this.gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

        this.servidor = HttpServer.create(new InetSocketAddress(PUERTO), 20);

        // ── Registro de endpoints ──────────────────────────────────────────────
        servidor.createContext("/",               this::handleIndex);
        servidor.createContext("/health",         this::handleHealth);
        servidor.createContext("/api/etl",        this::handleEtl);
        servidor.createContext("/api/benchmark",  this::handleBenchmark);
        servidor.createContext("/api/tickers",    this::handleTickers);
        servidor.createContext("/api/similitud",  this::handleSimilitud);
        servidor.createContext("/api/correlacion",this::handleCorrelacion);
        servidor.createContext("/api/ohlc",       this::handleOhlc);
        servidor.createContext("/api/riesgo",     this::handleRiesgo);
        servidor.createContext("/static",         this::handleStatic);

        servidor.setExecutor(null);
    }

    public void iniciar() {
        servidor.start();
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Servidor unificado listo en http://localhost:" + PUERTO + "          ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Endpoints disponibles:                                      ║");
        System.out.println("║    GET /                  → Interfaz principal               ║");
        System.out.println("║    GET /api/etl           → Resumen ETL (Req 1)              ║");
        System.out.println("║    GET /api/benchmark     → Sorting + Volumen (Req 2)        ║");
        System.out.println("║    GET /api/similitud     → Similitud series (Req 3)         ║");
        System.out.println("║    GET /api/correlacion   → Matriz correlación (Req 4)       ║");
        System.out.println("║    GET /api/ohlc          → Candlestick (Req 4)              ║");
        System.out.println("║    GET /api/riesgo        → Ranking riesgo (Req 4)           ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Presiona Ctrl+C para detener.                               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }

    public void detener() {
        servidor.stop(1);
        System.out.println("[ServidorUnificado] Servidor detenido.");
    }

    // ─── HANDLERS ────────────────────────────────────────────────────────────

    /** GET / → sirve el index.html desde python_viz/ */
    private void handleIndex(HttpExchange ex) throws IOException {
        if (!"/".equals(ex.getRequestURI().getPath())
                && !"/index.html".equals(ex.getRequestURI().getPath())) {
            // Intentar servir como archivo estático
            handleStatic(ex);
            return;
        }
        servirArchivoEstatico(ex, "python_viz/index.html", "text/html; charset=UTF-8");
    }

    /** GET /static/* → sirve archivos estáticos desde python_viz/static/ */
    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        // Mapear /static/foo.css → python_viz/static/foo.css
        String filePath = "python_viz" + path;
        String mime = detectarMime(path);
        servirArchivoEstatico(ex, filePath, mime);
    }

    private void servirArchivoEstatico(HttpExchange ex, String filePath, String mime) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            String body = "404 - No encontrado: " + filePath;
            ex.sendResponseHeaders(404, body.length());
            ex.getResponseBody().write(body.getBytes());
            ex.getResponseBody().close();
            return;
        }
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private String detectarMime(String path) {
        if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    /** GET /health */
    private void handleHealth(HttpExchange ex) throws IOException {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "ok");
        r.put("activos", similitudService.getTickersDisponibles().size());
        enviarJson(ex, 200, gson.toJson(r));
    }

    /** GET /api/etl */
    private void handleEtl(HttpExchange ex) throws IOException {
        enviarJson(ex, 200, gson.toJson(etlResumen));
    }

    /** GET /api/benchmark */
    private void handleBenchmark(HttpExchange ex) throws IOException {
        enviarJson(ex, 200, gson.toJson(benchmarkService.getResultado()));
    }

    /** GET /api/tickers */
    private void handleTickers(HttpExchange ex) throws IOException {
        List<String> tickers = similitudService.getTickersDisponibles();
        Map<String, Object> r = new HashMap<>();
        r.put("tickers", tickers);
        r.put("total", tickers.size());
        enviarJson(ex, 200, gson.toJson(r));
    }

    /** GET /api/similitud?a=X&b=Y */
    private void handleSimilitud(HttpExchange ex) throws IOException {
        Map<String, String> params = parsearQueryString(ex.getRequestURI());
        String a = params.get("a");
        String b = params.get("b");

        if (a == null || b == null) {
            enviarJson(ex, 400, "{\"error\":\"Parámetros 'a' y 'b' requeridos\"}");
            return;
        }
        if (a.equals(b)) {
            enviarJson(ex, 400, "{\"error\":\"Los tickers deben ser diferentes\"}");
            return;
        }

        SimilitudService.ResultadoSimilitud res = similitudService.calcularSimilitud(
                a.toUpperCase(), b.toUpperCase());

        if (res == null) {
            enviarJson(ex, 404, "{\"error\":\"No se pudo calcular: verifica que los tickers existan\"}");
            return;
        }
        enviarJson(ex, 200, gson.toJson(res));
    }

    /** GET /api/correlacion */
    private void handleCorrelacion(HttpExchange ex) throws IOException {
        SimilitudService.ResultadoCorrelacion res = similitudService.getCorrelacionCompleta();
        if (res == null) {
            enviarJson(ex, 503, "{\"error\":\"Matriz no disponible\"}");
            return;
        }
        enviarJson(ex, 200, gson.toJson(res));
    }

    /** GET /api/ohlc?ticker=X&dias=180 */
    private void handleOhlc(HttpExchange ex) throws IOException {
        Map<String, String> params = parsearQueryString(ex.getRequestURI());
        String ticker = params.get("ticker");
        int dias = 180;
        try { dias = Integer.parseInt(params.getOrDefault("dias", "180")); }
        catch (NumberFormatException ignored) {}

        if (ticker == null || ticker.isEmpty()) {
            enviarJson(ex, 400, "{\"error\":\"Parámetro 'ticker' requerido\"}");
            return;
        }

        SimilitudService.ResultadoOHLC res = similitudService.getOHLC(ticker.toUpperCase(), dias);
        if (res == null) {
            enviarJson(ex, 404, "{\"error\":\"Ticker no encontrado: " + ticker + "\"}");
            return;
        }
        enviarJson(ex, 200, gson.toJson(res));
    }

    /** GET /api/riesgo */
    private void handleRiesgo(HttpExchange ex) throws IOException {
        Map<String, Object> r = new HashMap<>();
        r.put("activos",  riesgoService.getActivosOrdenados());
        r.put("resumen",  riesgoService.getResumen());
        enviarJson(ex, 200, gson.toJson(r));
    }

    // ─── UTILIDADES ──────────────────────────────────────────────────────────

    private Map<String, String> parsearQueryString(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) return params;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) params.put(kv[0].toLowerCase(), kv[1].toUpperCase().trim());
        }
        return params;
    }

    private void enviarJson(HttpExchange ex, int codigo, String json) throws IOException {
        byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        ex.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(cuerpo); }
    }
}