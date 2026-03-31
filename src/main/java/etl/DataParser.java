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

/*
 * DataParser.java - Transforma el JSON crudo de Yahoo Finance en objetos Java.
 *
 * Yahoo Finance no devuelve una lista de registros lista para procesar.
 * Devuelve arrays paralelos: un array con todas las fechas,
 * otro con todos los precios de apertura, otro con los de cierre, etc.
 * Todos tienen el mismo tamaño y la posicion i de cada array corresponde
 * al mismo dia de mercado.
 *
 * Este parser tiene tres responsabilidades:
 *   1. Navegar la estructura anidada del JSON hasta llegar a esos arrays.
 *   2. Limpiar los datos: Yahoo a veces incluye entradas nulas para dias
 *      donde no hubo operaciones. Esas filas se descartan.
 *   3. Construir un objeto RegistroFinanciero por cada dia valido.
 *
 * Libreria usada: Gson (Google).
 * Gson convierte el String JSON en objetos Java navegables (JsonObject, JsonArray).
 * Sin Gson tendriamos que parsear el JSON manualmente con indexOf y substring,
 * lo cual propondria un reto muy dificil y pueden surgir muchos errores.
 */
public class DataParser {

    /**
     * Parsea el JSON de Yahoo Finance y lo convierte en una lista de registros financieros.
     *
     * @param jsonCrudo El String JSON exactamente como lo devuelve Yahoo Finance.
     * @param ticker El simbolo del activo (ej: "VOO") para asignarlo a cada registro,
     * ya que el JSON no siempre lo incluye de forma explicita.
     * @return Lista de RegistroFinanciero con los datos limpios.
     * Devuelve una lista vacia si el JSON es invalido o no tiene datos.
     */
    public List<RegistroFinanciero> parsearYahooJson(String jsonCrudo, String ticker) {

        List<RegistroFinanciero> registros = new ArrayList<>();

        try {

            //1: Navegacion por la estructura JSON

            /*
             * JsonParser.parseString() es el punto de entrada de Gson.
             * Convierte el String JSON en un arbol de objetos navegables.
             * getAsJsonObject() le dice a Gson que el nivel raiz es un objeto { },
             * no un array [ ] ni un valor primitivo.
             */
            JsonObject root = JsonParser.parseString(jsonCrudo).getAsJsonObject();

            /*
             * La estructura del JSON de Yahoo Finance tiene varios niveles de anidamiento.
             * Tenemos que bajar nivel por nivel hasta llegar a los arrays de precios:
             *
             *   root
             *    └── "chart"          <- JsonObject
             *         └── "result"    <- JsonArray (siempre tiene un solo elemento para un ticker)
             *              └── [0]    <- JsonObject con los datos del ticker
             *                   ├── "timestamp"    <- array de fechas en Unix
             *                   └── "indicators"
             *                        └── "quote"   <- JsonArray (siempre tiene un solo elemento)
             *                             └── [0]  <- JsonObject con los arrays de precios
             */
            JsonObject chart      = root.getAsJsonObject("chart");
            JsonArray  result     = chart.getAsJsonArray("result");

            // Si Yahoo devolvio result: null o vacio, el ticker no existe o no tiene datos.
            if (result == null || result.isEmpty()) return registros;

            JsonObject firstResult = result.get(0).getAsJsonObject();
            JsonArray  timestamps  = firstResult.getAsJsonArray("timestamp");

            JsonObject indicators  = firstResult.getAsJsonObject("indicators");
            JsonArray  quoteArray  = indicators.getAsJsonArray("quote");
            JsonObject quote       = quoteArray.get(0).getAsJsonObject();

            // Extraemos los cinco arrays de datos. Todos tienen el mismo tamaño que timestamps.
            JsonArray opens   = quote.getAsJsonArray("open");    // Precio de apertura del dia
            JsonArray closes  = quote.getAsJsonArray("close");   // Precio de cierre del dia
            JsonArray highs   = quote.getAsJsonArray("high");    // Precio maximo del dia
            JsonArray lows    = quote.getAsJsonArray("low");     // Precio minimo del dia
            JsonArray volumes = quote.getAsJsonArray("volume");  // Unidades negociadas ese dia

            //2: Recorrido, limpieza y construccion de objetos

            // Contador para reportar cuantos registros se descartaron por estar incompletos.
            int registrosDescartados = 0;

            /*
             * Recorremos los arrays en paralelo usando el indice i.
             * La posicion i en timestamps corresponde a la misma posicion i en opens, closes, etc.
             * Es decir: timestamps[i], opens[i], closes[i]... son todos del mismo dia de mercado.
             */
            for (int i = 0; i < timestamps.size(); i++) {

                /*
                 * LIMPIEZA DE DATOS:
                 * Yahoo Finance a veces incluye entradas nulas en los arrays para dias
                 * donde el mercado estuvo cerrado o hubo un fallo en la recoleccion de datos.
                 * Si intentaramos leer getAsDouble() sobre un elemento nulo, Gson lanzaria
                 * una excepcion y perderiamos todos los registros restantes.
                 *
                 * Decidimos descartar la fila completa (en lugar de interpolar un valor)
                 * ya que no nos interesa inventar precios o volumenes, porque
                 * afectaria la veracidad del benchmark y del analisis.
                 */
                if (opens.get(i).isJsonNull() || closes.get(i).isJsonNull() || volumes.get(i).isJsonNull()) {
                    registrosDescartados++;
                    continue;
                }

                /*
                 * Conversion de timestamp Unix a LocalDate:
                 * Yahoo devuelve las fechas como numeros enteros (segundos desde 1970-01-01).
                 *
                 * El proceso de conversion es:
                 *   1. Instant.ofEpochSecond() convierte los segundos a un instante en el tiempo.
                 *   2. atZone() aplica la zona horaria de Nueva York (donde opera NYSE/NASDAQ).
                 *   Esto es importante porque sin zona horaria, el mismo timestamp podria
                 *   interpretarse como un dia diferente segun el servidor que ejecute el codigo.
                 *   3. toLocalDate() descarta la hora y nos queda solo la fecha (yyyy-MM-dd).
                 */
                long timestampSec = timestamps.get(i).getAsLong();
                LocalDate fecha = Instant.ofEpochSecond(timestampSec)
                        .atZone(ZoneId.of("America/New_York"))
                        .toLocalDate();

                // Extraemos los valores numericos de cada array en la posicion i del dia actual.
                double open   = opens.get(i).getAsDouble();
                double close  = closes.get(i).getAsDouble();
                double high   = highs.get(i).getAsDouble();
                double low    = lows.get(i).getAsDouble();
                long   volume = volumes.get(i).getAsLong();

                // Con todos los datos del dia listos, construimos el objeto y lo agregamos a la lista.
                RegistroFinanciero registro = new RegistroFinanciero(ticker, fecha, open, close, high, low, volume);
                registros.add(registro);
            }

            // Reportamos el resultado de la limpieza para que quede visible en la consola.
            if (registrosDescartados > 0) {
                System.out.println("  [Limpieza] Se descartaron " + registrosDescartados + " registros nulos para " + ticker);
            }
            System.out.println("  Se procesaron " + registros.size() + " registros limpios para " + ticker);

        } catch (Exception e) {
            // Capturamos cualquier error inesperado: JSON malformado, campo faltante en la estructura, etc.
            System.err.println("Error al parsear el JSON de " + ticker + ": " + e.getMessage());
        }

        return registros;
    }
}
