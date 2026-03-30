package sorting;

import java.util.List;
import model.RegistroFinanciero;

/**
 * Implementación del algoritmo Bitonic Sort.
 * Basado en la construcción y posterior ordenamiento de secuencias bitónicas
 * (una secuencia primero ascendente y luego descendente).
 * Nota de diseño: El algoritmo puro requiere que el tamaño de la colección sea una potencia de 2.
 */
public class BitonicSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        // Determina el tamaño de la lista a procesar. Para evitar fallos estructurales
        // en listas que no son potencia de 2, el algoritmo se ejecuta sobre un límite calculado.
        int n = listaDatos.size();

        // Calcula la mayor potencia de 2 menor o igual al tamaño de la lista
        int limitePotencia2 = 1;
        while (limitePotencia2 * 2 <= n) {
            limitePotencia2 *= 2;
        }

        // Ejecuta el algoritmo sobre la porción válida (potencia de 2) de la colección.
        ejecutarBitonicSort(listaDatos, 0, limitePotencia2, true);

        // Si la lista no era potencia de 2 exacta, aplica un ordenamiento de inserción residual
        // sobre los elementos restantes para garantizar el orden completo de la colección.
        if (limitePotencia2 < n) {
            ordenamientoResidual(listaDatos);
        }
    }

    /**
     * Construye y clasifica recursivamente una secuencia bitónica.
     *
     * @param arreglo La colección principal.
     * @param inicio Índice de arranque de la subsecuencia.
     * @param cantidad Cantidad de elementos a evaluar.
     * @param ordenAscendente Define si la partición se ordena de menor a mayor.
     */
    private void ejecutarBitonicSort(List<RegistroFinanciero> arreglo, int inicio, int cantidad, boolean ordenAscendente) {
        if (cantidad > 1) {
            int mitad = cantidad / 2;

            // Ordena la primera mitad en orden ascendente
            ejecutarBitonicSort(arreglo, inicio, mitad, true);

            // Ordena la segunda mitad en orden descendente
            ejecutarBitonicSort(arreglo, inicio + mitad, mitad, false);

            // Fusiona ambas mitades en el orden especificado
            fusionarBitonic(arreglo, inicio, cantidad, ordenAscendente);
        }
    }

    /**
     * Compara e intercambia los elementos de las dos mitades de una secuencia.
     */
    private void fusionarBitonic(List<RegistroFinanciero> arreglo, int inicio, int cantidad, boolean ordenAscendente) {
        if (cantidad > 1) {
            int mitad = cantidad / 2;
            for (int i = inicio; i < inicio + mitad; i++) {
                compararEIntercambiar(arreglo, i, i + mitad, ordenAscendente);
            }
            fusionarBitonic(arreglo, inicio, mitad, ordenAscendente);
            fusionarBitonic(arreglo, inicio + mitad, mitad, ordenAscendente);
        }
    }

    /**
     * Evalúa dos registros en posiciones distantes y los intercambia si no cumplen
     * el orden requerido.
     */
    private void compararEIntercambiar(List<RegistroFinanciero> arreglo, int i, int j, boolean ordenAscendente) {
        boolean esMenor = esEstrictamenteMenor(arreglo.get(i), arreglo.get(j));
        if (ordenAscendente != esMenor) {
            RegistroFinanciero temporal = arreglo.get(i);
            arreglo.set(i, arreglo.get(j));
            arreglo.set(j, temporal);
        }
    }

    /**
     * Metodo auxiliar de insercion binaria para tratar los que su tamaño
     * original no era una potencia exacta de 2.
     */
    private void ordenamientoResidual(List<RegistroFinanciero> listaDatos) {
        for (int i = 1; i < listaDatos.size(); i++) {
            RegistroFinanciero clave = listaDatos.get(i);
            int j = i - 1;
            while (j >= 0 && esEstrictamenteMenor(clave, listaDatos.get(j))) {
                listaDatos.set(j + 1, listaDatos.get(j));
                j--;
            }
            listaDatos.set(j + 1, clave);
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
        return "Bitonic Sort";
    }
}