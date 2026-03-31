package sorting;

import java.util.List;
import model.RegistroFinanciero;

/*
 * SelectionSort.java - Implementacion del algoritmo Selection Sort.
 *
 * La idea de Selection Sort es simple: en cada pasada, busca el elemento
 * mas pequeño de la parte no ordenada y lo coloca al final de la parte ordenada.
 * Es como ordenar cartas buscando siempre la mas baja y poniendola al frente.
 *
 * Complejidad: O(n^2) en todos los casos (mejor, promedio y peor).
 * No importa si la lista ya esta casi ordenada, siempre hace el mismo numero
 * de comparaciones. Por eso es uno de los mas lentos en el benchmark.
 *
 * Ventaja: hace muy pocos intercambios (exactamente n-1 en el peor caso),
 * lo que lo hace util cuando escribir en memoria es costoso.
 *
 * Usa compareTo() de RegistroFinanciero, que ordena por fecha y luego por close.
 */
public class SelectionSort implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> datos) {
        int n = datos.size();

        // El indice i representa el limite entre la parte ya ordenada (izquierda)
        // y la parte pendiente (derecha). En cada iteracion, i avanza un lugar.
        for (int i = 0; i < n - 1; i++) {

            // Asumimos que el primer elemento de la parte no ordenada es el minimo.
            // Si encontramos algo menor, actualizamos minIdx.
            int minIdx = i;

            // Recorremos toda la parte no ordenada buscando el elemento mas pequeño.
            for (int j = i + 1; j < n; j++) {
                // compareTo < 0 significa que datos[j] es menor que datos[minIdx]
                if (datos.get(j).compareTo(datos.get(minIdx)) < 0) {
                    minIdx = j;
                }
            }

            // Solo hacemos el intercambio si encontramos un minimo diferente al actual.
            // Evitamos un swap innecesario cuando el minimo ya esta en su lugar.
            if (minIdx != i) {
                RegistroFinanciero temp = datos.get(minIdx);
                datos.set(minIdx, datos.get(i));
                datos.set(i, temp);
            }
        }
    }

    @Override
    public String getNombre() {
        return "Selection Sort";
    }
}
