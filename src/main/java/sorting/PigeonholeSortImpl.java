package sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/*
 * PigeonholeSortImpl.java - Implementacion del algoritmo Pigeonhole Sort (Casillero).
 *
 * Pigeonhole Sort es un algoritmo no comparativo que funciona como un casillero
 * de correos: cada "casilla" (pigeonhole) corresponde a un valor entero posible,
 * y cada elemento va directamente a la casilla que le corresponde segun su clave.
 *
 * El proceso es:
 *   1. Encontrar el rango de claves (min y max) para saber cuantas casillas crear.
 *   2. Crear una casilla por cada valor entero posible en ese rango.
 *   3. Colocar cada elemento en su casilla correspondiente (clave - min = indice).
 *   4. Recolectar las casillas en orden para obtener la lista ordenada.
 *
 * Diferencia con Counting Sort: Pigeonhole Sort guarda los elementos reales en
 * las casillas (no solo contadores), por lo que funciona con objetos complejos
 * como RegistroFinanciero, no solo con enteros.
 *
 * Diferencia con Bucket Sort: las casillas de Pigeonhole son exactas (una por
 * valor entero posible), mientras que las cubetas de Bucket Sort son rangos.
 *
 * Complejidad: O(n + Rango) donde Rango = max - min + 1.
 * Es muy eficiente cuando el rango de valores es pequeno relativo a n.
 * Puede ser ineficiente si el rango es enorme (ej: fechas de 50 anos = ~18000 dias).
 *
 * Esta implementacion es generica. Recibe una funcion (ToIntFunction) que extrae
 * la clave entera de cada elemento T. En Main.java se usa:
 *   r -> (int) r.getFecha().toEpochDay()
 * que convierte la fecha a dias desde 1970, un entero unico por dia.
 */
public class PigeonholeSortImpl<T extends Comparable<T>> implements Sorter<T> {

    /*
     * Funcion que extrae la clave entera de un elemento T.
     * Se inyecta en el constructor como lambda para que el algoritmo sea
     * reutilizable con cualquier tipo T sin modificar su codigo interno.
     * La clave debe ser un entero no negativo para que funcione como indice de casilla.
     */
    private final ToIntFunction<T> keyExtractor;

    public PigeonholeSortImpl(ToIntFunction<T> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    @Override
    public void sort(List<T> lista) {
        if (lista == null || lista.isEmpty()) return;

        // PASO 1: Encontrar el minimo y maximo de las claves para calcular el rango.
        int min = keyExtractor.applyAsInt(lista.get(0));
        int max = min;

        for (T elemento : lista) {
            int key = keyExtractor.applyAsInt(elemento);
            if (key < min) min = key;
            if (key > max) max = key;
        }

        // PASO 2: Calcular el numero de casillas necesarias.
        // El rango es max - min + 1: si min=5 y max=8, necesitamos 4 casillas (5,6,7,8).
        int range = max - min + 1;

        /*
         * PASO 3: Crear las casillas.
         *
         * Cada casilla es una List<T> porque puede haber multiples elementos con la misma clave.
         * Por ejemplo, varios registros del mismo dia (mismo toEpochDay) pero de activos distintos.
         * Si usaramos un array simple de T, solo podriamos guardar un elemento por casilla.
         */
        List<List<T>> pigeonholes = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            pigeonholes.add(new ArrayList<>());
        }

        // PASO 4: Distribuir cada elemento en su casilla.
        // Restamos min al indice para que la casilla 0 corresponda al valor minimo.
        // Sin esta resta, si min=19000 (dias desde 1970), necesitariamos 19000 casillas vacias al inicio.
        for (T elemento : lista) {
            int key = keyExtractor.applyAsInt(elemento);
            int indiceCasilla = key - min;
            pigeonholes.get(indiceCasilla).add(elemento);
        }

        // PASO 5: Recolectar los elementos de las casillas en orden y sobreescribir la lista original.
        // Recorremos las casillas de la 0 a la range-1 (orden ascendente de claves).
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
        return "Pigeonhole Sort";
    }
}
