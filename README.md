# 🎮 RESPAWN - E-Commerce Backend Service

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=OpenJDK&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=Spring-Boot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=Spring-Security&logoColor=white"/>
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
  <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=Thymeleaf&logoColor=white"/>
  <img src="https://img.shields.io/badge/Lombok-BC252A?style=flat-square"/>
  <img src="https://img.shields.io/badge/Querydsl-0769AD?style=flat-square"/>
</p>

## 📝 Project Overview
**RESPAWN**은 게이머들을 타겟으로 한 종합 이커머스(쇼핑몰) 플랫폼의 백엔드 RESTful API 서비스입니다.
클라우드 환경(Oracle Cloud - Ubuntu)에 배포되어 운영되며, Nginx와 Docker를 활용해 안정적인 인프라를 구축했습니다.

<br>

## 👥 Team & Collaboration
- **개발 인원:** 총 2명
  - **Backend:** [wonji426](https://github.com/wonji426) (Spring Boot, DB 설계, API 개발 및 클라우드 배포)
  - **Frontend:** [Yuyeseul](https://github.com/Yuyeseul) (React, UI/UX, API 연동)
- **협업 방식:** 프론트엔드와 백엔드로 역할을 나누어 전담하면서도, 서로의 파트에 대한 지속적인 **코드 리뷰(Code Review)**와 피드백을 진행했습니다. 적극적인 소통을 통해 코드를 함께 점검하고 수정하며 프로젝트의 전반적인 완성도와 안정성을 높였습니다.

<br>

## ⚙️ System Architecture

```text
[ Client (React) ] 
       │ (HTTPS)
       ▼
[ Nginx (Reverse Proxy & SSL/TLS) ] 
       │
       ▼
[ Docker Container ]
  [ Spring Boot App ] ◀ ─ ─ ─ ─ ▶ [ MySQL / Redis / MongoDB ]
       │
       ▼
[ External API (Iamport, SMS/Mail, Google GenAI) ]
```

<br>

## 💻 Tech Stack & Environment

### Framework & Core
- **Language:** Java 17
- **Framework:** Spring Boot, Spring WebFlux (비동기 통신)
- **Tools:** Lombok, Spring Boot DevTools

### Database & ORM
- **RDBMS:** MySQL (Production), H2 (Local/Test)
- **NoSQL / Cache:** Redis (Spring Data Redis), MongoDB (Spring Data MongoDB)
- **ORM / Query:** Spring Data JPA, Querydsl
- **Monitoring:** p6spy (SQL 쿼리 로깅)

### Security & Authentication
- **Security:** Spring Security
- **Social Login:** OAuth2 Client
- **View Template:** Thymeleaf (일부 인증/관리 페이지용)

### External API & AI Integration
- **Payments:** Iamport REST Client (결제 검증 서비스)
- **Authentication:** Nurigo SDK (휴대폰 번호 인증), Spring Boot Mail (이메일 인증)
- **AI:** Spring AI (Google GenAI 모델, MongoDB Atlas Vector Store)

### Infrastructure & DevOps
- **Cloud:** Oracle Cloud Infrastructure (Ubuntu)
- **Container:** Docker
- **Web Server:** Nginx
- **Security:** Let's Encrypt (Certbot) for HTTPS

<br>

## ✨ Core Features & API

- **사용자 인증 및 인가 시스템**
  - Spring Security 및 JWT/Session 기반 로그인/로그아웃
  - OAuth2를 활용한 소셜 로그인 지원
  - Mail 및 SMS(Nurigo) API를 연동한 강력한 본인 인증 및 계정 복구(아이디/비번 찾기)
- **상품 관리 및 장바구니 API**
  - Querydsl을 활용한 복잡한 게이밍 상품 검색 및 동적 쿼리 페이징 처리
  - Redis를 활용한 장바구니 데이터의 빠른 CRUD 및 세션 관리
- **결제 검증 시스템 (Iamport 연동)**
  - 아임포트(Iamport) API를 이용한 결제 금액 위변조 방지 및 안전한 결제 승인 로직
  - 프론트엔드와 안전하게 연동되는 결제 상태 동기화
- **AI 챗봇 서비스 (Spring AI & Gemini)**
  - Spring WebFlux와 Google GenAI를 활용한 비동기 AI 응답 처리
  - MongoDB Atlas Vector Store를 활용한 데이터 임베딩 및 스마트 검색 지원

<br>

## 🛠 Trouble Shooting & Deep Dive

<details>
<summary><b>1. 로컬 H2 환경에서 운영 MySQL 환경으로의 데이터베이스 마이그레이션</b></summary>
<div markdown="1">
- **문제 상황:** 초기 개발 시 사용하던 H2 데이터베이스 구조를 클라우드 환경의 MySQL로 전환하는 과정에서 문법 차이 및 데이터 타입 충돌 발생.
- **해결 방안:** Spring Data JPA의 `ddl-auto` 속성을 적절히 제어하고, MySQL 방언(Dialect)에 맞춘 application.yml 설정 분리(로컬, 운영 환경 분리)를 통해 안정적으로 마이그레이션 완료.
</div>
</details>

<details>
<summary><b>2. Oracle Cloud(Ubuntu) 배포 시 Nginx 리버스 프록시 및 HTTPS 설정 이슈</b></summary>
<div markdown="1">
- **문제 상황:** 도커로 띄운 Spring Boot 서버와 외부 통신을 연결하는 과정에서 "White Label" 에러 페이지가 발생하거나 프론트엔드 통신 시 URL 오타(HTTPS 콜론 누락 등)로 인한 연결 문제 발생.
- **해결 방안:** Nginx를 리버스 프록시로 설정하여 80/443 포트 요청을 Spring Boot의 8080 포트로 포워딩. Certbot을 활용해 SSL/TLS 인증서를 발급받고 Nginx 설정 파일에 적용하여 안전한 HTTPS 통신 환경 구축 완료.
</div>
</details>

<br>

## 📂 Project Directory Structure

```text
src
 ├── main
 │    ├── java
 │    │    └── com.shop.respawn
 │    │         ├── chatBot        // AI 챗봇 (Gemini) 관련 로직
 │    │         ├── config         // 전역 설정 클래스
 │    │         ├── controller     // API 엔드포인트
 │    │         ├── domain         // JPA 엔티티 및 도메인 모델
 │    │         ├── dto            // 데이터 전송 객체 (Request/Response)
 │    │         ├── email          // 이메일 전송 및 인증 처리
 │    │         ├── exception      // 커스텀 예외 및 글로벌 예외 처리 핸들러
 │    │         ├── repository     // DB 접근 계층 (JPA, MongoDB, Redis)
 │    │         ├── security       // Spring Security, 인증/인가, OAuth 설정
 │    │         ├── service        // 핵심 비즈니스 로직
 │    │         ├── sms            // SMS 발송 및 인증 처리
 │    │         ├── util           // 공통 유틸리티 (등급/쿠폰 정책, 인증, 스케줄러 등)
 │    │         └── RespawnApplication.java  // 애플리케이션 진입점
 │    └── resources                // 환경 설정 파일 (application.yml 등)
 └── test                          // 단위 및 통합 테스트 코드
```

<br>

## 🚀 Getting Started

### 1. Repository Clone

```bash
git clone [https://github.com/wonji426/RESPAWN_Backend.git](https://github.com/wonji426/RESPAWN_Backend.git)
cd RESPAWN_Backend
```

### 2. Application Properties 설정
`src/main/resources/application.yml` 파일에 데이터베이스 연결 정보를 설정합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/respawn
    username: root
    password: your_password
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
