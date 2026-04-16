package ventana;

import model.RegistroFinanciero;
import java.util.ArrayList;
import java.util.List;

/*
 * MetricasActivo.java — Modelo que calcula SUS PROPIAS métricas de dispersión.
 *
 * Responsabilidad: dado un ticker y su serie histórica de precios (List<RegistroFinanciero>),
 * este objeto calcula de forma autónoma e interna:
 *
 *   1. Serie de retornos diarios          r_t = (close_t − close_{t-1}) / close_{t-1}
 *   2. Media aritmética de retornos       μ   = (1/n) × Σ r_t
 *   3. Varianza poblacional               σ²  = (1/n) × Σ (r_t − μ)²
 *   4. Desviación estándar diaria         σ_d = √σ²
 *   5. Volatilidad histórica anualizada   σ_a = σ_d × √252
 *   6. Retorno mínimo / máximo            worst day / best day
 *   7. Sharpe aproximado                  (μ × 252) / σ_a
 *   8. Categoría de riesgo                CategoriaRiesgo.clasificar(σ_a)
 *
 * Esto garantiza que cada instrumento financiero individual (acción o ETF)
 * compute su propio perfil de riesgo de forma encapsulada.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * FÓRMULAS IMPLEMENTADAS
 * ═══════════════════════════════════════════════════════════════════════
 *
 *   Retorno diario (aritmético lineal — estándar para horizontes cortos):
 *     r_t = (close_t − close_{t-1}) / close_{t-1}
 *
 *   Media aritmética:
 *     μ = (1/n) × Σ_{t=1}^{n} r_t
 *
 *   Varianza POBLACIONAL (dividimos por n, no n−1):
 *     σ² = (1/n) × Σ_{t=1}^{n} (r_t − μ)²
 *     Justificación: disponemos del historial COMPLETO del activo,
 *     no de una muestra aleatoria de una población mayor.
 *
 *   Desviación estándar diaria:
 *     σ_d = √σ²
 *
 *   Volatilidad histórica anualizada (convención de mercado):
 *     σ_a = σ_d × √252
 *     Justificación: 252 días hábiles de mercado por año calendario.
 *     √252 ≈ 15.8745 es el factor de anualización estándar en la industria.
 *
 *   Sharpe aproximado (sin tasa libre de riesgo Rf):
 *     Sharpe = (μ × 252) / σ_a
 *     Donde μ × 252 es el retorno anualizado estimado.
 *     Un Sharpe > 1.0 indica que el retorno compensa el riesgo asumido.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * COMPLEJIDAD DEL CÁLCULO
 * ═══════════════════════════════════════════════════════════════════════
 *   O(n) en dos pasadas lineales sobre la serie de retornos:
 *     Pasada 1: calcular retornos, acumular suma, detectar min/max.
 *     Pasada 2: calcular desviación cuadrática respecto a la media.
 *   Total: O(n) con constante pequeña.
 */
public class MetricasActivo {

    // Factor de anualización estándar: √252 días hábiles por año.
    private static final double FACTOR_ANUALIZACION = Math.sqrt(252.0);

    // ─── Identificación ──────────────────────────────────────────────────────
    public final String ticker;

    // ─── Métricas de dispersión calculadas internamente ───────────────────────

    /** Media aritmética de los retornos diarios. Ej: 0.0008 = +0.08%/día. */
    public final double mediaRetornoDiario;

    /** Desviación estándar diaria (σ_d). Calculada con varianza poblacional. */
    public final double desviacionEstandar;

    /**
     * Volatilidad histórica anualizada: σ_a = σ_d × √252.
     * Es la métrica principal para clasificar el nivel de riesgo del activo.
     */
    public final double volatilidadAnual;

    /** Mejor día: mayor retorno diario positivo registrado en la serie. */
    public final double retornoMaximo;

    /** Peor día: mayor caída diaria registrada en la serie (valor negativo). */
    public final double retornoMinimo;

    /**
     * Sharpe aproximado = (μ × 252) / σ_a.
     * Mide retorno ajustado por riesgo. Positivo = compensó el riesgo.
     */
    public final double sharpeAproximado;

    /** Número de retornos diarios calculados (= días de precios − 1). */
    public final int nDias;

    // ─── Clasificación de riesgo (calculada automáticamente de σ_a) ──────────

    /** Nombre de la categoría en español: "Conservador", "Moderado" o "Agresivo". */
    public final String categoriaNombre;

    /** Color HEX para la UI. Ej: "#22c55e" (verde = conservador). */
    public final String categoriaColor;

    /** Descripción de la categoría para tooltips y la interfaz web. */
    public final String categoriaDescripcion;

    /**
     * Score numérico de riesgo = volatilidadAnual.
     * Usado por ClasificadorRiesgo para ordenar la lista ascendentemente.
     */
    public final double scoreRiesgo;

    // ─── Resultados de patrones (provistos externamente por el analizador) ───
    /** Lista de ResultadoPatron, uno por patrón detectado con ventana deslizante. */
    public final List<ResultadoPatron> patrones;

    // ═════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR PRINCIPAL — Recibe precios y calcula todo internamente
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Construye las métricas del activo calculando toda la estadística internamente.
     * Cada instancia es responsable de su propio cálculo de dispersión.
     *
     * @param ticker   Símbolo del activo (ej: "AAPL").
     * @param serie    Lista de registros ordenados cronológicamente para ESTE activo.
     *                 Debe contener al menos 2 registros para calcular 1 retorno.
     * @param patrones Resultados del análisis de ventana deslizante, calculados
     *                 externamente por AnalizadorVentanaDeslizante.
     */
    public MetricasActivo(String ticker,
                          List<RegistroFinanciero> serie,
                          List<ResultadoPatron> patrones) {
        this.ticker   = ticker;
        this.patrones = patrones;

        // ── PASO 1: Calcular la serie de retornos diarios ─────────────────────
        List<Double> retornos = calcularRetornosDiarios(serie);
        this.nDias = retornos.size();

        // ── PASO 2: Primera pasada — media, máximo y mínimo ──────────────────
        double[] estadisticasPasada1 = primeraPasada(retornos);
        this.mediaRetornoDiario = estadisticasPasada1[0];
        this.retornoMinimo      = estadisticasPasada1[1];
        this.retornoMaximo      = estadisticasPasada1[2];

        // ── PASO 3: Segunda pasada — varianza y desviación estándar ───────────
        this.desviacionEstandar = segundaPasada(retornos, this.mediaRetornoDiario);

        // ── PASO 4: Volatilidad histórica anualizada — σ_a = σ_d × √252 ──────
        this.volatilidadAnual = this.desviacionEstandar * FACTOR_ANUALIZACION;

        // ── PASO 5: Sharpe aproximado (sin Rf) ────────────────────────────────
        //   Sharpe = (μ_d × 252) / σ_a
        double retornoAnualizado = this.mediaRetornoDiario * 252.0;
        this.sharpeAproximado = this.volatilidadAnual > 0.0
                ? retornoAnualizado / this.volatilidadAnual
                : 0.0;

        // ── PASO 6: Clasificación automática por volatilidad anual ────────────
        this.scoreRiesgo = this.volatilidadAnual;
        CategoriaRiesgo cat   = CategoriaRiesgo.clasificar(this.volatilidadAnual);
        this.categoriaNombre      = cat.nombre;
        this.categoriaColor       = cat.color;
        this.categoriaDescripcion = cat.descripcion;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS DE CÁLCULO (lógica encapsulada por activo)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * PASO 1 — Calcula la serie de retornos diarios a partir de precios de cierre.
     *
     *   r_t = (close_t − close_{t-1}) / close_{t-1}
     *
     * Condiciones de filtrado (datos anómalos del ETL):
     *   - Ignora días con close_{t-1} ≤ 0 (evita división por cero).
     *   - Ignora días con close_t ≤ 0    (precio inválido).
     *
     * @param serie Lista de registros del activo ordenados por fecha.
     * @return Lista de retornos diarios. Longitud típica = serie.size() − 1.
     */
    private List<Double> calcularRetornosDiarios(List<RegistroFinanciero> serie) {
        List<Double> retornos = new ArrayList<>();
        if (serie == null || serie.size() < 2) return retornos;

        for (int t = 1; t < serie.size(); t++) {
            double closeHoy  = serie.get(t).getClose();
            double closeAyer = serie.get(t - 1).getClose();

            if (closeAyer > 0.0 && closeHoy > 0.0) {
                retornos.add((closeHoy - closeAyer) / closeAyer);
            }
        }
        return retornos;
    }

    /**
     * PASO 2 — Primera pasada lineal: calcula media, mínimo y máximo.
     *
     *   μ = (1/n) × Σ r_t
     *
     * Un único recorrido O(n) acumula suma, actualiza min y max simultáneamente.
     *
     * @param retornos Serie de retornos diarios.
     * @return Array [media, minimo, maximo]. Retorna [0.0, 0.0, 0.0] si vacío.
     */
    private double[] primeraPasada(List<Double> retornos) {
        if (retornos.isEmpty()) return new double[]{0.0, 0.0, 0.0};

        double suma   = 0.0;
        double minVal = retornos.get(0);
        double maxVal = retornos.get(0);

        for (double r : retornos) {
            suma += r;
            if (r < minVal) minVal = r;
            if (r > maxVal) maxVal = r;
        }

        double media = suma / retornos.size();
        return new double[]{media, minVal, maxVal};
    }

    /**
     * PASO 3 — Segunda pasada lineal: calcula la desviación estándar (varianza poblacional).
     *
     *   σ² = (1/n) × Σ (r_t − μ)²       ← varianza poblacional
     *   σ_d = √σ²                          ← desviación estándar diaria
     *
     * Requiere la media pre-calculada en primeraPasada() para evitar
     * una pasada adicional.
     *
     * Usamos varianza POBLACIONAL (÷ n) porque tenemos el historial COMPLETO
     * del activo, no una muestra parcial de una población mayor.
     *
     * @param retornos Lista de retornos diarios.
     * @param media    Media aritmética ya calculada en primeraPasada().
     * @return Desviación estándar diaria σ_d ≥ 0. Devuelve 0.0 si vacío.
     */
    private double segundaPasada(List<Double> retornos, double media) {
        if (retornos.isEmpty()) return 0.0;

        double sumaCuadrados = 0.0;
        for (double r : retornos) {
            double desviacion = r - media;           // (r_t − μ)
            sumaCuadrados += desviacion * desviacion; // (r_t − μ)²
        }

        // Varianza poblacional: suma cuadrados / n
        double varianza = sumaCuadrados / retornos.size();

        // Desviación estándar diaria: √varianza
        return Math.sqrt(varianza);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MÉTODO DE DIAGNÓSTICO
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public String toString() {
        return String.format(
                "MetricasActivo{ticker='%s', categoría='%s', σ_d=%.5f, σ_a=%.4f (%.2f%%), μ_d=%.5f, Sharpe=%.3f, n=%d días}",
                ticker, categoriaNombre,
                desviacionEstandar, volatilidadAnual, volatilidadAnual * 100,
                mediaRetornoDiario, sharpeAproximado, nDias
        );
    }
}