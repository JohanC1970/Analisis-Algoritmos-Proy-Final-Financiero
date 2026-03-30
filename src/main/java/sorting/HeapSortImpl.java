package sorting;

import java.util.List;
import model.RegistroFinanciero;

/**
 * Implementación del algoritmo Heap Sort.
 * Organiza los datos utilizando una estructura de árbol binario implícita (Max-Heap)
 * para extraer progresivamente el elemento máximo y ubicarlo al final de la colección.
 */
public class HeapSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        int n = listaDatos.size();

        // Fase 1: Construcción del Max-Heap.
        // Se asegura de que cada nodo padre sea mayor que sus hijos.
        for (int i = n / 2 - 1; i >= 0; i--) {
            construirMonticulo(listaDatos, n, i);
        }

        // Fase 2: Extracción de elementos.
        // Se mueve la raíz (el mayor) al final y se reconstruye el montículo con los elementos restantes.
        for (int i = n - 1; i > 0; i--) {
            RegistroFinanciero temporal = listaDatos.get(0);
            listaDatos.set(0, listaDatos.get(i));
            listaDatos.set(i, temporal);

            construirMonticulo(listaDatos, i, 0);
        }
    }

    /**
     * Mantiene la propiedad estructural del Max-Heap.
     * Evalúa un subárbol y realiza intercambios hacia abajo si un nodo es menor que sus hijos.
     *
     * @param listaDatos Colección de datos.
     * @param tamanoMonticulo Límite actual de elementos que forman parte del montículo.
     * @param indiceRaiz Posición del nodo a evaluar.
     */
    private void construirMonticulo(List<RegistroFinanciero> listaDatos, int tamanoMonticulo, int indiceRaiz) {
        int indiceMayor = indiceRaiz;
        int hijoIzquierdo = 2 * indiceRaiz + 1;
        int hijoDerecho = 2 * indiceRaiz + 2;

        if (hijoIzquierdo < tamanoMonticulo && esEstrictamenteMenor(listaDatos.get(indiceMayor), listaDatos.get(hijoIzquierdo))) {
            indiceMayor = hijoIzquierdo;
        }

        if (hijoDerecho < tamanoMonticulo && esEstrictamenteMenor(listaDatos.get(indiceMayor), listaDatos.get(hijoDerecho))) {
            indiceMayor = hijoDerecho;
        }

        if (indiceMayor != indiceRaiz) {
            RegistroFinanciero intercambio = listaDatos.get(indiceRaiz);
            listaDatos.set(indiceRaiz, listaDatos.get(indiceMayor));
            listaDatos.set(indiceMayor, intercambio);

            construirMonticulo(listaDatos, tamanoMonticulo, indiceMayor);
        }
    }

    private boolean esEstrictamenteMenor(RegistroFinanciero registroA, RegistroFinanciero registroB) {
        long fechaA = obtenerFechaNumerica(registroA);
        long fechaB = obtenerFechaNumerica(registroB);
        if (fechaA != fechaB) return fechaA < fechaB;
        return obtenerPrecioCentavos(registroA) < obtenerPrecioCentavos(registroB);
    }

    private long obtenerFechaNumerica(RegistroFinanciero registro) {
        if (registro.getFecha() == null) return 0;
        return (registro.getFecha().getYear() * 10000L) +
                (registro.getFecha().getMonthValue() * 100L) +
                registro.getFecha().getDayOfMonth();
    }

    private long obtenerPrecioCentavos(RegistroFinanciero registro) {
        return (long) (registro.getClose() * 100);
    }

    @Override
    public String getNombre() {
        return "Heap Sort";
    }
}