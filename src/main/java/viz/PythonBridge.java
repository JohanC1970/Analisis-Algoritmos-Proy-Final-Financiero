package viz;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * PythonBridge es el puente de integración entre el backend Java y los scripts
 * de visualización en Python.
 *
 * Utiliza ProcessBuilder para lanzar el intérprete de Python como un subproceso,
 * pasándole el script y los argumentos necesarios. Captura stdout y stderr del
 * proceso hijo y los redirige a la consola de Java para trazabilidad completa.
 *
 * Contrato de archivos esperado (generados por el benchmark en Java):
 * data/benchmark.csv  → tiempos de los algoritmos
 * data/volumen.csv    → 15 días con mayor volumen (ordenados asc.)
 */
public class PythonBridge {

    /** Ruta al script de visualización, relativa a la raíz del proyecto. */
    private static final String SCRIPT_PATH = "python_viz/visualizacion.py";

    /**
     * Ejecuta el script de visualización de Python de forma síncrona.
     * El proceso Java espera a que Python termine antes de continuar.
     *
     * @param rutaBenchmarkCsv Ruta al CSV con los tiempos de los algoritmos.
     * @param rutaVolumenCsv   Ruta al CSV con los 15 días de mayor volumen.
     * @return true si Python terminó con código de salida 0 (éxito), false en caso contrario.
     */
    public boolean ejecutarVisualizacion(String rutaBenchmarkCsv, String rutaVolumenCsv) {
        System.out.println("\n[PythonBridge] Llamando a Python para graficar resultados...");
        System.out.println("[PythonBridge] Script : " + SCRIPT_PATH);
        System.out.println("[PythonBridge] CSV benchmark : " + rutaBenchmarkCsv);
        System.out.println("[PythonBridge] CSV volumen   : " + rutaVolumenCsv);

        // Construimos el comando dinámico
        List<String> comando = buildComando(rutaBenchmarkCsv, rutaVolumenCsv);

        try {
            ProcessBuilder pb = new ProcessBuilder(comando);

            // Directorio de trabajo = raíz del proyecto para que las rutas relativas funcionen
            pb.directory(new File(System.getProperty("user.dir")));

            // Separamos stdout y stderr para poder leerlos en hilos independientes
            pb.redirectErrorStream(false);

            Process proceso = pb.start();

            // Lanzamos dos hilos lectores para evitar bloqueos por buffer lleno
            Thread hiloStdout = new Thread(() -> leerFlujo(
                    new BufferedReader(new InputStreamReader(proceso.getInputStream())),
                    "[Python stdout]"
            ));
            Thread hiloStderr = new Thread(() -> leerFlujo(
                    new BufferedReader(new InputStreamReader(proceso.getErrorStream())),
                    "[Python stderr]"
            ));

            hiloStdout.start();
            hiloStderr.start();

            // Esperamos a que el proceso y ambos lectores terminen
            int codigoSalida = proceso.waitFor();
            hiloStdout.join();
            hiloStderr.join();

            if (codigoSalida == 0) {
                System.out.println("[PythonBridge] Visualización completada exitosamente.");
            } else {
                System.err.println("[PythonBridge] Python terminó con código de error: " + codigoSalida);
            }

            return codigoSalida == 0;

        } catch (Exception e) {
            System.err.println("[PythonBridge] Error al lanzar el proceso Python: " + e.getMessage());
            System.err.println("[PythonBridge] Asegúrate de que Python esté instalado y en el PATH del sistema.");
            return false;
        }
    }

    /**
     * Construye la lista de tokens del comando a ejecutar,
     * detectando automáticamente el ejecutable correcto de Python.
     */
    private List<String> buildComando(String rutaBenchmarkCsv, String rutaVolumenCsv) {
        List<String> cmd = new ArrayList<>();

        // Detectar el comando de Python correcto para la máquina actual
        String interprete = detectarComandoPython();
        System.out.println("[PythonBridge] Comando detectado para esta máquina: " + interprete);

        cmd.add(interprete);
        cmd.add(SCRIPT_PATH);
        cmd.add(rutaBenchmarkCsv);
        cmd.add(rutaVolumenCsv);
        return cmd;
    }

    /**
     * Prueba diferentes comandos de Python en segundo plano para ver cuál
     * está instalado y configurado correctamente en la computadora actual.
     * Esto evita conflictos entre sistemas operativos o instalaciones (py vs python vs python3).
     */
    private String detectarComandoPython() {

        // PRIMERO: intentar con el .venv del propio proyecto (más confiable)
        String rutaVenv = System.getProperty("user.dir")
                + File.separator + ".venv"
                + File.separator + "Scripts"
                + File.separator + "python.exe";

        File pythonVenv = new File(rutaVenv);
        if (pythonVenv.exists()) {
            System.out.println("[PythonBridge] Usando Python del .venv del proyecto.");
            return rutaVenv;
        }

        // SEGUNDO: buscar en rutas comunes de instalación de Python en Windows
        String[] rutasWindows = {
                "C:\\Python313\\python.exe",
                "C:\\Python312\\python.exe",
                "C:\\Python311\\python.exe",
                "C:\\Python310\\python.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python313\\python.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
                System.getProperty("user.home") + "\\AppData\\Roaming\\uv\\python\\cpython-3.14.3-windows-x86_64-none\\python.exe"
        };

        for (String ruta : rutasWindows) {
            if (new File(ruta).exists()) {
                System.out.println("[PythonBridge] Python encontrado en: " + ruta);
                return ruta;
            }
        }

        // TERCERO: intentar comandos del PATH como fallback
        String[] posiblesComandos = {"py", "python", "python3"};
        for (String comando : posiblesComandos) {
            try {
                ProcessBuilder pb = new ProcessBuilder(comando, "--version");
                pb.redirectErrorStream(true);
                Process proceso = pb.start();
                if (proceso.waitFor() == 0) {
                    return comando;
                }
            } catch (Exception ignored) {}
        }

        return "python";
    }

    /**
     * Lee línea a línea un flujo de texto (stdout o stderr del proceso hijo)
     * e imprime cada línea en la consola de Java con un prefijo identificador.
     *
     * @param reader  Lector del flujo.
     * @param prefijo Etiqueta para distinguir stdout de stderr en la consola.
     */
    private void leerFlujo(BufferedReader reader, String prefijo) {
        try {
            String linea;
            while ((linea = reader.readLine()) != null) {
                System.out.println(prefijo + " " + linea);
            }
        } catch (Exception e) {
            System.err.println(prefijo + " Error leyendo flujo: " + e.getMessage());
        }
    }
}