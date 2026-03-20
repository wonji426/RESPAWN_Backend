# 1. 베이스 이미지 설정 (Java 버전과 일치시켜주세요)
# 1. 자바 실행 환경(JRE) 이미지 가져오기 (가벼운 alpine 버전 추천)
FROM eclipse-temurin:21-jdk-alpine

# 2. 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일 경로를 변수로 지정 (파일명 정확히 일치)
ARG JAR_FILE=build/libs/respawn-0.0.1-SNAPSHOT.jar

# 4. 호스트(내 PC)의 JAR 파일을 컨테이너 내부의 app.jar로 복사
COPY ${JAR_FILE} app.jar

# 5. 컨테이너가 시작될 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]