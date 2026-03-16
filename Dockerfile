FROM eclipse-temurin:17-jdk AS stage1
WORKDIR /app
COPY gradle gradle 
COPY src src
COPY build.gradle .
COPY gradlew .
COPY settings.gradle .
RUN chmod +x gradlew
RUN ./gradlew bootJar

# 두번째 스테이지(컨테이너) : 이미지 경량화를 위해 스테이지 분리 작업 
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=stage1 /app/build/libs/*.jar hodadoc.jar
ENTRYPOINT ["java", "-jar", "hodadoc.jar"]

# 도커이미지 빌드
# docker build -t abilitytony/myorder-system:latest .

# 도커 컨테이너 실행
# docker run --name myspring -d -p 8080:8080 -e SPRING_DATASOURCE_URL="jdbc:mariadb://host.docker.internal:3306/ordersystem?useSSL=true" -e SPRING_DATASOURCE_USERNAME=root -e SPRING_DATASOURCE_PASSWORD=test1234 -e SPRING_REDIS_HOST="host.docker.internal" abilitytony/myorder-system:latest
