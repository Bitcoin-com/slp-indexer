FROM openjdk:11-jre-slim
ARG version
COPY slp-indexer-service-app/target/slp-indexer-service-app-$version-exec.jar /slp-indexer-app.jar
EXPOSE 7331
EXPOSE 5338
ENTRYPOINT ["java", "-jar", "-Dcom.sun.management.jmxremote.port=32015", "-Dcom.sun.management.jmxremote.rmi.port=32015", "-Djava.rmi.server.hostname=127.0.0.1", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "slp-indexer-app.jar"]