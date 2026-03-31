package sorting;

import java.util.List;

/*
 * TreeSortImpl.java - Implementacion del algoritmo Tree Sort.
 *
 * Tree Sort usa un Arbol Binario de Busqueda (BST - Binary Search Tree) como
 * estructura intermedia para ordenar. La idea es simple:
 *
 *   1. Insertar todos los elementos de la lista en un BST.
 *      En un BST, cada nodo cumple: todo lo que esta a su izquierda es menor,
 *      todo lo que esta a su derecha es mayor o igual.
 *
 *   2. Recorrer el arbol en "inorden" (izquierda -> raiz -> derecha).
 *      Este recorrido visita los nodos exactamente en orden ascendente.
 *
 *   3. Sobreescribir la lista original con los valores en el orden del recorrido.
 *
 * El arbol se construye dentro de la clase como una clase interna Node,
 * sin usar ninguna estructura de Java como TreeMap o TreeSet.
 *
 * Complejidad:
 *   - Promedio: O(n log n) cuando el arbol queda balanceado.
 *   - Peor caso: O(n^2) si la lista ya esta ordenada, porque el arbol degenera
 *     en una lista enlazada (cada nodo solo tiene hijo derecho).
 *
 * Esta implementacion es generica y usa compareTo() para las comparaciones.
 */
public class TreeSortImpl<T extends Comparable<T>> implements Sorter<T> {

    /*
     * Nodo del arbol binario de busqueda.
     * Cada nodo guarda un valor y referencias a su hijo izquierdo y derecho.
     * Es una clase interna porque solo tiene sentido dentro de TreeSortImpl.
     */
    private class Node {
        T value;
        Node left;
        Node right;

        public Node(T item) {
            value = item;
            // Los hijos empiezan en null: el nodo es una hoja al momento de crearse.
            left = right = null;
        }
    }

    // La raiz del arbol. Se reinicia a null al inicio de cada llamada a sort()
    // para que el arbol no acumule datos de ejecuciones anteriores del benchmark.
    private Node root;

    @Override
    public void sort(List<T> lista) {

        // Reiniciamos el arbol para esta ejecucion.
        root = null;

        // Insertamos cada elemento de la lista en el BST.
        // Despues de este bucle, el arbol tiene todos los elementos organizados.
        for (T elemento : lista) {
            insertar(elemento);
        }

        /*
         * Usamos un array de un elemento como contador compartido para el recorrido inorden.
         * En Java no podemos pasar un int por referencia, pero si podemos pasar un array
         * y modificar su contenido. index[0] actua como el indice actual de escritura
         * en la lista original.
         */
        int[] index = {0};
        inOrderRec(root, lista, index);
    }

    // Metodo publico de insercion: delega en el metodo recursivo.
    private void insertar(T value) {
        root = insertarRec(root, value);
    }

    /*
     * Inserta un valor en el subarbol con raiz en "root" y devuelve la nueva raiz.
     *
     * Si el nodo actual es null, llegamos a un lugar vacio: creamos el nuevo nodo aqui.
     * Si el valor es menor que el nodo actual, va al subarbol izquierdo.
     * Si es mayor o igual, va al subarbol derecho (los iguales van a la derecha
     * para mantener la estabilidad del orden relativo).
     *
     * @param root Raiz del subarbol actual (puede ser null).
     * @param value El valor a insertar.
     * @return La raiz del subarbol despues de la insercion.
     */
    private Node insertarRec(Node root, T value) {
        // Caso base: encontramos un espacio vacio, aqui va el nuevo nodo.
        if (root == null) {
            root = new Node(value);
            return root;
        }

        // compareTo < 0: value es menor que root.value -> va al subarbol izquierdo.
        if (value.compareTo(root.value) < 0) {
            root.left = insertarRec(root.left, value);
        } else {
            // value es mayor o igual -> va al subarbol derecho.
            // Los duplicados van a la derecha para preservar el orden relativo (estabilidad).
            root.right = insertarRec(root.right, value);
        }

        return root;
    }

    /*
     * Recorrido inorden del arbol: izquierda -> raiz -> derecha.
     *
     * Este recorrido visita los nodos en orden ascendente porque:
     *   - Todo lo de la izquierda es menor que la raiz.
     *   - Todo lo de la derecha es mayor o igual.
     * Al visitar izquierda primero, luego la raiz, luego la derecha,
     * obtenemos los valores de menor a mayor.
     *
     * Cada vez que visitamos un nodo, escribimos su valor en la lista original
     * en la posicion index[0] y avanzamos el contador.
     *
     * @param root   Nodo actual del recorrido.
     * @param lista  La lista original donde escribimos los valores ordenados.
     * @param index  Array de un elemento que actua como contador compartido.
     */
    private void inOrderRec(Node root, List<T> lista, int[] index) {
        if (root != null) {
            // 1. Primero visitamos toda la rama izquierda (los menores).
            inOrderRec(root.left, lista, index);

            // 2. Escribimos el valor del nodo actual en la lista y avanzamos el indice.
            lista.set(index[0], root.value);
            index[0]++;

            // 3. Luego visitamos toda la rama derecha (los mayores).
            inOrderRec(root.right, lista, index);
        }
    }

    @Override
    public String getNombre() {
        return "Tree Sort";
    }
}
