rm -rf build
mkdir "build"
mvn -f slp-indexer-service/pom.xml clean install --log-file build/slp-indexer-service.txt
cp slp-indexer-service/slp-indexer-service-app/target/slp-indexer-service-app-0.0.1-SNAPSHOT-exec.jar build/slp-indexer-service-0.0-1.jar

mvn -f slp-indexer-api-service/pom.xml clean install --log-file build/slp-indexer-api-service.txt
cp slp-indexer-api-service/slp-indexer-api-service-app/target/slp-indexer-api-service-app-0.0.1-SNAPSHOT-exec.jar build/slp-indexer-api-service-0.0-1.jar
