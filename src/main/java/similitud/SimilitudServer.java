package similitud;

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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/*
 * SimilitudServer.java - Servidor HTTP embebido que expone el Requerimiento 3 como API REST.
 *
 * Usa com.sun.net.httpserver.HttpServer, que forma parte del JDK estándar desde Java 6.
 * No requiere ninguna dependencia externa (no Spring, no Jetty, no Tomcat).
 * Es suficiente para servir las peticiones de la interfaz web de Flask.
 *
 * ENDPOINTS EXPUESTOS:
 *
 *   GET /tickers
 *     → Lista de todos los tickers disponibles en el dataset.
 *     → Respuesta: { "tickers": ["AAPL", "AMZN", ...] }
 *
 *   GET /similitud?a=AAPL&b=MSFT
 *     → Ejecuta los 4 algoritmos entre los dos tickers especificados.
 *     → Respuesta: JSON con ResultadoSimilitud completo.
 *
 *   GET /health
 *     → Health check. Responde { "status": "ok" }.
 *     → Flask lo llama al arrancar para verificar que Java está listo.
 *
 * CORS:
 *   Añadimos cabeceras CORS en todas las respuestas para que Flask (puerto 5000)
 *   pueda llamar a Java (puerto 8080) sin que el navegador bloquee la petición.
 *
 * GSON:
 *   Usamos la misma librería Gson que ya está en el pom.xml del proyecto para
 *   serializar los objetos Java a JSON. No se añade ninguna dependencia nueva.
 */
public class SimilitudServer {

    private static final int PUERTO = 8080;

    private final HttpServer servidor;
    private final SimilitudService servicio;
    private final Gson gson;

    /**
     * Crea e inicializa el servidor HTTP con el servicio de similitud.
     *
     * @param servicio El SimilitudService ya construido con los datos del ETL.
     * @throws IOException Si no se puede abrir el puerto 8080.
     */
    public SimilitudServer(SimilitudService servicio) throws IOException {
        this.servicio = servicio;
        this.gson     = new GsonBuilder().serializeSpecialFloatingPointValues().create();

        // Creamos el servidor en el puerto 8080 con una cola de hasta 10 conexiones pendientes.
        this.servidor = HttpServer.create(new InetSocketAddress(PUERTO), 10);

        // Registramos los cinco endpoints.
        servidor.createContext("/tickers",     this::handleTickers);
        servidor.createContext("/similitud",   this::handleSimilitud);
        servidor.createContext("/health",      this::handleHealth);
        servidor.createContext("/correlacion", this::handleCorrelacion);
        servidor.createContext("/ohlc",        this::handleOhlc);

        // Executor null = usa el hilo actual (suficiente para las pocas peticiones esperadas).
        servidor.setExecutor(null);
    }

    /**
     * Arranca el servidor. Llamar una vez; el servidor escucha hasta que se llame stop().
     */
    public void iniciar() {
        servidor.start();
        System.out.println("[SimilitudServer] Servidor HTTP iniciado en http://localhost:" + PUERTO);
        System.out.println("[SimilitudServer] Endpoints disponibles:");
        System.out.println("  GET http://localhost:" + PUERTO + "/health");
        System.out.println("  GET http://localhost:" + PUERTO + "/tickers");
        System.out.println("  GET http://localhost:" + PUERTO + "/similitud?a=TICKER1&b=TICKER2");
        System.out.println("  GET http://localhost:" + PUERTO + "/correlacion");
        System.out.println("  GET http://localhost:" + PUERTO + "/ohlc?ticker=AAPL&dias=180");
    }

    /**
     * Detiene el servidor ordenadamente.
     */
    public void detener() {
        servidor.stop(1);
        System.out.println("[SimilitudServer] Servidor detenido.");
    }

    // ─── HANDLERS DE ENDPOINTS ───────────────────────────────────────────────────

    /**
     * GET /health → { "status": "ok", "tickers": N }
     * Permite que Flask verifique que Java está listo antes de empezar a servir peticiones.
     */
    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "ok");
        respuesta.put("tickers", servicio.getTickersDisponibles().size());
        enviarJson(exchange, 200, gson.toJson(respuesta));
    }

    /**
     * GET /tickers → { "tickers": ["AAPL", "AMZN", ...] }
     * Lista de tickers disponibles para los selectores de la interfaz web.
     */
    private void handleTickers(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            enviarJson(exchange, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }

        List<String> tickers = servicio.getTickersDisponibles();
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("tickers", tickers);
        respuesta.put("total", tickers.size());
        enviarJson(exchange, 200, gson.toJson(respuesta));
    }

    /**
     * GET /similitud?a=TICKER_A&b=TICKER_B
     * Ejecuta los 4 algoritmos y devuelve el JSON con ResultadoSimilitud completo.
     */
    private void handleSimilitud(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            enviarJson(exchange, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }

        // ─── Parseo de parámetros de query string ────────────────────────────────
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery(); // Ej: "a=AAPL&b=MSFT"

        if (query == null || query.isEmpty()) {
            enviarJson(exchange, 400, "{\"error\":\"Parámetros requeridos: a y b\"}");
            return;
        }

        String tickerA = null;
        String tickerB = null;

        // Parseamos manualmente el query string sin librerías externas.
        // Formato: key=value&key=value
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) {
                if ("a".equals(kv[0])) tickerA = kv[1].toUpperCase().trim();
                if ("b".equals(kv[0])) tickerB = kv[1].toUpperCase().trim();
            }
        }

        if (tickerA == null || tickerB == null) {
            enviarJson(exchange, 400, "{\"error\":\"Parámetros 'a' y 'b' son requeridos\"}");
            return;
        }

        if (tickerA.equals(tickerB)) {
            enviarJson(exchange, 400, "{\"error\":\"Los dos tickers deben ser diferentes\"}");
            return;
        }

        // ─── Cálculo de similitud ────────────────────────────────────────────────
        System.out.println("[SimilitudServer] Petición recibida: " + tickerA + " vs " + tickerB);

        SimilitudService.ResultadoSimilitud resultado = servicio.calcularSimilitud(tickerA, tickerB);

        if (resultado == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No se pudo calcular similitud. Verifica que los tickers existan y tengan fechas en común.");
            error.put("tickerA", tickerA);
            error.put("tickerB", tickerB);
            enviarJson(exchange, 404, gson.toJson(error));
            return;
        }

        // Serializamos el resultado completo a JSON con Gson.
        enviarJson(exchange, 200, gson.toJson(resultado));
    }

    /**
     * GET /correlacion
     * Devuelve la matriz de correlación de Pearson entre todos los activos.
     * El cálculo ya está pre-computado en SimilitudService; esta llamada es O(1).
     *
     * Respuesta: { "tickers": [...], "matriz": [[1.0, ...], ...] }
     */
    private void handleCorrelacion(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            enviarJson(exchange, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }

        SimilitudService.ResultadoCorrelacion resultado = servicio.getCorrelacionCompleta();
        if (resultado == null) {
            enviarJson(exchange, 503, "{\"error\":\"Matriz de correlación no disponible\"}");
            return;
        }

        System.out.println("[SimilitudServer] /correlacion → " + resultado.tickers.length + " tickers");
        enviarJson(exchange, 200, gson.toJson(resultado));
    }

    /**
     * GET /ohlc?ticker=AAPL&dias=180
     * Devuelve los últimos N días de datos OHLC del ticker indicado.
     * El parámetro {@code dias} es opcional (default 180).
     *
     * Respuesta: { "ticker": "AAPL", "datos": [{fecha, open, high, low, close, volumen}, ...] }
     */
    private void handleOhlc(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            enviarJson(exchange, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }

        URI uri   = exchange.getRequestURI();
        String qs = uri.getQuery();

        String ticker = null;
        int    dias   = 180;

        if (qs != null) {
            for (String par : qs.split("&")) {
                String[] kv = par.split("=", 2);
                if (kv.length != 2) continue;
                switch (kv[0]) {
                    case "ticker": ticker = kv[1].toUpperCase().trim(); break;
                    case "dias":
                        try { dias = Integer.parseInt(kv[1].trim()); }
                        catch (NumberFormatException ignored) {}
                        break;
                }
            }
        }

        if (ticker == null || ticker.isEmpty()) {
            enviarJson(exchange, 400, "{\"error\":\"Parámetro 'ticker' requerido\"}");
            return;
        }

        SimilitudService.ResultadoOHLC resultado = servicio.getOHLC(ticker, dias);
        if (resultado == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Ticker no encontrado: " + ticker);
            enviarJson(exchange, 404, gson.toJson(err));
            return;
        }

        System.out.printf("[SimilitudServer] /ohlc?ticker=%s&dias=%d → %d registros%n",
                ticker, dias, resultado.datos.size());
        enviarJson(exchange, 200, gson.toJson(resultado));
    }

    // ─── UTILIDADES HTTP ─────────────────────────────────────────────────────────

    /**
     * Envía una respuesta JSON con las cabeceras correctas y CORS.
     *
     * @param exchange El intercambio HTTP.
     * @param codigo   Código de estado HTTP (200, 400, 404, etc.).
     * @param json     El cuerpo de la respuesta como String JSON.
     */
    private void enviarJson(HttpExchange exchange, int codigo, String json) throws IOException {
        byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);

        // Cabeceras de respuesta.
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // CORS: permite peticiones desde cualquier origen (necesario para Flask en otro puerto).
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        exchange.sendResponseHeaders(codigo, cuerpo.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(cuerpo);
        }
    }
}
