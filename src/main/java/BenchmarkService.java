import model.RegistroFinanciero;
import sorting.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BenchmarkService.java — Servicio de análisis de algoritmos de ordenamiento.
 *
 * Recibe el dataset maestro ya cargado (no descarga nada), ejecuta los 12
 * algoritmos de sorting sobre copias independientes del dataset y expone
 * los resultados como objetos Java listos para serializar a JSON.
 *
 * Al separar este servicio de Main, la lógica de benchmark es reutilizable
 * por cualquier servidor HTTP sin duplicar la descarga de datos.
 *
 * COMPLEJIDAD DEL BENCHMARK:
 *   O(12 × n log n) en el caso promedio, donde n = total de registros.
 *   Cada algoritmo recibe una copia fresca de la lista para que la medición
 *   sea justa (todos parten del mismo estado desordenado).
 */
public class BenchmarkService {

    // ─── Resultado de un solo algoritmo ──────────────────────────────────────
    public static class ResultadoAlgoritmo {
        public final String nombre;
        public final String complejidad;
        public final long   tiempoMs;
        public final int    tamano;

        public ResultadoAlgoritmo(String nombre, String complejidad, long tiempoMs, int tamano) {
            this.nombre      = nombre;
            this.complejidad = complejidad;
            this.tiempoMs    = tiempoMs;
            this.tamano      = tamano;
        }
    }

    // ─── Registro del top-15 de volumen ──────────────────────────────────────
    public static class RegistroVolumen {
        public final String activo;
        public final String fecha;
        public final long   volumen;
        public final double close;

        public RegistroVolumen(String activo, String fecha, long volumen, double close) {
            this.activo  = activo;
            this.fecha   = fecha;
            this.volumen = volumen;
            this.close   = close;
        }
    }

    // ─── Respuesta completa del endpoint /api/benchmark ──────────────────────
    public static class ResultadoBenchmark {
        public final List<ResultadoAlgoritmo> algoritmos;
        public final List<RegistroVolumen>    top15Volumen;
        public final int                      totalRegistros;

        public ResultadoBenchmark(List<ResultadoAlgoritmo> algoritmos,
                                  List<RegistroVolumen> top15Volumen,
                                  int totalRegistros) {
            this.algoritmos     = algoritmos;
            this.top15Volumen   = top15Volumen;
            this.totalRegistros = totalRegistros;
        }
    }

    // ─── Estado interno ───────────────────────────────────────────────────────
    private final ResultadoBenchmark resultado;

    /**
     * Constructor: recibe el dataset ya cargado y ejecuta el benchmark completo.
     * Llamar una sola vez al arrancar la aplicación.
     *
     * @param registros Lista maestra de registros del ETL.
     */
    @SuppressWarnings("unchecked")
    public BenchmarkService(List<RegistroFinanciero> registros) {
        System.out.println("[BenchmarkService] Iniciando benchmark con " + registros.size() + " registros...");

        // ── 12 algoritmos de sorting ───────────────────────────────────────────
        Sorter<RegistroFinanciero>[] algoritmos = new Sorter[]{
                new BinaryInsertionSortImpl(),
                new BitonicSortImpl(),
                new GnomeSortImpl(),
                new HeapSortImpl(),
                new QuickSortImpl(),
                new RadixSortImpl(),
                new SelectionSort(),
                new TimSortImpl<>(),
                new CombSortImpl<>(),
                new TreeSortImpl<>(),
                new PigeonholeSortImpl<RegistroFinanciero>(r -> (int) r.getFecha().toEpochDay()),
                new BucketSortImpl<RegistroFinanciero>(r -> r.getClose())
        };

        String[] nombres = {
                "BinaryInsertionSort", "BitonicSort", "GnomeSort", "HeapSort",
                "QuickSort", "RadixSort", "SelectionSort", "TimSort",
                "CombSort", "TreeSort", "PigeonholeSort", "BucketSort"
        };

        String[] complejidades = {
                "O(n²)", "O(n log² n)", "O(n²)", "O(n log n)",
                "O(n log n)", "O(nk)", "O(n²)", "O(n log n)",
                "O(n² / 2^p)", "O(n log n)", "O(n + Rango)", "O(n + k)"
        };

        List<ResultadoAlgoritmo> resultados = new ArrayList<>();

        for (int i = 0; i < algoritmos.length; i++) {
            List<RegistroFinanciero> copia = new ArrayList<>(registros);
            long inicio = System.currentTimeMillis();
            try {
                algoritmos[i].sort(copia);
            } catch (Exception e) {
                System.err.println("[BenchmarkService] Error en " + nombres[i] + ": " + e.getMessage());
            }
            long tiempo = System.currentTimeMillis() - inicio;
            resultados.add(new ResultadoAlgoritmo(nombres[i], complejidades[i], tiempo, registros.size()));
            System.out.printf("  %-22s → %5d ms%n", nombres[i], tiempo);
        }

        // ── Top-15 por volumen ─────────────────────────────────────────────────
        List<RegistroFinanciero> copiaVol = new ArrayList<>(registros);
        copiaVol.sort((a, b) -> Double.compare(b.getVolumen(), a.getVolumen()));

        List<RegistroVolumen> top15 = new ArrayList<>();
        int limite = Math.min(15, copiaVol.size());
        for (int i = 0; i < limite; i++) {
            RegistroFinanciero r = copiaVol.get(i);
            top15.add(new RegistroVolumen(
                    r.getActivo(),
                    r.getFecha().toString(),
                    r.getVolumen(),
                    r.getClose()
            ));
        }

        System.out.println("[BenchmarkService] Benchmark completado.");
        this.resultado = new ResultadoBenchmark(resultados, top15, registros.size());
    }

    /** Devuelve el resultado completo pre-calculado. Llamada O(1). */
    public ResultadoBenchmark getResultado() {
        return resultado;
    }
}