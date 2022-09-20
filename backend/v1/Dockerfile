FROM openjdk:16
WORKDIR /app

COPY sharer/target/scala-2.13/sharer-assembly-0.1.jar /app/sharer-backend.jar
COPY librarian/target/scala-2.13/librarian-assembly-0.1.jar /app/librarian-backend.jar
CMD java -jar /app/${RDX_APP_NAME}-backend.jar