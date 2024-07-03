FROM openjdk:17-oracle

WORKDIR /app

COPY target/SearchEngine-1.0-SNAPSHOT.jar searchengine.jar

CMD ["java", "-jar", "searchengine.jar"]