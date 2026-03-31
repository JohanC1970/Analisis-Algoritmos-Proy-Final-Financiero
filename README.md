# 1. Ejecución Rápida (Sin necesidad de IDE)

La forma más fácil y directa de correr este proyecto es mediante su ejecución directa, por lo que **no necesitas tener instalado ni configurado ningún entorno de desarrollo (IDE)** como Eclipse, IntelliJ o VS Code.

**Requisitos previos:**
- **Java (JDK 21 o superior)** instalado en tu sistema.
- **Python (3.9 o superior)** instalado en tu sistema.

**Pasos para ejecutar:**
1. Instala las librerías gráficas de Python abriendo una terminal y ejecutando por única vez: `pip install pandas matplotlib numpy seaborn`
2. Ve a la carpeta del proyecto y simplemente haz **doble clic** en el archivo ejecutable del proyecto (como el archivo `.bat`, `.sh` o el `.jar` principal, dependiendo de cómo lo hayas descargado).
3. ¡Listo! El programa se ejecutará automáticamente en la consola: descargará los datos financieros en tiempo real, procesará los algoritmos y, al finalizar, abrirá Python por sí solo para generar tus gráficas.

---

# 2. Documentación Técnica del Proyecto

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
