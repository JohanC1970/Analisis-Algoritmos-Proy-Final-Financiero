package sorting;

import java.util.List;
import model.RegistroFinanciero;

/**
 * Implementación del algoritmo Radix Sort mediante el uso de cubetas (buckets).
 * La distribución de los elementos se gestiona a través de estructuras de datos
 * tipo Cola (Queue) construidas desde cero mediante listas simplemente enlazadas.
 */
public class RadixSortImpl implements Sorter<RegistroFinanciero> {

    /**
     * Entidad fundamental de la lista enlazada.
     * Actúa como contenedor en memoria para un objeto RegistroFinanciero y
     * mantiene la referencia al siguiente elemento de la secuencia.
     */
    private class NodoRegistro {
        public RegistroFinanciero datoFinanciero;
        public NodoRegistro enlaceSiguiente;

        public NodoRegistro(RegistroFinanciero registro) {
            this.datoFinanciero = registro;
            this.enlaceSiguiente = null;
        }
    }

    /**
     * Estructura de datos que implementa la política FIFO (First In, First Out).
     * Se utiliza para agrupar temporalmente los registros durante la evaluación de cada dígito.
     */
    private class ColaRegistros {
        private NodoRegistro frenteCola;
        private NodoRegistro finalCola;
        private int tam;

        public ColaRegistros() {
            frenteCola = null;
            finalCola = null;
            tam = 0;
        }

        /**
         * Inserta un nuevo registro en la parte posterior de la estructura.
         * * @param registro El objeto a almacenar.
         */
        public void insertarRegistro(RegistroFinanciero registro) {
            tam++;
            NodoRegistro nuevoNodo = new NodoRegistro(registro);

            if (frenteCola == null) {
                frenteCola = nuevoNodo;
                finalCola = frenteCola;
            } else {
                finalCola.enlaceSiguiente = nuevoNodo;
                finalCola = nuevoNodo;
            }
        }

        /**
         * Extrae y retorna el registro ubicado en la parte frontal de la estructura.
         * * @return El objeto RegistroFinanciero más antiguo en la cola.
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

        /**
         * Evalua el estado de la estructura para determinar si contiene elementos.
         * * @return true si la cantidad de elementos es cero, false en caso contrario.
         */
        public boolean careceDeElementos() {
            return (tam == 0);
        }
    }

    /** * Arreglo estatico que contiene 10 instancias de ColaRegistros.
     * Representan las cubetas correspondientes a los dígitos del sistema decimal (0 al 9).
     */
    private ColaRegistros[] arregloCubetas;

    /**
     * Inicializa la clase y reserva en memoria las 10 estructuras de cola requeridas.
     */
    public RadixSortImpl() {
        arregloCubetas = new ColaRegistros[10];
        for (int indice = 0; indice < 10; indice++) {
            arregloCubetas[indice] = new ColaRegistros();
        }
    }

    /***
     * Implementacion de la logica principal
     * @param listaDatos
     */
    @Override
    public void sort(List<RegistroFinanciero> listaDatos) {
        if (listaDatos == null || listaDatos.size() <= 1) return;

        // Fase 1: Ordenamiento basado en el criterio de desempate (Precio de cierre).
        ejecutarFaseOrdenamiento(listaDatos, false);

        // Fase 2: Ordenamiento basado en el criterio principal (Fecha).
        // Al ser un algoritmo estable, se preserva el orden parcial de la Fase 1.
        ejecutarFaseOrdenamiento(listaDatos, true);
    }

    /**
     * Coordina el proceso de ordenamiento para un criterio específico,
     * determinando la magnitud del valor máximo y derivando la ejecución principal.
     *
     * @param listaDatos Colección de registros a ordenar.
     * @param evaluarFecha Determina si la evaluación se realiza sobre la fecha (true) o el precio (false).
     */
    private void ejecutarFaseOrdenamiento(List<RegistroFinanciero> listaDatos, boolean evaluarFecha) {
        long valorMaximoEncontrado = 0;
        int totalDigitosRequeridos;

        // Búsqueda lineal del valor numérico más alto en la colección.
        for (int indice = 0; indice < listaDatos.size(); indice++) {
            long valorProcesado = obtenerValorNumerico(listaDatos.get(indice), evaluarFecha);
            if (valorMaximoEncontrado < valorProcesado) {
                valorMaximoEncontrado = valorProcesado;
            }
        }

        // Cálculo algebraico para determinar la cantidad de posiciones decimales del número mayor.
        totalDigitosRequeridos = String.valueOf(valorMaximoEncontrado).length();

        procesarDistribucionYRecoleccion(listaDatos, totalDigitosRequeridos, evaluarFecha);
    }

    /**
     * Aplica el algoritmo Radix Sort distribuyendo los datos en cubetas
     * iteración tras iteración según la posición decimal en evaluación.
     *
     * @param listaDatos Colección de registros a alterar.
     * @param totalDigitos Límite de iteraciones del ciclo principal.
     * @param evaluarFecha Criterio activo de ordenamiento.
     */
    private void procesarDistribucionYRecoleccion(List<RegistroFinanciero> listaDatos, int totalDigitos, boolean evaluarFecha) {
        int indiceReasignacion;

        // El ciclo externo define la posición decimal actual (1=unidades, 2=decenas, etc.).
        for (int posicionActual = 1; posicionActual <= totalDigitos; posicionActual++) {
            indiceReasignacion = 0;

            // 1. Proceso de Distribución (Scatter): Asignación a cubetas.
            for (int indiceLista = 0; indiceLista < listaDatos.size(); indiceLista++) {
                RegistroFinanciero registroActual = listaDatos.get(indiceLista);
                long valorNumerico = obtenerValorNumerico(registroActual, evaluarFecha);
                int digitoAislado = extraerDigitoEnPosicion(valorNumerico, posicionActual);

                arregloCubetas[digitoAislado].insertarRegistro(registroActual);
            }

            // 2. Proceso de Recolección (Gather): Reconstrucción de la lista.
            for (int indiceCubeta = 0; indiceCubeta < arregloCubetas.length; indiceCubeta++) {
                while (!arregloCubetas[indiceCubeta].careceDeElementos()) {
                    listaDatos.set(indiceReasignacion, arregloCubetas[indiceCubeta].extraerRegistro());
                    indiceReasignacion++;
                }
            }
        }
    }

    // ==========================================
    // MÉTODOS DE TRANSFORMACIÓN Y MATEMÁTICA
    // ==========================================

    /**
     * Aísla un dígito específico de un valor numérico largo.
     *
     * @param numeroOrigen El valor numérico completo.
     * @param posicionDecimal La magnitud a aislar (1 para unidades, 2 para decenas, etc.).
     * @return El dígito calculado en el rango de 0 a 9.
     */
    private int extraerDigitoEnPosicion(long numeroOrigen, int posicionDecimal) {
        return (int) ((numeroOrigen / (long) Math.pow(10, posicionDecimal - 1)) % 10);
    }

    /**
     * Convierte los atributos complejos del registro en representaciones numéricas
     * enteras compatibles con la lógica de agrupamiento de Radix Sort.
     *
     * @param registro Objeto del cual se extraerá la información.
     * @param evaluarFecha Indica el atributo objetivo a transformar.
     * @return Representación numérica del atributo (Formato AAAAMMDD o precio en centavos).
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