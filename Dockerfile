# ── Etapa 1: Build con Maven ──────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -q -DskipTests

# ── Etapa 2: Runtime con Java + Python ───────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# Instalar Python y dependencias
RUN apt-get update && apt-get install -y python3 python3-pip --no-install-recommends \
    && pip3 install pandas matplotlib numpy seaborn reportlab --break-system-packages \
    && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado y los recursos del frontend
COPY --from=build /app/target/app.jar ./app.jar
COPY python_viz ./python_viz

# Exponer puerto (Railway usa $PORT)
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]