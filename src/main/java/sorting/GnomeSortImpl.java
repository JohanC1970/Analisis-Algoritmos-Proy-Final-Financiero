package sorting;

import java.util.List;
import model.RegistroFinanciero;

/*
 * GnomeSortImpl.java - Implementacion del algoritmo Gnome Sort.
 *
 * Gnome Sort funciona como un gnomo de jardin ordenando macetas:
 * avanza por la lista y cuando encuentra dos elementos fuera de orden,
 * los intercambia y retrocede un paso para verificar que el intercambio
 * no rompio el orden anterior. Si todo esta bien, vuelve a avanzar.
 *
 * Es conceptualmente similar a Insertion Sort, pero en lugar de buscar
 * la posicion correcta con un bucle interno, se mueve hacia atras de a
 * un paso hasta encontrar donde encaja el elemento.
 *
 * Complejidad: O(n^2) en el peor caso. En listas casi ordenadas puede
 * acercarse a O(n) porque retrocede muy poco.
 *
 * Comparacion propia: usa esEstrictamenteMenor() en lugar de compareTo()
 * para tener control explicito sobre el criterio (fecha primero, close segundo).
 */
public class GnomeSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        int indiceActual = 0;
        int totalElementos = listaDatos.size();

        /*
         * El indice indiceActual actua como el "gnomo": se mueve hacia adelante
         * cuando el orden es correcto y hacia atras cuando hay que corregir.
         * El bucle termina cuando el gnomo llega al final de la lista.
         */
        while (indiceActual < totalElementos) {

            // Si estamos al inicio, no hay elemento anterior con quien comparar: avanzamos.
            if (indiceActual == 0) {
                indiceActual++;
            }

            // Si el elemento actual es mayor o igual al anterior, el orden es correcto: avanzamos.
            // La negacion de esEstrictamenteMenor equivale a "mayor o igual".
            if (!esEstrictamenteMenor(listaDatos.get(indiceActual), listaDatos.get(indiceActual - 1))) {
                indiceActual++;
            } else {
                // El elemento actual es menor que el anterior: estan en el orden equivocado.
                // Los intercambiamos y retrocedemos un paso para verificar el nuevo par.
                RegistroFinanciero temporal = listaDatos.get(indiceActual);
                listaDatos.set(indiceActual, listaDatos.get(indiceActual - 1));
                listaDatos.set(indiceActual - 1, temporal);
                indiceActual--;
            }
        }
    }

    /*
     * Compara dos registros usando fecha como criterio principal y precio de cierre
     * como criterio de desempate. Devuelve true si A debe ir antes que B.
     *
     * Convertimos la fecha a un numero entero en formato AAAAMMDD para poder
     * comparar con una simple resta de longs, evitando llamadas encadenadas
     * a los metodos de LocalDate en cada comparacion del bucle.
     */
    private boolean esEstrictamenteMenor(RegistroFinanciero registroA, RegistroFinanciero registroB) {
        long fechaA = obtenerFechaNumerica(registroA);
        long fechaB = obtenerFechaNumerica(registroB);
        if (fechaA != fechaB) return fechaA < fechaB;
        // Desempate por precio de cierre convertido a centavos (entero) para evitar
        // problemas de precision con doubles en la comparacion.
        return obtenerPrecioCentavos(registroA) < obtenerPrecioCentavos(registroB);
    }

    /*
     * Convierte la fecha de un registro al formato numerico AAAAMMDD.
     * Ejemplo: 2024-03-15 -> 20240315
     * Esto permite comparar fechas con una simple comparacion de longs.
     */
    private long obtenerFechaNumerica(RegistroFinanciero registro) {
        if (registro.getFecha() == null) return 0;
        return (registro.getFecha().getYear() * 10000L) +
                (registro.getFecha().getMonthValue() * 100L) +
                registro.getFecha().getDayOfMonth();
    }

    /*
     * Convierte el precio de cierre a centavos enteros para compararlo sin
     * riesgo de errores de precision de punto flotante.
     * Ejemplo: 455.37 -> 45537
     */
    private long obtenerPrecioCentavos(RegistroFinanciero registro) {
        return (long) (registro.getClose() * 100);
    }

    @Override
    public String getNombre() {
        return "Gnome Sort";
    }
}
