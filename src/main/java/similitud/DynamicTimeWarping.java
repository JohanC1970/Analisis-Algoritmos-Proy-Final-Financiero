package similitud;

import java.util.List;

public class DynamicTimeWarping implements AlgoritmoSimilitud{

    /*
     * Ancho de la banda de Sakoe-Chiba como fracción de la longitud de la serie.
     * 0.10 = 10%: para series de 1250 días, permite desfases de hasta 125 días.
     * Reduce la complejidad de O(n²) a O(n * 0.1n) = O(0.1n²).
     */
    private static final double FRACCION_BANDA = 0.10;


    /**
     * Calcula la distancia DTW entre dos series de retornos diarios.
     *
     * Usa programación dinámica con la banda de Sakoe-Chiba para eficiencia.
     *
     * @param serieA Retornos diarios del activo A.
     * @param serieB Retornos diarios del activo B.
     * @return Distancia DTW. Siempre >= 0. Menor = más similares.
     */
    @Override
    public double calcular(List<Double> serieA, List<Double> serieB) {
        if (serieA == null || serieB == null || serieA.isEmpty() || serieB.isEmpty()) {
            return Double.NaN;
        }

        int n = serieA.size();
        int m = serieB.size();

        // Calculamos el ancho de banda. Mínimo 1 para que siempre haya al menos la diagonal.
        int anchoBanda = Math.max(1, (int) Math.round(Math.max(n, m) * FRACCION_BANDA));


        /*
         * Inicializamos toda la matriz con POSITIVE_INFINITY.
         * Las celdas fuera de la banda de Sakoe-Chiba quedarán con infinito,
         * lo que equivale a "este camino no está permitido" en la recursión.
         * Solo las celdas dentro de la banda se calculan realmente.
         */

        double[][] matrizDTW = new double[n][m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                matrizDTW[i][j] = Double.POSITIVE_INFINITY;
            }
        }


        // Llenado de la matriz con programacion dinamica
        for (int i = 0; i < n; i++) {
            /*
             * Límites de la banda de Sakoe-Chiba para la fila i.
             * Solo calculamos las columnas j que están dentro de la banda,
             * es decir, donde |i - j| <= anchoBanda.
             */
            int jInicio = Math.max(0, i - anchoBanda);
            int jFin    = Math.min(m - 1, i + anchoBanda);

            for (int j = jInicio; j <= jFin; j++) {

                // Costo local: diferencia absoluta entre los dos retornos.
                // Usamos valor absoluto porque nos interesa la magnitud de la diferencia,
                // no su signo (un desfase de +0.02 es tan diferente como uno de -0.02).
                double costoLocal = Math.abs(serieA.get(i) - serieB.get(j));

                // Recuperamos el mínimo costo acumulado de los tres vecinos permitidos.
                // Si un vecino está fuera de los límites (i-1 < 0 o j-1 < 0), ya tiene
                // POSITIVE_INFINITY, por lo que no afecta al mínimo.
                double costoAnterior;

                if (i == 0 && j == 0) {
                    // Esquina inicial: no hay vecinos, el costo es solo el local.
                    costoAnterior = 0.0;
                } else if (i == 0) {
                    // Primera fila: solo podemos venir de la izquierda (j-1).
                    costoAnterior = matrizDTW[0][j - 1];
                } else if (j == 0) {
                    // Primera columna: solo podemos venir de arriba (i-1).
                    costoAnterior = matrizDTW[i - 1][0];
                } else {
                    // Caso general: mínimo de los tres vecinos posibles.
                    double desdeArriba     = matrizDTW[i - 1][j];      // avanzar en A
                    double desdeIzquierda  = matrizDTW[i][j - 1];      // avanzar en B
                    double desdeDiagonal   = matrizDTW[i - 1][j - 1];  // ambos avanzan

                    costoAnterior = Math.min(desdeArriba, Math.min(desdeIzquierda, desdeDiagonal));
                }

                matrizDTW[i][j] = costoLocal + costoAnterior;
            }
        }

        // La distancia DTW es el valor en la esquina inferior derecha de la matriz.
        // Si esta celda sigue siendo POSITIVE_INFINITY, la banda era demasiado estrecha,
        // lo que no debería ocurrir con FRACCION_BANDA = 0.10 y series de igual longitud.
        return matrizDTW[n - 1][m - 1];
    }

    @Override
    public String getNombre() {
        return "Dynamic Time Warping (DTW)";
    }

    @Override
    public String getInterpretacion() {
        return "Menor valor = mayor similitud. Robusto ante desfases temporales entre patrones.";
    }

    @Override
    public String getComplejidad() {
        return "O(n × b)";
    }
}
