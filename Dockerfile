# Usar la imagen base de Maven para compilar la aplicación
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el archivo de configuración y sus dependencias
COPY pom.xml ./
RUN mvn dependency:resolve-plugins dependency:resolve -B

# Copiar el resto del código fuente a la imagen
COPY src ./src

# Construir la aplicación
RUN mvn package -DskipTests

# Usar una imagen base de Java para ejecutar la aplicación
FROM eclipse-temurin:17-jre

# Versión del agente oficial de OpenTelemetry (no vendor-specific)
ARG OTEL_JAVA_AGENT_VERSION=2.17.0

# Crear el directorio para la aplicación en el contenedor
WORKDIR /app

# Descargar el agente oficial de OpenTelemetry en una ruta estable
RUN mkdir -p /opt/opentelemetry
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar /opt/opentelemetry/opentelemetry-javaagent.jar

# Copiar el archivo WAR generado en la fase anterior
COPY --from=build /app/target/gestmusica.war /app/gestmusica.war

# Exponer el puerto que utiliza la aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación con la zona horaria configurada
CMD ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-javaagent:/opt/opentelemetry/opentelemetry-javaagent.jar", \
  "-jar", "/app/gestmusica.war"]
