package sorting;

import java.util.List;
import model.RegistroFinanciero;

/*
 * HeapSortImpl.java - Implementacion del algoritmo Heap Sort.
 *
 * Heap Sort aprovecha una estructura de datos llamada Max-Heap (monticulo maximo):
 * un arbol binario donde cada nodo padre es siempre mayor que sus hijos.
 * La raiz del arbol es siempre el elemento mas grande de todos.
 *
 * El algoritmo tiene dos fases:
 * Fase 1: Construccion del heap: reorganizamos la lista para que cumpla la propiedad
 * de Max-Heap. Esto se hace en O(n).
 * Fase 2: Extraccion: repetidamente sacamos la raiz (el maximo), la ponemos
 * al final de la lista, y reconstruimos el heap con los elementos restantes.
 * Cada extraccion cuesta O(log n), y hacemos n extracciones: O(n log n).
 *
 * El heap no se implementa con una estructura de arbol real (nodos y punteros).
 * Se representa de forma implicita sobre la misma lista usando indices:
 *   - Hijo izquierdo del nodo i: indice 2*i + 1
 *   - Hijo derecho del nodo i:indice 2*i + 2
 *   - Padre del nodo i: indice (i-1) / 2
 *
 * Complejidad: O(n log n) garantizado en todos los casos.
 * No tiene peor caso como Quick Sort. Buena opcion cuando se necesita
 * rendimiento predecible.
 */
public class HeapSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        int n = listaDatos.size();

        /*
         * Fase 1: Construccion del Max-Heap.
         *
         * Empezamos desde el ultimo nodo que tiene hijos (indice n/2 - 1)
         * y vamos hacia atras hasta la raiz. Para cada nodo, llamamos a
         * construirMonticulo para asegurarnos de que ese subarbol cumpla
         * la propiedad del heap.
         *
         * Porque empezar desde n/2 - 1: los nodos desde n/2 hasta n-1
         * son hojas (no tienen hijos), asi que ya cumplen la propiedad trivialmente.
         */
        for (int i = n / 2 - 1; i >= 0; i--) {
            construirMonticulo(listaDatos, n, i);
        }

        /*
         * Fase 2: Extraccion ordenada.
         *
         * La raiz del heap (posicion 0) siempre tiene el elemento mas grande.
         * Lo intercambiamos con el ultimo elemento de la parte no ordenada,
         * lo "sacamos" del heap reduciendo el tamano en 1, y reconstruimos
         * el heap para que la nueva raiz vuelva a ser el maximo.
         *
         * Al final, los elementos quedan ordenados de menor a mayor en la lista.
         */
        for (int i = n - 1; i > 0; i--) {
            // Intercambiamos la raiz (maximo actual) con el ultimo elemento del heap.
            RegistroFinanciero temporal = listaDatos.get(0);
            listaDatos.set(0, listaDatos.get(i));
            listaDatos.set(i, temporal);

            // Reconstruimos el heap sobre los primeros i elementos (el ultimo ya esta en su lugar).
            construirMonticulo(listaDatos, i, 0);
        }
    }

    /**
     * Mantiene la propiedad del Max-Heap para el subarbol con raiz en indiceRaiz.
     *
     * Compara el nodo raiz con sus dos hijos. Si alguno de los hijos es mayor,
     * lo intercambia con la raiz y llama recursivamente sobre el subarbol afectado
     * para que la propiedad se propague hacia abajo (proceso llamado "heapify down").
     *
     * @param listaDatos       La lista que representa el heap.
     * @param tamanoMonticulo  Cuantos elementos del inicio de la lista forman el heap activo.
     *                         Los elementos despues de este limite ya estan ordenados y no se tocan.
     * @param indiceRaiz       El nodo desde donde se verifica y corrige el heap.
     */
    private void construirMonticulo(List<RegistroFinanciero> listaDatos, int tamanoMonticulo, int indiceRaiz) {

        // Asumimos que la raiz es el mayor hasta que se demuestre lo contrario.
        int indiceMayor = indiceRaiz;

        // Calculamos los indices de los hijos usando la formula del heap implicito.
        int hijoIzquierdo = 2 * indiceRaiz + 1;
        int hijoDerecho   = 2 * indiceRaiz + 2;

        // Si el hijo izquierdo existe y es mayor que el actual maximo, lo marcamos.
        if (hijoIzquierdo < tamanoMonticulo &&
                esEstrictamenteMenor(listaDatos.get(indiceMayor), listaDatos.get(hijoIzquierdo))) {
            indiceMayor = hijoIzquierdo;
        }

        // Si el hijo derecho existe y es mayor que el actual maximo, lo marcamos.
        if (hijoDerecho < tamanoMonticulo &&
                esEstrictamenteMenor(listaDatos.get(indiceMayor), listaDatos.get(hijoDerecho))) {
            indiceMayor = hijoDerecho;
        }

        // Si el maximo no es la raiz, intercambiamos y propagamos el ajuste hacia abajo.
        if (indiceMayor != indiceRaiz) {
            RegistroFinanciero intercambio = listaDatos.get(indiceRaiz);
            listaDatos.set(indiceRaiz, listaDatos.get(indiceMayor));
            listaDatos.set(indiceMayor, intercambio);

            // Llamada recursiva: el nodo que bajamos puede haber roto el heap en su nuevo subarbol.
            construirMonticulo(listaDatos, tamanoMonticulo, indiceMayor);
        }
    }

    // Devuelve true si A debe ir antes que B (A es menor en el orden del proyecto).
    private boolean esEstrictamenteMenor(RegistroFinanciero registroA, RegistroFinanciero registroB) {
        long fechaA = obtenerFechaNumerica(registroA);
        long fechaB = obtenerFechaNumerica(registroB);
        if (fechaA != fechaB) return fechaA < fechaB;
        return obtenerPrecioCentavos(registroA) < obtenerPrecioCentavos(registroB);
    }

    // Fecha en formato AAAAMMDD para comparacion numerica directa.
    private long obtenerFechaNumerica(RegistroFinanciero registro) {
        if (registro.getFecha() == null) return 0;
        return (registro.getFecha().getYear() * 10000L) +
                (registro.getFecha().getMonthValue() * 100L) +
                registro.getFecha().getDayOfMonth();
    }

    // Precio de cierre en centavos para evitar errores de precision con doubles.
    private long obtenerPrecioCentavos(RegistroFinanciero registro) {
        return (long) (registro.getClose() * 100);
    }

    @Override
    public String getNombre() {
        return "Heap Sort";
    }
}
