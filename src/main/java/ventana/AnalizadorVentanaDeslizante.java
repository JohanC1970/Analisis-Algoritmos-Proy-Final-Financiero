package ventana;

import model.RegistroFinanciero;
import java.util.ArrayList;
import java.util.List;

/*
 * AnalizadorVentanaDeslizante.java - Motor del algoritmo de ventana deslizante.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * ALGORITMO DE VENTANA DESLIZANTE (SLIDING WINDOW)
 * ═══════════════════════════════════════════════════════════════════════
 *   Dado un historial de precios S = [s₀, s₁, …, s_{n-1}] y un tamaño de
 *   ventana w (TAMANO_VENTANA):
 *
 *   Para cada posición i ∈ {0, …, n − w}:
 *     ventana_i = S[i .. i + w − 1]        ← sublista de w elementos
 *     Para cada patrón P registrado:
 *       si P.detectar(ventana_i) → contador[P]++
 *
 *   Total de ventanas analizadas = n − w + 1
 *
 *   Resultado por patrón:
 *     frecuencia[P] = contador[P] / (n − w + 1)
 *
 * ═══════════════════════════════════════════════════════════════════════
 * ELECCIÓN DE TAMANO_VENTANA = 20
 * ═══════════════════════════════════════════════════════════════════════
 *   20 días de mercado ≈ 1 mes bursátil (4 semanas de 5 días hábiles).
 *   Esta granularidad captura patrones de corto-mediano plazo sin ser
 *   tan pequeña como para generar ruido ni tan grande como para perder
 *   resolución temporal. Es la elección estándar en análisis técnico.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * COMPLEJIDAD TOTAL
 * ═══════════════════════════════════════════════════════════════════════
 *   O(n × w × p)
 *   donde n = días de historia, w = TAMANO_VENTANA, p = nº de patrones.
 *   Para n=1250, w=20, p=2 → ~50.000 operaciones elementales.
 *   Es completamente lineal en n para w y p fijos.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * NOTA SOBRE subList()
 * ═══════════════════════════════════════════════════════════════════════
 *   List.subList(from, to) devuelve una VISTA sobre la lista original,
 *   no una copia. Por eso el bucle es O(1) por ventana en términos de
 *   creación de objetos: no se aloca memoria por cada ventana.
 *   Los patrones que acceden a ventana.get(i) acceden directamente a la
 *   lista subyacente con desplazamiento de índice.
 */
public class AnalizadorVentanaDeslizante {

    /*
     * Tamaño de la ventana deslizante en días de mercado.
     * 20 días ≈ 1 mes bursátil.
     */
    public static final int TAMANO_VENTANA = 20;

    // Lista de patrones a detectar en cada ventana.
    private final List<PatronVentana> patrones;

    public AnalizadorVentanaDeslizante(List<PatronVentana> patrones) {
        this.patrones = patrones;
    }

    /**
     * Ejecuta el análisis de ventana deslizante sobre la serie histórica de un activo.
     *
     * @param serie Lista de registros ordenados cronológicamente para UN solo activo.
     *              Si la serie tiene menos de TAMANO_VENTANA elementos, no se puede
     *              formar ninguna ventana completa y todos los contadores quedan en 0.
     * @return Lista de ResultadoPatron, uno por cada patrón registrado, en el mismo
     *         orden en que los patrones fueron dados al constructor.
     */
    public List<ResultadoPatron> analizar(List<RegistroFinanciero> serie) {
        List<ResultadoPatron> resultados = new ArrayList<>();

        // Si la serie es demasiado corta, devolvemos resultados vacíos pero válidos.
        if (serie == null || serie.size() < TAMANO_VENTANA) {
            for (PatronVentana patron : patrones) {
                resultados.add(new ResultadoPatron(
                    patron.getNombre(), patron.getDescripcion(), patron.getFormalizacion(),
                    0, 0
                ));
            }
            return resultados;
        }

        int totalVentanas = serie.size() - TAMANO_VENTANA + 1;

        // contadores[p] acumula cuántas ventanas contenían el patrón p.
        int[] contadores = new int[patrones.size()];

        /*
         * BUCLE PRINCIPAL DE LA VENTANA DESLIZANTE
         *
         * La ventana arranca en i=0 y termina en i = serie.size() - TAMANO_VENTANA.
         * subList(i, i + TAMANO_VENTANA) es O(1) porque no copia datos.
         */
        for (int i = 0; i <= serie.size() - TAMANO_VENTANA; i++) {
            // Vista sin copia de la sublista actual: días [i, i+TAMANO_VENTANA).
            List<RegistroFinanciero> ventanaActual = serie.subList(i, i + TAMANO_VENTANA);

            for (int p = 0; p < patrones.size(); p++) {
                if (patrones.get(p).detectar(ventanaActual)) {
                    contadores[p]++;
                }
            }
        }

        // Construimos los ResultadoPatron finales con los contadores acumulados.
        for (int p = 0; p < patrones.size(); p++) {
            PatronVentana patron = patrones.get(p);
            resultados.add(new ResultadoPatron(
                patron.getNombre(),
                patron.getDescripcion(),
                patron.getFormalizacion(),
                totalVentanas,
                contadores[p]
            ));
        }

        return resultados;
    }
}
