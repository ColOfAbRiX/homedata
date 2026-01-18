# #  BUILD STAGE  # #

FROM sbtscala/scala-sbt:eclipse-temurin-alpine-24.0.1_9_1.12.0_3.7.4 AS builder

WORKDIR /tmp

# Copy build definition first (cache dependencies)
COPY project ./project
COPY build.sbt .
COPY *.sbt ./

RUN sbt update

# Copy sources
COPY . .

# Build the fat jar
RUN sbt homedata/assembly

# #  RUNTIME STAGE  # #

FROM eclipse-temurin:27-jre-alpine AS jre

WORKDIR /opt/homedata

COPY --from=builder /tmp/homedata/target/scala-*/homedata_*_*.jar homedata.jar

ENTRYPOINT ["java", "-jar", "/opt/homedata/homedata.jar"]
