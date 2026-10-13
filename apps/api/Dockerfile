FROM maven:3.9.9-eclipse-temurin-21@sha256:3a4ab3276a087bf276f79cae96b1af04f53731bec53fb2e651aca79e4b10211e AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre@sha256:cff19e6215689161eb6162c11b86b0c60ddf802164f2eaf48d570f8fb79a36c5 AS runtime

WORKDIR /app

COPY --from=build /workspace/target/api-0.0.1-SNAPSHOT.jar /app/sanctuary-api.jar
COPY certs/rds-us-east-1-bundle.pem /app/rds-ca.pem

# This image starts Java directly; the inherited Go supervisor is unused.
RUN rm -f /usr/bin/pebble && chown -R 10001:10001 /app

# Copy the cleaned filesystem so removed binaries are not shipped in lower layers.
FROM scratch
COPY --from=runtime / /
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ENV LANG=C.UTF-8
WORKDIR /app
USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/sanctuary-api.jar"]
