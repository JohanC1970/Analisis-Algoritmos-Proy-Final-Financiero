package similitud;

import java.util.List;

public class EuclideanDistance implements AlgoritmoSimilitud{

    /**
     * Calcula la distancia euclidiana entre dos series de retornos diarios.
     *
     * Si las series tienen diferente longitud, tomamos el mínimo para evitar
     * IndexOutOfBoundsException. En la práctica, el servicio que llama este método
     * ya se encarga de alinear las series por fecha antes de invocar calcular().
     *
     * @param serieA Retornos diarios del activo A.
     * @param serieB Retornos diarios del activo B.
     * @return Distancia euclidiana. Siempre >= 0.
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

        /*
         * Acumulamos la suma de cuadrados de las diferencias.
         * NO calculamos la raiz en cada paso.
         * Acumulamos todo primero y aplicamos una sola raíz cuadrada al final.
         * Esto es más eficiente y evita errores de redondeo acumulados.
         */
        double sumaCuadrados = 0.0;

        for(int i = 0; i<n; i++){
            double diferencia = serieA.get(i) - serieB.get(i);
            sumaCuadrados += diferencia * diferencia;
        }

        return Math.sqrt(sumaCuadrados);
    }

    @Override
    public String getNombre() {
        return "Distancia Euclidiana";
    }

    @Override
    public String getInterpretacion() {
        return "Menor valor = mayor similitud. Valor 0 = series idénticas.";
    }

    @Override
    public String getComplejidad() {
        return "O(n)";
    }


}
