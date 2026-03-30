package sorting;

import java.util.List;
import model.RegistroFinanciero;

/**
 * Implementación del algoritmo Binary Insertion Sort.
 * Utiliza una adaptación de la búsqueda binaria clásica para encontrar
 * la posición exacta de inserción, optimizando el número de comparaciones.
 */
public class BinaryInsertionSortImpl implements Sorter<RegistroFinanciero> {

    /***
     * Logida de ordenamiento principal
     * @param listaDatos
     */
    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        for (int indiceActual = 1; indiceActual < listaDatos.size(); indiceActual++) {
            RegistroFinanciero dato = listaDatos.get(indiceActual);

            // Invocamos la búsqueda binaria con los límites de la sublista ya ordenada
            int posicionInsercion = busquedaBinaria(listaDatos, dato, 0, indiceActual - 1);

            // Desplazamiento de elementos hacia la derecha para hacer espacio
            int indiceDesplazamiento = indiceActual - 1;
            while (indiceDesplazamiento >= posicionInsercion) {
                listaDatos.set(indiceDesplazamiento + 1, listaDatos.get(indiceDesplazamiento));
                indiceDesplazamiento--;
            }

            // Inserción en la posición encontrada
            listaDatos.set(posicionInsercion, dato);
        }
    }

    /**
     * Metodo de ordenamiento binario
     * donde debe insertarse el elemento para mantener la lista ordenada.
     */
    private int busquedaBinaria(List<RegistroFinanciero> arreglo, RegistroFinanciero dato, int limInf, int limSup) {
        int centro;

        while (true) {
            centro = (limInf + limSup) / 2;

            // Condición de salida exacta del profesor
            if (limInf > limSup) {
                // Cuando los límites se cruzan, limInf es la posición correcta para insertar
                return limInf;
            }

            // Adaptación de: if (arreglo[centro] < dato)
            if (esEstrictamenteMenor(arreglo.get(centro), dato)) {
                limInf = centro + 1;
            }
            // Adaptación de: else if (arreglo[centro] > dato)
            else if (esEstrictamenteMenor(dato, arreglo.get(centro))) {
                limSup = centro - 1;
            }
            // Adaptación de: else (cuando el dato es exactamente igual)
            else {
                // En la búsqueda clásica aquí retornaría 'true'.
                // Para nuestro ordenamiento, avanzamos a la derecha para mantener la estabilidad.
                limInf = centro + 1;
            }
        }
    }

    /**
     * Evalúa jerárquicamente dos registros financieros.
     * Criterio principal: Fecha. Criterio secundario: Precio de cierre (Close).
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
        return "Binary Insertion Sort";
    }
}