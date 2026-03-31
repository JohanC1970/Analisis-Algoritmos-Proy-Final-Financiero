@echo off
title Proyecto Final - Analisis de Algoritmos
color 0A

echo ========================================================
echo     Iniciando Proyecto Final: Analisis de Algoritmos
echo ========================================================
echo.

:: Verifica si Java esta instalado
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java no esta instalado o no esta en el PATH.
    echo Por favor, instala Java para continuar.
    pause
    exit /b
)

:: Ejecuta el archivo JAR (Asegurate de que el nombre coincida con tu .jar)
java -jar Analisis-Algoritmos-Proy-Final-Financiero.jar

echo.
echo ========================================================
echo     Proceso finalizado. Puedes revisar las graficas.
echo ========================================================
pause