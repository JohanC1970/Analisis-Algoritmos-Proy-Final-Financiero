package sorting;

import java.util.List;

public interface Sorter<T extends Comparable<T>> {

    /**
     * Ordena la lista proporcionada en su lugar (in-place).
     * @param lista La lista de elementos a ordenar.
     */
    void sort(List<T> lista);

    /**
     * Nos servirá para obtener el nombre del algoritmo
     * y poder exportar los tiempos a Python más adelante.
     * @return El nombre del algoritmo (ej. "Selection Sort").
     */
    String getNombre();
}
