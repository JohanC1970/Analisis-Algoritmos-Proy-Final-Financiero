package ventana;

/*
 * ResultadoPatron.java - Resultados agregados del análisis de un patrón
 * sobre la serie histórica completa de un activo.
 *
 * Almacena cuántas de las ventanas analizadas contenían el patrón,
 * y la frecuencia relativa (proporción). Estos datos son serializados
 * a JSON por Gson y enviados a la interfaz web para su visualización.
 *
 * CAMPOS:
 *   ventanasAnalizadas : total de ventanas que se evaluaron con este patrón.
 *   ventanasConPatron  : cuántas de esas ventanas devolvieron detectar() = true.
 *   frecuencia         : ventanasConPatron / ventanasAnalizadas ∈ [0.0, 1.0]
 *
 * EJEMPLO DE INTERPRETACIÓN:
 *   Si frecuencia = 0.42, significa que en el 42% de los meses bursátiles
 *   analizados se detectó una racha alcista de ≥3 días.
 */
public class ResultadoPatron {

    // Metadatos del patrón (copiados desde PatronVentana para que el JSON sea autónomo).
    public final String nombrePatron;
    public final String descripcion;
    public final String formalizacion;

    // Estadísticas de detección sobre toda la serie histórica del activo.
    public final int    ventanasAnalizadas;
    public final int    ventanasConPatron;
    public final double frecuencia;         // ∈ [0.0, 1.0]
    public final double frecuenciaPct;      // frecuencia × 100 (para mostrar en la UI como %)

    /**
     * Construye el resultado calculando la frecuencia automáticamente.
     *
     * @param nombrePatron       Nombre del patrón.
     * @param descripcion        Descripción legible.
     * @param formalizacion      Formalización matemática.
     * @param ventanasAnalizadas Total de ventanas evaluadas.
     * @param ventanasConPatron  Ventanas donde se detectó el patrón.
     */
    public ResultadoPatron(String nombrePatron, String descripcion, String formalizacion,
                           int ventanasAnalizadas, int ventanasConPatron) {
        this.nombrePatron       = nombrePatron;
        this.descripcion        = descripcion;
        this.formalizacion      = formalizacion;
        this.ventanasAnalizadas = ventanasAnalizadas;
        this.ventanasConPatron  = ventanasConPatron;
        this.frecuencia         = ventanasAnalizadas > 0
                                  ? (double) ventanasConPatron / ventanasAnalizadas
                                  : 0.0;
        this.frecuenciaPct      = this.frecuencia * 100.0;
    }
}
