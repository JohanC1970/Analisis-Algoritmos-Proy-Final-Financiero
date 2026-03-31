#!/bin/bash

echo "========================================================"
echo "    Iniciando Proyecto Final: Análisis de Algoritmos"
echo "========================================================"
echo ""

# Verifica si Java está instalado
if ! command -v java &> /dev/null
then
    echo " [ERROR] Java no está instalado o no está en el PATH."
    echo "Por favor, instala Java para continuar."
    exit 1
fi

# Ejecuta el archivo JAR (Asegúrate de que el nombre coincida)
java -jar Analisis-Algoritmos-Proy-Final-Financiero.jar

echo ""
echo "========================================================"
echo "    Proceso finalizado. Puedes revisar las gráficas."
echo "========================================================"