package similitud;

import java.util.List;

public class PearsonCorrelation implements AlgoritmoSimilitud{

    /**
     * Calcula la correlación de Pearson entre dos series de retornos diarios.
     *
     * Implementación en dos pasadas para mayor claridad:
     *   Pasada 1: calcular las medias de A y B.
     *   Pasada 2: calcular covarianza y desviaciones estándar simultáneamente.
     *
     * @param serieA Retornos diarios del activo A.
     * @param serieB Retornos diarios del activo B.
     * @return Correlación de Pearson en el rango [-1, 1].
     */
    @Override
    public double calcular(List<Double> serieA, List<Double> serieB) {

        if (serieA == null || serieB == null || serieA.isEmpty() || serieB.isEmpty()) {
            return Double.NaN;
        }

        /*
         * Se toma la longitud menor de las listas, con el proposito de evitar
         * desbordamiento en listas que no tienen un tamaño igual
         */
        int n = Math.min(serieA.size(), serieB.size());

        // Calcular las medias aritmeticas de cada serie
        double sumaA = 0.0;
        double sumaB = 0.0;

        for(int i = 0; i<n; i++){
            sumaA += serieA.get(i);
            sumaB += serieB.get(i);
        }

        double mediaA = sumaA / n;
        double mediaB = sumaB / n;

        // Calcular covarianza y varianzas
        double covarianza = 0.0;
        double varianzaA  = 0.0;
        double varianzaB  = 0.0;

        for (int i = 0; i < n; i++) {
            double desvA = serieA.get(i) - mediaA;
            double desvB = serieB.get(i) - mediaB;

            covarianza += desvA * desvB;
            varianzaA  += desvA * desvA;
            varianzaB  += desvB * desvB;
        }

        double denominador = Math.sqrt(varianzaA * varianzaB);

        // Aplicamos la fórmula de Pearson y devolvemos el resultado.
        // El resultado está matemáticamente garantizado en [-1, 1].
        return covarianza/denominador;
    }


    @Override
    public String getNombre() {
        return "Correlación de Pearson";
    }

    @Override
    public String getInterpretacion() {
        return "Rango [-1, 1]. Cerca de 1 = se mueven igual. Cerca de -1 = movimientos opuestos. Cerca de 0 = independientes.";
    }

    @Override
    public String getComplejidad() {
        return "O(n)";
    }
}
