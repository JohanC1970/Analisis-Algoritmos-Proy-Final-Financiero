package similitud;

import java.util.List;

public class CosineSimilarity implements AlgoritmoSimilitud {

    /**
     * Calcula la similitud por coseno entre dos vectores de retornos diarios.
     *
     * Implementación en una sola pasada: acumulamos producto punto y ambas normas
     * al mismo tiempo, lo que es O(n) con constante mínima.
     *
     * @param serieA Retornos diarios del activo A.
     * @param serieB Retornos diarios del activo B.
     * @return Similitud coseno en el rango [-1, 1].
     */
    @Override
    public double calcular(List<Double> serieA, List<Double> serieB) {

        if (serieA == null || serieB == null || serieA.isEmpty() || serieB.isEmpty()) {
            return Double.NaN;
        }

        int n = Math.min(serieA.size(), serieB.size());

        /*
         * Acumulamos los tres valores necesarios en UNA SOLA PASADA:
         *   productoPunto: sum(a_i * b_i)   → numerador de la fórmula.
         *   normaA²:       sum(a_i²)         → para calcular ||A||.
         *   normaB²:       sum(b_i²)         → para calcular ||B||.
         *
         * Al final aplicamos las raíces cuadradas una sola vez sobre los acumuladores.
         * Esto es equivalente a calcular ||A|| = sqrt(sum a_i²) pero más eficiente
         * porque evitamos una raíz cuadrada por iteración.
         */
        double productoPunto = 0.0;
        double normaACuadrada = 0.0;
        double normaBCuadrada = 0.0;

        for (int i = 0; i < n; i++) {
            double ai = serieA.get(i);
            double bi = serieB.get(i);

            productoPunto   += ai * bi;
            normaACuadrada  += ai * ai;
            normaBCuadrada  += bi * bi;
        }

        /*
         * Calculamos el denominador: ||A|| * ||B||.
         * Usamos sqrt(normaA² * normaB²) en lugar de sqrt(normaA²) * sqrt(normaB²)
         * para evitar dos llamadas a sqrt y reducir el error de punto flotante.
         *
         * Matemáticamente: sqrt(a) * sqrt(b) = sqrt(a * b) cuando a,b >= 0.
         */
        double denominador = Math.sqrt(normaACuadrada * normaBCuadrada);

        // Caso degenerado: al menos una serie tiene magnitud cero.
        if (denominador == 0.0) {
            return Double.NaN;
        }

        // Calculamos y devolvemos la similitud por coseno.
        // El resultado está matemáticamente en [-1, 1] por la desigualdad de Cauchy-Schwarz.
        // Math.max/min de seguridad por posibles errores numéricos de punto flotante.
        double resultado = productoPunto / denominador;
        return Math.max(-1.0, Math.min(1.0, resultado));
    }



    @Override
    public String getNombre() {
        return "Similitud por Coseno";
    }

    @Override
    public String getInterpretacion() {
        return "Rango [-1, 1]. Cerca de 1 = misma dirección de movimientos. Mide orientación, no magnitud.";
    }

    @Override
    public String getComplejidad() {
        return "O(n)";
    }
}
