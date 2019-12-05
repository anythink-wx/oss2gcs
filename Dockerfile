FROM openjdk:11.0.5-jre-stretch

COPY target/change-file-1.0-SNAPSHOT.jar /data/
COPY config.properties /data/
WORKDIR /data/
CMD ["java","-jar","change-file-1.0-SNAPSHOT.jar"]