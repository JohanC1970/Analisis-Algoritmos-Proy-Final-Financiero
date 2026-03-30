import etl.ApiClient;
import etl.DataParser;
import model.RegistroFinanciero;
import viz.PythonBridge; // ¡NUEVO! Importamos el puente

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String[] TICKERS = {
            "VOO", "AAPL", "MSFT", "GOOGL", "AMZN",
            "TSLA", "META", "NVDA", "SPY", "QQQ",
            "JPM", "V", "WMT", "JNJ", "PG",
            "MA", "UNH", "HD", "BAC", "DIS"
    };
    private static final String DATA_DIR  = "data/";

    public static void main(String[] args) {
        System.out.println("=== Iniciando pipeline ETL ===\n");

        boolean creada = new File(DATA_DIR).mkdirs();
        if (creada) System.out.println("Carpeta data/ creada.");

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
            System.err.println("❌ No se obtuvieron datos. Verifica tu conexión a internet.");
            return;
        }

        System.out.println("\nTotal registros obtenidos: " + todos.size() + "\n");

        // PASO 2: Generar los CSV
        generarTop15VolumenCSV(todos); // ¡NUEVO! Llamamos al método corregido
        generarBenchmarkCSV(todos);

        System.out.println("\n=== ¡Pipeline Java completado! ===");

        // ¡NUEVO! PASO 3: Llamar automáticamente a Python
        PythonBridge bridge = new PythonBridge();
        boolean exito = bridge.ejecutarVisualizacion(DATA_DIR + "benchmark.csv", DATA_DIR + "volumen.csv");

        if(exito) {
            System.out.println("🎉 ¡Todo el proyecto finalizó con éxito! Revisa la carpeta 'output/'");
        } else {
            System.out.println("⚠️ Hubo un error al generar las gráficas.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ¡NUEVO! Método corregido para extraer solo el TOP 15
    // ─────────────────────────────────────────────────────────────
    private static void generarTop15VolumenCSV(List<RegistroFinanciero> registros) {
        // Hacemos una copia para no alterar el orden de la lista original
        List<RegistroFinanciero> copiaVolumen = new ArrayList<>(registros);

        // Usamos el sort nativo de Java SOLO para preparar este reporte (es válido porque no es el benchmark)
        copiaVolumen.sort((a, b) -> Double.compare(b.getVolumen(), a.getVolumen()));

        try (FileWriter fw = new FileWriter(DATA_DIR + "volumen.csv")) {
            fw.write("activo,fecha,volumen,close\n"); // Añadimos close porque Python lo espera

            // Tomamos solo los primeros 15
            int limite = Math.min(15, copiaVolumen.size());
            for (int i = 0; i < limite; i++) {
                RegistroFinanciero r = copiaVolumen.get(i);
                fw.write(r.getActivo() + "," + r.getFecha() + "," + r.getVolumen() + "," + r.getClose() + "\n");
            }
            System.out.println("✅ volumen.csv (Top 15) generado");
        } catch (IOException e) {
            System.err.println("❌ Error generando volumen.csv: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // benchmark.csv → algoritmo, tiempo_ms, complejidad
    // ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static void generarBenchmarkCSV(List<RegistroFinanciero> registros) {

        // ¡NUEVO! Añadidos los 12 algoritmos completos
        sorting.Sorter<RegistroFinanciero>[] algoritmos = new sorting.Sorter[]{
                new sorting.BinaryInsertionSortImpl(),
                new sorting.BitonicSortImpl(),
                new sorting.GnomeSortImpl(),
                new sorting.HeapSortImpl(),
                new sorting.QuickSortImpl(),
                new sorting.RadixSortImpl(),
                new sorting.SelectionSort(),
                new sorting.TimSortImpl<>(), // Los que hicimos nosotros
                new sorting.CombSortImpl<>(),
                new sorting.TreeSortImpl<>(),
                new sorting.PigeonholeSortImpl<RegistroFinanciero>(r -> (int) r.getFecha().toEpochDay()),
                new sorting.BucketSortImpl<RegistroFinanciero>(r -> r.getClose())
        };

        String[] nombres = {
                "BinaryInsertionSort", "BitonicSort", "GnomeSort", "HeapSort",
                "QuickSort", "RadixSort", "SelectionSort", "TimSort",
                "CombSort", "TreeSort", "PigeonholeSort", "BucketSort"
        };

        String[] complejidades = {
                "O(n log n)", "O(n log² n)", "O(n²)", "O(n log n)",
                "O(n log n)", "O(nk)", "O(n²)", "O(n log n)",
                "O(n² / 2^p)", "O(n log n)", "O(n + Rango)", "O(n + k)"
        };

        try (FileWriter fw = new FileWriter(DATA_DIR + "benchmark.csv")) {
            fw.write("algoritmo,tiempo_ms,complejidad\n");

            for (int i = 0; i < algoritmos.length; i++) {
                List<RegistroFinanciero> copia = new ArrayList<>(registros);
                long inicio = System.currentTimeMillis();

                try {
                    algoritmos[i].sort(copia);
                } catch (Exception e) {
                    System.err.println("  ⚠️ Error en " + nombres[i] + ": " + e.getMessage());
                }

                long tiempo = System.currentTimeMillis() - inicio;
                fw.write(nombres[i] + "," + tiempo + "," + complejidades[i] + "\n");
                System.out.println("  " + nombres[i] + " → " + tiempo + " ms");
            }

            System.out.println("✅ benchmark.csv generado");

        } catch (IOException e) {
            System.err.println("❌ Error generando benchmark.csv: " + e.getMessage());
        }
    }
}