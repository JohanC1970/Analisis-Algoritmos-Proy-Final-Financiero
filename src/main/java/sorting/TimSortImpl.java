package sorting;

import java.util.ArrayList;
import java.util.List;

public class TimSortImpl <T extends  Comparable<T>> implements Sorter<T>{

    private static final int MIN_MERGE = 32;

    @Override
    public void sort(List<T> lista) {

        int n = lista.size();

        // PASO 1: Dividir la lista en bloques pequeños (Runs) de tamaño 32
        // y ordenar cada uno de esos bloques utilizando Insertion Sort.
        for (int i = 0; i < n; i += MIN_MERGE) {
            // Asegurarnos de no salirnos de los límites de la lista en el último bloque
            int fin = Math.min((i + MIN_MERGE - 1), (n - 1));
            insertionSort(lista, i, fin);
        }

        // PASO 2: Fusionar (Merge) los bloques ya ordenados.
        // Empezamos fusionando bloques de tamaño 32, luego 64, luego 128, etc.
        for (int size = MIN_MERGE; size < n; size = 2 * size) {

            // Recorremos la lista fusionando pares de bloques
            for (int left = 0; left < n; left += 2 * size) {

                int mid = left + size - 1;
                int right = Math.min((left + 2 * size - 1), (n - 1));

                // Solo hacemos la fusión si realmente existe una mitad derecha
                if (mid < right) {
                    merge(lista, left, mid, right);
                }
            }
        }

    }

    /**
     * Insertion Sort clásico, adaptado para ordenar solo un fragmento de la lista.
     */
    private void insertionSort(List<T> lista, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            T temp = lista.get(i);
            int j = i - 1;

            // Desplazar los elementos mayores hacia la derecha
            // Usamos compareTo() porque estamos trabajando con objetos genéricos
            while (j >= left && lista.get(j).compareTo(temp) > 0) {
                lista.set(j + 1, lista.get(j));
                j--;
            }
            // Insertar el elemento en su posición correcta dentro del bloque
            lista.set(j + 1, temp);
        }
    }

    private void merge(List<T> lista, int left, int mid, int right) {
        int len1 = mid - left + 1;
        int len2 = right - mid;

        // 1. Crear listas temporales para copiar los datos
        List<T> leftList = new ArrayList<>(len1);
        List<T> rightList = new ArrayList<>(len2);

        for (int x = 0; x < len1; x++) {
            leftList.add(lista.get(left + x));
        }
        for (int x = 0; x < len2; x++) {
            rightList.add(lista.get(mid + 1 + x));
        }

        // 2. Índices iniciales para recorrer las listas temporales
        int i = 0; // Índice de leftList
        int j = 0; // Índice de rightList
        int k = left; // Índice de la lista original donde vamos a sobreescribir

        // 3. Comparar frente a frente y poner el menor en la lista original
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

        // 4. Si sobraron elementos en la lista izquierda, los copiamos
        while (i < len1) {
            lista.set(k, leftList.get(i));
            k++;
            i++;
        }

        // 5. Si sobraron elementos en la lista derecha, los copiamos
        while (j < len2) {
            lista.set(k, rightList.get(j));
            k++;
            j++;
        }
    }

    @Override
    public String getNombre() {
        return "TimSort";
    }


}
