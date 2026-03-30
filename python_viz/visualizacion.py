"""
visualizacion.py
================
Script de visualización para el proyecto de Análisis de Algoritmos - UniQuindío.

Recibe dos rutas de archivos CSV como argumentos de línea de comandos:
  1. benchmark_tiempos.csv  → tiempos de ejecución de los 12 algoritmos de ordenamiento
  2. top15_volumen.csv      → los 15 días con mayor volumen de negociación (orden ascendente)

Genera y guarda las gráficas en la carpeta 'output/' relativa al directorio de trabajo.

Uso:
    python3 python_viz/visualizacion.py <ruta_benchmark.csv> <ruta_volumen.csv>

Formato esperado de benchmark_tiempos.csv:
    algoritmo,tiempo_ms,complejidad
    Quick Sort,12.34,O(n log n)
    Heap Sort,15.67,O(n log n)
    ...

Formato esperado de top15_volumen.csv:
    activo,fecha,volumen,close
    VOO,2020-03-20,45000000,280.5
    ...
"""

import sys
import os
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np

# Usar backend no interactivo por defecto; se cambia a TkAgg si hay display disponible
matplotlib.use("Agg")

# ─────────────────────────────────────────────
# CONSTANTES
# ─────────────────────────────────────────────
OUTPUT_DIR = "output"
COLOR_BENCHMARK = "#2196F3"   # azul Material
COLOR_VOLUMEN   = "#4CAF50"   # verde Material
DPI             = 150


def crear_directorio_output():
    """Crea la carpeta output/ si no existe."""
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"[visualizacion] Carpeta de salida: '{OUTPUT_DIR}/'")


# ─────────────────────────────────────────────
# GRÁFICA 1 – Benchmark de algoritmos
# ─────────────────────────────────────────────

def graficar_benchmark(ruta_csv: str):
    """
    Lee el CSV de tiempos de benchmark y genera un diagrama de barras
    con los 12 algoritmos ordenados de menor a mayor tiempo (ascendente).

    Parámetros
    ----------
    ruta_csv : str
        Ruta al archivo CSV con columnas: algoritmo, tiempo_ms, complejidad
    """
    print(f"[visualizacion] Leyendo benchmark desde: {ruta_csv}")

    df = pd.read_csv(ruta_csv)

    # Validación mínima de columnas requeridas
    columnas_req = {"algoritmo", "tiempo_ms"}
    if not columnas_req.issubset(df.columns):
        raise ValueError(
            f"El CSV de benchmark debe tener las columnas: {columnas_req}. "
            f"Columnas encontradas: {set(df.columns)}"
        )

    # Ordenar ascendentemente por tiempo para el diagrama
    df_sorted = df.sort_values("tiempo_ms", ascending=True).reset_index(drop=True)

    # Etiquetas: nombre + complejidad si la columna existe
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

    # Etiqueta de valor encima de cada barra
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
    ax.set_ylabel("Tiempo de ejecución (ms)", fontsize=11)
    ax.set_xlabel("Algoritmo de ordenamiento", fontsize=11)
    ax.set_title(
        "Comparación de tiempos de ejecución – 12 Algoritmos de Ordenamiento\n"
        "(orden ascendente por tiempo)",
        fontsize=13,
        fontweight="bold",
        pad=15,
    )
    ax.yaxis.set_major_formatter(mticker.FormatStrFormatter("%.2f"))
    ax.grid(axis="y", linestyle="--", alpha=0.5)
    ax.set_axisbelow(True)

    plt.tight_layout()

    ruta_salida = os.path.join(OUTPUT_DIR, "benchmark_algoritmos.png")
    fig.savefig(ruta_salida, dpi=DPI, bbox_inches="tight")
    print(f"[visualizacion] Gráfica guardada: {ruta_salida}")

    # Intentar mostrar en pantalla si hay entorno gráfico disponible
    _mostrar_si_posible(fig)
    plt.close(fig)


# ─────────────────────────────────────────────
# GRÁFICA 2 – Top 15 días por volumen
# ─────────────────────────────────────────────

def graficar_top15_volumen(ruta_csv: str):
    """
    Lee el CSV de los 15 días con mayor volumen y genera un diagrama de barras
    horizontal ordenado ascendentemente.

    Parámetros
    ----------
    ruta_csv : str
        Ruta al archivo CSV con columnas: activo, fecha, volumen, close
    """
    print(f"[visualizacion] Leyendo top-15 volumen desde: {ruta_csv}")

    df = pd.read_csv(ruta_csv)

    columnas_req = {"activo", "fecha", "volumen"}
    if not columnas_req.issubset(df.columns):
        raise ValueError(
            f"El CSV de volumen debe tener las columnas: {columnas_req}. "
            f"Columnas encontradas: {set(df.columns)}"
        )

    # Garantizar orden ascendente por volumen (el CSV ya debería venir ordenado,
    # pero lo reforzamos aquí para robustez)
    df_sorted = df.sort_values("volumen", ascending=True).reset_index(drop=True)

    # Etiqueta del eje Y: "ACTIVO – FECHA"
    etiquetas_y = [
        f"{row['activo']} – {row['fecha']}"
        for _, row in df_sorted.iterrows()
    ]

    # Paleta de colores degradada para distinguir magnitudes
    colores = plt.cm.YlGn(np.linspace(0.35, 0.9, len(df_sorted))) #noqa

    fig, ax = plt.subplots(figsize=(12, 8))

    barras = ax.barh(
        range(len(df_sorted)),
        df_sorted["volumen"],
        color=colores,
        edgecolor="white",
        linewidth=0.6,
    )

    # Etiqueta de valor al final de cada barra
    max_vol = df_sorted["volumen"].max()
    for barra, valor in zip(barras, df_sorted["volumen"]):
        ax.text(
            valor + max_vol * 0.005,
            barra.get_y() + barra.get_height() / 2,
            f"{valor:,.0f}",
            va="center",
            ha="left",
            fontsize=8,
            color="#333333",
        )

    ax.set_yticks(range(len(df_sorted)))
    ax.set_yticklabels(etiquetas_y, fontsize=9)
    ax.set_xlabel("Volumen de negociación (unidades)", fontsize=11)
    ax.set_title(
        "Top 15 días con mayor volumen de negociación\n(orden ascendente)",
        fontsize=13,
        fontweight="bold",
        pad=15,
    )
    ax.xaxis.set_major_formatter(mticker.FuncFormatter(lambda x, _: f"{x:,.0f}"))
    ax.grid(axis="x", linestyle="--", alpha=0.5)
    ax.set_axisbelow(True)

    plt.tight_layout()

    ruta_salida = os.path.join(OUTPUT_DIR, "top15_volumen.png")
    fig.savefig(ruta_salida, dpi=DPI, bbox_inches="tight")
    print(f"[visualizacion] Gráfica guardada: {ruta_salida}")

    _mostrar_si_posible(fig)
    plt.close(fig)


# ─────────────────────────────────────────────
# UTILIDADES
# ─────────────────────────────────────────────

def _mostrar_si_posible(fig):
    """
    Intenta mostrar la figura en pantalla si hay un entorno gráfico disponible.
    Si no lo hay (servidor, CI, ejecución headless), simplemente la omite.
    """
    try:
        import matplotlib
        if matplotlib.get_backend() == "Agg":
            # Intentar cambiar a un backend interactivo
            matplotlib.use("TkAgg")
            plt.show(block=False)
            plt.pause(3)
    except Exception:
        pass  # Sin entorno gráfico: solo se guarda el archivo


# ─────────────────────────────────────────────
# PUNTO DE ENTRADA
# ─────────────────────────────────────────────

def main():
    if len(sys.argv) < 3:
        print(
            "Uso: python3 visualizacion.py <ruta_benchmark.csv> <ruta_volumen.csv>",
            file=sys.stderr,
        )
        sys.exit(1)

    ruta_benchmark = sys.argv[1]
    ruta_volumen   = sys.argv[2]

    # Verificar existencia de archivos antes de procesar
    for ruta in (ruta_benchmark, ruta_volumen):
        if not os.path.isfile(ruta):
            print(f"[visualizacion] ERROR: No se encontró el archivo '{ruta}'", file=sys.stderr)
            sys.exit(1)

    crear_directorio_output()

    try:
        graficar_benchmark(ruta_benchmark)
    except Exception as e:
        print(f"[visualizacion] ERROR en gráfica de benchmark: {e}", file=sys.stderr)
        sys.exit(1)

    try:
        graficar_top15_volumen(ruta_volumen)
    except Exception as e:
        print(f"[visualizacion] ERROR en gráfica de volumen: {e}", file=sys.stderr)
        sys.exit(1)

    print("[visualizacion] Proceso completado. Imágenes en la carpeta 'output/'.")


if __name__ == "__main__":
    main()
