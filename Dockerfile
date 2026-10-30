# Image unique des micro-services Java.
# Construire avec : docker build --build-arg MODULE=account-service -t testadria/account-service .

FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /src

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY account-service/pom.xml account-service/pom.xml
COPY transaction-service/pom.xml transaction-service/pom.xml
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY config-service/pom.xml config-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml

COPY account-service/src account-service/src
COPY transaction-service/src transaction-service/src
COPY discovery-service/src discovery-service/src
COPY config-service/src config-service/src
COPY gateway-service/src gateway-service/src
COPY ai-agent-service/src ai-agent-service/src

ARG MODULE
RUN chmod +x mvnw \
    && test -n "$MODULE" \
    && ./mvnw -B -ntp -pl "$MODULE" -am package -DskipTests \
    && jar="$(find "/src/${MODULE}/target" -maxdepth 1 -type f -name "${MODULE}-*.jar" ! -name "*-plain.jar" -print -quit)" \
    && test -n "$jar" \
    && cp "$jar" /src/application.jar

FROM eclipse-temurin:21-jre-noble
# La sonde de disponibilité lit SERVER_PORT au démarrage du conteneur.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 app \
    && printf '#!/bin/sh\nexec curl -fsS "http://127.0.0.1:%sSERVER_PORT/actuator/health/readiness"\n' "$(printf '\044')" > /usr/local/bin/ready \
    && chmod 755 /usr/local/bin/ready

WORKDIR /app
COPY --from=build --chown=app:app /src/application.jar /app/app.jar

ENV SERVER_PORT=8080 \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health \
    MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true \
    MANAGEMENT_HEALTH_LIVENESSSTATE_ENABLED=true \
    MANAGEMENT_HEALTH_READINESSSTATE_ENABLED=true

EXPOSE 8080
USER app

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=12 \
    CMD ["/usr/local/bin/ready"]

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
