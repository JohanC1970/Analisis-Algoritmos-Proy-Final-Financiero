import etl.ApiClient;
import etl.DataParser;
import model.RegistroFinanciero;

import java.util.List;

/**
 * Punto de entrada del programa.
 *
 * Orquesta el pipeline ETL completo:
 *   1. EXTRAER  → Descarga el JSON histórico desde Yahoo Finance (ApiClient)
 *   2. TRANSFORMAR → Convierte el JSON en objetos Java limpios (DataParser)
 *   3. VERIFICAR → Muestra los primeros registros para confirmar que todo funciona
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Iniciando el proceso ETL para el proyecto financiero...\n");

        // Instanciamos las dos clases del pipeline ETL
        ApiClient cliente = new ApiClient();
        DataParser parser  = new DataParser();

        // Ticker del activo a analizar. VOO es el ETF que replica el índice S&P 500.
        // Se puede cambiar por cualquier símbolo válido de Yahoo Finance (ej: "AAPL", "MSFT")
        String ticker = "VOO";

        // --- PASO 1: EXTRAER ---
        // Descargamos el JSON crudo con 5 años de datos históricos diarios
        System.out.println("Paso 1: Descargando datos de " + ticker + "...");
        String jsonRespuesta = cliente.descargarDatosHistoricos(ticker);

        if (jsonRespuesta != null) {

            // --- PASO 2: TRANSFORMAR ---
            // Convertimos el JSON en una lista de objetos RegistroFinanciero
            System.out.println("Paso 2: Parseando el JSON a objetos Java...");
            List<RegistroFinanciero> registros = parser.parsearYahooJson(jsonRespuesta, ticker);

            // --- PASO 3: VERIFICAR ---
            if (!registros.isEmpty()) {
                System.out.println("\n¡Éxito! Se obtuvieron " + registros.size() + " registros históricos.");
                System.out.println("Mostrando los primeros 5 registros convertidos:\n");

                // Imprimimos solo los primeros 5 como muestra de que el pipeline funciona
                for (int i = 0; i < Math.min(5, registros.size()); i++) {
                    System.out.println("   " + registros.get(i).toString());
                }

                System.out.println("\n¡La tubería de datos funciona correctamente! Ya tienes la materia prima en memoria.");
            } else {
                System.out.println("La lista de registros está vacía. Revisa el JSON o el Parser.");
            }
        }
    }
}
