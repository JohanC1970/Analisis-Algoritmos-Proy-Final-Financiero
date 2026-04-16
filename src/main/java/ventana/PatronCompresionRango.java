package ventana;

import model.RegistroFinanciero;
import java.util.List;

/*
 * PatronCompresionRango.java — Patrón 2: Compresión de Rango Intradiario.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * FORMALIZACIÓN
 * ═══════════════════════════════════════════════════════════════════════
 *   Sea V = [v₀, v₁, …, v_{w-1}] una ventana de w registros ordenados por fecha.
 *   Sea RD(i) = (high(i) − low(i)) / close(i)   el Rango Diario Relativo del día i.
 *     (normalizado por close para que sea comparable entre activos de distinto precio)
 *
 *   Sea H1 = {v₀, …, v_{⌊w/2⌋−1}}   la primera mitad de la ventana.
 *   Sea H2 = {v_{⌊w/2⌋}, …, v_{w-1}} la segunda mitad de la ventana.
 *
 *   Sea RDP(S) = (1/|S|) × Σᵢ∈S RD(i)   el Rango Diario Promedio de un segmento S.
 *
 *   El patrón "Compresión de Rango" está PRESENTE en la ventana si y solo si:
 *       RDP(H2) < α × RDP(H1)   con α = UMBRAL_COMPRESION = 0.70
 *
 *   Interpretación: la volatilidad intradiaria de la segunda mitad de la ventana
 *   cayó al menos (1 − α) × 100% = 30% respecto a la primera mitad.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * RELEVANCIA FINANCIERA
 * ═══════════════════════════════════════════════════════════════════════
 *   La compresión del rango intradiario señala que el activo está en fase de
 *   "consolidación": los participantes del mercado reducen su actividad especulativa
 *   y el precio oscila dentro de un rango cada vez más estrecho. Esta contracción
 *   de volatilidad suele preceder movimientos bruscos (breakout) en cualquier
 *   dirección, lo que la hace valiosa para detectar activos en punto de inflexión.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * EJEMPLO con ventana de 10 días y α = 0.70
 * ═══════════════════════════════════════════════════════════════════════
 *   H1 (días 0–4): rangos relativos = [0.025, 0.030, 0.022, 0.028, 0.026]
 *     RDP(H1) = 0.0262  →  alta volatilidad intradiaria
 *   H2 (días 5–9): rangos relativos = [0.018, 0.015, 0.012, 0.014, 0.016]
 *     RDP(H2) = 0.0150
 *   Umbral: 0.70 × 0.0262 = 0.0183
 *   ¿0.0150 < 0.0183? → SÍ → PATRÓN DETECTADO (compresión del 43%)
 *
 * ═══════════════════════════════════════════════════════════════════════
 * COMPLEJIDAD
 * ═══════════════════════════════════════════════════════════════════════
 *   O(w) por ventana: un solo recorrido lineal para calcular ambos RDP.
 */
public class PatronCompresionRango implements PatronVentana {

    /*
     * Umbral de compresión (α).
     * La segunda mitad debe tener un RDP estrictamente menor que α × RDP(primera mitad).
     * Con α = 0.70: la oscilación intradiaria promedio bajó al menos un 30%.
     */
    private static final double UMBRAL_COMPRESION = 0.70;

    @Override
    public boolean detectar(List<RegistroFinanciero> ventana) {
        // Necesitamos al menos 4 registros para tener mitades de ≥2 días cada una.
        if (ventana == null || ventana.size() < 4) return false;

        int mitad = ventana.size() / 2;

        double rdpH1 = calcularRangoDiarioPromedio(ventana, 0, mitad);
        double rdpH2 = calcularRangoDiarioPromedio(ventana, mitad, ventana.size());

        // Caso degenerado: si H1 tiene rango nulo, no podemos calcular la compresión.
        if (rdpH1 <= 0.0) return false;

        // La compresión está presente si H2 es significativamente más estrecho que H1.
        return rdpH2 < UMBRAL_COMPRESION * rdpH1;
    }

    /**
     * Calcula el Rango Diario Promedio (RDP) para el segmento [inicio, fin) de la ventana.
     *
     * RDP(S) = (1/|S|) × Σᵢ∈S [(high(i) − low(i)) / close(i)]
     *
     * Ignora registros con close ≤ 0 para evitar división por cero.
     *
     * @param ventana Lista completa de la ventana.
     * @param inicio  Índice de inicio del segmento (inclusive).
     * @param fin     Índice de fin del segmento (exclusive).
     * @return Rango diario promedio del segmento; 0.0 si no hay datos válidos.
     */
    private double calcularRangoDiarioPromedio(List<RegistroFinanciero> ventana, int inicio, int fin) {
        double sumaRangos = 0.0;
        int    contador   = 0;

        for (int i = inicio; i < fin; i++) {
            RegistroFinanciero r = ventana.get(i);

            // Protección contra precios negativos o nulos (datos anómalos del ETL).
            if (r.getClose() > 0.0 && r.getHigh() >= r.getLow()) {
                sumaRangos += (r.getHigh() - r.getLow()) / r.getClose();
                contador++;
            }
        }

        return contador > 0 ? sumaRangos / contador : 0.0;
    }

    @Override
    public String getNombre() {
        return "Compresión de Rango (≥30%)";
    }

    @Override
    public String getDescripcion() {
        return "Detecta ventanas donde el rango intradiario promedio (high−low)/close " +
               "de la segunda mitad cae al menos un 30% respecto a la primera mitad, " +
               "indicando consolidación que puede preceder un breakout.";
    }

    @Override
    public String getFormalizacion() {
        return "RDP(H2) < 0.70 × RDP(H1)  donde  RDP(S) = (1/|S|) × Σ[(high−low)/close]";
    }
}
