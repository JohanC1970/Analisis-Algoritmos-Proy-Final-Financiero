package etl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/*
 * ApiClient.java - Responsable de la comunicacion con Yahoo Finance.
 *
 * Esta clase hace una sola cosa: recibir un ticker (simbolo bursatil),
 * construir la URL correcta, hacer la peticion HTTP y devolver el JSON
 * crudo como texto. No interpreta ni transforma nada.
 *
 * La API que usamos es el endpoint publico de Yahoo Finance para graficas historicas.
 * No requiere autenticacion ni API key, pero si requiere simular un navegador
 * en los headers, de lo contrario Yahoo bloquea la peticion.
 *
 * Endpoint base:
 *   https://query1.finance.yahoo.com/v8/finance/chart/{ticker}?range=5y&interval=1d
 */
public class ApiClient {

    /**
     * El HttpClient es el objeto que gestiona las conexiones HTTP.
     * Lo declaramos como static y final por dos razones:
     *   1. static: que exista una sola instancia compartida por toda la clase,
     *      en lugar de crear un cliente nuevo por cada descarga (lo cual es costoso).
     *   2. final: que nadie pueda reemplazarlo accidentalmente despues de crearlo.
     *
     * Lo configuramos con HTTP/2 porque es mas eficiente que HTTP/1.1 para
     * multiples peticiones seguidas: reutiliza la misma conexion TCP en lugar
     * de abrir y cerrar una por cada ticker.
     *
     * El timeout de 10 segundos es un limite de seguridad: si Yahoo no responde
     * en ese tiempo, la peticion falla limpiamente en lugar de quedarse colgada
     * indefinidamente bloqueando el hilo principal.
     */
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Descarga los datos historicos de precios de un activo desde Yahoo Finance.
     *
     * Construye la URL con el ticker recibido y pide los ultimos 5 años de datos
     * con granularidad diaria. Cada dia de mercado abierto es un punto de datos.
     *
     * @param ticker Simbolo bursatil del activo. Ejemplos: "VOO", "AAPL", "MSFT".
     * Debe ser un simbolo valido reconocido por Yahoo Finance.
     * @return El cuerpo de la respuesta HTTP como String en formato JSON.
     * Devuelve null si hubo un error de red o el servidor respondio con error.
     */
    public String descargarDatosHistoricos(String ticker) {

        /*
         * Construimos la URL dinamicamente con el ticker y dos parametros clave:
         *   range=5y: queremos los ultimos 5 años de historia del activo.
         *   interval=1d: un punto de datos por dia de mercado (granularidad diaria).
         * Con 5 años de datos diarios obtenemos aproximadamente 1255 registros por ticker.
         */
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker + "?range=5y&interval=1d";

        try {
            /*
             * Construimos el objeto HttpRequest con el patron Builder.
             * El header "User-Agent: Mozilla/5.0" es obligatorio aqui:
             * Yahoo Finance detecta y bloquea peticiones que no incluyan un User-Agent
             * reconocible como navegador, devolviendo un error o directamente
             * un JSON de error.
             */
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            // Enviamos la peticion de forma sincrona (bloqueante) y esperamos la respuesta completa.
            // BodyHandlers.ofString() le dice al cliente que lea el cuerpo de la respuesta como texto.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Datos descargados exitosamente para: " + ticker);
                // Devolvemos el JSON crudo sin modificarlo.
                return response.body();
            } else {
                // Codigos como 404 (ticker no encontrado), 429 (limite de peticiones),
                // o 500 (error del servidor) llegan aqui. Los reportamos y devolvemos null.
                System.err.println("Error al descargar " + ticker + ". Codigo HTTP: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            // Este bloque captura problemas de red: sin conexion a internet, timeout superado, URL mal formada.
            System.err.println("Excepcion al conectar con la API para " + ticker + ": " + e.getMessage());
            return null;
        }
    }
}
