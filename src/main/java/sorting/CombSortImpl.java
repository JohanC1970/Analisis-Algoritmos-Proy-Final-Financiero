package sorting;
import java.util.List;

/**
 * Implementación del algoritmo Comb Sort (Ordenamiento Peine).
 * Es una mejora del Bubble Sort. En lugar de comparar elementos adyacentes,
 * compara elementos separados por un "gap" (brecha) que se va reduciendo
 * progresivamente. Esto elimina rápidamente los valores pequeños al final
 * de la lista (conocidos como "tortugas").
 */
public class CombSortImpl<T extends Comparable<T>> implements Sorter<T> {

    @Override
    public void sort(List<T> lista) {
        int n = lista.size();

        // Inicializamos el gap con el tamaño total de la lista
        int gap = n;

        // Bandera para saber si hubo intercambios en la pasada actual
        boolean swapped = true;

        // El ciclo continúa mientras el gap no sea 1 o mientras sigan ocurriendo intercambios
        while (gap != 1 || swapped) {

            // 1. Actualizar el gap para la iteración actual
            gap = getNextGap(gap);

            // Asumimos que no habrá intercambios hasta que se demuestre lo contrario
            swapped = false;

            // 2. Recorrer la lista comparando elementos separados por el 'gap'
            for (int i = 0; i < n - gap; i++) {

                T elementoActual = lista.get(i);
                T elementoSiguiente = lista.get(i + gap);

                // Como usamos Genéricos (T), reemplazamos el '>' por compareTo()
                // Si el elementoActual es mayor al elementoSiguiente, retorna un número > 0
                if (elementoActual.compareTo(elementoSiguiente) > 0) {

                    // 3. Realizar el intercambio (Swap)
                    // La variable temporal debe ser de tipo T, no int
                    T temp = elementoActual;

                    // Colocamos el elemento menor en la posición izquierda
                    lista.set(i, elementoSiguiente);

                    // Colocamos el elemento mayor (que guardamos en temp) en la posición derecha
                    lista.set(i + gap, temp);

                    // Marcamos que sí hubo un intercambio
                    swapped = true;
                }
            }
        }
    }

    /**
     * Calcula la siguiente brecha (gap) dividiendo la actual por el "Shrink Factor" (Factor de reducción).
     * Empircamente se ha demostrado que el factor ideal es 1.3.
     * En lugar de usar decimales pesados (gap / 1.3), es más eficiente multiplicar por 10 y dividir por 13.
     *
     * @param gap El tamaño de la brecha actual.
     * @return El nuevo tamaño de la brecha (mínimo 1).
     */
    private int getNextGap(int gap) {
        gap = (gap * 10) / 13;

        if (gap < 1) {
            return 1;
        }
        return gap;
    }

    // Veo que agregaste este método a tu interfaz Sorter, ¡muy buena idea para la impresión en consola!
    @Override
    public String getNombre() {
        return "Comb Sort";
    }
}

