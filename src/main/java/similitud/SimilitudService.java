package similitud;

import model.RegistroFinanciero;

import java.time.LocalDate;
import java.util.*;

public class SimilitudService {


    /*
     * Los cuatro algoritmos del Requerimiento 3, inicializados una sola vez.
     * Todos implementan AlgoritmoSimilitud, por lo que se tratan uniformemente.
     */
    private final List<AlgoritmoSimilitud> algoritmos;

    /*
     * Mapa maestro: ticker -> (fecha -> retorno diario).
     * Se construye una sola vez al crear el servicio a partir de la lista de registros.
     * TreeMap por fecha para que las fechas queden ordenadas cronológicamente,
     * lo que es importante para calcular retornos secuenciales correctamente.
     */
    private final Map<String, TreeMap<LocalDate, Double>> retornosPorActivo;

    /*
     * Lista de todos los tickers disponibles para mostrar en el selector de la UI.
     */
    private final List<String> tickersDisponibles;

    /**
     * Constructor: indexa todos los registros y calcula los retornos diarios.
     *
     * @param registros Lista maestra con todos los RegistroFinanciero del ETL.
     */
    public SimilitudService(List<RegistroFinanciero> registros) {
        this.algoritmos = Arrays.asList(
                new EuclideanDistance(),
                new PearsonCorrelation(),
                new DynamicTimeWarping(),
                new CosineSimilarity()
        );

        this.retornosPorActivo = new HashMap<>();
        this.tickersDisponibles = new ArrayList<>();

        construirRetornosPorActivo(registros);
    }

    /**
     * Construye el mapa de retornos diarios por activo a partir de la lista de registros.
     *
     * Proceso:
     *   1. Agrupar registros por ticker.
     *   2. Para cada ticker, ordenar por fecha.
     *   3. Calcular el retorno diario de cada par de días consecutivos.
     *
     * @param registros Lista maestra del ETL.
     */
    private void construirRetornosPorActivo(List<RegistroFinanciero> registros) {

        // PASO 1: Agrupar precios de cierre por ticker, ordenados por fecha.
        Map<String, TreeMap<LocalDate, Double>> preciosPorActivo = new HashMap<>();

        for (RegistroFinanciero r : registros) {
            preciosPorActivo
                    .computeIfAbsent(r.getActivo(), k -> new TreeMap<>())
                    .put(r.getFecha(), r.getClose());
        }

        // PASO 2: Para cada ticker, calcular los retornos diarios.
        for (Map.Entry<String, TreeMap<LocalDate, Double>> entry : preciosPorActivo.entrySet()) {
            String ticker = entry.getKey();
            TreeMap<LocalDate, Double> precios = entry.getValue();

            TreeMap<LocalDate, Double> retornos = calcularRetornos(precios);

            // Solo guardamos el ticker si tiene al menos 2 días (mínimo para calcular 1 retorno).
            if (!retornos.isEmpty()) {
                retornosPorActivo.put(ticker, retornos);
                tickersDisponibles.add(ticker);
            }
        }

        Collections.sort(tickersDisponibles); // Ordenamos alfabéticamente para la UI.
        System.out.println("[SimilitudService] Retornos calculados para " + tickersDisponibles.size() + " activos.");
    }

    /**
     * Calcula la serie de retornos diarios a partir de precios de cierre ordenados por fecha.
     *
     * Fórmula: r_t = ln(close_t / close_{t-1})
     *
     * Se usa el retorno logarítmico (en lugar del aritmético) por dos razones:
     *   1. Aditividad temporal: la suma de retornos logarítmicos diarios equivale
     *      exactamente al retorno logarítmico del período completo, sin el sesgo
     *      de composición que introduce el retorno aritmético.
     *   2. Mejor comportamiento estadístico: los retornos logarítmicos son más
     *      simétricos y aproximadamente normales, lo que mejora la fiabilidad de
     *      los algoritmos de similitud (Pearson, coseno) que asumen distribuciones
     *      centradas.
     *
     * El primer día no tiene retorno (no hay día anterior), así que la serie de retornos
     * tiene n-1 elementos si hay n días de precios.
     *
     * @param precios TreeMap fecha -> precio de cierre, ordenado cronológicamente.
     * @return TreeMap fecha -> retorno diario. La fecha de cada retorno es la del día "t"
     *         (el día actual, no el anterior).
     */
    private TreeMap<LocalDate, Double> calcularRetornos(TreeMap<LocalDate, Double> precios) {
        TreeMap<LocalDate, Double> retornos = new TreeMap<>();

        List<Map.Entry<LocalDate, Double>> entradas = new ArrayList<>(precios.entrySet());

        for (int i = 1; i < entradas.size(); i++) {
            double precioHoy   = entradas.get(i).getValue();
            double precioAyer  = entradas.get(i - 1).getValue();
            LocalDate fechaHoy = entradas.get(i).getKey();

            // Protección contra precios no positivos (logaritmo indefinido para ≤ 0).
            if (precioAyer <= 0.0 || precioHoy <= 0.0) continue;

            double retorno = Math.log(precioHoy / precioAyer);
            retornos.put(fechaHoy, retorno);
        }

        return retornos;
    }

    /**
     * Ejecuta los cuatro algoritmos de similitud entre dos activos.
     *
     * Alinea las series por fecha (intersección), extrae las listas de retornos
     * en orden cronológico, y llama a cada algoritmo.
     *
     * @param tickerA Símbolo del primer activo (ej: "AAPL").
     * @param tickerB Símbolo del segundo activo (ej: "MSFT").
     * @return ResultadoSimilitud con los 4 valores calculados y metadatos.
     *         Devuelve null si alguno de los tickers no existe o no hay fechas en común.
     */
    public ResultadoSimilitud calcularSimilitud(String tickerA, String tickerB) {

        // Validamos que ambos tickers existan en el dataset.
        if (!retornosPorActivo.containsKey(tickerA)) {
            System.err.println("[SimilitudService] Ticker no encontrado: " + tickerA);
            return null;
        }
        if (!retornosPorActivo.containsKey(tickerB)) {
            System.err.println("[SimilitudService] Ticker no encontrado: " + tickerB);
            return null;
        }

        TreeMap<LocalDate, Double> retornosA = retornosPorActivo.get(tickerA);
        TreeMap<LocalDate, Double> retornosB = retornosPorActivo.get(tickerB);

        // ─── ALINEACIÓN TEMPORAL: intersección de fechas ─────────────────────────
        /*
         * Buscamos las fechas en que AMBOS activos tienen retorno calculado.
         * Usamos retainAll sobre una copia para no modificar el original.
         * El resultado es el conjunto de fechas comunes, que usamos para
         * extraer las series alineadas.
         */
        Set<LocalDate> fechasA = new TreeSet<>(retornosA.keySet()); // TreeSet = ordenado
        Set<LocalDate> fechasB = new TreeSet<>(retornosB.keySet());

        Set<LocalDate> fechasComunes = new TreeSet<>(fechasA);
        fechasComunes.retainAll(fechasB); // Intersección

        if (fechasComunes.isEmpty()) {
            System.err.println("[SimilitudService] Sin fechas en común entre " + tickerA + " y " + tickerB);
            return null;
        }

        // ─── EXTRACCIÓN DE SERIES ALINEADAS ──────────────────────────────────────
        List<Double> serieA = new ArrayList<>();
        List<Double> serieB = new ArrayList<>();

        // Recorremos las fechas en orden cronológico (TreeSet las mantiene ordenadas).
        for (LocalDate fecha : fechasComunes) {
            serieA.add(retornosA.get(fecha));
            serieB.add(retornosB.get(fecha));
        }

        int nDias = serieA.size();
        System.out.println("[SimilitudService] Comparando " + tickerA + " vs " + tickerB
                + " usando " + nDias + " días comunes.");

        // ─── EJECUCIÓN DE LOS 4 ALGORITMOS ───────────────────────────────────────
        List<ResultadoAlgoritmo> resultados = new ArrayList<>();

        for (AlgoritmoSimilitud algoritmo : algoritmos) {
            long inicio = System.currentTimeMillis();
            double valor = algoritmo.calcular(serieA, serieB);
            long tiempo = System.currentTimeMillis() - inicio;

            resultados.add(new ResultadoAlgoritmo(
                    algoritmo.getNombre(),
                    algoritmo.getComplejidad(),
                    algoritmo.getInterpretacion(),
                    valor,
                    tiempo
            ));

            System.out.printf("  [%s] %.6f  (%d ms)%n", algoritmo.getNombre(), valor, tiempo);
        }

        // ─── ESTADÍSTICAS BÁSICAS DE CADA SERIE ──────────────────────────────────
        EstadisticasSerie statsA = calcularEstadisticas(tickerA, serieA);
        EstadisticasSerie statsB = calcularEstadisticas(tickerB, serieB);

        return new ResultadoSimilitud(tickerA, tickerB, nDias, resultados, statsA, statsB);
    }

    /**
     * Calcula estadísticas descriptivas básicas de una serie de retornos.
     * Se muestran en la UI como contexto junto a los valores de similitud.
     *
     * @param ticker Símbolo del activo.
     * @param serie  Lista de retornos diarios.
     * @return EstadisticasSerie con media, desv.std, min y max.
     */
    private EstadisticasSerie calcularEstadisticas(String ticker, List<Double> serie) {
        double suma = 0.0;
        double min  = Double.MAX_VALUE;
        double max  = Double.MIN_VALUE;

        for (double r : serie) {
            suma += r;
            if (r < min) min = r;
            if (r > max) max = r;
        }

        double media = suma / serie.size();

        double sumaCuadrados = 0.0;
        for (double r : serie) {
            double diff = r - media;
            sumaCuadrados += diff * diff;
        }

        double desvStd = Math.sqrt(sumaCuadrados / serie.size());
        double volatilidad = desvStd * Math.sqrt(252); // Anualizada (252 días de mercado/año)

        return new EstadisticasSerie(ticker, media, desvStd, volatilidad, min, max, serie.size());
    }

    /**
     * Devuelve la lista de todos los tickers disponibles para la UI.
     *
     * @return Lista de tickers ordenados alfabéticamente.
     */
    public List<String> getTickersDisponibles() {
        return Collections.unmodifiableList(tickersDisponibles);
    }

    // ─── CLASES DE DATOS INTERNAS ────────────────────────────────────────────────

    /**
     * Resultado de un solo algoritmo de similitud.
     */
    public static class ResultadoAlgoritmo {
        public final String nombre;
        public final String complejidad;
        public final String interpretacion;
        public final double valor;
        public final long   tiempoMs;

        public ResultadoAlgoritmo(String nombre, String complejidad, String interpretacion,
                                  double valor, long tiempoMs) {
            this.nombre         = nombre;
            this.complejidad    = complejidad;
            this.interpretacion = interpretacion;
            this.valor          = valor;
            this.tiempoMs       = tiempoMs;
        }
    }

    /**
     * Estadísticas descriptivas de una serie de retornos.
     */
    public static class EstadisticasSerie {
        public final String ticker;
        public final double mediaRetorno;
        public final double desvStd;
        public final double volatilidadAnual;
        public final double minRetorno;
        public final double maxRetorno;
        public final int    nObservaciones;

        public EstadisticasSerie(String ticker, double mediaRetorno, double desvStd,
                                 double volatilidadAnual, double minRetorno,
                                 double maxRetorno, int nObservaciones) {
            this.ticker           = ticker;
            this.mediaRetorno     = mediaRetorno;
            this.desvStd          = desvStd;
            this.volatilidadAnual = volatilidadAnual;
            this.minRetorno       = minRetorno;
            this.maxRetorno       = maxRetorno;
            this.nObservaciones   = nObservaciones;
        }
    }

    /**
     * Resultado completo de comparar dos activos: los 4 valores + estadísticas.
     */
    public static class ResultadoSimilitud {
        public final String tickerA;
        public final String tickerB;
        public final int    diasComunes;
        public final List<ResultadoAlgoritmo> resultados;
        public final EstadisticasSerie statsA;
        public final EstadisticasSerie statsB;

        public ResultadoSimilitud(String tickerA, String tickerB, int diasComunes,
                                  List<ResultadoAlgoritmo> resultados,
                                  EstadisticasSerie statsA, EstadisticasSerie statsB) {
            this.tickerA    = tickerA;
            this.tickerB    = tickerB;
            this.diasComunes = diasComunes;
            this.resultados = resultados;
            this.statsA     = statsA;
            this.statsB     = statsB;
        }
    }

}
