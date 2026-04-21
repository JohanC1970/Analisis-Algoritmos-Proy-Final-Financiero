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
from datetime import datetime
from flask import Flask, render_template, Response

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import matplotlib.ticker as mticker
import numpy as np

from flask import Flask, jsonify, request, send_from_directory

from reportlab.lib.pagesizes import letter
from reportlab.lib.units import inch
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Image as RLImage,
    Table, TableStyle, PageBreak, HRFlowable
)

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


# ─── NUEVOS ENDPOINTS DE LA API (Req. 4) ────────────────────────────────────

@app.route("/api/correlacion")
def api_correlacion():
    """Proxy simple al endpoint /correlacion de Java."""
    try:
        r = requests.get(f"{JAVA_API_URL}/correlacion", timeout=60)
        return jsonify(r.json()), r.status_code
    except Exception as e:
        return jsonify({"error": f"No se pudo conectar con Java: {str(e)}"}), 503


@app.route("/api/ohlc")
def api_ohlc():
    """Proxy simple al endpoint /ohlc de Java."""
    ticker = request.args.get("ticker", "").upper()
    dias   = request.args.get("dias", "180")

    if not ticker:
        return jsonify({"error": "Parámetro 'ticker' requerido"}), 400

    try:
        r = requests.get(
            f"{JAVA_API_URL}/ohlc",
            params={"ticker": ticker, "dias": dias},
            timeout=15
        )
        return jsonify(r.json()), r.status_code
    except Exception as e:
        return jsonify({"error": f"No se pudo conectar con Java: {str(e)}"}), 503


@app.route("/api/reporte-pdf")
def api_reporte_pdf():
    """
    Genera el reporte PDF completo del dashboard.
    Llama a los endpoints Java/Flask, genera gráficos con matplotlib
    y ensambla el PDF con reportlab.
    """
    try:
        # 1. Matriz de correlación
        corr_resp = requests.get(f"{JAVA_API_URL}/correlacion", timeout=60)
        corr_data = corr_resp.json()

        # 2. Lista de tickers
        tickers_resp = requests.get(f"{JAVA_API_URL}/tickers", timeout=10)
        tickers = tickers_resp.json().get("tickers", [])

        # 3. OHLC de los primeros 4 tickers
        ohlc_list = []
        for ticker in tickers[:4]:
            try:
                ohlc_resp = requests.get(
                    f"{JAVA_API_URL}/ohlc",
                    params={"ticker": ticker, "dias": 180},
                    timeout=15
                )
                ohlc_list.append(ohlc_resp.json())
            except Exception:
                pass

        # 4. Datos de riesgo (puerto 8000)
        riesgo_data = None
        try:
            riesgo_resp = requests.get("http://localhost:8000/riesgo", timeout=15)
            riesgo_data = riesgo_resp.json()
        except Exception:
            pass

        # 5. Generar y devolver el PDF
        pdf_buf = _generar_pdf_reporte(corr_data, tickers, ohlc_list, riesgo_data)

        return Response(
            pdf_buf.read(),
            mimetype="application/pdf",
            headers={"Content-Disposition": "attachment; filename=reporte_bursatil.pdf"}
        )
    except Exception as e:
        return jsonify({"error": f"Error al generar PDF: {str(e)}"}), 500


# ─── HELPERS PARA GENERACIÓN DE PDF ─────────────────────────────────────────

def _compute_sma(closes: list, k: int) -> list:
    """Calcula SMA de orden k. Devuelve None para los primeros k-1 elementos."""
    result = []
    for i in range(len(closes)):
        if i < k - 1:
            result.append(None)
        else:
            total = 0.0
            for j in range(i - k + 1, i + 1):
                total += closes[j]
            result.append(total / k)
    return result


def _figura_heatmap(tickers: list, matriz: list) -> io.BytesIO:
    """Genera el mapa de calor de correlación como BytesIO (PNG)."""
    n = len(tickers)
    data = np.array(matriz, dtype=float)

    cell = max(0.4, min(0.55, 9.0 / n))
    fig, ax = plt.subplots(figsize=(n * cell + 1.5, n * cell + 1.0))
    fig.patch.set_facecolor("#0f172a")
    ax.set_facecolor("#0f172a")

    im = ax.imshow(data, cmap="RdBu", vmin=-1, vmax=1, aspect="auto")

    ax.set_xticks(range(n))
    ax.set_yticks(range(n))
    ax.set_xticklabels(tickers, rotation=45, ha="right", color="white",
                       fontsize=max(5, 9 - n // 6))
    ax.set_yticklabels(tickers, color="white", fontsize=max(5, 9 - n // 6))

    fs = max(4, 8 - n // 5)
    for i in range(n):
        for j in range(n):
            val = data[i, j]
            if not np.isnan(val):
                txt_color = "white" if abs(val) > 0.45 else "#222"
                ax.text(j, i, f"{val:.2f}", ha="center", va="center",
                        fontsize=fs, color=txt_color)

    cbar = plt.colorbar(im, ax=ax, fraction=0.046, pad=0.04)
    cbar.ax.yaxis.set_tick_params(color="white")
    plt.setp(cbar.ax.yaxis.get_ticklabels(), color="white")

    ax.set_title("Matriz de Correlación de Pearson (Retornos Logarítmicos)",
                 color="white", fontsize=11, pad=12)
    ax.tick_params(colors="white")
    for spine in ax.spines.values():
        spine.set_edgecolor("#334155")

    plt.tight_layout()
    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=120, bbox_inches="tight",
                facecolor=fig.get_facecolor())
    buf.seek(0)
    plt.close(fig)
    return buf


def _figura_velas(ticker: str, datos: list, sma20: list, sma50: list) -> io.BytesIO:
    """Genera el gráfico de velas + SMAs como BytesIO (PNG)."""
    n = len(datos)
    if n == 0:
        return None

    fig, ax = plt.subplots(figsize=(12, 3.8))
    fig.patch.set_facecolor("#0f172a")
    ax.set_facecolor("#1e293b")

    for i, d in enumerate(datos):
        is_green = d["close"] >= d["open"]
        color    = "#22c55e" if is_green else "#ef4444"
        # Mecha (high-low)
        ax.plot([i, i], [d["low"], d["high"]], color=color, linewidth=0.7, zorder=1)
        # Cuerpo
        body_bot = min(d["open"], d["close"])
        body_h   = max(abs(d["close"] - d["open"]), (d["high"] - d["low"]) * 0.05)
        ax.add_patch(mpatches.Rectangle(
            (i - 0.35, body_bot), 0.7, body_h, color=color, zorder=2
        ))

    closes = [d["close"] for d in datos]

    def _draw_sma(sma_vals, clr, lbl):
        xi = [i for i, v in enumerate(sma_vals) if v is not None]
        yi = [v for v in sma_vals if v is not None]
        if xi:
            ax.plot(xi, yi, color=clr, linewidth=1.4, label=lbl, zorder=3)

    _draw_sma(sma20, "#3b82f6", "SMA 20")
    _draw_sma(sma50, "#f59e0b", "SMA 50")

    step = max(1, n // 10)
    xticks = list(range(0, n, step))
    ax.set_xticks(xticks)
    ax.set_xticklabels([datos[i]["fecha"][:10] for i in xticks],
                       rotation=40, ha="right", color="#94a3b8", fontsize=7)
    ax.yaxis.set_tick_params(labelcolor="#94a3b8", labelsize=8)
    ax.tick_params(colors="#475569", which="both")

    for spine in ax.spines.values():
        spine.set_edgecolor("#334155")
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.set_ylabel("Precio (USD)", color="#94a3b8", fontsize=9)
    ax.set_xlim(-1, n)
    ax.set_title(f"{ticker} — Gráfico de Velas + Medias Móviles",
                 color="white", fontsize=10, fontweight="bold")
    ax.grid(axis="y", linestyle="--", alpha=0.2, color="#475569")
    ax.legend(loc="upper left", facecolor="#1e293b",
              edgecolor="#334155", labelcolor="white", fontsize=8)

    plt.tight_layout()
    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=120, bbox_inches="tight",
                facecolor=fig.get_facecolor())
    buf.seek(0)
    plt.close(fig)
    return buf


def _generar_pdf_reporte(corr_data: dict, tickers: list,
                          ohlc_list: list, riesgo_data: dict) -> io.BytesIO:
    """Ensambla el PDF completo y devuelve un BytesIO."""
    buf = io.BytesIO()
    doc = SimpleDocTemplate(
        buf, pagesize=letter,
        leftMargin=0.75 * inch, rightMargin=0.75 * inch,
        topMargin=0.75 * inch,  bottomMargin=0.75 * inch
    )

    styles = getSampleStyleSheet()
    title_st = ParagraphStyle("T", parent=styles["Title"],
                              fontSize=22, textColor=colors.HexColor("#1e40af"),
                              spaceAfter=10)
    h1_st = ParagraphStyle("H1", parent=styles["Heading1"],
                            fontSize=15, textColor=colors.HexColor("#1e40af"),
                            spaceAfter=6, spaceBefore=14)
    h2_st = ParagraphStyle("H2", parent=styles["Heading2"],
                            fontSize=12, textColor=colors.HexColor("#1e3a8a"),
                            spaceAfter=4, spaceBefore=8)
    body_st = ParagraphStyle("B", parent=styles["Normal"],
                             fontSize=10, leading=14)

    story = []
    AW = letter[0] - 1.5 * inch   # available width

    # ── PORTADA ──────────────────────────────────────────────────────────────
    story += [
        Spacer(1, 0.4 * inch),
        Paragraph("Dashboard Bursátil", title_st),
        Paragraph("Reporte Técnico de Análisis Financiero", h2_st),
        HRFlowable(width="100%", thickness=1, color=colors.HexColor("#1e40af")),
        Spacer(1, 0.25 * inch),
        Paragraph(f"<b>Fecha de generación:</b> {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}",
                  body_st),
        Spacer(1, 0.15 * inch),
        Paragraph("<b>Activos analizados:</b>", body_st),
        Spacer(1, 0.08 * inch),
        Paragraph(", ".join(tickers) if tickers else "—", body_st),
        Spacer(1, 0.25 * inch),
        Paragraph(
            "Este reporte contiene el análisis de correlación entre activos financieros, "
            "gráficos de velas japonesas con medias móviles simples (SMA 20 y SMA 50) "
            "y un resumen de métricas de riesgo.",
            body_st),
        PageBreak(),
    ]

    # ── SECCIÓN 1: HEATMAP ───────────────────────────────────────────────────
    story += [
        Paragraph("Sección 1 — Mapa de Calor de Correlación", h1_st),
        Paragraph(
            "Correlación de Pearson entre los retornos logarítmicos diarios de cada par de activos. "
            "Rojo → correlación positiva (+1). Azul → correlación negativa (-1).",
            body_st),
        Spacer(1, 0.15 * inch),
    ]
    corr_tickers = corr_data.get("tickers", [])
    corr_matriz  = corr_data.get("matriz",  [])
    if corr_tickers and corr_matriz:
        hm_buf = _figura_heatmap(corr_tickers, corr_matriz)
        story.append(RLImage(hm_buf, width=AW, height=AW * 0.72))
    story.append(PageBreak())

    # ── SECCIÓN 2: VELAS ─────────────────────────────────────────────────────
    story += [
        Paragraph("Sección 2 — Gráficos de Velas con Medias Móviles", h1_st),
        Paragraph(
            "Velas japonesas de los primeros 4 activos (180 días). "
            "SMA 20 en azul · SMA 50 en naranja.",
            body_st),
    ]
    for ohlc in ohlc_list:
        t_name = ohlc.get("ticker", "")
        datos  = ohlc.get("datos",  [])
        if not datos:
            continue
        closes = [d["close"] for d in datos]
        sma20  = _compute_sma(closes, 20)
        sma50  = _compute_sma(closes, 50)
        story += [
            Spacer(1, 0.15 * inch),
            Paragraph(t_name, h2_st),
        ]
        vbuf = _figura_velas(t_name, datos, sma20, sma50)
        if vbuf:
            story.append(RLImage(vbuf, width=AW, height=2.6 * inch))
    story.append(PageBreak())

    # ── SECCIÓN 3: TABLA DE RIESGO ───────────────────────────────────────────
    story += [
        Paragraph("Sección 3 — Resumen de Métricas de Riesgo", h1_st),
    ]
    if riesgo_data and "activos" in riesgo_data:
        story.append(Paragraph(
            "Activos ordenados de menor a mayor riesgo. "
            "Volatilidad anualizada calculada con retornos logarítmicos diarios × √252.",
            body_st))
        story.append(Spacer(1, 0.15 * inch))

        thead = [["#", "Ticker", "Volatilidad Anual", "Sharpe", "Categoría"]]
        rows  = []
        for i, a in enumerate(riesgo_data["activos"], 1):
            vol    = a.get("volatilidadAnual",   0)
            sharpe = a.get("sharpeAproximado",   0)
            cat    = a.get("categoriaNombre",    "—")
            rows.append([str(i), a.get("ticker", ""),
                         f"{vol:.4f}  ({vol*100:.2f}%)",
                         f"{sharpe:.3f}", cat])

        t_data = thead + rows
        cw = [0.35*inch, 0.85*inch, 1.95*inch, 0.95*inch, 1.45*inch]
        style_cmds = [
            ("BACKGROUND",    (0, 0), (-1, 0), colors.HexColor("#1e40af")),
            ("TEXTCOLOR",     (0, 0), (-1, 0), colors.white),
            ("FONTNAME",      (0, 0), (-1, 0), "Helvetica-Bold"),
            ("FONTSIZE",      (0, 0), (-1, 0), 9),
            ("ALIGN",         (0, 0), (-1, -1), "CENTER"),
            ("FONTSIZE",      (0, 1), (-1, -1), 8),
            ("GRID",          (0, 0), (-1, -1), 0.4, colors.HexColor("#cbd5e1")),
            ("VALIGN",        (0, 0), (-1, -1), "MIDDLE"),
            ("TOPPADDING",    (0, 0), (-1, -1), 3),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
        ]
        for i in range(1, len(t_data)):
            bg = colors.HexColor("#f8fafc") if i % 2 == 0 else colors.HexColor("#e2e8f0")
            style_cmds.append(("BACKGROUND", (0, i), (-1, i), bg))

        tbl = Table(t_data, colWidths=cw, repeatRows=1)
        tbl.setStyle(TableStyle(style_cmds))
        story.append(tbl)
    else:
        story.append(Paragraph(
            "Datos de riesgo no disponibles. Verifique que MainAnalisisRiesgo "
            "esté en ejecución en el puerto 8000.",
            body_st))

    doc.build(story)
    buf.seek(0)
    return buf


# ─── SIRVE EL FRONTEND ────────────────────────────────────────────────────────

@app.route("/")
def index():
    """Sirve el HTML principal de la interfaz."""
    return send_from_directory(os.path.dirname(os.path.abspath(__file__)), "similitud.html")

@app.route('/riesgo')
def riesgo():
    return render_template('analisis_riesgo.html')


@app.route('/dashboard')
def dashboard():
    """Sirve el dashboard HTML del Requerimiento 4."""
    return send_from_directory(os.path.dirname(os.path.abspath(__file__)), "dashboard.html")


# ─── ARRANQUE ─────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    verificar_java()
    print(f"\n[app.py] Interfaz web disponible en http://localhost:{FLASK_PORT}")
    app.run(host="0.0.0.0", port=FLASK_PORT, debug=False)