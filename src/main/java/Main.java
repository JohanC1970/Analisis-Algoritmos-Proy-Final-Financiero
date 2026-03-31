import etl.ApiClient;
import etl.DataParser;
import model.RegistroFinanciero;
import viz.PythonBridge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Main.java - Punto de entrada y orquestador del pipeline completo.
 *
 * Este archivo conecta todas las piezas del proyecto en orden:
 *   1. Descarga datos historicos de 20 activos financieros desde Yahoo Finance.
 *   2. Los transforma en objetos Java (RegistroFinanciero).
 *   3. Genera dos archivos CSV: uno con el top 15 de volumen y otro con los
 *      tiempos de ejecucion de los 12 algoritmos de ordenamiento (benchmark).
 *   4. Llama automaticamente al script de Python para generar las graficas.
 *   5. Abre las imagenes resultantes en el visor del sistema operativo.
 *
 * Es importante entender que Main no hace logica de negocio por si solo:
 * delega cada responsabilidad a la clase que corresponde y solo coordina el flujo.
 */
public class Main {

    /*
     * Lista de los 20 tickers que vamos a analizar.
     * Son activos del mercado estadounidense: ETFs (VOO, SPY, QQQ),
     * tecnologicas (AAPL, MSFT, GOOGL, NVDA), financieras (JPM, V, BAC), etc.
     * Cambiar o ampliar esta lista es suficiente para que todo el pipeline
     * procese los nuevos activos automaticamente.
     */
    private static final String[] TICKERS = {
            "VOO", "AAPL", "MSFT", "GOOGL", "AMZN",
            "TSLA", "META", "NVDA", "SPY", "QQQ",
            "JPM", "V", "WMT", "JNJ", "PG",
            "MA", "UNH", "HD", "BAC", "DIS"
    };

    // Carpeta donde se guardan los CSV generados. Se crea si no existe.
    private static final String DATA_DIR = "data/";

    public static void main(String[] args) {
        System.out.println("=== Iniciando pipeline ETL ===\n");

        // Nos aseguramos de que la carpeta data/ exista antes de intentar escribir en ella.
        // mkdirs() crea la carpeta y cualquier carpeta padre que falte. Devuelve true si la creo.
        boolean creada = new File(DATA_DIR).mkdirs();
        if (creada) System.out.println("Carpeta data/ creada.");

        ApiClient cliente = new ApiClient();
        DataParser parser  = new DataParser();

        /*
         * "todos" es la lista acumuladora central del pipeline.
         * A medida que procesamos cada ticker, sus registros se van agregando aqui.
         * Al final, esta lista contiene todos los dias de cotizacion de los 20 activos
         * mezclados, lo que nos da un dataset grande y variado para el benchmark.
         */
        List<RegistroFinanciero> todos = new ArrayList<>();

        // Iteramos sobre cada ticker, descargamos su JSON y lo parseamos.
        // Si la descarga falla (null), simplemente lo saltamos y seguimos con el siguiente.
        for (String ticker : TICKERS) {
            System.out.println("Descargando: " + ticker);
            String json = cliente.descargarDatosHistoricos(ticker);
            if (json != null) {
                todos.addAll(parser.parsearYahooJson(json, ticker));
            }
        }

        // Si la lista quedo vacia, no tiene sentido continuar. Probablemente hay un problema de red.
        if (todos.isEmpty()) {
            System.err.println("No se obtuvieron datos. Verifica tu conexion a internet.");
            return;
        }

        System.out.println("\nTotal registros obtenidos: " + todos.size() + "\n");

        // PASO 2: Generar los archivos CSV que Python va a leer para graficar.
        generarTop15VolumenCSV(todos);
        generarBenchmarkCSV(todos);

        System.out.println("\n=== Pipeline Java completado ===");

        // PASO 3: Invocar el script de Python desde Java usando PythonBridge.
        // Esto es lo que cierra el ciclo del proyecto: Java procesa, Python visualiza.
        PythonBridge bridge = new PythonBridge();
        boolean exito = bridge.ejecutarVisualizacion(DATA_DIR + "benchmark.csv", DATA_DIR + "volumen.csv");

        // Intentamos abrir las imagenes generadas directamente en el visor del SO.
        System.out.println("Abriendo graficas automaticamente...");
        abrirImagen("output/benchmark_algoritmos.png");
        abrirImagen("output/top15_volumen.png");
        abrirImagen("output/benchmark_algoritmos_zoom.png");

        System.out.println("=== Pipeline completo ===");

        if (exito) {
            System.out.println("Todo el proyecto finalizo con exito. Revisa la carpeta 'output/'");
        } else {
            System.out.println("Hubo un error al generar las graficas.");
        }
    }

    /*
     * Genera el archivo volumen.csv con los 15 registros de mayor volumen negociado
     * de todo el dataset combinado.
     *
     * Por que solo 15: la grafica de barras en Python se vuelve ilegible con mas entradas.
     * Por que una copia: ordenamos por volumen solo para este reporte. Si ordenaramos
     * la lista original "todos", alterariamos el orden de entrada al benchmark y los
     * resultados de los algoritmos ya no serian comparables entre si.
     *
     * Formato del CSV generado:
     *   activo,fecha,volumen,close
     *   SPY,2020-03-20,1234567890,274.5
     *   ...
     */
    private static void generarTop15VolumenCSV(List<RegistroFinanciero> registros) {

        // Creamos una copia superficial de la lista para poder ordenarla sin tocar el original.
        // new ArrayList<>(registros) copia las referencias, no los objetos, lo cual es suficiente aqui.
        List<RegistroFinanciero> copiaVolumen = new ArrayList<>(registros);

        // Ordenamos de mayor a menor volumen usando un comparador lambda.
        // Nota: este sort es el de la libreria estandar de Java, NO es parte del benchmark.
        // El benchmark mide nuestros propios algoritmos implementados, no este.
        copiaVolumen.sort((a, b) -> Double.compare(b.getVolumen(), a.getVolumen()));

        try (FileWriter fw = new FileWriter(DATA_DIR + "volumen.csv")) {
            fw.write("activo,fecha,volumen,close\n");

            // Math.min nos protege si por alguna razon el dataset tiene menos de 15 registros.
            int limite = Math.min(15, copiaVolumen.size());
            for (int i = 0; i < limite; i++) {
                RegistroFinanciero r = copiaVolumen.get(i);
                fw.write(r.getActivo() + "," + r.getFecha() + "," + r.getVolumen() + "," + r.getClose() + "\n");
            }
            System.out.println("volumen.csv (Top 15) generado");

        } catch (IOException e) {
            System.err.println("Error generando volumen.csv: " + e.getMessage());
        }
    }

    /*
     * Genera el archivo benchmark.csv midiendo el tiempo de ejecucion de cada
     * uno de los 12 algoritmos de ordenamiento implementados en el proyecto.
     *
     * La idea del benchmark es simple: le damos a cada algoritmo exactamente
     * la misma lista (una copia fresca cada vez) y medimos cuanto tarda en ordenarla.
     * Asi la comparacion es justa porque todos parten del mismo estado desordenado.
     *
     * Formato del CSV generado:
     *   algoritmo,tiempo_ms,complejidad
     *   QuickSort,45,O(n log n)
     *   SelectionSort,3200,O(n^2)
     *   ...
     */
    @SuppressWarnings("unchecked")
    private static void generarBenchmarkCSV(List<RegistroFinanciero> registros) {

        /*
         * Instanciamos los 12 algoritmos. Todos implementan la interfaz sorting.Sorter<T>,
         * lo que nos permite tratarlos de forma uniforme en el bucle de abajo.
         * Algunos constructores reciben una funcion (lambda) que extrae la clave numerica
         * necesaria para algoritmos no comparativos como PigeonholeSort y BucketSort.
         */
        sorting.Sorter<RegistroFinanciero>[] algoritmos = new sorting.Sorter[]{
                new sorting.BinaryInsertionSortImpl(),
                new sorting.BitonicSortImpl(),
                new sorting.GnomeSortImpl(),
                new sorting.HeapSortImpl(),
                new sorting.QuickSortImpl(),
                new sorting.RadixSortImpl(),
                new sorting.SelectionSort(),
                new sorting.TimSortImpl<>(),
                new sorting.CombSortImpl<>(),
                new sorting.TreeSortImpl<>(),
                // PigeonholeSort necesita saber como convertir un registro a un entero (su "cubo").
                // Usamos la fecha convertida a dias desde 1970 (toEpochDay) como clave entera.
                new sorting.PigeonholeSortImpl<RegistroFinanciero>(r -> (int) r.getFecha().toEpochDay()),
                // BucketSort necesita una clave de tipo double para distribuir en cubetas.
                // Usamos el precio de cierre como criterio de distribucion.
                new sorting.BucketSortImpl<RegistroFinanciero>(r -> r.getClose())
        };

        // Nombres legibles para el CSV. El orden debe coincidir exactamente con el array de arriba.
        String[] nombres = {
                "BinaryInsertionSort", "BitonicSort", "GnomeSort", "HeapSort",
                "QuickSort", "RadixSort", "SelectionSort", "TimSort",
                "CombSort", "TreeSort", "PigeonholeSort", "BucketSort"
        };

        // Complejidad teorica de cada algoritmo en notacion Big-O.
        // Estos valores son los que aparecen en la grafica de Python para contextualizar los tiempos.
        String[] complejidades = {
                "O(n^2)", "O(n log^2 n)", "O(n^2)", "O(n log n)",
                "O(n log n)", "O(nk)", "O(n^2)", "O(n log n)",
                "O(n^2 / 2^p)", "O(n log n)", "O(n + Rango)", "O(n + k)"
        };

        try (FileWriter fw = new FileWriter(DATA_DIR + "benchmark.csv")) {
            fw.write("algoritmo,tiempo_ms,complejidad\n");

            for (int i = 0; i < algoritmos.length; i++) {

                // Creamos una copia fresca de la lista para cada algoritmo.
                // Si reutilizaramos la misma lista, el segundo algoritmo recibiria
                // una lista ya ordenada, lo que falsificaria completamente la medicion.
                List<RegistroFinanciero> copia = new ArrayList<>(registros);

                // Tomamos el tiempo justo antes y justo despues de ordenar.
                // System.currentTimeMillis() tiene precision de milisegundos, suficiente para este caso.
                long inicio = System.currentTimeMillis();

                try {
                    algoritmos[i].sort(copia);
                } catch (Exception e) {
                    // Si un algoritmo falla (por ejemplo, por un caso borde), lo registramos
                    // pero no detenemos el benchmark. Los demas algoritmos siguen ejecutandose.
                    System.err.println("Error en " + nombres[i] + ": " + e.getMessage());
                }

                long tiempo = System.currentTimeMillis() - inicio;

                // Escribimos la fila en el CSV: nombre, tiempo medido, complejidad teorica.
                fw.write(nombres[i] + "," + tiempo + "," + complejidades[i] + "\n");
                System.out.println("  " + nombres[i] + " -> " + tiempo + " ms");
            }

            System.out.println("benchmark.csv generado");

        } catch (IOException e) {
            System.err.println("Error generando benchmark.csv: " + e.getMessage());
        }
    }

    /*
     * Intenta abrir un archivo de imagen usando el visor predeterminado del sistema operativo.
     *
     * java.awt.Desktop es la clase de Java que permite interactuar con el escritorio del SO:
     * abrir archivos, URLs, etc. No todos los entornos lo soportan (por ejemplo, servidores
     * sin interfaz grafica), por eso verificamos con isDesktopSupported() antes de intentarlo.
     *
     * Si el archivo no existe o el entorno no soporta Desktop, simplemente lo informamos
     * en consola sin lanzar una excepcion que detenga el programa.
     */
    private static void abrirImagen(String ruta) {
        try {
            java.io.File archivo = new java.io.File(ruta);
            if (archivo.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(archivo);
            } else {
                System.out.println("No se pudo abrir automaticamente: " + ruta);
            }
        } catch (Exception e) {
            System.err.println("Error al intentar abrir la imagen: " + e.getMessage());
        }
    }
}
