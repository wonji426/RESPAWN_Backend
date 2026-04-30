# 🎮 RESPAWN - E-Commerce Backend Service

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=OpenJDK&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.3-6DB33F?style=flat-square&logo=Spring-Boot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=Spring-Security&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_WebSocket-6DB33F?style=flat-square&logo=Spring&logoColor=white"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=Gradle&logoColor=white"/>
  <br>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=MySQL&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=Redis&logoColor=white"/>
  <img src="https://img.shields.io/badge/MongoDB-4EA94B?style=flat-square&logo=MongoDB&logoColor=white"/>
  <img src="https://img.shields.io/badge/H2_Database-003B57?style=flat-square"/>
  <br>
  <img src="https://img.shields.io/badge/Oracle_Cloud-F80000?style=flat-square&logo=Oracle&logoColor=white"/>
  <img src="https://img.shields.io/badge/Ubuntu-E95420?style=flat-square&logo=Ubuntu&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=Docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=Nginx&logoColor=white"/>
  <img src="https://img.shields.io/badge/Let's_Encrypt-003A70?style=flat-square&logo=Let'sEncrypt&logoColor=white"/>
  <br>
  <img src="https://img.shields.io/badge/Google_GenAI-4285F4?style=flat-square&logo=Google&logoColor=white"/>
  <img src="https://img.shields.io/badge/Lombok-BC252A?style=flat-square"/>
  <img src="https://img.shields.io/badge/Querydsl-0769AD?style=flat-square"/>
</p>

## 📝 Project Overview
**RESPAWN**은 하이엔드 게이밍 기어(기계식 키보드, 게이밍 헤드셋 등)를 타겟으로 한 종합 이커머스(쇼핑몰) 플랫폼의 백엔드 RESTful API 서비스입니다.
클라우드 환경(Oracle Cloud - Ubuntu)에 배포되어 운영되며, Nginx와 Docker를 활용해 안정적인 인프라를 구축했습니다. 특히, 실시간 채팅 기능을 통한 유저 소통 강화, Redis를 활용한 안전한 본인 인증, 그리고 Google Gemini AI를 활용한 맞춤형 상품 추천 등 인터랙티브하고 차별화된 쇼핑 경험을 제공합니다.

<br>

## 👥 Team & Collaboration
- **개발 인원:** 총 2명
  - **Backend:** [wonji426](https://github.com/wonji426) (Spring Boot, DB/JPA 설계, 웹소켓 실시간 통신, API 개발 및 클라우드 배포)
  - **Frontend:** [Yuyeseul](https://github.com/Yuyeseul) (React, UI/UX 설계, 웹소켓/API 연동)
- **협업 방식:** 프론트엔드와 백엔드로 역할을 나누어 전담하면서도, 서로의 파트에 대한 지속적인 **코드 리뷰(Code Review)**와 피드백을 진행했습니다. 적극적인 소통을 통해 코드를 함께 점검하고 수정하며 프로젝트의 전반적인 완성도와 안정성을 높였습니다.

<br>

## ⚙️ System Architecture
```text
[ Client (React SPA) ] 
       │ (HTTPS / WSS)
       ▼
[ Nginx (Reverse Proxy & SSL/TLS) ] 
       │
       ▼
[ Docker Container ]
 [ Spring Boot App ] ◀ ─ ─ ▶ [ MySQL (Primary DB) ]
 (REST/STOMP Broker) ◀ ─ ─ ▶ [ Redis (Auth Verification & TTL Cache) ]
       │             ◀ ─ ─ ▶ [ MongoDB (AI Vector Store) ]
       ▼
[ External API (Portone, SMS/Mail, Google GenAI) ]
```

<br>

## 💻 Tech Stack & Environment

### Framework & Core
- **Language:** Java 21
- **Framework:** Spring Boot 3.5.3, Spring WebFlux (비동기 통신)
- **Real-Time Communication:** Spring WebSocket, STOMP 프로토콜
- **Tools:** Gradle, Lombok, Spring Boot DevTools

### Database & ORM
- **RDBMS:** MySQL (Production), H2 (Local/Test)
- **NoSQL / Cache:** Redis (인증번호 TTL 관리), MongoDB (Spring Data MongoDB)
- **ORM / Query:** Spring Data JPA, Hibernate, Querydsl
- **Monitoring:** p6spy (SQL 쿼리 로깅)

### Security & Authentication
- **Security:** Spring Security 6
- **Social Login:** OAuth2 Client

### External API & AI Integration
- **Payments:** Portone (구 Iamport) REST API (결제 검증 및 환불 처리)
- **Authentication:** Nurigo SDK (휴대폰 번호 SMS 인증), Spring Boot Mail (이메일 인증)
- **AI:** Spring AI (Google GenAI 모델, MongoDB Atlas Vector Store)

### Infrastructure & DevOps
- **Cloud:** Oracle Cloud Infrastructure (Ubuntu)
- **Container:** Docker, Docker Compose
- **Web Server:** Nginx (Proxy Pass & WebSocket Upgrade 지원)
- **Security:** Let's Encrypt (Certbot) for HTTPS

<br>

## ✨ Core Features & API

- **실시간 채팅 기능 (WebSocket & STOMP)**
  - Spring WebSocket과 STOMP 프로토콜을 활용하여 클라이언트와 서버 간 양방향 실시간 통신 구현
  - Spring 내장 Simple Broker를 활용해 고객센터 문의 및 유저 간 커뮤니케이션 지원
- **고도화된 사용자 인증 및 관리 시스템**
  - Spring Security를 활용한 안전한 로그인 및 권한 인가 처리
  - Redis를 활용한 강력한 본인 인증: 아이디/비밀번호 찾기 시 발급되는 메일 및 SMS(Nurigo) 인증 번호에 TTL(Time To Live)을 적용하여 안전하게 관리
  - 구매 실적 기반의 자동화된 유저 등급(Grade) 시스템 및 쿠폰 발급 스케줄러 적용
- **상품 관리 및 장바구니 API**
  - Querydsl을 활용한 복잡한 게이밍 상품 검색 및 동적 쿼리 페이징 처리
  - RDBMS 기반으로 무결성을 보장하는 안전한 장바구니(Cart) 및 주문 도메인 설계
- **결제 검증 시스템 (Portone 연동)**
  - 아임포트(Portone) API를 이용한 결제 금액 위변조 방지 및 안전한 결제 승인/환불 로직
  - 프론트엔드와 안전하게 연동되는 결제 상태 동기화 및 모바일 환경 리다이렉트 완비
- **AI 챗봇 서비스 (Spring AI & Gemini)**
  - Spring WebFlux와 Google GenAI를 활용한 비동기 AI 응답 처리
  - MongoDB Atlas Vector Store를 활용한 데이터 임베딩 및 맞춤형 스마트 검색 지원

<br>

## 🛠 Trouble Shooting & Deep Dive

<details>
<summary><b>1. Nginx 환경에서의 WebSocket (WSS) 연결 프록시 및 타임아웃 이슈</b></summary>
<div markdown="1">
- **문제 상황:** Nginx 리버스 프록시 환경에서 HTTPS(443)를 통해 WebSocket(WSS) 연결을 시도할 때, 초기 핸드쉐이크가 실패하거나 일정 시간 접속 유지 후 강제로 끊어지는 현상 발생.
- **해결 방안:** Nginx 설정 파일에 `proxy_set_header Upgrade $http_upgrade;` 및 `proxy_set_header Connection "upgrade";`를 명시하여 프로토콜 전환을 지원하고 타임아웃 옵션을 최적화.
</div>
</details>

<details>
<summary><b>2. 로컬 H2 환경에서 운영 MySQL 환경으로의 데이터베이스 마이그레이션</b></summary>
<div markdown="1">
- **문제 상황:** 초기 개발 시 사용하던 H2 데이터베이스 구조를 클라우드 환경의 MySQL로 전환하는 과정에서 문법 차이 및 예약어 충돌 발생.
- **해결 방안:** Spring Data JPA의 `ddl-auto` 속성을 환경별로 적절히 제어하고, MySQL 방언(Dialect)에 맞춘 application.yml 설정 분리(로컬, 운영) 및 `@Column(name="...")` 명시적 변경을 통해 안정적으로 마이그레이션 완료.
</div>
</details>

<details>
<summary><b>3. Oracle Cloud(Ubuntu) 배포 시 Nginx 리버스 프록시 및 HTTPS 설정 이슈</b></summary>
<div markdown="1">
- **문제 상황:** 도커로 띄운 Spring Boot 서버와 외부 통신을 연결하는 과정에서 에러 페이지가 발생하거나 통신 불량 문제 발생.
- **해결 방안:** Nginx를 리버스 프록시로 설정하여 80/443 포트 요청을 Spring Boot의 8080 포트로 포워딩. Certbot을 활용해 SSL/TLS 인증서를 발급받고 Nginx 설정 파일에 적용하여 안전한 HTTPS 환경 구축.
</div>
</details>

<br>

## 📂 Project Directory Structure
```text
src
 ├── main
 │    ├── java
 │    │    └── com.shop.respawn
 │    │          ├── chat           // 실시간 통신 (WebSocket & STOMP) 로직
 │    │          ├── chatBot        // AI 챗봇 (Gemini) 관련 로직
 │    │          ├── config         // 전역 설정 클래스 (Security, WebSocket, Redis 등)
 │    │          ├── controller     // REST API 및 MessageMapping 엔드포인트
 │    │          ├── domain         // JPA 엔티티 및 도메인 모델
 │    │          ├── dto            // 데이터 전송 객체 (Request/Response)
 │    │          ├── email          // 이메일 전송 및 Redis 인증 확인 로직
 │    │          ├── exception      // 커스텀 예외 및 글로벌 예외 처리 핸들러
 │    │          ├── repository     // DB 접근 계층 (JPA, MongoDB)
 │    │          ├── security       // Spring Security, 인증/인가, OAuth 설정
 │    │          ├── service        // 핵심 비즈니스 로직
 │    │          ├── sms            // SMS 발송 및 Redis 인증 확인 로직
 │    │          ├── util           // 공통 유틸리티 (등급/쿠폰 정책, 인증, 스케줄러 등)
 │    │          └── RespawnApplication.java  // 애플리케이션 진입점
 │    └── resources                 // 환경 설정 파일 (application.yml, secret 등)
 └── test                           // 단위 및 통합 테스트 코드
```

<br>

## 🚀 Getting Started

### 1. Repository Clone

```bash
git clone [https://github.com/wonji426/RESPAWN_Backend.git](https://github.com/wonji426/RESPAWN_Backend.git)
cd RESPAWN_Backend
```

### 2. Application Properties 설정
`src/main/resources/application-dev.yml` (또는 환경 변수) 파일에 데이터베이스 및 외부 API 연결 정보를 설정합니다.
```yaml
app:
  base-url: http://localhost:8080
  frontend-url: http://localhost:3000

spring:
  ai:
    google:
      genai:
        api-key: your_genai_key
        chat:
          options:
            model: "gemini-2.5-flash"
            temperature: 0.2
        embedding:
          api-key: your_genai_embedding_key
          text:
            options:
              model: gemini-embedding-2-preview

    #Vector Serch
    vectorstore:
      mongodb:
        initialize-schema: false
        collection-name: item_vector_store
        index-name: vector_index
        path-name: embedding

  datasource:
    url: jdbc:h2:tcp://localhost/~/respawn
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        #        show_sql: true
        format_sql: true

  data:
    mongodb:
      uri: your_mongodb_uri
    web:
      pageable:
        default-page-size: 10
        max-page-size: 1000
    redis:
      host: localhost
      port: 6379
      password: your_password

  mail:
    host: smtp.gmail.com
    port: 587
    username: your_username
    password: your_password
    properties:
      mail:
        smtp:
          auth: true #SMTP 서버에 인증이 필요할 경우 true로 설정
          starttls:
            enable: true #SMTP 서버가 TLS를 사용하여 안전한 연결을 요구하는 경우 true로 설정
            required: true
          timeout: 5000

  sms:
    api-key: your_api_key
    api-secret: your_api_secret
    provider: https://api.coolsms.co.kr
    sender: your_number

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your_client_id
            client-secret: your_client_secret
            redirect-uri: ${app.base-url}/login/oauth2/code/google
            scope:
              - email
              - profile
          naver:
            client-id: your_client_id
            client-secret: your_client_secret
            scope:
              - name
              - email
              - mobile
            client-name: Naver
            authorization-grant-type: authorization_code
            redirect-uri: ${app.base-url}/login/oauth2/code/naver
          kakao:
            client-name: kakao
            authorization-grant-type: authorization_code
            redirect-uri: ${app.base-url}/login/oauth2/code/kakao
            client-id: your_client_id
            client-secret: your_client_secret
            client-authentication-method: client_secret_post
            scope:
              - profile_nickname
              - account_email

        provider:
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            user-name-attribute: id
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me

#import
imp.api.key: your_api_key
imp.api.secretkey: your_api_secretkey

#log
logging:
  level:
    org.hibernate.SQL: debug
    org.springframework.data.mongodb.core.MongoTemplate: debug
#  org.hibernate.type: trace

#image
uploadPath: your_upload_path
readPath: your_read_path
```

### 3. Build & Run

```bash
# Build the project
./gradlew clean build

# Run the application
java -jar build/libs/RESPAWN_Backend-0.0.1-SNAPSHOT.jar
```

<br>

## 🔗 Links
- **Backend Repository:** [RESPAWN_Backend](https://github.com/wonji426/RESPAWN_Backend.git)
- **Frontend Repository:** [RESPAWN_Frontend](https://github.com/Yuyeseul/RESPAWN_Frontend.git)
