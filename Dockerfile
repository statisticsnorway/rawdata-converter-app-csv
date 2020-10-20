FROM openjdk:14-alpine
COPY target/rawdata-converter-app-csv-*.jar rawdata-converter-app-csv.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "rawdata-converter-app-csv.jar"]