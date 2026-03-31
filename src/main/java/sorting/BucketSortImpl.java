package sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/*
 * BucketSortImpl.java - Implementacion del algoritmo Bucket Sort.
 *
 * Bucket Sort es un algoritmo no comparativo: en lugar de comparar elementos entre si,
 * los distribuye en "cubetas" (rangos de valores) y luego ordena cada cubeta por separado.
 *
 * El proceso tiene tres etapas:
 *   1. Encontrar el rango de valores (minimo y maximo) para saber cuantas cubetas crear.
 *   2. Distribuir cada elemento en la cubeta que le corresponde segun su valor.
 *   3. Ordenar cada cubeta individualmente con Insertion Sort y concatenar los resultados.
 *
 * Porque Insertion Sort dentro de cada cubeta: las cubetas tienden a ser pequeñas
 * Para listas pequeñas, Insertion Sort es mas rapido que cualquier algoritmo O(n log n)
 *
 * Complejidad: O(n + k) en el caso promedio con distribucion uniforme,
 * donde k es el numero de cubetas. Puede degradarse a O(n^2) si todos los
 * elementos caen en la misma cubeta.
 *
 * Esta implementacion es generica. Recibe una funcion (ToDoubleFunction) que
 * extrae el valor numerico de cada elemento T para calcular a que cubeta pertenece.
 * En Main.java se usa r -> r.getClose() para ordenar por precio de cierre.
 */
public class BucketSortImpl<T extends Comparable<T>> implements Sorter<T> {


    private final ToDoubleFunction<T> valueExtractor;

    public BucketSortImpl(ToDoubleFunction<T> valueExtractor) {
        this.valueExtractor = valueExtractor;
    }

    @Override
    public void sort(List<T> lista) {
        int n = lista.size();
        if (n <= 1) return;

        // PASO 1: Encontrar el minimo y el maximo para conocer el rango total de valores.
        // Necesitamos estos valores para calcular a que cubeta pertenece cada elemento.
        double min = valueExtractor.applyAsDouble(lista.get(0));
        double max = min;

        for (T elemento : lista) {
            double val = valueExtractor.applyAsDouble(elemento);
            if (val < min) min = val;
            if (val > max) max = val;
        }

        // Si todos los valores son identicos, la lista ya esta ordenada: no hay nada que hacer.
        if (min == max) return;

        // PASO 2: Crear las cubetas.
        // Usamos n cubetas (una por elemento) como practica estandar.
        // Con n cubetas y distribucion uniforme, cada cubeta recibe 1 elemento en promedio.
        int numCubetas = n;
        List<List<T>> cubetas = new ArrayList<>(numCubetas);
        for (int i = 0; i < numCubetas; i++) {
            cubetas.add(new ArrayList<>());
        }

        // PASO 3: Distribuir cada elemento en su cubeta correspondiente.
        for (T elemento : lista) {
            double val = valueExtractor.applyAsDouble(elemento);

            /*
             * Formula para calcular el indice de la cubeta:
             *   (val - min) / (max - min) -> normaliza el valor al rango [0.0, 1.0]
             *   * (numCubetas - 1) -> escala al rango [0, numCubetas-1]
             *
             * El elemento con el valor minimo va a la cubeta 0.
             * El elemento con el valor maximo va a la cubeta numCubetas-1.
             * Los demas se distribuyen proporcionalmente entre medias.
             */
            int indiceCubeta = (int) (((val - min) / (max - min)) * (numCubetas - 1));
            cubetas.get(indiceCubeta).add(elemento);
        }

        // PASO 4: Ordenar cada cubeta y recolectar los resultados en la lista original.
        int indiceListaOriginal = 0;

        for (List<T> cubeta : cubetas) {
            if (!cubeta.isEmpty()) {
                // Ordenamos la cubeta con Insertion Sort (eficiente para listas pequenas).
                insertionSort(cubeta);

                // Sobreescribimos la lista original con los elementos ya ordenados de esta cubeta.
                for (T elemento : cubeta) {
                    lista.set(indiceListaOriginal, elemento);
                    indiceListaOriginal++;
                }
            }
        }
    }

    /**
     * Insertion Sort para ordenar el contenido de una cubeta.
     *
     * Usa compareTo() de la interfaz Comparable para mantener consistencia
     * con el orden natural del tipo T (en RegistroFinanciero: fecha, luego close).
     *
     * @param cubeta  La lista de elementos de una cubeta a ordenar.
     */
    private void insertionSort(List<T> cubeta) {
        for (int i = 1; i < cubeta.size(); i++) {
            T temp = cubeta.get(i);
            int j = i - 1;

            // Desplazamos hacia la derecha los elementos mayores que temp.
            while (j >= 0 && cubeta.get(j).compareTo(temp) > 0) {
                cubeta.set(j + 1, cubeta.get(j));
                j--;
            }
            cubeta.set(j + 1, temp);
        }
    }

    @Override
    public String getNombre() {
        return "Bucket Sort";
    }
}
