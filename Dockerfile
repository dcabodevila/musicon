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

# Crear el directorio para la aplicación en el contenedor
WORKDIR /app

# Copiar el archivo WAR generado en la fase anterior
COPY --from=build /app/target/gestmusica.war /app/gestmusica.war

# Exponer el puerto que utiliza la aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación con la zona horaria configurada
CMD ["java", "-jar", "/app/gestmusica.war"]