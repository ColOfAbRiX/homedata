# #  BUILD STAGE  # #

ARG SCALA_VERSION=3.8.3
ARG HOMEDATA_VERSION=0.1.0
ARG SBT_VERSION=1.12.9
ARG JDK_VERSION=25.0.1_8

FROM sbtscala/scala-sbt:eclipse-temurin-${JDK_VERSION}_${SBT_VERSION}_${SCALA_VERSION} AS builder

WORKDIR /tmp/project-build

# Copy build definition and cache dependencies
COPY project ./project
COPY build.sbt .
RUN sbt update

# Copy sources and build the fat jar
COPY src ./src
RUN sbt assembly

# #  RUNTIME STAGE  # #

ARG JDK_VERSION

FROM eclipse-temurin:${JDK_VERSION}-jre AS jre

ARG SCALA_VERSION
ARG SBT_VERSION
ARG HOMEDATA_VERSION

WORKDIR /opt/homedata

COPY --from=builder \
    /tmp/project-build/target/scala-${SCALA_VERSION}/homedata_${HOMEDATA_VERSION}_${SCALA_VERSION}.jar \
    homedata.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/homedata/homedata.jar"]
