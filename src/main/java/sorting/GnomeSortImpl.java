package sorting;

import java.util.List;
import model.RegistroFinanciero;

/**
 * Implementación del algoritmo Gnome Sort.
 * Opera de manera similar a Insertion Sort, moviendo un elemento hacia atrás
 * en la lista hasta encontrar su posición ordenada mediante intercambios sucesivos.
 */
public class GnomeSortImpl implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        int indiceActual = 0;
        int totalElementos = listaDatos.size();

        // Recorre la colección desplazando elementos fuera de orden hacia su posición correcta.
        while (indiceActual < totalElementos) {
            if (indiceActual == 0) {
                indiceActual++;
            }

            // Si el elemento actual es mayor o igual al anterior, el orden es correcto y se avanza.
            if (!esEstrictamenteMenor(listaDatos.get(indiceActual), listaDatos.get(indiceActual - 1))) {
                indiceActual++;
            } else {
                // Si el elemento actual es menor, se intercambia con el anterior y se retrocede un paso.
                RegistroFinanciero temporal = listaDatos.get(indiceActual);
                listaDatos.set(indiceActual, listaDatos.get(indiceActual - 1));
                listaDatos.set(indiceActual - 1, temporal);
                indiceActual--;
            }
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
        return "Gnome Sort";
    }
}