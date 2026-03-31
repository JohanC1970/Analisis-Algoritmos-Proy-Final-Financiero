package model;

import java.time.LocalDate;

/**
 * RegistroFinanciero.java - Modelo de datos central del proyecto.
 *
 * Esta clase representa un dia de cotizacion de un activo financiero.
 * Cada objeto de esta clase equivale a una fila en una tabla de datos historicos:
 * tiene el simbolo del activo, la fecha, los cuatro precios del dia
 * (apertura, cierre, maximo y minimo) y el volumen negociado.
 *
 * Es la clase mas importante del proyecto porque todos los algoritmos de
 * ordenamiento trabajan sobre listas de objetos de este tipo.
 *
 * Implementa Comparable para definir un orden natural entre registros,
 * lo que permite que los algoritmos de sorting puedan comparar elementos
 * sin necesitar un Comparator externo.
 */
public class RegistroFinanciero implements Comparable<RegistroFinanciero> {

    // Simbolo bursatil del activo. Ejemplos: "VOO", "AAPL", "MSFT".
    // Nos sirve para identificar a que empresa o fondo pertenece cada registro
    // cuando mezclamos los datos de los 20 activos en una sola lista.
    private String activo;

    // Fecha del dia de mercado.
    private LocalDate fecha;

    // Los cuatro precios que se requieren
    private double open;   // Precio al que abrio el activo cuando el mercado abrio ese dia.
    private double close;  // Precio al que cerro cuando el mercado cerro ese dia.
    private double high;   // Precio mas alto que alcanzo en algun momento del dia.
    private double low;    // Precio mas bajo que alcanzo en algun momento del dia.

    // Numero total de acciones o unidades que cambiaron de mano ese dia.
    // Es un long (no int) porque los volumenes de activos como SPY o AAPL
    // pueden superar facilmente los 2,000 millones, que es el limite de int.
    private long volumen;

    //Constructor
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

    // Getters estandar. No hay setters porque un registro historico no deberia
    // modificarse una vez creado: los datos del pasado no cambian.
    public String    getActivo()  { return activo;   }
    public LocalDate getFecha()   { return fecha;    }
    public double    getOpen()    { return open;     }
    public double    getClose()   { return close;    }
    public double    getHigh()    { return high;     }
    public double    getLow()     { return low;      }
    public long      getVolumen() { return volumen;  }

    /**
     * Define el orden natural de los registros para los algoritmos de sorting.
     *
     * Criterio de ordenamiento:
     *   1. Primero por fecha ascendente: el registro mas antiguo va primero, ya que lo necesitamos
     *   para analizar series de tiempo cronologicamente.
     *   2. Si dos registros tienen la misma fecha (puede pasar cuando mezclamos
     *   varios activos), desempatamos por precio de cierre ascendente.
     *
     * @param o  El otro registro con el que se compara.
     * @return   Negativo si este va antes, positivo si va despues, 0 si son equivalentes.
     */
    @Override
    public int compareTo(RegistroFinanciero o) {

        // LocalDate.compareTo() ya implementa la comparacion cronologica correctamente.
        int comparacionFecha = this.fecha.compareTo(o.fecha);

        // Si las fechas son distintas, ese resultado define el orden y no necesitamos mas.
        if (comparacionFecha != 0) {
            return comparacionFecha;
        }

        // Llegamos aqui solo si las fechas son identicas. Desempatamos por precio de cierre.
        return Double.compare(this.close, o.close);
    }


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
