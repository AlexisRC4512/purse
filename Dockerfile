FROM amazoncorretto:17-alpine
WORKDIR /app
COPY target/purse-0.0.1.jar /app/app.jar
ENV SPRING_CONFIG_PORT=8084
ENV SPRING_CLOUD_CONFIG_URI=http://config-server:8888
EXPOSE $SPRING_CONFIG_PORT
CMD ["java", "-Dserver.port=${SPRING_CONFIG_PORT}", "-Dspring.cloud.config.uri=${SPRING_CLOUD_CONFIG_URI}", "-jar", "/app/app.jar"]