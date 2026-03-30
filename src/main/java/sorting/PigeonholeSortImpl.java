package sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public class PigeonholeSortImpl <T extends Comparable<T>> implements Sorter<T>{

    private final ToIntFunction<T> keyExtractor;

    /**
     * Constructor que recibe la estrategia para extraer el número entero.
     * @param keyExtractor Función que convierte T en un int.
     */
    public PigeonholeSortImpl(ToIntFunction<T> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }


    @Override
    public void sort(List<T> lista) {
        if (lista == null || lista.isEmpty()) {
            return; // Nada que ordenar
        }

        // PASO 1: Encontrar el valor mínimo y máximo para saber cuántas cajas necesitamos
        int min = keyExtractor.applyAsInt(lista.get(0));
        int max = min;

        for (T elemento : lista) {
            int key = keyExtractor.applyAsInt(elemento);
            if (key < min) min = key;
            if (key > max) max = key;
        }

        // PASO 2: Calcular el Rango (Cantidad total de cajas o "pigeonholes")
        int range = max - min + 1;

        // PASO 3: Crear los Casilleros.
        // Como podemos tener elementos duplicados (ej. dos registros con la misma fecha),
        // cada caja no puede guardar un solo elemento, debe guardar una "Lista" de elementos.
        List<List<T>> pigeonholes = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            pigeonholes.add(new ArrayList<>());
        }

        // PASO 4: Repartir los elementos en sus respectivos casilleros
        for (T elemento : lista) {
            int key = keyExtractor.applyAsInt(elemento);
            // Calculamos el índice exacto de la caja restando el mínimo
            int indiceCaja = key - min;
            pigeonholes.get(indiceCaja).add(elemento);
        }

        // PASO 5: Recoger los elementos en orden y sobreescribir la lista original
        int indiceListaOriginal = 0;
        for (int i = 0; i < range; i++) {
            List<T> cajaActual = pigeonholes.get(i);
            for (T elemento : cajaActual) {
                lista.set(indiceListaOriginal, elemento);
                indiceListaOriginal++;
            }
        }
    }

    @Override
    public String getNombre() {
        return "";
    }
}
