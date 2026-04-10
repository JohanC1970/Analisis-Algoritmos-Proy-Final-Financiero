"""
app.py - Servidor web Flask para el Requerimiento 3 (Similitud de Series de Tiempo).

Este script sirve la interfaz web que permite al usuario:
  1. Seleccionar dos activos del portafolio.
  2. Ver sus series de retornos graficadas.
  3. Ver los 4 valores de similitud calculados por Java.

Flask actúa como proxy/servidor de frontend:
  - Sirve el HTML/CSS/JS de la interfaz.
  - Reenvía las peticiones de similitud al servidor Java (localhost:8080).
  - Genera los gráficos de las series con matplotlib.

PREREQUISITOS:
  1. Java corriendo: mvn compile exec:java -Dexec.mainClass="MainSimilitud"
     (o ejecutar MainSimilitud desde el IDE)
  2. Python con dependencias: pip install flask matplotlib pandas numpy requests

USO:
  python python_viz/app.py
  Abrir: http://localhost:5000
"""

import sys
import os
import json
import time
import requests
import io
import base64

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np

from flask import Flask, jsonify, request, send_from_directory

# ─── CONFIGURACIÓN ────────────────────────────────────────────────────────────

JAVA_API_URL = "http://localhost:8080"
FLASK_PORT   = 5000
TIMEOUT_JAVA = 30  # segundos - DTW puede tardar en series largas

app = Flask(__name__, static_folder="static")


# ─── VERIFICACIÓN DE JAVA AL ARRANCAR ────────────────────────────────────────

def verificar_java():
    """Espera hasta 60 segundos a que el servidor Java esté listo."""
    print(f"[app.py] Verificando servidor Java en {JAVA_API_URL}...")
    for intento in range(12):
        try:
            r = requests.get(f"{JAVA_API_URL}/health", timeout=5)
            if r.status_code == 200:
                data = r.json()
                print(f"[app.py] Java listo. {data.get('tickers', '?')} activos disponibles.")
                return True
        except Exception:
            pass
        print(f"[app.py] Java no responde aún, reintento {intento + 1}/12...")
        time.sleep(5)
    print("[app.py] ADVERTENCIA: Java no respondió. Asegúrate de ejecutar MainSimilitud primero.")
    return False


# ─── ENDPOINTS DE LA API (PROXY → JAVA) ─────────────────────────────────────

@app.route("/api/tickers")
def api_tickers():
    """Proxy al endpoint /tickers de Java."""
    try:
        r = requests.get(f"{JAVA_API_URL}/tickers", timeout=10)
        return jsonify(r.json()), r.status_code
    except Exception as e:
        return jsonify({"error": f"No se pudo conectar con Java: {str(e)}"}), 503


@app.route("/api/similitud")
def api_similitud():
    """Proxy al endpoint /similitud de Java. Añade gráficos generados en Python."""
    ticker_a = request.args.get("a", "").upper()
    ticker_b = request.args.get("b", "").upper()

    if not ticker_a or not ticker_b:
        return jsonify({"error": "Parámetros 'a' y 'b' requeridos"}), 400

    try:
        r = requests.get(
            f"{JAVA_API_URL}/similitud",
            params={"a": ticker_a, "b": ticker_b},
            timeout=TIMEOUT_JAVA
        )
        datos = r.json()

        if r.status_code != 200:
            return jsonify(datos), r.status_code

        # Añadimos los gráficos como imágenes base64 incrustadas en el JSON.
        # Así el frontend no necesita hacer peticiones extra para las imágenes.
        datos["grafico_barras"] = generar_grafico_barras(datos)

        return jsonify(datos), 200

    except requests.exceptions.Timeout:
        return jsonify({"error": "El cálculo tardó demasiado. DTW en series largas puede necesitar más tiempo."}), 504
    except Exception as e:
        return jsonify({"error": f"Error al contactar Java: {str(e)}"}), 503


# ─── GENERACIÓN DE GRÁFICOS CON MATPLOTLIB ──────────────────────────────────

def generar_grafico_barras(datos: dict) -> str:
    """
    Genera un gráfico de barras comparando los valores de los 4 algoritmos.
    Devuelve la imagen como string base64 para incrustarla directamente en el HTML.

    El gráfico normaliza los valores de distancia y similitud a un rango [0, 1]
    para que sean comparables visualmente, con anotaciones que muestran el valor real.
    """
    resultados = datos.get("resultados", [])
    if not resultados:
        return ""

    nombres   = [r["nombre"] for r in resultados]
    valores   = [r["valor"] for r in resultados]
    complejos = [r["complejidad"] for r in resultados]

    # Colores por algoritmo (fijos para consistencia visual).
    colores = ["#2196F3", "#4CAF50", "#FF9800", "#9C27B0"]

    fig, ax = plt.subplots(figsize=(10, 5))
    fig.patch.set_facecolor("#0f172a")
    ax.set_facecolor("#1e293b")

    barras = ax.bar(
        range(len(resultados)),
        [abs(v) if not (v != v) else 0 for v in valores],  # NaN → 0
        color=colores[:len(resultados)],
        edgecolor="#334155",
        linewidth=0.8,
        alpha=0.9,
        width=0.6
    )

    # Etiqueta con el valor real encima de cada barra.
    for barra, r in zip(barras, resultados):
        val_str = f"{r['valor']:.4f}" if r['valor'] == r['valor'] else "N/A"
        ax.text(
            barra.get_x() + barra.get_width() / 2,
            barra.get_height() + max([abs(v) for v in valores if v == v] or [1]) * 0.02,
            val_str,
            ha="center", va="bottom",
            fontsize=10, color="white", fontweight="bold"
        )

    # Etiquetas del eje X con nombre y complejidad.
    etiquetas = [f"{n}\n{c}" for n, c in zip(nombres, complejos)]
    ax.set_xticks(range(len(resultados)))
    ax.set_xticklabels(etiquetas, color="#94a3b8", fontsize=9)
    ax.tick_params(colors="#475569", which="both")
    ax.spines["bottom"].set_color("#334155")
    ax.spines["left"].set_color("#334155")
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.set_ylabel("Valor calculado (magnitud absoluta)", color="#94a3b8", fontsize=10)
    ax.set_title(
        f"Resultados de similitud: {datos.get('tickerA')} vs {datos.get('tickerB')}",
        color="white", fontsize=13, fontweight="bold", pad=15
    )
    ax.yaxis.set_major_formatter(mticker.FormatStrFormatter("%.4f"))
    ax.grid(axis="y", linestyle="--", alpha=0.3, color="#475569")
    ax.set_axisbelow(True)

    plt.tight_layout()

    # Convertir a base64 para incrustar en JSON.
    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=120, bbox_inches="tight", facecolor=fig.get_facecolor())
    buf.seek(0)
    img_b64 = base64.b64encode(buf.read()).decode("utf-8")
    plt.close(fig)

    return f"data:image/png;base64,{img_b64}"


# ─── SIRVE EL FRONTEND ────────────────────────────────────────────────────────

@app.route("/")
def index():
    """Sirve el HTML principal de la interfaz."""
    return send_from_directory(os.path.dirname(os.path.abspath(__file__)), "similitud.html")


# ─── ARRANQUE ─────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    verificar_java()
    print(f"\n[app.py] Interfaz web disponible en http://localhost:{FLASK_PORT}")
    app.run(host="0.0.0.0", port=FLASK_PORT, debug=False)