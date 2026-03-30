package etl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.RegistroFinanciero;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * DataParser se encarga de transformar el JSON crudo de Yahoo Finance
 * en una lista de objetos Java listos para usar.
 *
 * Yahoo Finance devuelve los datos en arrays paralelos: un array de fechas,
 * un array de precios de apertura, uno de cierre, etc. Todos del mismo tamaño
 * y con la misma posición correspondiente al mismo día.
 *
 * Este parser navega esa estructura, limpia los datos inválidos (nulos)
 * aplicando la técnica de eliminación (Listwise Deletion) para mantener
 * la veracidad de los datos financieros, y construye un objeto 
 * RegistroFinanciero por cada día de mercado.
 */
public class DataParser {

    /**
     * Parsea el JSON de Yahoo Finance y lo convierte en una lista de registros financieros.
     *
     * El proceso tiene 3 etapas:
     * 1. Navegar la estructura JSON hasta llegar a los arrays de datos.
     * 2. Recorrer los arrays en paralelo, día por día.
     * 3. Limpiar registros inválidos (nulos) documentando el proceso y construir los objetos.
     *
     * @param jsonCrudo El String JSON tal como lo devuelve Yahoo Finance.
     * @param ticker    El símbolo del activo (ej: "VOO") para asignarlo a cada registro.
     * @return Lista de RegistroFinanciero con los datos limpios y listos para procesar.
     * Devuelve una lista vacía si el JSON es inválido o no contiene datos.
     */
    public List<RegistroFinanciero> parsearYahooJson(String jsonCrudo, String ticker) {

        List<RegistroFinanciero> registros = new ArrayList<>();

        try {
            // --- ETAPA 1: Navegación por la estructura JSON ---

            // Convertimos el String JSON en un objeto navegable con Gson
            JsonObject root = JsonParser.parseString(jsonCrudo).getAsJsonObject();

            // Bajamos nivel por nivel hasta llegar a los datos:
            // root → "chart" → "result" (array) → primer elemento → datos
            JsonObject chart = root.getAsJsonObject("chart");
            JsonArray result = chart.getAsJsonArray("result");

            // Si Yahoo no devolvió resultados (ticker inválido, mercado cerrado, etc.), retornamos vacío
            if (result == null || result.isEmpty()) return registros;

            // Yahoo siempre devuelve un solo elemento en "result" para consultas de un ticker
            JsonObject firstResult = result.get(0).getAsJsonObject();

            // El array "timestamp" contiene las fechas en formato Unix (segundos)
            JsonArray timestamps = firstResult.getAsJsonArray("timestamp");

            // Los precios están dentro de "indicators" → "quote" → primer elemento
            JsonObject indicators = firstResult.getAsJsonObject("indicators");
            JsonArray quoteArray = indicators.getAsJsonArray("quote");
            JsonObject quote = quoteArray.get(0).getAsJsonObject();

            // Extraemos cada array de precios. Todos tienen el mismo tamaño que "timestamps"
            JsonArray opens   = quote.getAsJsonArray("open");    // Precio de apertura del día
            JsonArray closes  = quote.getAsJsonArray("close");   // Precio de cierre del día
            JsonArray highs   = quote.getAsJsonArray("high");    // Precio máximo del día
            JsonArray lows    = quote.getAsJsonArray("low");     // Precio mínimo del día
            JsonArray volumes = quote.getAsJsonArray("volume");  // Volumen de acciones negociadas

            // --- ETAPA 2 y 3: Recorrido, limpieza y construcción de objetos ---
            
            int registrosDescartados = 0; // NUEVO: Contador para documentar la limpieza de datos

            for (int i = 0; i < timestamps.size(); i++) {

                // LIMPIEZA DE DATOS (Data Cleaning - Listwise Deletion):
                // Yahoo a veces incluye entradas nulas para días festivos o fallos de la API.
                // Justificación: Se opta por eliminar (descartar) el registro nulo en lugar 
                // de interpolar, para mantener la integridad estricta de los datos financieros.
                // Inventar un volumen de transacciones alteraría el benchmark real.
                if (opens.get(i).isJsonNull() || closes.get(i).isJsonNull() || volumes.get(i).isJsonNull()) {
                    registrosDescartados++;
                    continue; // Descartamos la fila completa
                }

                // Convertimos el timestamp Unix (en segundos) a LocalDate.
                // Usamos la zona horaria de Nueva York porque es donde opera la NYSE/NASDAQ.
                long timestampSec = timestamps.get(i).getAsLong();
                LocalDate fecha = Instant.ofEpochSecond(timestampSec)
                        .atZone(ZoneId.of("America/New_York"))
                        .toLocalDate();

                // Extraemos los valores numéricos de cada array en la posición i
                double open   = opens.get(i).getAsDouble();
                double close  = closes.get(i).getAsDouble();
                double high   = highs.get(i).getAsDouble();
                double low    = lows.get(i).getAsDouble();
                long   volume = volumes.get(i).getAsLong();

                // Creamos el objeto con todos los datos del día y lo agregamos a la lista
                RegistroFinanciero registro = new RegistroFinanciero(ticker, fecha, open, close, high, low, volume);
                registros.add(registro);
            }

            // NUEVO: Informamos en consola los resultados de la limpieza
            if (registrosDescartados > 0) {
                System.out.println("  ⚙️ [Data Cleaning] Se eliminaron " + registrosDescartados + " registros incompletos/nulos.");
            }
            System.out.println("  ✅ Se procesaron " + registros.size() + " registros limpios para " + ticker);

        } catch (Exception e) {
            // Capturamos cualquier error inesperado: JSON malformado, campos faltantes, etc.
            System.err.println("❌ Error al parsear el JSON de " + ticker + ": " + e.getMessage());
        }

        return registros;
    }
}