package ventana;

import model.RegistroFinanciero;
import java.util.*;

/*
 * ClasificadorRiesgo.java - Orquestador del análisis de riesgo por activo.
 *
 * Responsabilidades de ESTA clase (lo que NO delega):
 *   1. Agrupar los registros del ETL por ticker.
 *   2. Ordenar cada grupo cronológicamente.
 *   3. Ejecutar el AnalizadorVentanaDeslizante (dos patrones) sobre cada serie.
 *   4. Instanciar MetricasActivo — que calcula sus propias métricas internamente.
 *   5. Ordenar la lista resultante de menor a mayor riesgo.
 *   6. Imprimir el ranking en consola para verificación.
 *
 * Responsabilidades de MetricasActivo (lo que SÍ delega):
 *   - Cálculo de retornos diarios:   r_t = (close_t − close_{t-1}) / close_{t-1}
 *   - Media aritmética:              μ   = (1/n) × Σ r_t
 *   - Varianza poblacional:          σ²  = (1/n) × Σ (r_t − μ)²
 *   - Desviación estándar diaria:    σ_d = √σ²
 *   - Volatilidad histórica anual:   σ_a = σ_d × √252
 *   - Sharpe aproximado:             (μ × 252) / σ_a
 *   - Clasificación de riesgo:       CategoriaRiesgo.clasificar(σ_a)
 *
 * ═══════════════════════════════════════════════════════════════════════
 * COMPLEJIDAD TOTAL
 * ═══════════════════════════════════════════════════════════════════════
 *   Por activo: O(n) — MetricasActivo hace dos pasadas lineales.
 *   Análisis de patrones por activo: O(n × w × p).
 *   Total global: O(T × n) donde T = nº tickers, n = días por activo.
 */
public class ClasificadorRiesgo {

    private final AnalizadorVentanaDeslizante analizador;

    public ClasificadorRiesgo() {
        // Registramos los dos patrones definidos en el requerimiento.
        List<PatronVentana> patrones = Arrays.asList(
                new PatronRachaAlcista(3),   // Patrón 1: racha alcista ≥3 días consecutivos.
                new PatronCompresionRango()  // Patrón 2: compresión de rango intradiario ≥30%.
        );
        this.analizador = new AnalizadorVentanaDeslizante(patrones);
    }

    /**
     * Procesa todos los registros del ETL y devuelve la lista de activos
     * ordenada de menor a mayor riesgo (volatilidad anualizada ascendente).
     *
     * Cada MetricasActivo en la lista calculó sus propias métricas de dispersión
     * internamente en su constructor.
     *
     * @param registros Lista maestra con todos los RegistroFinanciero del ETL.
     * @return Lista de MetricasActivo ordenada por volatilidadAnual ascendente.
     */
    public List<MetricasActivo> clasificarYOrdenar(List<RegistroFinanciero> registros) {

        // PASO 1: Agrupar por ticker y ordenar cronológicamente.
        Map<String, List<RegistroFinanciero>> porTicker = agruparPorTicker(registros);

        // PASO 2: Crear MetricasActivo para cada activo.
        //         Cada objeto calcula sus propias métricas de dispersión al construirse.
        List<MetricasActivo> listaMetricas = new ArrayList<>();

        for (Map.Entry<String, List<RegistroFinanciero>> entrada : porTicker.entrySet()) {
            String ticker = entrada.getKey();
            List<RegistroFinanciero> serie = entrada.getValue();

            if (serie.size() < 2) {
                System.out.println("[ClasificadorRiesgo] OMITIDO " + ticker +
                        " — menos de 2 días de datos.");
                continue;
            }

            // PASO 2a: Análisis de patrones con ventana deslizante (sobre precios).
            List<ResultadoPatron> patronesDetectados = analizador.analizar(serie);

            // PASO 2b: Construir MetricasActivo — calcula sus métricas internamente.
            MetricasActivo m = new MetricasActivo(ticker, serie, patronesDetectados);
            listaMetricas.add(m);
        }

        // PASO 3: Ordenar por scoreRiesgo = volatilidadAnual, ascendente.
        listaMetricas.sort(Comparator.comparingDouble(m -> m.scoreRiesgo));

        // PASO 4: Imprimir ranking en consola para verificación.
        imprimirRanking(listaMetricas);

        return listaMetricas;
    }

    // ─── MÉTODOS PRIVADOS ─────────────────────────────────────────────────────

    /**
     * Agrupa los registros por ticker en un TreeMap (orden alfabético)
     * y ordena cada grupo cronológicamente por fecha.
     *
     * El TreeMap garantiza orden determinista cuando dos activos tienen
     * la misma volatilidad (desempate alfabético).
     */
    private Map<String, List<RegistroFinanciero>> agruparPorTicker(
            List<RegistroFinanciero> registros) {

        Map<String, List<RegistroFinanciero>> mapa = new TreeMap<>();

        for (RegistroFinanciero r : registros) {
            mapa.computeIfAbsent(r.getActivo(), k -> new ArrayList<>()).add(r);
        }

        // Ordenar cada grupo por fecha ascendente (necesario para calcular r_t correctamente).
        for (List<RegistroFinanciero> lista : mapa.values()) {
            lista.sort(Comparator.comparing(RegistroFinanciero::getFecha));
        }

        return mapa;
    }

    /**
     * Imprime el ranking de riesgo en la consola con formato tabular.
     * Útil para verificar que el cálculo es correcto antes de arrancar el servidor.
     */
    private void imprimirRanking(List<MetricasActivo> listaMetricas) {
        System.out.println("\n[ClasificadorRiesgo] ══ RANKING DE RIESGO (menor → mayor) ════════");
        System.out.printf("  %-6s  %-13s  %-11s  %-9s  %-9s  %-7s%n",
                "TICKER", "CATEGORÍA", "σ_a (anual)", "σ_d (día)", "μ_d (día)", "SHARPE");
        System.out.println("  " + "─".repeat(65));

        for (MetricasActivo m : listaMetricas) {
            System.out.printf("  %-6s  %-13s  %8.2f%%    %8.5f   %+.5f   %6.3f%n",
                    m.ticker,
                    m.categoriaNombre,
                    m.volatilidadAnual   * 100,
                    m.desviacionEstandar,
                    m.mediaRetornoDiario,
                    m.sharpeAproximado);
        }
        System.out.println("[ClasificadorRiesgo] Total activos: " + listaMetricas.size() + "\n");
    }
}