package sorting;

import java.util.List;
import model.RegistroFinanciero;

/**
 * Implementación del algoritmo Quick Sort.
 * Utiliza el esquema de partición basado en un pivote central y el metodo
 * de divide y vencerás para ordenar la estructura de datos.
 */
public class QuickSortImpl implements Sorter<RegistroFinanciero> {

    // ==========================================
    // LÓGICA DE ORDENAMIENTO PRINCIPAL
    // ==========================================

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;
        quickSort(listaDatos, 0, listaDatos.size() - 1);
    }

    /**
     * Aplica la recursividad y la lógica de partición sobre la lista de datos.
     * Selecciona un elemento central como pivote y agrupa los elementos menores
     * a su izquierda y los mayores a su derecha.
     *
     * @param arreglo La colección de registros a procesar.
     * @param inferior El índice inicial del subarreglo a ordenar.
     * @param superior El índice final del subarreglo a ordenar.
     */
    private void quickSort(List<RegistroFinanciero> arreglo, int inferior, int superior) {
        int i = inferior;
        int j = superior;

        // Selección del pivote en la posición central del arreglo actual.
        RegistroFinanciero pivote = arreglo.get((inferior + superior) / 2);

        do {
            // Avanza el puntero izquierdo mientras los elementos sean estrictamente menores al pivote.
            while (esEstrictamenteMenor(arreglo.get(i), pivote)) {
                i++;
            }
            // Retrocede el puntero derecho mientras los elementos sean estrictamente mayores al pivote.
            while (esEstrictamenteMenor(pivote, arreglo.get(j))) {
                j--;
            }

            // Intercambia los elementos si los punteros no se han cruzado.
            if (i <= j) {
                RegistroFinanciero auxiliar = arreglo.get(i);
                arreglo.set(i, arreglo.get(j));
                arreglo.set(j, auxiliar);
                i++;
                j--;
            }
        } while (i <= j);

        // Llamadas recursivas para procesar las particiones generadas.
        if (j > inferior) {
            quickSort(arreglo, inferior, j);
        }
        if (i < superior) {
            quickSort(arreglo, i, superior);
        }
    }

    // ==========================================
    // MÉTODOS DE TRANSFORMACIÓN Y COMPARACIÓN
    // ==========================================

    /**
     * Evalúa la jerarquia de dos registros financieros.
     * Criterio primario: Fecha. Criterio secundario: Precio de cierre (Close).
     */
    private boolean esEstrictamenteMenor(RegistroFinanciero registroA, RegistroFinanciero registroB) {
        long fechaA = obtenerFechaNumerica(registroA);
        long fechaB = obtenerFechaNumerica(registroB);

        if (fechaA != fechaB) {
            return fechaA < fechaB;
        }

        long precioA = obtenerPrecioCentavos(registroA);
        long precioB = obtenerPrecioCentavos(registroB);
        return precioA < precioB;
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
        return "Quick Sort";
    }
}