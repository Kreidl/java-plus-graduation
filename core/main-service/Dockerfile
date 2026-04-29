FROM maven:3.9.11-amazoncorretto-21 AS builder
WORKDIR /application

COPY pom.xml ./
COPY main-service/pom.xml main-service/pom.xml
COPY stat/pom.xml stat/pom.xml
COPY stat/stat-dto/pom.xml stat/stat-dto/pom.xml
COPY stat/stat-client/pom.xml stat/stat-client/pom.xml
COPY stat/stat-server/pom.xml stat/stat-server/pom.xml

ENV MAVEN_OPTS="-Dmaven.repo.local=/app/.m2/repository"
RUN mvn dependency:go-offline -pl main-service -am -B

COPY main-service/src main-service/src
COPY stat stat

RUN mvn clean package -pl main-service -am -DskipTests


FROM amazoncorretto:21.0.8-alpine AS layers
WORKDIR /application

COPY --from=builder /application/main-service/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted


FROM amazoncorretto:21.0.8-alpine
VOLUME /tmp

RUN adduser -S spring-user
USER spring-user

WORKDIR /application

COPY --from=layers /application/extracted/dependencies/ ./
COPY --from=layers /application/extracted/spring-boot-loader/ ./
COPY --from=layers /application/extracted/snapshot-dependencies/ ./
COPY --from=layers /application/extracted/application/ ./

RUN java -XX:ArchiveClassesAtExit=app.jsa \
    -Dspring.context.exit=onRefresh \
    -jar app.jar & exit 0

ENV JAVA_CDS_OPTS="-XX:SharedArchiveFile=app.jsa -Xlog:class+load:file=/tmp/classload.log"
ENV JAVA_ERROR_FILE_OPTS="-XX:ErrorFile=/tmp/java_error.log"

ENTRYPOINT exec java \
    $JAVA_ERROR_FILE_OPTS \
    $JAVA_CDS_OPTS \
    -jar app.jar
