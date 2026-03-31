package viz;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/*
 * PythonBridge.java - Puente de integracion entre Java y el script de visualizacion Python.
 *
 * Una vez que Java termina de generar los CSV con los resultados del benchmark y el
 * top de volumen, necesitamos que Python los lea y genere las graficas. El problema
 * es que Java no puede ejecutar codigo Python directamente: son dos runtimes distintos.
 *
 * La solucion es usar ProcessBuilder, que permite a Java lanzar cualquier proceso
 * del sistema operativo como si lo escribieramos en la terminal. En este caso,
 * lanzamos: python visualizacion.py data/benchmark.csv data/volumen.csv
 *
 * Desafios que resuelve esta clase:
 *   1. Detectar automaticamente donde esta instalado Python en la maquina del usuario,
 *      porque la ruta varia segun el SO y la version instalada.
 *   2. Capturar la salida de Python (stdout y stderr) y mostrarla en la consola de Java
 *      para que los errores de Python sean visibles sin tener que abrir otra terminal.
 *   3. Evitar bloqueos: si no leemos los buffers de stdout y stderr del proceso hijo,
 *      pueden llenarse y el proceso se congela. Por eso usamos hilos lectores.
 */
public class PythonBridge {

    // Ruta al script de Python relativa a la raiz del proyecto.
    // Si el script se mueve, solo hay que cambiar esta constante.
    private static final String SCRIPT_PATH = "python_viz/visualizacion.py";

    /*
     * Ejecuta el script de visualizacion de Python y espera a que termine.
     *
     * El metodo es sincrono: Java se bloquea hasta que Python termina.
     * Esto es intencional porque queremos que las graficas esten listas
     * antes de intentar abrirlas con abrirImagen() en Main.java.
     *
     * @param rutaBenchmarkCsv  Ruta al CSV con los tiempos de los algoritmos.
     * @param rutaVolumenCsv    Ruta al CSV con el top 15 de volumen.
     * @return                  true si Python termino exitosamente (codigo de salida 0),
     *                          false si hubo algun error.
     */
    public boolean ejecutarVisualizacion(String rutaBenchmarkCsv, String rutaVolumenCsv) {
        System.out.println("\n[PythonBridge] Llamando a Python para graficar resultados...");
        System.out.println("[PythonBridge] Script : " + SCRIPT_PATH);
        System.out.println("[PythonBridge] CSV benchmark : " + rutaBenchmarkCsv);
        System.out.println("[PythonBridge] CSV volumen   : " + rutaVolumenCsv);

        List<String> comando = buildComando(rutaBenchmarkCsv, rutaVolumenCsv);

        try {
            ProcessBuilder pb = new ProcessBuilder(comando);

            // Establecemos el directorio de trabajo como la raiz del proyecto.
            // Esto es importante para que las rutas relativas dentro del script Python
            // (como "output/") se resuelvan correctamente.
            pb.directory(new File(System.getProperty("user.dir")));

            // No redirigimos stderr a stdout (redirectErrorStream = false) porque
            // queremos distinguir mensajes normales de errores en la consola de Java.
            pb.redirectErrorStream(false);

            Process proceso = pb.start();

            /*
             * Por que necesitamos hilos para leer stdout y stderr:
             *
             * Los procesos del SO tienen buffers de salida de tamano limitado (~4KB en Windows).
             * Si el proceso hijo (Python) escribe mas de eso sin que nadie lea el buffer,
             * se bloquea esperando que alguien lo vacie. Si Java tambien esta bloqueado
             * esperando que Python termine (waitFor), tenemos un deadlock.
             *
             * La solucion es lanzar dos hilos que lean continuamente stdout y stderr
             * mientras el proceso principal espera con waitFor().
             */
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

            // Esperamos a que Python termine y obtenemos su codigo de salida.
            // 0 = exito, cualquier otro valor = error.
            int codigoSalida = proceso.waitFor();

            // Esperamos a que los hilos lectores terminen de procesar toda la salida.
            hiloStdout.join();
            hiloStderr.join();

            if (codigoSalida == 0) {
                System.out.println("[PythonBridge] Visualizacion completada exitosamente.");
            } else {
                System.err.println("[PythonBridge] Python termino con codigo de error: " + codigoSalida);
            }

            return codigoSalida == 0;

        } catch (Exception e) {
            System.err.println("[PythonBridge] Error al lanzar el proceso Python: " + e.getMessage());
            System.err.println("[PythonBridge] Asegurate de que Python este instalado y en el PATH del sistema.");
            return false;
        }
    }

    /*
     * Construye la lista de tokens del comando a ejecutar.
     *
     * ProcessBuilder recibe el comando como una lista de strings donde el primer
     * elemento es el ejecutable y los siguientes son los argumentos.
     * Equivale a escribir en la terminal: python visualizacion.py benchmark.csv volumen.csv
     *
     * @param rutaBenchmarkCsv  Primer argumento para el script Python.
     * @param rutaVolumenCsv    Segundo argumento para el script Python.
     * @return                  Lista de tokens lista para pasarle a ProcessBuilder.
     */
    private List<String> buildComando(String rutaBenchmarkCsv, String rutaVolumenCsv) {
        List<String> cmd = new ArrayList<>();

        String interprete = detectarComandoPython();
        System.out.println("[PythonBridge] Comando detectado para esta maquina: " + interprete);

        cmd.add(interprete);
        cmd.add(SCRIPT_PATH);
        cmd.add(rutaBenchmarkCsv);
        cmd.add(rutaVolumenCsv);
        return cmd;
    }

    /*
     * Detecta automaticamente el ejecutable de Python disponible en la maquina.
     *
     * El problema es que Python puede estar instalado en muchos lugares distintos
     * dependiendo del SO, la version y como se instalo. Esta funcion prueba en orden:
     *
     *   1. El entorno virtual (.venv) del propio proyecto: es el mas confiable porque
     *      tiene exactamente las dependencias que el proyecto necesita (pandas, matplotlib).
     *
     *   2. Rutas comunes de instalacion en Windows: C:\Python313\, AppData\Local\Programs\, etc.
     *      Cubrimos varias versiones (3.10 a 3.14) para mayor compatibilidad.
     *
     *   3. Comandos del PATH del sistema: "py" (launcher de Windows), "python", "python3".
     *      Probamos ejecutando "python --version" y viendo si responde sin error.
     *
     * @return  La ruta o comando de Python que funciona en esta maquina.
     */
    private String detectarComandoPython() {

        // Opcion 1: Python del entorno virtual del proyecto.
        // Si existe .venv/Scripts/python.exe, es la opcion mas segura.
        String rutaVenv = System.getProperty("user.dir")
                + File.separator + ".venv"
                + File.separator + "Scripts"
                + File.separator + "python.exe";

        if (new File(rutaVenv).exists()) {
            System.out.println("[PythonBridge] Usando Python del .venv del proyecto.");
            return rutaVenv;
        }

        // Opcion 2: Rutas fijas de instalacion en Windows.
        // Cubrimos las versiones mas comunes y la ruta de uv (gestor de paquetes moderno).
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

        // Opcion 3: Comandos del PATH del sistema.
        // Intentamos ejecutar cada comando con --version para ver si responde.
        // Si el proceso termina con codigo 0, el comando existe y funciona.
        String[] posiblesComandos = {"py", "python", "python3"};
        for (String comando : posiblesComandos) {
            try {
                ProcessBuilder pb = new ProcessBuilder(comando, "--version");
                pb.redirectErrorStream(true);
                Process proceso = pb.start();
                if (proceso.waitFor() == 0) {
                    return comando;
                }
            } catch (Exception ignored) {
                // Este comando no existe o no funciona: probamos el siguiente.
            }
        }

        // Si nada funciono, devolvemos "python" como ultimo recurso y dejamos que
        // el SO muestre el error correspondiente cuando intente ejecutarlo.
        return "python";
    }

    /*
     * Lee un flujo de texto linea por linea e imprime cada linea en la consola de Java.
     *
     * Se ejecuta en un hilo separado para no bloquear el hilo principal mientras
     * Python esta corriendo. El prefijo ([Python stdout] o [Python stderr]) permite
     * distinguir el origen de cada mensaje en la consola.
     *
     * @param reader   El lector del flujo (stdout o stderr del proceso Python).
     * @param prefijo  Etiqueta que se antepone a cada linea impresa.
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
