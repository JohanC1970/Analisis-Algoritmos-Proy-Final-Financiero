package sorting;

import java.util.List;

/*
 * CombSortImpl.java - Implementacion del algoritmo Comb Sort.
 *
 * Comb Sort es una mejora directa de Bubble Sort. El problema de Bubble Sort es
 * que solo compara elementos adyacentes, lo que hace que los elementos pequeños
 * al final de la lista (llamados "tortugas") tarden muchisimo en llegar a su
 * posicion correcta al inicio.
 *
 * Comb Sort resuelve esto comparando elementos separados por un "gap" (brecha)
 * que empieza grande y se va reduciendo en cada pasada hasta llegar a 1.
 * Cuando el gap es 1, el algoritmo se comporta exactamente como Bubble Sort,
 * pero para ese punto las tortugas ya fueron eliminadas y la lista esta casi ordenada.
 *
 * El factor de reduccion del gap es 1.3, determinado empiricamente como el valor
 * que da mejor rendimiento en la practica.
 *
 * Complejidad: O(n^2 / 2^p) donde p es el numero de incrementos del gap.
 * En la practica se comporta mucho mejor que Bubble Sort para listas grandes.
 *
 * Esta implementacion es generica (T extends Comparable<T>) y usa compareTo()
 * en lugar de un comparador propio, por lo que funciona con cualquier tipo
 * que tenga un orden natural definido.
 */
public class CombSortImpl<T extends Comparable<T>> implements Sorter<T> {

    @Override
    public void sort(List<T> lista) {
        int n = lista.size();

        // El gap empieza con el tamaño total de la lista y se reduce en cada pasada.
        int gap = n;

        /*
         * swapped es una bandera que indica si hubo algun intercambio en la pasada actual.
         * Empieza en true para que el bucle entre al menos una vez.
         * Cuando gap llega a 1 y no hay intercambios, la lista esta ordenada y el bucle termina.
         */
        boolean swapped = true;

        // El bucle continua mientras el gap no sea 1 o mientras sigan ocurriendo intercambios.
        // Ambas condiciones deben ser falsas para terminar: gap==1 Y sin intercambios.
        while (gap != 1 || swapped) {

            // Calculamos el nuevo gap para esta pasada.
            gap = getNextGap(gap);

            // Asumimos que no habra intercambios hasta que encontremos uno.
            swapped = false;

            // Recorremos la lista comparando cada elemento con el que esta "gap" posiciones adelante.
            // El limite es n-gap para no salirnos del arreglo al acceder a i+gap.
            for (int i = 0; i < n - gap; i++) {

                T elementoActual = lista.get(i);
                T elementoSiguiente = lista.get(i + gap);

                // compareTo > 0 significa que elementoActual es mayor que elementoSiguiente:
                // estan en el orden equivocado y hay que intercambiarlos.
                if (elementoActual.compareTo(elementoSiguiente) > 0) {

                    // Intercambio
                    lista.set(i, elementoSiguiente);
                    lista.set(i + gap, elementoActual);

                    // Marcamos que hubo al menos un intercambio en esta pasada.
                    swapped = true;
                }
            }
        }
    }

    /**
     * Calcula el siguiente gap dividiendo el actual por el factor de reduccion
     *
     * En lugar de usar division de punto flotante (gap / 1.3), multiplicamos por 10
     * y dividimos por 13, que es equivalente pero mas eficiente al trabajar solo con enteros.
     *
     * El gap nunca puede ser menor que 1: si el calculo da 0, lo forzamos a 1
     * para que el algoritmo haga al menos una pasada final como Bubble Sort.
     *
     * @param gap El gap de la pasada actual.
     * @return El gap para la siguiente pasada (minimo 1).
     */
    private int getNextGap(int gap) {
        gap = (gap * 10) / 13;

        if (gap < 1) {
            return 1;
        }
        return gap;
    }

    @Override
    public String getNombre() {
        return "Comb Sort";
    }
}