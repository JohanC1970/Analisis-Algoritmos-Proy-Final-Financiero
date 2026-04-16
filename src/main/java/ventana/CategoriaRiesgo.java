package ventana;

/*
 * CategoriaRiesgo.java - Categorías de riesgo para la clasificación de activos.
 *
 * Las fronteras están basadas en la volatilidad histórica anualizada
 * (σ_anual = σ_diaria × √252), que es el estándar académico y profesional
 * para medir riesgo de mercado en renta variable.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * UMBRALES UTILIZADOS
 * ═══════════════════════════════════════════════════════════════════════
 *   CONSERVADOR : σ_anual  < 15%
 *     Típico de: ETFs de renta fija, utilities, REITs defensivos.
 *     Referencia: el índice SPAB (bonos agregados EE.UU.) tiene ~5% de vol. anual.
 *
 *   MODERADO    : 15% ≤ σ_anual < 30%
 *     Típico de: ETFs de renta variable diversificados (VOO, SPY ~16-20%),
 *     acciones de grandes empresas establecidas.
 *
 *   AGRESIVO    : σ_anual ≥ 30%
 *     Típico de: tecnológicas de alto crecimiento (TSLA ~60-80%),
 *     activos especulativos o de nicho.
 *     Referencia: TSLA tiene históricamente ~60% de vol. anual.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * NOTA SOBRE EL PORTAFOLIO DEL PROYECTO
 * ═══════════════════════════════════════════════════════════════════════
 *   Con los 20 activos del proyecto (VOO, AAPL, TSLA, etc.) se espera que:
 *   - CONSERVADOR: VOO, SPY, QQQ, WMT, JNJ, PG, UNH (ETFs y defensivas)
 *   - MODERADO:    AAPL, MSFT, GOOGL, AMZN, JPM, V, MA, HD, BAC, DIS
 *   - AGRESIVO:    TSLA, META, NVDA (alta volatilidad históricamente)
 */
public enum CategoriaRiesgo {

    CONSERVADOR(
        "Conservador",
        "#22c55e",          // Verde: bajo riesgo.
        "conservative",
        0.00,
        0.15,
        "Volatilidad anual < 15%. Movimientos predecibles, bajo riesgo de pérdida severa."
    ),

    MODERADO(
        "Moderado",
        "#f59e0b",          // Ámbar: riesgo medio.
        "moderate",
        0.15,
        0.30,
        "Volatilidad anual 15–30%. Equilibrio entre riesgo y potencial de retorno."
    ),

    AGRESIVO(
        "Agresivo",
        "#ef4444",          // Rojo: alto riesgo.
        "aggressive",
        0.30,
        Double.MAX_VALUE,
        "Volatilidad anual ≥ 30%. Movimientos amplios e impredecibles, alto riesgo de pérdida."
    );

    // ─── Campos descriptivos ─────────────────────────────────────────────────
    public final String nombre;          // Nombre en español para la UI.
    public final String color;           // Color HEX para badges y gráficas.
    public final String clave;           // Clave en inglés para CSS/lógica.
    public final double limiteInferior;  // Límite inferior de σ_anual (inclusive).
    public final double limiteSuperior;  // Límite superior de σ_anual (exclusive).
    public final String descripcion;     // Texto explicativo para la UI.

    CategoriaRiesgo(String nombre, String color, String clave,
                    double limiteInferior, double limiteSuperior, String descripcion) {
        this.nombre          = nombre;
        this.color           = color;
        this.clave           = clave;
        this.limiteInferior  = limiteInferior;
        this.limiteSuperior  = limiteSuperior;
        this.descripcion     = descripcion;
    }

    /**
     * Clasifica un activo según su volatilidad anualizada.
     *
     * Recorre los valores del enum en orden de declaración (CONSERVADOR → MODERADO → AGRESIVO).
     * El primero cuyo rango [limiteInferior, limiteSuperior) incluya el valor dado
     * es la categoría asignada.
     *
     * @param volatilidadAnual Volatilidad histórica anualizada (ej: 0.22 = 22%).
     *                         Debe ser ≥ 0.
     * @return La categoría de riesgo correspondiente.
     */
    public static CategoriaRiesgo clasificar(double volatilidadAnual) {
        if (Double.isNaN(volatilidadAnual) || volatilidadAnual < 0) return AGRESIVO;
        for (CategoriaRiesgo cat : values()) {
            if (volatilidadAnual >= cat.limiteInferior && volatilidadAnual < cat.limiteSuperior) {
                return cat;
            }
        }
        return AGRESIVO; // Fallback para valores extremos (no debería ocurrir).
    }
}
