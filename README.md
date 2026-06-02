# HT AX Hackathon 신청 시스템

해태제과 사내 행사인 **제1회 HT AX 해커톤**의 참가 신청 웹 애플리케이션입니다.
참가자는 신청서를 제출하고, 관리자는 신청 목록 조회, 상세 확인, 수정, 삭제,
첨부파일 다운로드를 할 수 있습니다.

## 기술 구성

- Java 17
- Spring Boot 3.x, 내장 Tomcat
- Gradle Groovy
- Thymeleaf, Spring Web
- Spring Data JPA, Validation
- 로컬 DB: MySQL
- 운영 DB: Oracle

이 애플리케이션은 실행 가능한 JAR 방식입니다. 서버에 Tomcat, Gradle 또는 시스템
Java 17을 설치하지 않고, 배포 폴더 안에 Linux용 JDK 17을 함께 넣어 실행합니다.

## 로컬 개발

MySQL DB를 생성합니다.

```sql
CREATE DATABASE ht_ax_hackathon
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

루트 디렉터리에서 `.env`를 만들고 로컬 DB 정보를 입력합니다.

```bash
cp .env.example .env
```

```properties
DB_URL=jdbc:mysql://localhost:3306/ht_ax_hackathon?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=your_password
DB_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
FILE_UPLOAD_DIR=uploads
```

실행:

```bash
./gradlew bootRun
```

접속:

- 랜딩 페이지: `http://localhost:8080/`
- 참가 신청: `http://localhost:8080/apply`
- 관리자 목록: `http://localhost:8080/admin/applications`

## 운영 배포 구조

운영 서버의 배포 폴더 예시는 `/opt/ht-ax-hackathon`입니다.

```text
/opt/ht-ax-hackathon/
├── jdk-17/                 # 서버 OS와 CPU에 맞는 Linux용 JDK 17
│   └── bin/java
├── ht-ax-hackathon.jar     # 빌드한 Spring Boot JAR
├── .env                    # Oracle 접속 정보와 서버 설정
├── uploads/                # 사용자가 업로드한 실제 첨부파일
└── logs/
    └── application.log
```

`uploads`에는 PDF, 이미지 등 사용자가 첨부한 실제 파일이 UUID 파일명으로 저장됩니다.
DB에는 원본 파일명, 저장 파일명, 파일 경로, 크기, Content-Type이 기록됩니다.
재배포할 때 `.env`와 `uploads`를 삭제하면 안 됩니다.

## 배포 전 확인

인프라 담당자와 DBA에게 다음 항목을 확인합니다.

- 웹 서버 OS와 CPU 아키텍처: 예를 들어 Linux x86_64
- 웹 서버 IP
- Oracle DB 서버 IP와 포트: 일반적으로 `1521`
- 웹 서버에서 Oracle DB 포트로 통신 가능한지 여부
- Oracle 서비스명 또는 SID
- Oracle 계정과 비밀번호
- Oracle 버전: 12c 미만이면 PK 생성 전략 변경 검토 필요
- 외부 인터넷 공개용 도메인과 HTTPS 적용 여부

웹 서버에서 DB 포트 접근을 확인할 수 있습니다.

```bash
nc -vz DB서버IP 1521
```

`nc`가 없다면 인프라 담당자에게 통신 가능 여부를 확인 요청합니다.

## Oracle 테이블 준비

운영 Oracle DB에는 다음 테이블이 필요합니다.

```text
HACKATHON_APPLICATIONS
TEAM_MEMBERS
ATTACHMENT_FILES
```

운영 Oracle 10g DB는 `IDENTITY` 컬럼을 지원하지 않습니다. 애플리케이션은 테이블별
시퀀스를 사용하므로 최초 배포 전에 DBA와 협의하여 아래 SQL을 실행합니다.

부분 생성된 테이블이 있다면 데이터가 없는 것을 확인한 뒤 삭제합니다.

```sql
DROP TABLE attachment_files CASCADE CONSTRAINTS;
DROP TABLE team_members CASCADE CONSTRAINTS;
DROP TABLE hackathon_applications CASCADE CONSTRAINTS;

DROP SEQUENCE seq_attachment_files;
DROP SEQUENCE seq_team_members;
DROP SEQUENCE seq_hackathon_applications;
```

없는 객체의 삭제 오류는 무시합니다. 테이블과 시퀀스를 생성합니다. Oracle 10g는
식별자 길이가 최대 30자이므로 제약 조건 이름은 짧게 지정합니다.

```sql
CREATE SEQUENCE seq_hackathon_applications START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_team_members START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_attachment_files START WITH 1 INCREMENT BY 1;

CREATE TABLE hackathon_applications (
    id NUMBER(19) PRIMARY KEY,
    team_name VARCHAR2(100) NOT NULL,
    topic VARCHAR2(200) NOT NULL,
    content CLOB NOT NULL,
    status VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_app_status
        CHECK (status IN ('SUBMITTED', 'REVIEWING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE team_members (
    id NUMBER(19) PRIMARY KEY,
    department VARCHAR2(100) NOT NULL,
    employee_no VARCHAR2(50) NOT NULL,
    name VARCHAR2(50) NOT NULL,
    application_id NUMBER(19) NOT NULL,
    CONSTRAINT fk_team_app
        FOREIGN KEY (application_id)
        REFERENCES hackathon_applications (id)
);

CREATE TABLE attachment_files (
    id NUMBER(19) PRIMARY KEY,
    original_file_name VARCHAR2(255) NOT NULL,
    stored_file_name VARCHAR2(255) NOT NULL,
    file_path VARCHAR2(1000) NOT NULL,
    file_size NUMBER(19) NOT NULL,
    content_type VARCHAR2(255),
    application_id NUMBER(19) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_file_app
        FOREIGN KEY (application_id)
        REFERENCES hackathon_applications (id)
);
```

테이블 생성 후 운영 환경에서는 다음 설정을 사용합니다.

```properties
JPA_DDL_AUTO=validate
```

`validate`는 기존 테이블 구조를 검증하며 테이블을 자동 생성하거나 변경하지 않습니다.

## 배포 JAR 생성

개발 PC의 프로젝트 루트에서 실행합니다.

```bash
./gradlew clean test bootJar
```

생성 파일:

```text
build/libs/ht-ax-hackathon-0.0.1-SNAPSHOT.jar
```

서버에 전달할 때 파일명을 단순하게 변경해도 됩니다.

```text
ht-ax-hackathon.jar
```

## 서버 배포

### 1. 디렉터리 생성

```bash
mkdir -p /opt/ht-ax-hackathon/uploads
mkdir -p /opt/ht-ax-hackathon/logs
```

### 2. JDK 17 배치

서버 OS와 CPU 아키텍처에 맞는 **Linux용 JDK 17 압축 파일**을 준비합니다.
압축을 풀어 다음 파일이 존재하게 만듭니다.

```text
/opt/ht-ax-hackathon/jdk-17/bin/java
```

Windows용 JDK를 Linux 서버에 복사하면 실행되지 않습니다.

### 3. JAR 배치

빌드한 JAR를 다음 경로에 복사합니다.

```text
/opt/ht-ax-hackathon/ht-ax-hackathon.jar
```

### 4. Oracle 환경변수 작성

서버의 `/opt/ht-ax-hackathon/.env` 파일에 실제 운영 정보를 입력합니다.
실제 IP와 비밀번호가 들어간 `.env`는 Git에 커밋하지 않습니다.

```properties
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=8080
DB_URL=jdbc:oracle:thin:@//DB서버IP:1521/서비스명
DB_USERNAME=운영계정
DB_PASSWORD=운영비밀번호
DB_DRIVER_CLASS_NAME=oracle.jdbc.OracleDriver
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false
FILE_UPLOAD_DIR=/opt/ht-ax-hackathon/uploads
```

Oracle이 SID 방식이면 `DB_URL`만 변경합니다.

```properties
DB_URL=jdbc:oracle:thin:@DB서버IP:1521:SID
```

`.env` 읽기 권한을 제한합니다.

```bash
chmod 600 /opt/ht-ax-hackathon/.env
```

### 5. 동봉한 JDK 17로 실행

서버에 기본 설치된 구버전 Java를 사용하지 않습니다. 반드시 배포 폴더 안의
`jdk-17/bin/java` 경로를 명시합니다.

```bash
cd /opt/ht-ax-hackathon
./jdk-17/bin/java -version
nohup ./jdk-17/bin/java -jar ht-ax-hackathon.jar > logs/application.log 2>&1 &
```

로그 확인:

```bash
tail -f /opt/ht-ax-hackathon/logs/application.log
```

서버 내부 동작 확인:

```bash
curl http://localhost:8080/
```

프로세스 확인:

```bash
ps -ef | grep ht-ax-hackathon.jar
```

## 외부 인터넷 공개

JAR를 실행하는 것과 외부 인터넷에서 접속 가능하게 만드는 것은 별도 작업입니다.
외부 공개가 필요하면 인프라 담당자가 DNS, HTTPS 인증서, 방화벽, 리버스 프록시를
설정해야 합니다.

권장 흐름:

```text
외부 사용자 브라우저
  ↓ HTTPS 443
https://발급도메인
  ↓ DNS + 방화벽 + Nginx 또는 사내 프록시
http://127.0.0.1:8080
  ↓
Spring Boot JAR
  ↓
Oracle DB + uploads 폴더
```

인프라 담당자에게 다음 내용을 전달합니다.

```text
외부 인터넷에서 접속 가능한 해커톤 신청 페이지가 필요합니다.

Spring Boot 애플리케이션은 웹 서버의 localhost:8080에서 실행합니다.
외부 공개 도메인 발급, DNS 연결, HTTPS 인증서 적용, 외부 방화벽 443 허용,
리버스 프록시에서 http://127.0.0.1:8080으로 전달 설정을 요청드립니다.

8080 포트는 외부에 직접 공개하지 않고 HTTPS 443을 통해서만 접근하도록 부탁드립니다.
```

도메인 연결 후 접속 URL:

- 랜딩 페이지: `https://발급도메인/`
- 참가 신청: `https://발급도메인/apply`
- 관리자 목록: `https://발급도메인/admin/applications`

현재 관리자 로그인 기능은 없습니다. 외부 공개 기간에는 관리자 URL을 공유하지 않고,
가능하면 인프라에서 `/admin` 경로를 사내 IP 또는 VPN으로 제한합니다.

## 중지와 재배포

프로세스 ID를 확인하고 정상 종료 신호를 보냅니다.

```bash
ps -ef | grep ht-ax-hackathon.jar
kill 프로세스ID
```

재배포할 때는 기존 `.env`와 `uploads` 디렉터리를 유지하고 JAR만 교체한 후 다시
실행합니다. 운영 서버 재부팅 후 자동 시작이 필요하면 인프라 담당자와 협의하여
`systemd` 서비스로 등록합니다.

## 관리자 기능

- 신청 목록 및 상세 조회
- 신청 정보 수정
- 첨부파일 다운로드
- 삭제 확인 화면을 거친 신청서 삭제
- 신청서 삭제 시 연관 DB 데이터와 실제 업로드 파일 삭제 시도

팀원 사번은 숫자 7자리만 허용합니다.

## 테스트

테스트는 H2 인메모리 DB를 사용합니다.

```bash
./gradlew test
```
