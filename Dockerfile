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

# Render injects PORT at runtime and defaults it to 10000, and it also inspects
# EXPOSE to work out where to send traffic. These two must agree.
#
# They did not, in the first deploy: EXPOSE said 8080 while the injected PORT
# made the app bind 10000. TLS completed and then nothing came back - the
# router was forwarding to a port with nothing on it. Silence after a
# successful handshake is what a routing/binding mismatch looks like, as
# opposed to the 502 you get from a crashed process.
ENV PORT=10000
EXPOSE 10000

# Containers get a slice of the host, not the whole machine. Without this the
# JVM can size its heap against the host's total RAM and get OOM-killed on a
# small free-tier instance.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Shell form on purpose, so $PORT and $JAVA_OPTS are expanded at runtime.
#
# server.address is pinned to 0.0.0.0 because Render requires it: a server bound
# only to localhost inside a container is unreachable from outside it. Spring
# Boot already defaults to all interfaces, but stating it means the requirement
# is visible rather than depending on a default staying put.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.address=0.0.0.0 -Dserver.port=$PORT -jar app.jar"]
