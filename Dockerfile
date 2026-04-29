# ============================================================================
# STAGE 1 — Build du JAR avec Maven
# ============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Optimisation : on copie d'abord pom.xml seul pour cacher les dépendances Maven.
# Tant que pom.xml ne change pas, ce layer reste en cache (build 10x plus rapide).
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Puis on copie le code source et on build (skipTests : les tests tourneront en CI séparément)
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================================================
# STAGE 2 — Runtime léger (JRE seulement, ~200 MB final)
# ============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Bonne pratique sécurité : ne pas exécuter en root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# On récupère UNIQUEMENT le JAR depuis le stage build (le reste est jeté)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

# Healthcheck via Actuator — Docker saura si le service est vraiment prêt
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]