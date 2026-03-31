"""
visualizacion.py
================
Script de visualizacion para el proyecto de Analisis de Algoritmos - UniQuindio.

Este script es invocado automaticamente por PythonBridge.java al final del pipeline.
Recibe dos rutas de CSV como argumentos de linea de comandos, genera tres graficas
y las guarda como imagenes PNG en la carpeta 'output/'.

Argumentos esperados (en orden):
  1. ruta_benchmark.csv  -> tiempos de ejecucion de los 12 algoritmos de ordenamiento
  2. ruta_volumen.csv    -> los 15 dias con mayor volumen de negociacion

Graficas que genera:
  - output/benchmark_algoritmos.png      -> barras verticales con todos los tiempos (escala completa)
  - output/benchmark_algoritmos_zoom.png -> misma grafica pero con eje Y limitado a 200ms
                                            para ver mejor los algoritmos rapidos
  - output/top15_volumen.png             -> barras horizontales con el top 15 de volumen

Uso desde terminal:
  python visualizacion.py data/benchmark.csv data/volumen.csv

Formato esperado de benchmark.csv:
  algoritmo,tiempo_ms,complejidad
  Quick Sort,12,O(n log n)
  ...

Formato esperado de volumen.csv:
  activo,fecha,volumen,close
  SPY,2020-03-20,1234567890,274.5
  ...
"""

import sys
import os
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np

# Usamos el backend "Agg" por defecto porque no requiere entorno grafico (pantalla).
# Agg renderiza las graficas directamente a archivos PNG sin intentar mostrarlas en ventana.
# Esto es importante porque el script puede ejecutarse desde Java en un entorno sin display.
matplotlib.use("Agg")

# ─────────────────────────────────────────────
# CONSTANTES DE CONFIGURACION
# ─────────────────────────────────────────────

OUTPUT_DIR      = "output"       # Carpeta donde se guardan las imagenes generadas.
COLOR_BENCHMARK = "#2196F3"      # Azul Material Design para las barras del benchmark.
COLOR_VOLUMEN   = "#4CAF50"      # Verde Material Design para las barras de volumen.
DPI             = 150            # Resolucion de las imagenes. 150 DPI es buena calidad sin ser enorme.


def crear_directorio_output():
    """Crea la carpeta output/ si no existe. exist_ok=True evita error si ya existe."""
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"[visualizacion] Carpeta de salida: '{OUTPUT_DIR}/'")


# ─────────────────────────────────────────────
# GRAFICA 1: Benchmark de algoritmos (escala completa)
# ─────────────────────────────────────────────

def graficar_benchmark(ruta_csv: str):
    """
    Lee el CSV de tiempos y genera un diagrama de barras verticales con los 12 algoritmos.
    Las barras se ordenan de menor a mayor tiempo para facilitar la comparacion visual.
    Si la columna 'complejidad' existe en el CSV, se incluye debajo del nombre en el eje X.

    Parametros:
        ruta_csv: ruta al archivo benchmark.csv generado por Java.
    """
    print(f"[visualizacion] Leyendo benchmark desde: {ruta_csv}")

    df = pd.read_csv(ruta_csv)

    # Validamos que el CSV tenga las columnas minimas necesarias antes de intentar graficar.
    # Si faltan columnas, lanzamos un error descriptivo en lugar de un KeyError crudo.
    columnas_req = {"algoritmo", "tiempo_ms"}
    if not columnas_req.issubset(df.columns):
        raise ValueError(
            f"El CSV de benchmark debe tener las columnas: {columnas_req}. "
            f"Columnas encontradas: {set(df.columns)}"
        )

    # Ordenamos ascendentemente por tiempo para que la grafica muestre los mas rapidos primero.
    df_sorted = df.sort_values("tiempo_ms", ascending=True).reset_index(drop=True)

    # Construimos las etiquetas del eje X. Si hay columna de complejidad, la incluimos
    # en una segunda linea debajo del nombre para dar contexto teorico en la grafica.
    if "complejidad" in df_sorted.columns:
        etiquetas = [
            f"{row['algoritmo']}\n{row['complejidad']}"
            for _, row in df_sorted.iterrows()
        ]
    else:
        etiquetas = df_sorted["algoritmo"].tolist()

    fig, ax = plt.subplots(figsize=(14, 7))

    barras = ax.bar(
        range(len(df_sorted)),
        df_sorted["tiempo_ms"],
        color=COLOR_BENCHMARK,
        edgecolor="white",
        linewidth=0.8,
        alpha=0.9,
    )

    # Agregamos el valor numerico encima de cada barra para lectura exacta.
    # max * 0.01 es un pequeno margen para que el texto no quede pegado a la barra.
    for barra, valor in zip(barras, df_sorted["tiempo_ms"]):
        ax.text(
            barra.get_x() + barra.get_width() / 2,
            barra.get_height() + max(df_sorted["tiempo_ms"]) * 0.01,
            f"{valor:.2f} ms",
            ha="center",
            va="bottom",
            fontsize=8,
            color="#333333",
        )

    ax.set_xticks(range(len(df_sorted)))
    ax.set_xticklabels(etiquetas, rotation=30, ha="right", fontsize=9)
    ax.set_ylabel("Tiempo de ejecucion (ms)", fontsize=11)
    ax.set_xlabel("Algoritmo de ordenamiento", fontsize=11)
    ax.set_title(
        "Comparacion de tiempos de ejecucion - 12 Algoritmos de Ordenamiento\n"
        "(orden ascendente por tiempo)",
        fontsize=13,
        fontweight="bold",
        pad=15,
    )
    # FormatStrFormatter asegura que el eje Y siempre muestre dos decimales.
    ax.yaxis.set_major_formatter(mticker.FormatStrFormatter("%.2f"))
    ax.grid(axis="y", linestyle="--", alpha=0.5)
    ax.set_axisbelow(True)  # Las lineas de la grilla quedan detras de las barras.

    plt.tight_layout()

    ruta_salida = os.path.join(OUTPUT_DIR, "benchmark_algoritmos.png")
    fig.savefig(ruta_salida, dpi=DPI, bbox_inches="tight")
    print(f"[visualizacion] Grafica guardada: {ruta_salida}")

    _mostrar_si_posible(fig)
    plt.close(fig)  # Liberamos la memoria de la figura despues de guardarla.


# ─────────────────────────────────────────────
# GRAFICA 2: Top 15 dias por volumen
# ─────────────────────────────────────────────

def graficar_top15_volumen(ruta_csv: str):
    """
    Lee el CSV de volumen y genera un diagrama de barras horizontal con los 15 dias
    de mayor volumen de negociacion. Las barras horizontales son mas legibles cuando
    las etiquetas del eje son largas (como "ACTIVO - FECHA").

    Parametros:
        ruta_csv: ruta al archivo volumen.csv generado por Java.
    """
    print(f"[visualizacion] Leyendo top-15 volumen desde: {ruta_csv}")

    df = pd.read_csv(ruta_csv)

    columnas_req = {"activo", "fecha", "volumen"}
    if not columnas_req.issubset(df.columns):
        raise ValueError(
            f"El CSV de volumen debe tener las columnas: {columnas_req}. "
            f"Columnas encontradas: {set(df.columns)}"
        )

    # Ordenamos ascendentemente para que la barra mas larga quede arriba en la grafica horizontal.
    df_sorted = df.sort_values("volumen", ascending=True).reset_index(drop=True)

    # Etiqueta del eje Y: combinamos activo y fecha para identificar cada barra unicamente.
    etiquetas_y = [
        f"{row['activo']} - {row['fecha']}"
        for _, row in df_sorted.iterrows()
    ]

    # Paleta de colores degradada: los dias con menor volumen son mas claros,
    # los de mayor volumen son mas oscuros. np.linspace genera 15 valores uniformes entre 0.35 y 0.9.
    colores = plt.cm.YlGn(np.linspace(0.35, 0.9, len(df_sorted)))  # noqa

    fig, ax = plt.subplots(figsize=(12, 8))

    barras = ax.barh(
        range(len(df_sorted)),
        df_sorted["volumen"],
        color=colores,
        edgecolor="white",
        linewidth=0.6,
    )

    # Etiqueta numerica al final de cada barra con formato de miles (1,234,567,890).
    max_vol = df_sorted["volumen"].max()
    for barra, valor in zip(barras, df_sorted["volumen"]):
        ax.text(
            valor + max_vol * 0.005,  # Pequeno margen a la derecha de la barra.
            barra.get_y() + barra.get_height() / 2,
            f"{valor:,.0f}",
            va="center",
            ha="left",
            fontsize=8,
            color="#333333",
        )

    ax.set_yticks(range(len(df_sorted)))
    ax.set_yticklabels(etiquetas_y, fontsize=9)
    ax.set_xlabel("Volumen de negociacion (unidades)", fontsize=11)
    ax.set_title(
        "Top 15 dias con mayor volumen de negociacion\n(orden ascendente)",
        fontsize=13,
        fontweight="bold",
        pad=15,
    )
    # FuncFormatter con lambda para mostrar los numeros del eje X con separadores de miles.
    ax.xaxis.set_major_formatter(mticker.FuncFormatter(lambda x, _: f"{x:,.0f}"))
    ax.grid(axis="x", linestyle="--", alpha=0.5)
    ax.set_axisbelow(True)

    plt.tight_layout()

    ruta_salida = os.path.join(OUTPUT_DIR, "top15_volumen.png")
    fig.savefig(ruta_salida, dpi=DPI, bbox_inches="tight")
    print(f"[visualizacion] Grafica guardada: {ruta_salida}")

    _mostrar_si_posible(fig)
    plt.close(fig)


# ─────────────────────────────────────────────
# GRAFICA 3: Benchmark con eje Y limitado a 200ms
# ─────────────────────────────────────────────

def graficar_benchmark_zoom(ruta_csv: str):
    """
    Genera la misma grafica de benchmark pero con el eje Y fijo en 0-200ms.

    El problema con la grafica de escala completa es que algoritmos lentos como
    SelectionSort o GnomeSort (que pueden tardar miles de ms) hacen que los algoritmos
    rapidos (QuickSort, HeapSort, TimSort) queden aplastados en la base y sean
    imposibles de comparar visualmente.

    Esta version recorta el eje Y en 200ms para que los algoritmos rapidos sean
    claramente visibles. Los algoritmos que superan 200ms se muestran cortados,
    pero eso es intencional: ya sabemos que son lentos, nos interesa ver los rapidos.

    Ademas usa una paleta de colores degradada (verde=rapido, rojo=lento) para
    reforzar visualmente la diferencia de rendimiento.

    Parametros:
        ruta_csv: ruta al archivo benchmark.csv generado por Java.
    """
    print(f"[visualizacion] Generando benchmark zoom (0-200ms) desde: {ruta_csv}")

    df = pd.read_csv(ruta_csv)

    columnas_req = {"algoritmo", "tiempo_ms"}
    if not columnas_req.issubset(df.columns):
        raise ValueError(f"Columnas requeridas: {columnas_req}. Encontradas: {set(df.columns)}")

    df_sorted = df.sort_values("tiempo_ms", ascending=True).reset_index(drop=True)

    if "complejidad" in df_sorted.columns:
        etiquetas = [
            f"{row['algoritmo']}\n{row['complejidad']}"
            for _, row in df_sorted.iterrows()
        ]
    else:
        etiquetas = df_sorted["algoritmo"].tolist()

    # Normalizamos los tiempos al rango [0, 1] para mapearlos a la paleta de colores.
    # El 1e-9 evita division por cero si todos los tiempos son iguales.
    valores   = df_sorted["tiempo_ms"].values
    norm_vals = (valores - valores.min()) / (valores.max() - valores.min() + 1e-9)
    # RdYlGn_r: rojo para valores altos (lentos), verde para valores bajos (rapidos).
    colores   = plt.cm.RdYlGn_r(norm_vals)  # noqa

    fig, ax = plt.subplots(figsize=(14, 7))

    barras = ax.bar(
        range(len(df_sorted)),
        df_sorted["tiempo_ms"],
        color=colores,
        edgecolor="white",
        linewidth=0.8,
        alpha=0.92,
    )

    # Etiqueta encima de cada barra. Para barras que superan 200ms, la etiqueta
    # se coloca en el limite del eje (200ms) para que sea visible aunque la barra este cortada.
    for barra, valor in zip(barras, df_sorted["tiempo_ms"]):
        ax.text(
            barra.get_x() + barra.get_width() / 2,
            min(barra.get_height(), 200) + 2,
            f"{valor:.2f} ms",
            ha="center",
            va="bottom",
            fontsize=8,
            color="#222222",
            fontweight="bold",
        )

    ax.set_xticks(range(len(df_sorted)))
    ax.set_xticklabels(etiquetas, rotation=30, ha="right", fontsize=9)
    ax.set_ylabel("Tiempo de ejecucion (ms)", fontsize=11)
    ax.set_xlabel("Algoritmo de ordenamiento", fontsize=11)
    ax.set_ylim(0, 200)  # Limite fijo del eje Y.
    ax.set_title(
        "Comparacion de tiempos de ejecucion - Escala 0 a 200 ms\n"
        "(algoritmos que superan 200 ms se muestran cortados | verde = mas rapido | rojo = mas lento)",
        fontsize=13,
        fontweight="bold",
        pad=15,
    )
    ax.yaxis.set_major_formatter(mticker.FormatStrFormatter("%.2f"))
    ax.grid(axis="y", linestyle="--", alpha=0.5)
    ax.set_axisbelow(True)

    plt.tight_layout()

    ruta_salida = os.path.join(OUTPUT_DIR, "benchmark_algoritmos_zoom.png")
    fig.savefig(ruta_salida, dpi=DPI, bbox_inches="tight")
    print(f"[visualizacion] Grafica guardada: {ruta_salida}")

    _mostrar_si_posible(fig)
    plt.close(fig)


# ─────────────────────────────────────────────
# UTILIDADES
# ─────────────────────────────────────────────

def _mostrar_si_posible(fig):
    """
    Intenta mostrar la figura en pantalla si hay un entorno grafico disponible.

    Cuando el script se ejecuta desde Java via ProcessBuilder, normalmente no hay
    una ventana disponible (entorno headless). En ese caso, simplemente no mostramos
    nada y la figura ya fue guardada como PNG. Si hay entorno grafico (ejecucion manual
    desde terminal), intentamos cambiar a TkAgg y mostrar la figura brevemente.
    """
    try:
        import matplotlib
        if matplotlib.get_backend() == "Agg":
            # Intentamos cambiar a un backend interactivo para mostrar la ventana.
            matplotlib.use("TkAgg")
            plt.show(block=False)
            plt.pause(3)  # Mostramos la ventana 3 segundos y continuamos.
    except Exception:
        pass  # Sin entorno grafico: ignoramos silenciosamente y seguimos.


# ─────────────────────────────────────────────
# PUNTO DE ENTRADA
# ─────────────────────────────────────────────

def main():
    # Verificamos que se pasaron los dos argumentos requeridos.
    if len(sys.argv) < 3:
        print(
            "Uso: python visualizacion.py <ruta_benchmark.csv> <ruta_volumen.csv>",
            file=sys.stderr,
        )
        sys.exit(1)

    ruta_benchmark = sys.argv[1]
    ruta_volumen   = sys.argv[2]

    # Verificamos que los archivos existan antes de intentar leerlos.
    # Un error claro aqui es mejor que un pandas FileNotFoundError mas adelante.
    for ruta in (ruta_benchmark, ruta_volumen):
        if not os.path.isfile(ruta):
            print(f"[visualizacion] ERROR: No se encontro el archivo '{ruta}'", file=sys.stderr)
            sys.exit(1)

    crear_directorio_output()

    # Ejecutamos cada grafica en un bloque try/except independiente para que
    # un error en una grafica no impida generar las demas.
    try:
        graficar_benchmark(ruta_benchmark)
    except Exception as e:
        print(f"[visualizacion] ERROR en grafica de benchmark: {e}", file=sys.stderr)
        sys.exit(1)

    try:
        graficar_benchmark_zoom(ruta_benchmark)
    except Exception as e:
        print(f"[visualizacion] ERROR en grafica zoom: {e}", file=sys.stderr)
        sys.exit(1)

    try:
        graficar_top15_volumen(ruta_volumen)
    except Exception as e:
        print(f"[visualizacion] ERROR en grafica de volumen: {e}", file=sys.stderr)
        sys.exit(1)

    print("[visualizacion] Proceso completado. Imagenes en la carpeta 'output/'.")


if __name__ == "__main__":
    main()
