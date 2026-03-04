# Stage development
FROM eclipse-temurin:21-jdk-alpine AS development
WORKDIR /app

# Installation des outils
RUN apk add --no-cache curl

# Copie des fichiers Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Télécharge les dépendances (pour le cache)
RUN ./mvnw dependency:go-offline

# Copie le reste du code
COPY src ./src

# Expose les ports
EXPOSE 8081 5005

# Commande avec configuration explicite des profils
CMD ["./mvnw", "spring-boot:run", \
     "-Dspring-boot.run.profiles=dev", \
     "-Dspring.config.additional-location=classpath:/application-dev.properties"]