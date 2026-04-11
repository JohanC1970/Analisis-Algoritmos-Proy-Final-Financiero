package similitud;

import java.util.List;

/*
 * Contrato comun para los cuatro algoritmos de similitud.
 *
 * Todos los algoritmos de similitud del Requerimiento 3 implementan esta interfaz.
 * Reciben dos series de retornos diarios (doubles) y devuelven un valor numerico
 * que cuantifica que tan parecidas son las dos series entre si.
 *
 * La interpretacion del valor devuelto varia por algoritmo:
 *   - Distancia Euclidiana: 0 = idénticas, mayor = mas diferentes.
 *   - Pearson: [-1, 1], donde 1 = perfectamente correlacionadas, -1 = inversas.
 *   - DTW: 0 = identicas, mayor = mas diferentes (igual que euclidiana pero mas flexible).
 *   - Coseno: [-1, 1], donde 1 = misma direccion, 0 = ortogonales, -1 = opuestas.
 *
 * El parametro T extends Number permite que en el futuro se use con int[], long[], etc.
 * Todas las implementaciones trabajan con List<Double>.
 */
public interface AlgoritmoSimilitud {

    /**
     * Calcula la similitud (o distancia) entre dos series de tiempo.
     * @param serieA Retornos diarios del primer activo. No debe ser null ni vacía.
     * @param serieB Retornos diarios del segundo activo. No debe ser null ni vacía.
     * @return Valor numerico que representa la similitud o distancia entre las series.
     */
    double calcular(List<Double> serieA, List<Double>serieB);

    /**
     * Nombre del algoritmo
     * @return Nombre del algoritmo (ej: "Distancia Euclidiana").
     */
    String getNombre();

    /**
     * Descripción corta de cómo interpretar el valor devuelto por calcular().
     * Se muestra en la UI para que el usuario entienda el resultado sin necesidad
     * de conocer los detalles matemáticos del algoritmo.
     *
     * @return String de interpretación (ej: "Menor valor = mayor similitud").
     */
    String getInterpretacion();

    /**
     * Complejidad algorítmica en notación Big-O.
     *
     * @return String con la complejidad (ej: "O(n)").
     */
    String getComplejidad();


}
