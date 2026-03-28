package model;

import java.time.LocalDate;

/**
 * RegistroFinanciero representa un día de cotización de un activo financiero.
 *
 * Cada instancia de esta clase equivale a una fila en una tabla de datos históricos:
 * contiene el símbolo del activo, la fecha, y los cuatro precios clave del día
 * (apertura, cierre, máximo y mínimo), más el volumen negociado.
 *
 * Esta clase implementa Comparable para que las listas de registros puedan
 * ordenarse cronológicamente, lo cual es fundamental para los algoritmos
 * de análisis y visualización del proyecto.
 */
public class RegistroFinanciero implements Comparable<RegistroFinanciero> {

    /** activo (ej: "VOO", "AAPL", "ECOPETROL.CL") */
    private String activo;

    /** Fecha del día de mercado al que corresponde este registro */
    private LocalDate fecha;

    /** Precio al que abrió el activo ese día (primer precio del día) */
    private double open;

    /** Precio al que cerró el activo ese día (último precio del día) */
    private double close;

    /** Precio más alto alcanzado durante el día */
    private double high;

    /** Precio más bajo alcanzado durante el día */
    private double low;

    /** Número total de acciones o unidades negociadas durante el día */
    private long volumen;

    /**
     * Crea un nuevo registro financiero con todos sus datos del día.
     *
     * @param activo  Símbolo del activo financiero (ej: "VOO")
     * @param fecha   Fecha del día de mercado
     * @param open    Precio de apertura
     * @param close   Precio de cierre
     * @param high    Precio máximo del día
     * @param low     Precio mínimo del día
     * @param volumen Volumen de unidades negociadas
     */
    public RegistroFinanciero(String activo, LocalDate fecha, double open, double close,
                               double high, double low, long volumen) {
        this.activo  = activo;
        this.fecha   = fecha;
        this.open    = open;
        this.close   = close;
        this.high    = high;
        this.low     = low;
        this.volumen = volumen;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getActivo() { return activo; }

    public LocalDate getFecha() { return fecha; }

    public double getOpen() { return open; }

    public double getClose() { return close; }

    public double getHigh() { return high; }

    public double getLow() { return low; }

    public long getVolumen() { return volumen; }

    // -------------------------------------------------------------------------
    // Comparable
    // -------------------------------------------------------------------------

    /**
     * Compara este registro con otro para definir su orden natural.
     *
     * Criterio de ordenamiento (en orden de prioridad):
     * 1. Por fecha ascendente (el más antiguo primero).
     * 2. Si las fechas son iguales (caso raro pero posible con múltiples activos),
     *    se ordena por precio de cierre ascendente.
     *
     * Este orden es el que usarán los algoritmos de sorting del proyecto.
     *
     * @param o El otro RegistroFinanciero con el que se compara.
     * @return Negativo si este registro va antes, positivo si va después, 0 si son iguales.
     */
    @Override
    public int compareTo(RegistroFinanciero o) {
        // Comparamos primero por fecha usando el compareTo nativo de LocalDate
        int comparacionFecha = this.fecha.compareTo(o.fecha);

        // Si las fechas son distintas, ese resultado ya define el orden
        if (comparacionFecha != 0) {
            return comparacionFecha;
        }

        // Fechas iguales: desempatamos por precio de cierre
        // Double.compare evita errores de precisión que tendría una resta directa
        return Double.compare(this.close, o.close);
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "RegistroFinanciero{" +
                "activo='" + activo + '\'' +
                ", fecha=" + fecha +
                ", close=" + close +
                ", volumen=" + volumen +
                '}';
    }
}
