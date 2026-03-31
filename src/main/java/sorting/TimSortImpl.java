package sorting;

import java.util.ArrayList;
import java.util.List;

/*
 * TimSortImpl.java - Implementacion del algoritmo Tim Sort.
 *
 * Tim Sort es el algoritmo que usa Java internamente en Collections.sort()
 * y Arrays.sort() para objetos. Fue diseñado por Tim Peters en 2002 para Python
 * y luego adoptado por Java. Es un hibrido entre Merge Sort e Insertion Sort.
 *
 * La idea central es que en datos del mundo real, las listas raramente estan
 * completamente desordenadas: suelen tener "runs" (subsecuencias ya ordenadas).
 * Tim Sort aprovecha esto:
 *
 *   Fase 1 - Dividir en "runs": parte la lista en bloques de tamaño MIN_MERGE (32).
 *            Cada bloque se ordena con Insertion Sort, que es muy eficiente para
 *            listas pequeñas (menos de ~50 elementos).
 *
 *   Fase 2 - Fusionar los runs: combina los bloques ordenados de a pares usando
 *            Merge Sort. Primero fusiona bloques de 32 en bloques de 64, luego
 *            de 64 en 128, y asi hasta que toda la lista es un solo bloque ordenado.
 *
 * Porque MIN_MERGE = 32: es el tamaño donde Insertion Sort supera a Merge Sort
 * en la practica por su menor overhead de memoria y mejor uso del cache del CPU.
 *
 * Complejidad: O(n log n) en el peor caso, O(n) en el mejor caso (lista ya ordenada).
 * Esta implementacion es generica y usa compareTo() de la interfaz Comparable.
 */
public class TimSortImpl<T extends Comparable<T>> implements Sorter<T> {

    // Tamaño de cada bloque que se ordena con Insertion Sort en la Fase 1.
    // 32 es el valor clasico de Tim Sort. Bloques mas grandes aumentan el costo
    // de Insertion Sort; bloques mas pequeños aumentan el numero de fusiones.
    private static final int MIN_MERGE = 32;

    @Override
    public void sort(List<T> lista) {
        int n = lista.size();

        /*
         * FASE 1: Ordenar cada bloque de tamaño MIN_MERGE con Insertion Sort.
         *
         * i avanza de MIN_MERGE en MIN_MERGE, marcando el inicio de cada bloque.
         * Math.min asegura que el ultimo bloque no se salga de los limites de la lista
         * si n no es multiplo exacto de MIN_MERGE.
         */
        for (int i = 0; i < n; i += MIN_MERGE) {
            int fin = Math.min((i + MIN_MERGE - 1), (n - 1));
            insertionSort(lista, i, fin);
        }

        /*
         * FASE 2: Fusionar los bloques ordenados de a pares.
         *
         * size empieza en MIN_MERGE (el tamaño de los bloques ya ordenados)
         * y se duplica en cada iteracion del bucle externo.
         * En cada nivel, recorremos la lista fusionando pares de bloques adyacentes.
         */
        for (int size = MIN_MERGE; size < n; size = 2 * size) {

            // left es el inicio del primer bloque de cada par a fusionar.
            // Avanza de 2*size en 2*size para saltar al siguiente par.
            for (int left = 0; left < n; left += 2 * size) {

                // mid es el ultimo indice del primer bloque (inicio del segundo bloque - 1).
                int mid   = left + size - 1;
                // right es el ultimo indice del segundo bloque, sin pasarse del fin de la lista.
                int right = Math.min((left + 2 * size - 1), (n - 1));

                // Solo fusionamos si realmente existe un segundo bloque (mid < right).
                // Si mid >= right, el "segundo bloque" esta vacio o no existe.
                if (mid < right) {
                    merge(lista, left, mid, right);
                }
            }
        }
    }

    /*
     * Insertion Sort clasico aplicado sobre un rango [left, right] de la lista.
     *
     * Toma cada elemento desde left+1 y lo inserta en su posicion correcta
     * dentro del rango ya ordenado a su izquierda, desplazando los mayores
     * una posicion a la derecha para abrir el hueco.
     *
     * @param lista  La lista completa.
     * @param left   Indice de inicio del rango a ordenar.
     * @param right  Indice de fin del rango a ordenar (inclusive).
     */
    private void insertionSort(List<T> lista, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            T temp = lista.get(i);
            int j = i - 1;

            // Desplazamos hacia la derecha todos los elementos mayores que temp.
            // compareTo > 0 significa que lista[j] es mayor que temp.
            while (j >= left && lista.get(j).compareTo(temp) > 0) {
                lista.set(j + 1, lista.get(j));
                j--;
            }
            // Insertamos temp en el hueco que abrimos.
            lista.set(j + 1, temp);
        }
    }

    /**
     * Fusiona dos sublistas adyacentes ya ordenadas en una sola sublista ordenada.
     *
     * Las dos sublistas son [left, mid] y [mid+1, right].
     * Copiamos ambas en listas temporales y luego las fusionamos de vuelta
     * en la lista original comparando elemento a elemento desde el frente de cada una.
     *
     * Por que listas temporales: no podemos fusionar in-place sin sobreescribir
     * datos que aun no hemos procesado. Las copias temporales evitan ese problema.
     *
     * @param lista  La lista completa.
     * @param left   Inicio de la primera sublista.
     * @param mid    Fin de la primera sublista / inicio de la segunda - 1.
     * @param right  Fin de la segunda sublista.
     */
    private void merge(List<T> lista, int left, int mid, int right) {
        int len1 = mid - left + 1;
        int len2 = right - mid;

        // Copiamos los datos de cada mitad en listas temporales.
        List<T> leftList  = new ArrayList<>(len1);
        List<T> rightList = new ArrayList<>(len2);

        for (int x = 0; x < len1; x++) leftList.add(lista.get(left + x));
        for (int x = 0; x < len2; x++) rightList.add(lista.get(mid + 1 + x));

        int i = 0;     // Puntero para recorrer leftList
        int j = 0;     // Puntero para recorrer rightList
        int k = left;  // Puntero para escribir en la lista original

        // Comparamos el frente de cada lista temporal y ponemos el menor en la lista original.
        // compareTo <= 0 garantiza estabilidad: si son iguales, el de la izquierda va primero.
        while (i < len1 && j < len2) {
            if (leftList.get(i).compareTo(rightList.get(j)) <= 0) {
                lista.set(k, leftList.get(i));
                i++;
            } else {
                lista.set(k, rightList.get(j));
                j++;
            }
            k++;
        }

        // Si quedaron elementos en leftList sin procesar, los copiamos directamente.
        while (i < len1) { lista.set(k++, leftList.get(i++)); }

        // Si quedaron elementos en rightList sin procesar, los copiamos directamente.
        while (j < len2) { lista.set(k++, rightList.get(j++)); }
    }

    @Override
    public String getNombre() {
        return "TimSort";
    }
}
