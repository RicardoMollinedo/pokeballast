# ---- build stage: JDK + Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
# build descriptors first, so the dependency layer is cached between code changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src/ src/
# the Flyway scripts live outside src/ and are packaged by Maven: without this the image has no schema
COPY database/ database/
# tests run in CI / locally with `./mvnw verify`; the image build only packages
RUN ./mvnw -q -B package -DskipTests

# ---- runtime stage: JRE only, non-root user ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/target/pokeproject-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
