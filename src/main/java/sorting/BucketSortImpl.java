package sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class BucketSortImpl <T extends Comparable<T>> implements Sorter<T>{

    private final ToDoubleFunction<T> valueExtractor;

    public BucketSortImpl(ToDoubleFunction<T> valueExtractor) {
        this.valueExtractor = valueExtractor;
    }

    @Override
    public void sort(List<T> lista) {
        int n = lista.size();
        if (n <= 1) return;

        // PASO 1: Encontrar el valor mínimo y máximo para calcular el rango total
        double min = valueExtractor.applyAsDouble(lista.get(0));
        double max = min;

        for (T elemento : lista) {
            double val = valueExtractor.applyAsDouble(elemento);
            if (val < min) min = val;
            if (val > max) max = val;
        }

        // Si todos los valores son exactamente iguales, ya está ordenado
        if (min == max) return;

        // PASO 2: Crear las cubetas.
        // Una buena práctica matemática es crear tantas cubetas como elementos hay (n)
        int numCubetas = n;
        List<List<T>> cubetas = new ArrayList<>(numCubetas);
        for (int i = 0; i < numCubetas; i++) {
            cubetas.add(new ArrayList<>());
        }

        // PASO 3: Repartir (Scatter) los elementos en sus cubetas correspondientes
        for (T elemento : lista) {
            double val = valueExtractor.applyAsDouble(elemento);

            // Fórmula para calcular a qué cubeta pertenece el valor:
            // (valor - min) / (max - min) nos da un porcentaje de 0.0 a 1.0.
            // Lo multiplicamos por (numCubetas - 1) para obtener el índice exacto.
            int indiceCubeta = (int) (((val - min) / (max - min)) * (numCubetas - 1));

            cubetas.get(indiceCubeta).add(elemento);
        }

        // PASO 4: Ordenar cada cubeta individualmente y juntar los resultados (Gather)
        int indiceListaOriginal = 0;

        for (List<T> cubeta : cubetas) {
            if (!cubeta.isEmpty()) {
                // Usamos nuestro propio método para ordenar la cubeta (Cumpliendo las reglas del proyecto)
                insertionSort(cubeta);

                // Sobreescribimos la lista original con los elementos ya ordenados de esta cubeta
                for (T elemento : cubeta) {
                    lista.set(indiceListaOriginal, elemento);
                    indiceListaOriginal++;
                }
            }
        }
    }

    /**
     * Método auxiliar para ordenar el interior de cada cubeta.
     * Bucket Sort suele usar Insertion Sort internamente porque las cubetas
     * tienden a ser pequeñas, y en listas pequeñas, Insertion Sort es imbatible.
     */
    private void insertionSort(List<T> cubeta) {
        for (int i = 1; i < cubeta.size(); i++) {
            T temp = cubeta.get(i);
            int j = i - 1;

            // Usamos compareTo() para mantener la consistencia con la interfaz Sorter
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
