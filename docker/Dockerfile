# #  BUILD STAGE  # #

FROM sbtscala/scala-sbt:eclipse-temurin-25.0.1_8_1.12.0_3.7.4 AS builder

WORKDIR /tmp

# Copy build definition first (cache dependencies)
COPY project ./project
COPY build.sbt .

# To account for the fact that I haven't yet published some libraries
RUN mkdir -p /root/.ivy2/local
COPY docker-image/lib/ /root/.ivy2/local/
RUN find /root/.ivy2/local -type d -exec chmod 755 {} \; \
 && find /root/.ivy2/local -type f -exec chmod 644 {} \;

RUN sbt update

# Copy sources
COPY homedata ./homedata
COPY tado4s ./tado4s
COPY timeflux ./timeflux

# Build the fat jar
RUN sbt homedata/assembly

# #  RUNTIME STAGE  # #

FROM eclipse-temurin:25-jre AS jre

WORKDIR /opt/homedata

COPY --from=builder \
    /tmp/homedata/target/scala-3.7.4/homedata_0.1.0_3.7.4.jar \
    homedata.jar

ENTRYPOINT ["java", "-jar", "/opt/homedata/homedata.jar"]
