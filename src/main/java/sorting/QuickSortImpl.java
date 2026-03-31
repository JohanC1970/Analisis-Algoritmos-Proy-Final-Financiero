package sorting;

import java.util.List;
import model.RegistroFinanciero;

/*
 * QuickSortImpl.java - Implementacion del algoritmo Quick Sort.
 *
 * Quick Sort es uno de los algoritmos mas usados en la practica.
 * La idea es elegir un elemento "pivote" y reorganizar la lista de forma
 * que todos los elementos menores al pivote queden a su izquierda y todos
 * los mayores queden a su derecha. Luego se repite el proceso recursivamente
 * en cada mitad (divide y venceras).
 *
 * Esta implementacion usa el elemento del centro como pivote, lo cual
 * reduce la probabilidad de caer en el peor caso comparado con elegir
 * siempre el primero o el ultimo.
 *
 * Complejidad:
 *   - Promedio: O(n log n)
 *   - Peor caso: O(n^2) si el pivote siempre cae en un extremo (lista ya ordenada
 *     con pivote en el primer o ultimo elemento). Con pivote central esto es raro.
 *
 * Comparacion propia: usa esEstrictamenteMenor() con fecha y close como criterios.
 */
public class QuickSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;
        quickSort(listaDatos, 0, listaDatos.size() - 1);
    }

    /*
     * Metodo recursivo principal de Quick Sort.
     *
     * Usa dos punteros (i desde la izquierda, j desde la derecha) que se mueven
     * hacia el centro. Cuando i encuentra un elemento mayor al pivote y j encuentra
     * uno menor, los intercambia. Cuando se cruzan, la particion esta completa.
     *
     * @param arreglo   La lista completa de registros.
     * @param inferior  Indice del inicio del subarreglo actual.
     * @param superior  Indice del fin del subarreglo actual.
     */
    private void quickSort(List<RegistroFinanciero> arreglo, int inferior, int superior) {
        int i = inferior;
        int j = superior;

        // El pivote es el elemento del centro del rango actual.
        // Elegir el centro en lugar del primero o el ultimo reduce el riesgo
        // de degradarse a O(n^2) con listas ya ordenadas o inversamente ordenadas.
        RegistroFinanciero pivote = arreglo.get((inferior + superior) / 2);

        do {
            // Movemos i hacia la derecha mientras los elementos sean menores al pivote.
            // Cuando para, arreglo[i] es un elemento que deberia estar en la mitad derecha.
            while (esEstrictamenteMenor(arreglo.get(i), pivote)) {
                i++;
            }

            // Movemos j hacia la izquierda mientras los elementos sean mayores al pivote.
            // Cuando para, arreglo[j] es un elemento que deberia estar en la mitad izquierda.
            while (esEstrictamenteMenor(pivote, arreglo.get(j))) {
                j--;
            }

            // Si los punteros no se cruzaron, encontramos dos elementos en el lugar equivocado:
            // los intercambiamos y avanzamos ambos punteros para continuar la particion.
            if (i <= j) {
                RegistroFinanciero auxiliar = arreglo.get(i);
                arreglo.set(i, arreglo.get(j));
                arreglo.set(j, auxiliar);
                i++;
                j--;
            }
        } while (i <= j);
        // El do-while termina cuando i y j se cruzan, lo que significa que la particion
        // esta completa: todo lo que esta a la izquierda de j es menor o igual al pivote,
        // y todo lo que esta a la derecha de i es mayor o igual.

        // Llamadas recursivas sobre las dos mitades generadas por la particion.
        // Solo llamamos si la mitad tiene mas de un elemento (condicion de parada).
        if (j > inferior) {
            quickSort(arreglo, inferior, j);
        }
        if (i < superior) {
            quickSort(arreglo, i, superior);
        }
    }

    /*
     * Devuelve true si registroA debe ir antes que registroB en el orden final.
     * Criterio principal: fecha en formato AAAAMMDD.
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

    // Convierte la fecha a AAAAMMDD para comparacion numerica directa.
    private long obtenerFechaNumerica(RegistroFinanciero registro) {
        if (registro.getFecha() == null) return 0;
        return (registro.getFecha().getYear() * 10000L) +
                (registro.getFecha().getMonthValue() * 100L) +
                registro.getFecha().getDayOfMonth();
    }

    // Convierte el precio de cierre a centavos para evitar errores de precision con doubles.
    private long obtenerPrecioCentavos(RegistroFinanciero registro) {
        return (long) (registro.getClose() * 100);
    }

    @Override
    public String getNombre() {
        return "Quick Sort";
    }
}
