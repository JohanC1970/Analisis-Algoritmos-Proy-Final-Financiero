# 1. Ejecución rápida (Sin necesidad de IDE)

La forma más fácil y directa de correr este proyecto es mediante su ejecución directa. **No necesitas tener instalado ni configurado ningún entorno de desarrollo (IDE)** como Eclipse, IntelliJ o VS Code.

**Requisitos previos:**
- **Java (JDK 21 o superior)** instalado en tu sistema.
- **Python (3.9 o superior)** instalado en tu sistema.

**Pasos para ejecutar:**
1. Asegúrate de tener las librerías gráficas de Python instaladas. Abre una terminal y ejecuta por única vez: 
   `pip install pandas matplotlib numpy seaborn`
2. Ve a la carpeta raíz del proyecto y haz **doble clic** en el archivo ejecutable (por ejemplo, el archivo `.bat`, `.sh` o el `.jar` principal compilado, dependiendo de tu sistema operativo).
3. ¡Listo! El programa se ejecutará automáticamente en la consola: descargará los datos financieros de la API, ejecutará los algoritmos y abrirá Python por sí solo para generar tus gráficas.

---

# 2. Documentación del Proyecto

# Análisis de Algoritmos — Proyecto Final Financiero
**Universidad del Quindío · Ingeniería de Sistemas y Computación**

Análisis financiero que descarga datos históricos de activos bursátiles, ejecuta benchmarks de 12 algoritmos de ordenamiento y genera visualizaciones automáticas mediante Python.

---

## Requisitos del sistema

### Java
| Requisito | Versión mínima |
|-----------|---------------|
| JDK | **21** |
| Maven | **3.8+** |

> El proyecto usa `java.net.http.HttpClient` (disponible desde Java 11) y records/features de Java 21. No compilará con versiones anteriores.


### Python
| Requisito | Versión mínima |
|-----------|---------------|
| Python | **3.9+** |


## Dependencias Java

Gestionadas por Maven (`pom.xml`):

| Librería | Versión | Uso |
|----------|---------|-----|
| `com.google.code.gson` | 2.10.1 | Parseo del JSON de la API de Yahoo Finance |

Maven las descarga solas al compilar. No se requiere instalación manual.


## Dependencias Python

Instalar desde la carpeta raíz del proyecto:


| Librería | Uso |
|----------|-----|
| `pandas` | Lectura y manipulación de los CSV generados por Java |
| `matplotlib` | Generación de los diagramas de barras |
| `numpy` | Cálculos auxiliares para escalado de colores en gráficas |
| `seaborn` | Estilos visuales complementarios |


## Estructura del proyecto

```text
├── src/
│   └── main/java/
│       ├── Main.java                  # Punto de entrada
│       ├── etl/
│       │   ├── ApiClient.java         # Peticiones HTTP a Yahoo Finance
│       │   └── DataParser.java        # Parseo del JSON a objetos Java
│       ├── model/
│       │   └── RegistroFinanciero.java
│       ├── sorting/                   # 12 implementaciones de algoritmos
│       │   ├── Sorter.java            # Interfaz común
│       │   ├── TimSortImpl.java
│       │   ├── CombSortImpl.java
│       │   ├── SelectionSort.java
│       │   ├── TreeSortImpl.java
│       │   ├── PigeonholeSortImpl.java
│       │   ├── BucketSortImpl.java
│       │   ├── QuickSortImpl.java
│       │   ├── HeapSortImpl.java
│       │   ├── BitonicSortImpl.java
│       │   ├── GnomeSortImpl.java
│       │   ├── BinaryInsertionSortImpl.java
│       │   └── RadixSortImpl.java
│       └── viz/
│           └── PythonBridge.java      # Integración Java → Python
├── python_viz/
│   ├── visualizacion.py               # Script de gráficas
│   └── requirements.txt
├── data/
│   ├── benchmark.csv                  # Generado en tiempo de ejecución
│   └── volumen.csv                    # Generado en tiempo de ejecución
├── output/                            # Imágenes PNG generadas
└── pom.xml
