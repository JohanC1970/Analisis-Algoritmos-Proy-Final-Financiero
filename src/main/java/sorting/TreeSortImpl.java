package sorting;

import java.util.List;

public class TreeSortImpl <T extends Comparable<T>> implements Sorter<T>{


    private class Node{
        T value;
        Node left;
        Node right;

        public Node(T item){
            value = item;
            left = right = null;
        }
    }

    //Variable para guardar la raiz del arbol
    private Node root;

    @Override
    public void sort(List<T> lista) {

        root = null;

        for(T elemento : lista){
            insertar(elemento);
        }

        int [] index = {0};
        inOrderRec(root,lista,index);

    }

    private void insertar(T value) {
        root = insertarRec(root, value);
    }

    private Node insertarRec(Node root, T value) {
        // Si llegamos a un espacio vacío, creamos la nueva "hoja"
        if (root == null) {
            root = new Node(value);
            return root;
        }

        // Comparamos para decidir si vamos a la izquierda o a la derecha
        // Si el valor a insertar es MENOR que el nodo actual, va a la izquierda
        if (value.compareTo(root.value) < 0) {
            root.left = insertarRec(root.left, value);
        }
        // Si es MAYOR O IGUAL, va a la derecha (esto mantiene estables los elementos duplicados)
        else {
            root.right = insertarRec(root.right, value);
        }

        return root;
    }

    private void inOrderRec(Node root, List<T> lista, int[] index) {
        if (root != null) {
            // 1. Visitar toda la rama izquierda (los menores)
            inOrderRec(root.left, lista, index);

            // 2. Guardar el nodo actual en la lista y avanzar el contador
            lista.set(index[0], root.value);
            index[0]++;

            // 3. Visitar toda la rama derecha (los mayores)
            inOrderRec(root.right, lista, index);
        }
    }

    @Override
    public String getNombre() {
        return "Tree Sort";
    }


}
