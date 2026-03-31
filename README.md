# Análisis de Algoritmos — Proyecto Final Financiero
**Universidad del Quindío · Ingeniería de Sistemas y Computación**

Pipeline de análisis financiero que descarga datos históricos de activos bursátiles, ejecuta benchmarks de 12 algoritmos de ordenamiento y genera visualizaciones automáticas mediante Python.

---

## Requisitos del sistema

### Java
| Requisito | Versión mínima |
|-----------|---------------|
| JDK | **21** |
| Maven | **3.8+** |

> El proyecto usa `java.net.http.HttpClient` (disponible desde Java 11) y records/features de Java 21. No compilará con versiones anteriores.

Verificar instalación:
```bash
java -version
mvn -version
```

### Python
| Requisito | Versión mínima |
|-----------|---------------|
| Python | **3.9+** |

Verificar instalación:
```bash
python --version
# o en algunos sistemas:
python3 --version
```

---

## Dependencias Java

Gestionadas automáticamente por Maven (`pom.xml`):

| Librería | Versión | Uso |
|----------|---------|-----|
| `com.google.code.gson` | 2.10.1 | Parseo del JSON de la API de Yahoo Finance |

Maven las descarga solas al compilar. No se requiere instalación manual.

---

## Dependencias Python

Instalar desde la carpeta raíz del proyecto:

```bash
pip install -r python_viz/requirements.txt
```

| Librería | Uso |
|----------|-----|
| `pandas` | Lectura y manipulación de los CSV generados por Java |
| `matplotlib` | Generación de los diagramas de barras |
| `numpy` | Cálculos auxiliares para escalado de colores en gráficas |
| `seaborn` | Estilos visuales complementarios |

> Se recomienda usar un entorno virtual para no contaminar la instalación global de Python.

```bash
# Crear y activar entorno virtual (opcional pero recomendado)
python -m venv .venv

# Windows
.venv\Scripts\activate

# macOS / Linux
source .venv/bin/activate

pip install -r python_viz/requirements.txt
```

---

## Estructura del proyecto

```
├── src/
│   └── main/java/
│       ├── Main.java                  # Punto de entrada
│       ├── etl/
│       │   ├── ApiClient.java         # Peticiones HTTP a Yahoo Finance
│       │   └── DataParser.java        # Parseo del JSON a objetos Java
│       ├── model/
│       │   └── RegistroFinanciero.java
│       ├── sorting/                   # 12 implementaciones de algoritmos
│       │   ├── Sorter.java            # Interfaz común
│       │   ├── TimSortImpl.java
│       │   ├── CombSortImpl.java
│       │   ├── SelectionSort.java
│       │   ├── TreeSortImpl.java
│       │   ├── PigeonholeSortImpl.java
│       │   ├── BucketSortImpl.java
│       │   ├── QuickSortImpl.java
│       │   ├── HeapSortImpl.java
│       │   ├── BitonicSortImpl.java
│       │   ├── GnomeSortImpl.java
│       │   ├── BinaryInsertionSortImpl.java
│       │   └── RadixSortImpl.java
│       └── viz/
│           └── PythonBridge.java      # Integración Java → Python
├── python_viz/
│   ├── visualizacion.py               # Script de gráficas
│   └── requirements.txt
├── data/
│   ├── benchmark.csv                  # Generado en tiempo de ejecución
│   └── volumen.csv                    # Generado en tiempo de ejecución
├── output/                            # Imágenes PNG generadas
└── pom.xml
```

---

## Cómo ejecutar

### 1. Clonar el repositorio
```bash
git clone <url-del-repositorio>
cd Analisis-Algoritmos-Proy-Final-Financiero
```

### 2. Instalar dependencias Python
```bash
pip install -r python_viz/requirements.txt
```

### 3. Compilar y ejecutar con Maven
```bash
mvn compile exec:java -Dexec.mainClass="Main"
```

O desde un IDE (IntelliJ IDEA, Eclipse, VS Code con extensión Java): ejecutar directamente `Main.java`.

### 4. Resultados
Al finalizar, se generan automáticamente en la carpeta `output/`:

| Archivo | Contenido |
|---------|-----------|
| `benchmark_algoritmos.png` | Diagrama de barras con los tiempos de los 12 algoritmos (escala completa) |
| `benchmark_algoritmos_zoom.png` | Mismo diagrama limitado a 200 ms para comparar algoritmos rápidos |
| `top15_volumen.png` | Top 15 días con mayor volumen de negociación (orden ascendente) |

---

## Conexión a internet

El programa descarga datos históricos de **Yahoo Finance** al ejecutarse. Se requiere conexión activa. Los activos consultados son:

`VOO, AAPL, MSFT, GOOGL, AMZN, TSLA, META, NVDA, SPY, QQQ, JPM, V, WMT, JNJ, PG, MA, UNH, HD, BAC, DIS`

> Yahoo Finance puede aplicar rate limiting. Si algún ticker falla con código HTTP 429, espera unos minutos y vuelve a ejecutar.

---

## Notas importantes

- **No se usan librerías de alto nivel** como `yfinance` o `pandas_datareader`. La descarga se realiza mediante peticiones HTTP directas (`java.net.http.HttpClient`).
- **Sin interfaz gráfica en Java.** Toda la visualización es delegada al script Python.
- El script Python es invocado automáticamente por `PythonBridge.java` al terminar el benchmark. Si Python no está en el PATH, el bridge intenta detectarlo en rutas comunes de instalación en Windows.
- Los archivos `data/benchmark.csv` y `data/volumen.csv` son sobreescritos en cada ejecución.
