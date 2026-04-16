package ventana;

import model.RegistroFinanciero;
import java.util.List;

/*
 * PatronVentana.java - Contrato común para todos los patrones detectables
 * mediante la técnica de ventana deslizante (sliding window).
 *
 * El diseño espeja a AlgoritmoSimilitud del Req. 3: una interfaz con un método
 * de cómputo (detectar) y métodos descriptivos que la UI y el benchmark pueden
 * usar sin conocer los detalles de cada implementación.
 *
 * RESPONSABILIDAD:
 *   Recibir una ventana (sublista ordenada cronológicamente de RegistroFinanciero)
 *   y decidir si el patrón definido por la implementación está presente o no.
 *
 * CONTRATO SOBRE LA VENTANA:
 *   - ventana.get(0)           = día más antiguo de la ventana.
 *   - ventana.get(size - 1)    = día más reciente de la ventana.
 *   - Los registros están ordenados ascendentemente por fecha sin huecos.
 *   - La ventana nunca es null ni vacía cuando la llama AnalizadorVentanaDeslizante.
 */
public interface PatronVentana {

    /**
     * Determina si el patrón está presente en la ventana dada.
     *
     * @param ventana Lista ordenada cronológicamente de registros dentro de la ventana.
     * @return true si el patrón se detecta; false en caso contrario.
     */
    boolean detectar(List<RegistroFinanciero> ventana);

    /** Nombre corto del patrón (ej: "Racha Alcista ≥3 días"). */
    String getNombre();

    /** Descripción legible para mostrar en la interfaz web. */
    String getDescripcion();

    /**
     * Formalización matemática o lógica del patrón.
     * Se muestra en la UI para que el usuario entienda el criterio exacto de detección.
     */
    String getFormalizacion();
}
