package sorting;

import java.util.List;

/*
 * Sorter.java - Contrato comun que deben cumplir todos los algoritmos de ordenamiento.
 *
 * En lugar de tener cada algoritmo con su propio metodo con nombre distinto,
 * definimos esta interfaz para que todos expongan exactamente los mismos dos metodos.
 * Eso nos permite en Main.java meterlos todos en un array y ejecutarlos en un bucle
 * sin saber ni importar que algoritmo especifico es cada uno.
 *
 * El parametro T extends Comparable<T> es la restriccion generica:
 * solo acepta tipos que sepan compararse entre si (que implementen compareTo).
 * Esto garantiza que cualquier algoritmo que use esta interfaz pueda ordenar
 * cualquier objeto que tenga un orden natural definido.
 */
public interface Sorter<T extends Comparable<T>> {

    /*
     * Ordena la lista recibida modificandola directamente (in-place).
     * Cuando este metodo termina, la lista debe estar ordenada de menor a mayor
     * segun el criterio de comparacion del tipo T.
     *
     * Se trabaja in-place (sobre la misma lista) para no desperdiciar memoria
     * creando copias. El benchmark en Main.java ya se encarga de pasar una
     * copia fresca a cada algoritmo antes de llamar a este metodo.
     *
     * @param lista La lista de elementos a ordenar. Se modifica directamente.
     */
    void sort(List<T> lista);

    /*
     * Devuelve el nombre legible del algoritmo.
     * Se usa para escribir la columna "algoritmo" en el benchmark.csv
     * y para los mensajes de consola durante la ejecucion.
     *
     * @return Nombre del algoritmo como String (ej: "Quick Sort", "Heap Sort").
     */
    String getNombre();
}
