package etl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ApiClient es el responsable de comunicarse con la API de Yahoo Finance.
 *
 * Su única tarea es hacer la petición HTTP y devolver el JSON crudo como texto.
 * No interpreta ni transforma los datos, eso lo hace DataParser.
 *
 * API utilizada: Yahoo Finance Chart API (no oficial, no requiere autenticación)
 * Endpoint base: https://query1.finance.yahoo.com/v8/finance/chart/{ticker}
 */
public class ApiClient {

    /**
     * Cliente HTTP compartido por todas las peticiones de esta clase.
     *
     * Se define como static para que solo exista una instancia durante toda la
     * ejecución del programa (patrón de recurso compartido). Usar HTTP/2 permite
     * conexiones más eficientes y reutilizables.
     */
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10)) // Si no responde en 10s, lanza excepción
            .build();

    /**
     * Descarga los datos históricos de precios de un activo financiero desde Yahoo Finance.
     *
     * Construye la URL con el ticker recibido y solicita los últimos 5 años de datos
     * con granularidad diaria (un registro por día de mercado abierto).
     *
     * @param ticker Símbolo bursátil del activo (ej: "VOO", "AAPL", "ECOPETROL.CL")
     * @return El cuerpo de la respuesta HTTP como String en formato JSON,
     *         o null si ocurrió un error de red o el servidor respondió con error.
     */
    public String descargarDatosHistoricos(String ticker) {

        // Construimos la URL dinámica con el ticker y los parámetros de consulta:
        // - range=5y → queremos los últimos 5 años de historia
        // - interval=1d → un punto de datos por día (granularidad diaria)
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker + "?range=5y&interval=1d";

        try {
            // Construimos la petición HTTP GET con un User-Agent de navegador.
            // Yahoo Finance bloquea peticiones sin User-Agent, por eso lo simulamos.
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            // Enviamos la petición y esperamos la respuesta completa como String
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Datos descargados exitosamente para: " + ticker);
                return response.body(); // Devolvemos el JSON crudo
            } else {
                // Cualquier código distinto de 200 (ejemplo: 404, 429, 500) se trata como error
                System.err.println("Error al descargar " + ticker + ". Código HTTP: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            // Capturamos errores de red: sin internet, timeout, URL inválida, etc.
            System.err.println("Excepción al conectar con la API para " + ticker + ": " + e.getMessage());
            return null;
        }
    }
}
