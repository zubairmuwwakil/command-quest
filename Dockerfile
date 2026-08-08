# Multi-stage: build with a full JDK, ship only the jar on a JRE.
#
# The build stage needs Maven, the compiler, the test suite and the whole
# ~/.m2 cache. None of that has any business running in production, and a
# single-stage image would carry all of it - several hundred MB of attack
# surface and cold-start weight for no benefit.

# ---------- stage 1: build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Copy the wrapper and POM first, on their own. Docker caches layers, so as
# long as pom.xml has not changed this dependency download is reused and a
# code-only change rebuilds in seconds rather than re-fetching the internet.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---------- stage 2: run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Never run as root. If the process is ever compromised, this is the
# difference between an incident and a much worse incident.
RUN useradd --create-home --shell /bin/false quest
USER quest

COPY --from=build --chown=quest:quest /build/target/*.jar app.jar

# Render, Fly and friends inject the port to listen on. Defaulting to 8080
# keeps "docker run -p 8080:8080" working locally with no arguments.
ENV PORT=8080
EXPOSE 8080

# Containers get a slice of the host, not the whole machine. Without this the
# JVM can size its heap against the host's total RAM and get OOM-killed on a
# small free-tier instance.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Shell form on purpose, so $PORT and $JAVA_OPTS are expanded at runtime.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
