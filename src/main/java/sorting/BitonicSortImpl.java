package sorting;

import java.util.List;
import model.RegistroFinanciero;

/*
 * BitonicSortImpl.java - Implementacion del algoritmo Bitonic Sort.
 *
 * Bitonic Sort es un algoritmo de ordenamiento paralelo.
 *
 * El concepto central es la "secuencia bitoncia": una secuencia que primero
 * sube y luego baja (o viceversa). El algoritmo construye y fusiona estas
 * secuencias de forma recursiva hasta ordenar toda la lista.
 *
 * Restriccion importante: el algoritmo puro requiere que el tamaño de la lista
 * sea una potencia de 2 (2, 4, 8, 16, 32...). Para manejar listas de cualquier
 * tamaño, esta implementacion aplica el algoritmo sobre la mayor potencia de 2
 * que cabe en la lista y luego usa Insertion Sort para los elementos restantes.
 *
 * Complejidad: O(n log^2 n) en tiempo, O(log^2 n) en profundidad de recursion.
 */
public class BitonicSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        int n = listaDatos.size();

        /*
         * Calculamos la mayor potencia de 2 que sea menor o igual al tamaño de la lista.
         * Ejemplo: si n=1300, limitePotencia2 = 1024.
         * El algoritmo bitonico puro solo puede trabajar sobre ese bloque de 1024 elementos.
         */
        int limitePotencia2 = 1;
        while (limitePotencia2 * 2 <= n) {
            limitePotencia2 *= 2;
        }

        // Ejecutamos Bitonic Sort sobre los primeros limitePotencia2 elementos.
        ejecutarBitonicSort(listaDatos, 0, limitePotencia2, true);

        /*
         * Si la lista no era exactamente una potencia de 2, los elementos restantes
         * (desde limitePotencia2 hasta n-1) no fueron procesados por el algoritmo bitonico.
         * Aplicamos Insertion Sort sobre toda la lista para integrarlos correctamente.
         * Insertion Sort es eficiente aqui porque la mayor parte ya esta ordenada.
         */
        if (limitePotencia2 < n) {
            ordenamientoResidual(listaDatos);
        }
    }

    /**
     * Construye y ordena recursivamente una secuencia bitoncia.
     *
     * Divide el rango en dos mitades: ordena la primera ascendentemente y la segunda
     * descendentemente, creando una secuencia bitoncia. Luego las fusiona en el
     * orden final especificado por ordenAscendente.
     *
     * @param arreglo La lista completa.
     * @param inicio Indice de inicio del rango a procesar.
     * @param cantidad Numero de elementos en el rango.
     * @param ordenAscendente true para ordenar ascendente, false para descendente.
     */
    private void ejecutarBitonicSort(List<RegistroFinanciero> arreglo, int inicio, int cantidad, boolean ordenAscendente) {
        if (cantidad > 1) {
            int mitad = cantidad / 2;

            // Primera mitad: ordenar ascendentemente para construir la "subida" de la secuencia bitoncia.
            ejecutarBitonicSort(arreglo, inicio, mitad, true);

            // Segunda mitad: ordenar descendentemente para construir la "bajada".
            ejecutarBitonicSort(arreglo, inicio + mitad, mitad, false);

            // Fusionar ambas mitades en el orden final requerido.
            fusionarBitonic(arreglo, inicio, cantidad, ordenAscendente);
        }
    }

    /**
     * Fusiona una secuencia bitoncia en el orden especificado.
     *
     * Compara elementos separados por mitad posiciones y los intercambia si no
     * cumplen el orden requerido. Luego aplica la fusion recursivamente sobre
     * cada mitad. Este patron de comparacion es lo que hace a Bitonic Sort
     * eficiente para implementaciones paralelas.
     */
    private void fusionarBitonic(List<RegistroFinanciero> arreglo, int inicio, int cantidad, boolean ordenAscendente) {
        if (cantidad > 1) {
            int mitad = cantidad / 2;

            // Comparamos cada elemento de la primera mitad con su par en la segunda mitad.
            for (int i = inicio; i < inicio + mitad; i++) {
                compararEIntercambiar(arreglo, i, i + mitad, ordenAscendente);
            }

            // Aplicamos la fusion recursivamente sobre cada mitad.
            fusionarBitonic(arreglo, inicio, mitad, ordenAscendente);
            fusionarBitonic(arreglo, inicio + mitad, mitad, ordenAscendente);
        }
    }

    /**
     * Compara dos elementos en las posiciones i y j e intercambia si no cumplen
     * el orden requerido.
     *
     * La logica del intercambio es: si queremos orden ascendente y arreglo[i] > arreglo[j],
     * los intercambiamos. Si queremos descendente y arreglo[i] < arreglo[j], tambien.
     * La expresion (ordenAscendente != esMenor) captura ambos casos en una sola condicion.
     */
    private void compararEIntercambiar(List<RegistroFinanciero> arreglo, int i, int j, boolean ordenAscendente) {
        boolean esMenor = esEstrictamenteMenor(arreglo.get(i), arreglo.get(j));

        // Si el orden deseado no coincide con la relacion actual, intercambiamos.
        if (ordenAscendente != esMenor) {
            RegistroFinanciero temporal = arreglo.get(i);
            arreglo.set(i, arreglo.get(j));
            arreglo.set(j, temporal);
        }
    }

    /**
     * Insertion Sort para ordenar los elementos residuales que quedaron fuera
     * del bloque de potencia de 2. Se aplica sobre toda la lista porque los
     * residuales estan al final y necesitan integrarse con los ya ordenados.
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

    // Devuelve true si A debe ir antes que B. Criterio: fecha, luego close en centavos.
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
        return "Bitonic Sort";
    }
}
