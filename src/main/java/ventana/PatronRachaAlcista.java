package ventana;

import model.RegistroFinanciero;
import java.util.List;

/*
 * PatronRachaAlcista.java — Patrón 1: Racha Alcista de K días consecutivos.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * FORMALIZACIÓN
 * ═══════════════════════════════════════════════════════════════════════
 *   Sea V = [v₀, v₁, …, v_{w-1}] una ventana de w registros ordenados por fecha.
 *   Sea close(i) el precio de cierre del registro vᵢ.
 *
 *   Una "Racha Alcista de longitud k" es una subsecuencia de k elementos
 *   contiguos {vᵢ, v_{i+1}, …, v_{i+k-1}} tal que:
 *       close(i+j) > close(i+j-1)   ∀j ∈ {1, …, k-1}
 *
 *   El patrón está PRESENTE en la ventana si y solo si:
 *       ∃i ∈ {0, …, w-1} : la racha alcista que comienza en i tiene longitud ≥ K_MIN
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PARÁMETRO
 * ═══════════════════════════════════════════════════════════════════════
 *   K_MIN (longitud mínima) = 3 por defecto.
 *   Elegimos 3 porque una sola jornada al alza puede ser ruido; tres días
 *   consecutivos sugieren momentum real y son estadísticamente relevantes.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * EJEMPLO con K_MIN = 3 y ventana de 5 días
 * ═══════════════════════════════════════════════════════════════════════
 *   Precios: [100, 102, 104, 106, 103]
 *   Día 1→2: 102 > 100 ✓  rachaActual = 2
 *   Día 2→3: 104 > 102 ✓  rachaActual = 3 ≥ 3 → DETECTADO
 *
 *   Precios: [100, 102, 99, 101, 103]
 *   Día 1→2: 102 > 100 ✓  rachaActual = 2
 *   Día 2→3:  99 < 102 ✗  rachaActual = 1 (reinicio)
 *   Día 3→4: 101 > 99  ✓  rachaActual = 2
 *   Día 4→5: 103 > 101 ✓  rachaActual = 3 ≥ 3 → DETECTADO
 *
 * ═══════════════════════════════════════════════════════════════════════
 * COMPLEJIDAD
 * ═══════════════════════════════════════════════════════════════════════
 *   O(w) por ventana, donde w = TAMANO_VENTANA.
 *   La detección termina en cuanto encuentra la primera racha válida
 *   (cortocircuito), por lo que en la práctica es incluso más rápida.
 */
public class PatronRachaAlcista implements PatronVentana {

    // Longitud mínima de la racha alcista para considerar que el patrón está presente.
    private final int longitudMinima;

    public PatronRachaAlcista(int longitudMinima) {
        this.longitudMinima = longitudMinima;
    }

    /**
     * Recorre la ventana contando cuántos días consecutivos el cierre sube.
     * En cuanto la racha actual alcanza longitudMinima, devuelve true de inmediato.
     * Si la racha se rompe, el contador vuelve a 1 (el día actual siempre cuenta como racha 1).
     */
    @Override
    public boolean detectar(List<RegistroFinanciero> ventana) {
        if (ventana == null || ventana.size() < longitudMinima) return false;

        int rachaActual = 1; // Un día solo siempre forma una "racha" de longitud 1.

        for (int i = 1; i < ventana.size(); i++) {
            double closeHoy  = ventana.get(i).getClose();
            double closeAyer = ventana.get(i - 1).getClose();

            if (closeHoy > closeAyer) {
                rachaActual++;
                // Cortocircuito: en cuanto encontramos una racha válida, terminamos.
                if (rachaActual >= longitudMinima) return true;
            } else {
                // Cualquier día que no sube rompe la racha: reiniciamos el contador.
                rachaActual = 1;
            }
        }
        return false;
    }

    /**
     * Calcula la longitud de la racha alcista más larga dentro de la ventana.
     * Usado para estadísticas detalladas (no para la detección binaria).
     *
     * @param ventana Lista de registros de la ventana.
     * @return Longitud máxima de racha alcista encontrada (mínimo 1).
     */
    public int calcularRachaMaxima(List<RegistroFinanciero> ventana) {
        if (ventana == null || ventana.size() < 2) return 1;

        int rachaActual = 1;
        int rachaMaxima = 1;

        for (int i = 1; i < ventana.size(); i++) {
            if (ventana.get(i).getClose() > ventana.get(i - 1).getClose()) {
                rachaActual++;
                if (rachaActual > rachaMaxima) rachaMaxima = rachaActual;
            } else {
                rachaActual = 1;
            }
        }
        return rachaMaxima;
    }

    @Override
    public String getNombre() {
        return "Racha Alcista (≥" + longitudMinima + " días)";
    }

    @Override
    public String getDescripcion() {
        return "Detecta ventanas donde el precio de cierre sube durante " +
               longitudMinima + " o más días consecutivos, indicando momentum alcista sostenido.";
    }

    @Override
    public String getFormalizacion() {
        return "∃i : close[i+j] > close[i+j−1]  ∀j ∈ {1,…," + (longitudMinima - 1) +
               "}  (longitud de racha ≥ " + longitudMinima + ")";
    }
}
