package sorting;

import java.util.List;
import model.RegistroFinanciero;

/*
 * BinaryInsertionSortImpl.java - Insertion Sort con busqueda binaria.
 *
 * El Insertion Sort clasico toma cada elemento y lo inserta en su posicion
 * correcta dentro de la parte ya ordenada, comparando uno a uno hacia atras.
 * El problema es que esas comparaciones son O(n) por elemento.
 *
 * Esta version mejora eso: en lugar de buscar la posicion de insercion
 * comparando elemento por elemento, usa busqueda binaria para encontrarla
 * en O(log n) comparaciones. Esto reduce el numero total de comparaciones,
 * aunque los desplazamientos de elementos siguen siendo O(n).
 *
 * Complejidad: O(n log n) en comparaciones, O(n^2) en movimientos de datos.
 * En la practica es mas rapido que Insertion Sort clasico para listas grandes.
 *
 * Comparacion propia: usa esEstrictamenteMenor() con fecha y close como criterios,
 * en lugar de compareTo(), para tener control total sobre la logica de orden.
 */
public class BinaryInsertionSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        /*
         * indiceActual recorre la lista desde el segundo elemento.
         * Todo lo que esta a la izquierda de indiceActual ya esta ordenado.
         * En cada iteracion tomamos el elemento en indiceActual y lo insertamos
         * en su lugar correcto dentro de la parte ordenada.
         */
        for (int indiceActual = 1; indiceActual < listaDatos.size(); indiceActual++) {

            // Guardamos el elemento a insertar antes de empezar a desplazar.
            RegistroFinanciero dato = listaDatos.get(indiceActual);

            // Buscamos la posicion correcta dentro del rango [0, indiceActual-1] ya ordenado.
            int posicionInsercion = busquedaBinaria(listaDatos, dato, 0, indiceActual - 1);

            // Desplazamos todos los elementos desde posicionInsercion hasta indiceActual-1
            // una posicion a la derecha para abrir el hueco donde va a entrar dato.
            int indiceDesplazamiento = indiceActual - 1;
            while (indiceDesplazamiento >= posicionInsercion) {
                listaDatos.set(indiceDesplazamiento + 1, listaDatos.get(indiceDesplazamiento));
                indiceDesplazamiento--;
            }

            // Colocamos el elemento en la posicion que encontro la busqueda binaria.
            listaDatos.set(posicionInsercion, dato);
        }
    }

    /**
     * Busqueda binaria adaptada para encontrar la posicion de insercion.
     *
     * A diferencia de la busqueda binaria clasica que devuelve true/false,
     * esta version devuelve el indice donde debe insertarse el elemento
     * para que la lista siga ordenada.
     *
     * Cuando los limites se cruzan (limInf > limSup), limInf apunta exactamente
     * al lugar donde debe ir el nuevo elemento.
     *
     * @param arreglo La parte ya ordenada de la lista.
     * @param dato El elemento que queremos insertar.
     * @param limInf Limite inferior del rango de busqueda.
     * @param limSup Limite superior del rango de busqueda.
     * @return El indice donde debe insertarse dato.
     */
    private int busquedaBinaria(List<RegistroFinanciero> arreglo, RegistroFinanciero dato, int limInf, int limSup) {
        int centro;

        while (true) {
            // Cuando los limites se cruzan, la busqueda termino.
            // limInf es la posicion correcta de insercion.
            if (limInf > limSup) {
                return limInf;
            }

            // Calculamos el punto medio del rango actual.
            centro = (limInf + limSup) / 2;

            if (esEstrictamenteMenor(arreglo.get(centro), dato)) {
                // El elemento del centro es menor que dato: la posicion esta en la mitad derecha.
                limInf = centro + 1;
            } else if (esEstrictamenteMenor(dato, arreglo.get(centro))) {
                // dato es menor que el elemento del centro: la posicion esta en la mitad izquierda.
                limSup = centro - 1;
            } else {
                // Los elementos son equivalentes (misma fecha y mismo close).
                // Avanzamos a la derecha para mantener la estabilidad del algoritmo:
                // los elementos iguales conservan su orden relativo original.
                limInf = centro + 1;
            }
        }
    }

    /**
     * Compara dos registros: devuelve true si A debe ir antes que B.
     * Criterio principal: fecha (formato numerico AAAAMMDD).
     * Criterio de desempate: precio de cierre en centavos.
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

    /*
     * Convierte la fecha a un entero AAAAMMDD para comparacion rapida.
     * Ejemplo: 2024-03-15 -> 20240315
     */
    private long obtenerFechaNumerica(RegistroFinanciero registro) {
        if (registro.getFecha() == null) return 0;
        return (registro.getFecha().getYear() * 10000L) +
                (registro.getFecha().getMonthValue() * 100L) +
                registro.getFecha().getDayOfMonth();
    }

    /*
     * Convierte el precio de cierre a centavos enteros para evitar
     * errores de precision con doubles en la comparacion.
     */
    private long obtenerPrecioCentavos(RegistroFinanciero registro) {
        return (long) (registro.getClose() * 100);
    }

    @Override
    public String getNombre() {
        return "Binary Insertion Sort";
    }
}
