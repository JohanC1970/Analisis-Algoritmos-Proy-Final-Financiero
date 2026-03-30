import etl.ApiClient;
import etl.DataParser;
import model.RegistroFinanciero;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String[] TICKERS = {"VOO", "AAPL", "MSFT"};
    private static final String DATA_DIR  = "data/";

    public static void main(String[] args) {
        System.out.println("=== Iniciando pipeline ETL ===\n");

        // Crear carpeta data/ si no existe
        boolean creada = new File(DATA_DIR).mkdirs();
        if (creada) System.out.println("Carpeta data/ creada.");

        ApiClient cliente = new ApiClient();
        DataParser parser  = new DataParser();
        List<RegistroFinanciero> todos = new ArrayList<>();

        // PASO 1: Descargar y parsear datos de cada ticker
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
        generarVolumenCSV(todos);
        generarBenchmarkCSV(todos);

        System.out.println("\n=== ¡Pipeline completado! ===");
        System.out.println("Ahora ejecuta en la terminal:");
        System.out.println("  cd python_viz");
        System.out.println("  python visualizacion.py ..\\data\\benchmark.csv ..\\data\\volumen.csv");
    }

    // ─────────────────────────────────────────────────────────────
    // volumen.csv → activo, fecha, volumen
    // ─────────────────────────────────────────────────────────────
    private static void generarVolumenCSV(List<RegistroFinanciero> registros) {
        try (FileWriter fw = new FileWriter(DATA_DIR + "volumen.csv")) {
            fw.write("activo,fecha,volumen\n");
            for (RegistroFinanciero r : registros) {
                fw.write(r.getActivo() + "," + r.getFecha() + "," + r.getVolumen() + "\n");
            }
            System.out.println("✅ volumen.csv generado (" + registros.size() + " registros)");
        } catch (IOException e) {
            System.err.println("❌ Error generando volumen.csv: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // benchmark.csv → algoritmo, tiempo_ms, complejidad
    // ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static void generarBenchmarkCSV(List<RegistroFinanciero> registros) {

        sorting.Sorter<RegistroFinanciero>[] algoritmos = new sorting.Sorter[]{
                new sorting.BinaryInsertionSortImpl(),
                new sorting.BitonicSortImpl(),
                new sorting.GnomeSortImpl(),
                new sorting.HeapSortImpl(),
                new sorting.QuickSortImpl(),
                new sorting.RadixSortImpl(),
                new sorting.SelectionSort()
        };

        String[] nombres = {
                "BinaryInsertionSort",
                "BitonicSort",
                "GnomeSort",
                "HeapSort",
                "QuickSort",
                "RadixSort",
                "SelectionSort"
        };

        String[] complejidades = {
                "O(n log n)",
                "O(n log² n)",
                "O(n²)",
                "O(n log n)",
                "O(n log n)",
                "O(nk)",
                "O(n²)"
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