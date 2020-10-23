FROM openjdk:15-alpine
RUN apk --no-cache add curl
COPY target/rawdata-converter-app-csv-*.jar rawdata-converter-app-csv.jar
COPY target/classes/logback*.xml /conf/
ENV LD_LIBRARY_PATH /lib64
EXPOSE 8080
CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-Dcom.sun.management.jmxremote", "-Xmx1g", "-jar", "rawdata-converter-app-csv.jar"]
