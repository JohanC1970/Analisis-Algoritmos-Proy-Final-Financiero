package ventana;

import model.RegistroFinanciero;
import java.util.*;

/*
 * AnalisisRiesgoService.java - Fachada del servicio de análisis de riesgo.
 *
 * Actúa como punto de entrada único para el AnalisisRiesgoServer:
 * recibe los datos del ETL, ejecuta la clasificación completa y expone
 * los resultados a través de métodos simples.
 *
 * Adicionalmente, calcula el ResumenRiesgo (cuántos activos hay en cada
 * categoría, volatilidades extremas y promedio) que se muestra en el
 * dashboard de la interfaz web.
 *
 * PATRÓN UTILIZADO: Facade (Fachada)
 *   El servidor HTTP no necesita conocer ClasificadorRiesgo,
 *   AnalizadorVentanaDeslizante ni ningún detalle de implementación.
 *   Solo interactúa con getActivosOrdenados(), getResumen() y buscarTicker().
 */
public class AnalisisRiesgoService {

    // Lista de activos con métricas completas, ordenada por riesgo ascendente.
    private final List<MetricasActivo> activosOrdenados;

    // Resumen agregado para el dashboard de la UI.
    private final ResumenRiesgo resumen;

    /**
     * Construye el servicio ejecutando la clasificación completa sobre todos los registros.
     * El constructor es costoso (hace el análisis completo), pero se llama una sola vez
     * al arrancar la aplicación.
     *
     * @param registros Lista maestra con todos los RegistroFinanciero del ETL.
     */
    public AnalisisRiesgoService(List<RegistroFinanciero> registros) {
        ClasificadorRiesgo clasificador = new ClasificadorRiesgo();
        this.activosOrdenados = clasificador.clasificarYOrdenar(registros);
        this.resumen          = calcularResumen(this.activosOrdenados);
    }

    /**
     * Devuelve la lista completa de activos ordenada por riesgo ascendente.
     * El primer elemento es el activo más conservador (menor volatilidad).
     *
     * @return Vista no modificable de la lista interna.
     */
    public List<MetricasActivo> getActivosOrdenados() {
        return Collections.unmodifiableList(activosOrdenados);
    }

    /**
     * Devuelve el resumen con conteos por categoría y estadísticas globales.
     *
     * @return ResumenRiesgo del portafolio completo.
     */
    public ResumenRiesgo getResumen() {
        return resumen;
    }

    /**
     * Busca las métricas de un ticker específico.
     *
     * @param ticker Símbolo del activo (case-insensitive, ej: "aapl" y "AAPL" son equivalentes).
     * @return MetricasActivo correspondiente, o null si el ticker no está en el dataset.
     */
    public MetricasActivo buscarTicker(String ticker) {
        String tickerNormalizado = ticker.toUpperCase().trim();
        for (MetricasActivo m : activosOrdenados) {
            if (m.ticker.equals(tickerNormalizado)) return m;
        }
        return null;
    }

    // ─── CÁLCULO DEL RESUMEN ──────────────────────────────────────────────────

    /**
     * Calcula el ResumenRiesgo agregado a partir de la lista de métricas.
     * Se llama una sola vez en el constructor; el resultado se cachea en this.resumen.
     */
    private ResumenRiesgo calcularResumen(List<MetricasActivo> activos) {
        int conservadores = 0, moderados = 0, agresivos = 0;
        double sumaVol = 0.0;
        double minVol  = activos.isEmpty() ? 0.0 : Double.MAX_VALUE;
        double maxVol  = 0.0;

        for (MetricasActivo m : activos) {
            // Clasificamos usando el enum para asegurarnos de que coincida con MetricasActivo.
            CategoriaRiesgo cat = CategoriaRiesgo.clasificar(m.volatilidadAnual);
            switch (cat) {
                case CONSERVADOR: conservadores++; break;
                case MODERADO:    moderados++;     break;
                case AGRESIVO:    agresivos++;     break;
            }
            sumaVol += m.volatilidadAnual;
            if (m.volatilidadAnual < minVol) minVol = m.volatilidadAnual;
            if (m.volatilidadAnual > maxVol) maxVol = m.volatilidadAnual;
        }

        double promedioVol = activos.isEmpty() ? 0.0 : sumaVol / activos.size();

        return new ResumenRiesgo(
            activos.size(), conservadores, moderados, agresivos,
            minVol == Double.MAX_VALUE ? 0.0 : minVol,
            maxVol,
            promedioVol,
            AnalizadorVentanaDeslizante.TAMANO_VENTANA
        );
    }

    // ─── CLASE INTERNA: RESUMEN AGREGADO ─────────────────────────────────────

    /**
     * Resumen estadístico del portafolio completo.
     * Se serializa a JSON por Gson para el endpoint GET /riesgo.
     */
    public static class ResumenRiesgo {
        public final int    totalActivos;
        public final int    conservadores;
        public final int    moderados;
        public final int    agresivos;
        public final double volatilidadMinima;    // Del activo más conservador.
        public final double volatilidadMaxima;    // Del activo más agresivo.
        public final double volatilidadPromedio;  // Promedio del portafolio.
        public final int    tamanoVentana;        // TAMANO_VENTANA usado en el análisis.

        // Porcentajes de cada categoría (calculados para la UI).
        public final double pctConservadores;
        public final double pctModerados;
        public final double pctAgresivos;

        public ResumenRiesgo(int totalActivos, int conservadores, int moderados, int agresivos,
                             double volatilidadMinima, double volatilidadMaxima,
                             double volatilidadPromedio, int tamanoVentana) {
            this.totalActivos         = totalActivos;
            this.conservadores        = conservadores;
            this.moderados            = moderados;
            this.agresivos            = agresivos;
            this.volatilidadMinima    = volatilidadMinima;
            this.volatilidadMaxima    = volatilidadMaxima;
            this.volatilidadPromedio  = volatilidadPromedio;
            this.tamanoVentana        = tamanoVentana;

            double total = totalActivos > 0 ? totalActivos : 1.0;
            this.pctConservadores = (conservadores / total) * 100.0;
            this.pctModerados     = (moderados     / total) * 100.0;
            this.pctAgresivos     = (agresivos     / total) * 100.0;
        }
    }
}
