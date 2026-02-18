# 1️⃣ Image Java légère
FROM eclipse-temurin:21-jdk-alpine

# 2️⃣ Dossier de travail dans le container
WORKDIR /app

# 3️⃣ Copier le fichier JAR généré
COPY target/*.jar app.jar

# 4️⃣ Exposer le port de ton app
EXPOSE 8081

# 5️⃣ Commande pour lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
