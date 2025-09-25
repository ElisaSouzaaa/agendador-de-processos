# 1. Fase de Build (Usa o Maven/JDK para construir o JAR)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

# 2. Fase de Runtime (Usa uma imagem Java leve para rodar o JAR)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o JAR do passo de build para a fase de runtime
COPY --from=build /app/target/agendador-desafio-dev-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta que o Spring Boot usa (8080)
EXPOSE 8080

# Comando de execucao (Inicia o JAR)
CMD ["java", "-jar", "app.jar"]
