package sorting;

import java.util.List;
import model.RegistroFinanciero;

/*
 * RadixSortImpl.java - Implementacion del algoritmo Radix Sort con colas enlazadas.
 *
 * Radix Sort es un algoritmo no comparativo que ordena procesando los digitos
 * de los valores numericos de a uno por vez, de menos significativo a mas significativo
 * (LSD - Least Significant Digit first).
 *
 * En cada pasada, distribuye los elementos en 10 cubetas (una por digito del 0 al 9)
 * segun el digito que se esta evaluando en esa pasada, y luego los recolecta en orden.
 * Despues de tantas pasadas como digitos tenga el numero mas grande, la lista queda ordenada.
 *
 * Esta implementacion tiene dos particularidades importantes:
 *
 *   1. Las cubetas son colas FIFO implementadas desde cero con listas enlazadas
 *      (NodoRegistro + ColaRegistros), sin usar ArrayList ni LinkedList de Java.
 *      Esto es un requisito del proyecto para demostrar el manejo de estructuras propias.
 *
 *   2. El ordenamiento se hace en dos fases para respetar el criterio compuesto
 *      (fecha primero, close como desempate):
 *        Fase 1: ordena por precio de cierre (criterio secundario).
 *        Fase 2: ordena por fecha (criterio principal).
 *      Como Radix Sort es estable (preserva el orden relativo de elementos iguales),
 *      al terminar la Fase 2 los registros con la misma fecha quedan ordenados por close.
 *
 * Complejidad: O(n * k) donde k es el numero de digitos del valor maximo.
 * Para fechas en formato AAAAMMDD (8 digitos), k=8. Para precios en centavos, k varia.
 */
public class RadixSortImpl implements Sorter<RegistroFinanciero> {

    // ─────────────────────────────────────────────────────────────────────────
    // Estructuras de datos propias: lista enlazada y cola FIFO
    // ─────────────────────────────────────────────────────────────────────────

    /*
     * Nodo de la lista enlazada simple.
     * Guarda un RegistroFinanciero y la referencia al siguiente nodo.
     * Es la unidad basica de almacenamiento de las colas.
     */
    private class NodoRegistro {
        public RegistroFinanciero datoFinanciero;
        public NodoRegistro enlaceSiguiente;

        public NodoRegistro(RegistroFinanciero registro) {
            this.datoFinanciero = registro;
            this.enlaceSiguiente = null;
        }
    }

    /*
     * Cola FIFO (First In, First Out) implementada con lista enlazada.
     *
     *
     * Mantiene referencias al frente (para extraer) y al final (para insertar)
     * para que ambas operaciones sean O(1).
     */
    private class ColaRegistros {
        private NodoRegistro frenteCola;  // Primer elemento: el que se extrae primero.
        private NodoRegistro finalCola;   // Ultimo elemento: donde se insertan los nuevos.
        private int tam;                  // Contador de elementos para saber si esta vacia.

        public ColaRegistros() {
            frenteCola = null;
            finalCola  = null;
            tam        = 0;
        }

        /*
         * Inserta un registro al final de la cola (operacion enqueue).
         * Si la cola esta vacia, el nuevo nodo es tanto el frente como el final.
         * Si no, lo enlazamos al final y actualizamos la referencia finalCola.
         */
        public void insertarRegistro(RegistroFinanciero registro) {
            tam++;
            NodoRegistro nuevoNodo = new NodoRegistro(registro);

            if (frenteCola == null) {
                frenteCola = nuevoNodo;
                finalCola  = frenteCola;
            } else {
                finalCola.enlaceSiguiente = nuevoNodo;
                finalCola = nuevoNodo;
            }
        }

        /*
         * Extrae y devuelve el registro del frente de la cola (operacion dequeue).
         * Avanza frenteCola al siguiente nodo. Si la cola queda vacia, limpiamos finalCola.
         */
        public RegistroFinanciero extraerRegistro() {
            tam--;
            RegistroFinanciero registroExtraido = frenteCola.datoFinanciero;
            frenteCola = frenteCola.enlaceSiguiente;

            if (frenteCola == null) {
                finalCola = null;
            }
            return registroExtraido;
        }

        // Devuelve true si la cola no tiene elementos.
        public boolean careceDeElementos() {
            return (tam == 0);
        }
    }

    // Las 10 cubetas permanentes, una por cada digito posible (0-9).
    // Se reutilizan en cada pasada del algoritmo (se vacian al recolectar).
    private ColaRegistros[] arregloCubetas;

    public RadixSortImpl() {
        arregloCubetas = new ColaRegistros[10];
        for (int indice = 0; indice < 10; indice++) {
            arregloCubetas[indice] = new ColaRegistros();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logica principal del algoritmo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        /*
         * Ordenamos en dos fases aprovechando la estabilidad de Radix Sort.
         *
         * Fase 1 (criterio secundario - precio de cierre):
         *   Ordenamos por close primero. Al ser estable, los registros con el mismo
         *   close quedan en su orden original relativo.
         *
         * Fase 2 (criterio principal - fecha):
         *   Ordenamos por fecha. Como el algoritmo es estable, los registros con la
         *   misma fecha conservan el orden que les dio la Fase 1 (por close).
         *   Resultado final: ordenado por fecha, con close como desempate.
         */
        ejecutarFaseOrdenamiento(listaDatos, false); // Fase 1: por precio de cierre
        ejecutarFaseOrdenamiento(listaDatos, true);  // Fase 2: por fecha
    }

    /*
     * Coordina una fase completa de Radix Sort para un criterio especifico.
     *
     * Primero encuentra el valor maximo en la lista para saber cuantos digitos
     * tiene y cuantas pasadas necesita hacer. Luego delega en el metodo que
     * ejecuta las pasadas de distribucion y recoleccion.
     *
     * @param listaDatos La lista a ordenar.
     * @param evaluarFecha true para ordenar por fecha, false para ordenar por close.
     */
    private void ejecutarFaseOrdenamiento(List<RegistroFinanciero> listaDatos, boolean evaluarFecha) {
        long valorMaximoEncontrado = 0;

        // Buscamos el valor numerico mas alto para determinar cuantos digitos tiene.
        for (int indice = 0; indice < listaDatos.size(); indice++) {
            long valorProcesado = obtenerValorNumerico(listaDatos.get(indice), evaluarFecha);
            if (valorMaximoEncontrado < valorProcesado) {
                valorMaximoEncontrado = valorProcesado;
            }
        }

        // Contamos los digitos del valor maximo convirtiendo a String y midiendo su longitud.
        // Ejemplo: 20240315 tiene 8 digitos -> necesitamos 8 pasadas.
        int totalDigitosRequeridos = String.valueOf(valorMaximoEncontrado).length();

        procesarDistribucionYRecoleccion(listaDatos, totalDigitosRequeridos, evaluarFecha);
    }

    /*
     * Ejecuta las pasadas de distribucion y recoleccion de Radix Sort.
     *
     * En cada pasada (posicionActual = 1, 2, 3...):
     *   - Distribucion (scatter): cada elemento va a la cubeta correspondiente
     *     al digito en la posicion actual de su valor numerico.
     *   - Recoleccion (gather): vaciamos las cubetas en orden (0 a 9) de vuelta
     *     a la lista original.
     *
     * @param listaDatos La lista a modificar.
     * @param totalDigitos Numero de pasadas a realizar.
     * @param evaluarFecha Criterio activo de esta fase.
     */
    private void procesarDistribucionYRecoleccion(List<RegistroFinanciero> listaDatos, int totalDigitos, boolean evaluarFecha) {
        int indiceReasignacion;

        // posicionActual=1 procesa las unidades, =2 las decenas, =3 las centenas, etc.
        for (int posicionActual = 1; posicionActual <= totalDigitos; posicionActual++) {
            indiceReasignacion = 0;

            // DISTRIBUCION: enviamos cada elemento a la cubeta de su digito actual.
            for (int indiceLista = 0; indiceLista < listaDatos.size(); indiceLista++) {
                RegistroFinanciero registroActual = listaDatos.get(indiceLista);
                long valorNumerico = obtenerValorNumerico(registroActual, evaluarFecha);
                int digitoAislado  = extraerDigitoEnPosicion(valorNumerico, posicionActual);

                arregloCubetas[digitoAislado].insertarRegistro(registroActual);
            }

            // RECOLECCION: vaciamos las cubetas en orden (0 a 9) de vuelta a la lista.
            // El orden de recoleccion es lo que produce el ordenamiento parcial por este digito.
            for (int indiceCubeta = 0; indiceCubeta < arregloCubetas.length; indiceCubeta++) {
                while (!arregloCubetas[indiceCubeta].careceDeElementos()) {
                    listaDatos.set(indiceReasignacion, arregloCubetas[indiceCubeta].extraerRegistro());
                    indiceReasignacion++;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Metodos auxiliares de conversion numerica
    // ─────────────────────────────────────────────────────────────────────────

    /*
     * Extrae el digito en una posicion decimal especifica de un numero.
     *
     * Funciona dividiendo el numero por 10^(posicion-1) para mover el digito
     * deseado a la posicion de las unidades, y luego tomando el modulo 10.
     *
     * Ejemplo: extraerDigitoEnPosicion(20240315, 3) -> digito en centenas -> 3
     *   20240315 / 10^2 = 202403
     *   202403 % 10 = 3
     *
     * @param numeroOrigen    El valor numerico completo.
     * @param posicionDecimal 1=unidades, 2=decenas, 3=centenas, etc.
     * @return                El digito en esa posicion (0 a 9).
     */
    private int extraerDigitoEnPosicion(long numeroOrigen, int posicionDecimal) {
        return (int) ((numeroOrigen / (long) Math.pow(10, posicionDecimal - 1)) % 10);
    }

    /*
     * Convierte los atributos de un registro a un numero entero largo para Radix Sort.
     *
     * Para fechas: formato AAAAMMDD (ej: 2024-03-15 -> 20240315).
     *   Este formato garantiza que el orden numerico coincide con el orden cronologico.
     *
     * Para precios: close multiplicado por 100 para convertir a centavos enteros.
     *   Esto elimina los decimales y evita errores de precision de punto flotante.
     *
     * @param registro El registro del que se extrae el valor.
     * @param evaluarFecha true para extraer la fecha, false para extraer el close.
     * @return Representacion numerica del atributo elegido.
     */
    private long obtenerValorNumerico(RegistroFinanciero registro, boolean evaluarFecha) {
        if (evaluarFecha) {
            if (registro.getFecha() == null) return 0;
            return (registro.getFecha().getYear() * 10000L) +
                    (registro.getFecha().getMonthValue() * 100L) +
                    registro.getFecha().getDayOfMonth();
        } else {
            return (long) (registro.getClose() * 100);
        }
    }

    @Override
    public String getNombre() {
        return "Radix Sort (Nodos)";
    }
}
