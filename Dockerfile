# Etapa 1: Construcción
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Etapa 2: Ejecución
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/target/gestmusica.war /app/gestmusica.war

# Configuración de memoria y optimización
ENV JAVA_OPTS="-XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=70 -XX:+UseContainerSupport -XX:+UseG1GC"

# Exponer puerto y definir comando
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/gestmusica.war"]