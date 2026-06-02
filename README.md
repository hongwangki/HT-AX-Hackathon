# HT AX 해커톤 신청 시스템 Windows 서버 배포 가이드

이 문서는 Windows 회사 서버의 `C:\HT-AX-Hackathon` 폴더에 애플리케이션을 배포하고,
PC와 휴대폰에서 도메인으로 접속할 수 있게 만드는 절차입니다.

## 1. 최종 폴더 구조

회사 서버의 `C:` 드라이브에 아래 구조로 파일을 배치합니다.

```text
C:\HT-AX-Hackathon\
├── jdk17\
│   └── bin\
│       └── java.exe
├── logs\
├── uploads\
├── ht-ax-hackathon.jar
└── .env
```

`jdk17`은 Windows x64용 JDK 17 압축 파일을 풀어 넣은 폴더입니다.
Java를 JAR 안에 넣는 것이 아니라 JAR와 같은 배포 폴더에 함께 둡니다.

`uploads`에는 사용자가 첨부한 실제 파일이 저장됩니다. 재배포할 때 삭제하면 안 됩니다.

## 2. 개발 PC에서 JAR 빌드

개발 PC에서 PowerShell 또는 명령 프롬프트를 열고 실행합니다.

```bat
cd C:\HT-AX-Hackathon
gradlew.bat clean test bootJar
```

`BUILD SUCCESSFUL`이 나오면 아래 파일이 생성됩니다.

```text
C:\HT-AX-Hackathon\build\libs\ht-ax-hackathon-0.0.1-SNAPSHOT.jar
```

이 파일을 회사 서버에 복사하면서 이름을 아래처럼 변경합니다.

```text
C:\HT-AX-Hackathon\ht-ax-hackathon.jar
```

## 3. 회사 서버에 파일 배치

회사 서버에 접속한 뒤 PowerShell을 열고 폴더를 만듭니다.

```powershell
New-Item -ItemType Directory -Force C:\HT-AX-Hackathon
New-Item -ItemType Directory -Force C:\HT-AX-Hackathon\logs
New-Item -ItemType Directory -Force C:\HT-AX-Hackathon\uploads
```

개발 PC에서 아래 파일과 폴더를 회사 서버의 `C:\HT-AX-Hackathon`에 복사합니다.

```text
Windows x64용 JDK 17 폴더  -> C:\HT-AX-Hackathon\jdk17
빌드한 JAR                 -> C:\HT-AX-Hackathon\ht-ax-hackathon.jar
```

복사 후 아래 파일이 존재해야 합니다.

```text
C:\HT-AX-Hackathon\jdk17\bin\java.exe
C:\HT-AX-Hackathon\ht-ax-hackathon.jar
```

## 4. DB 준비 상태 확인

SSMS에서 `HT_AX` DB를 선택하고 실행합니다.

```sql
USE HT_AX;
GO

SELECT name FROM sys.tables ORDER BY name;
SELECT name FROM sys.sequences ORDER BY name;
GO
```

아래 테이블 3개와 시퀀스 3개가 조회되어야 합니다.

```text
attachment_files
hackathon_applications
team_members

seq_attachment_files
seq_hackathon_applications
seq_team_members
```

이미 생성되어 있다면 DB에서 추가로 실행할 쿼리는 없습니다.

## 5. `.env` 작성

회사 서버에서 아래 파일을 만듭니다.

```text
C:\HT-AX-Hackathon\.env
```

실제 DB 접속 정보로 작성합니다.

```properties
DB_URL=jdbc:sqlserver://DB서버IP:1433;databaseName=HT_AX;encrypt=true;trustServerCertificate=true
DB_USERNAME=실제아이디
DB_PASSWORD=실제비밀번호
DB_DRIVER_CLASS_NAME=com.microsoft.sqlserver.jdbc.SQLServerDriver
JPA_DDL_AUTO=validate
FILE_UPLOAD_DIR=uploads
JPA_SHOW_SQL=false
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=8080
```

- SQL Server가 웹 서버와 같은 서버에 설치된 경우에만 `DB서버IP` 대신 `localhost`를 사용합니다.
- `.env`는 DB 비밀번호가 있으므로 Git에 올리거나 외부에 공유하면 안 됩니다.
- 파일명은 `.env.txt`가 아니라 정확히 `.env`여야 합니다.

## 6. 회사 서버에서 DB 통신 확인

회사 서버의 PowerShell에서 실행합니다.

```powershell
Test-NetConnection DB서버IP -Port 1433
```

아래 결과가 나와야 합니다.

```text
TcpTestSucceeded : True
```

`False`라면 애플리케이션 실행 전에 인프라 담당자에게 회사 서버에서 DB 서버의
`1433` 포트로 접근할 수 있도록 방화벽 설정을 요청합니다.

## 7. 최초 실행

회사 서버의 PowerShell에서 실행합니다.

```powershell
cd C:\HT-AX-Hackathon
.\jdk17\bin\java.exe -version
.\jdk17\bin\java.exe -jar .\ht-ax-hackathon.jar
```

첫 번째 명령에서 Java 버전이 `17`로 표시되어야 합니다.

두 번째 명령 실행 후 로그에 아래와 비슷한 메시지가 나오면 정상입니다.

```text
Tomcat started on port 8080
Started HtAxHackathonApplication
```

`No JTA platform available`은 오류가 아니므로 무시합니다.

## 8. 서버 내부 접속 확인

애플리케이션을 실행한 상태에서 PowerShell 창을 하나 더 열고 실행합니다.

```powershell
Invoke-WebRequest http://localhost:8080/
```

브라우저에서도 확인합니다.

```text
http://localhost:8080/
```

주요 화면:

```text
http://localhost:8080/                     랜딩 페이지
http://localhost:8080/apply                참가 신청
http://localhost:8080/admin/applications   관리자 신청 목록
```

신청서를 한 건 등록하고 아래 항목을 확인합니다.

- 신청 완료 화면이 표시되는지 확인
- 관리자 목록에 신청서가 표시되는지 확인
- 첨부파일 업로드와 다운로드가 되는지 확인
- 엑셀 다운로드가 되는지 확인
- `C:\HT-AX-Hackathon\uploads` 폴더에 첨부파일이 생성되는지 확인

## 9. 백그라운드 실행

최초 실행 확인이 끝나면 실행 중인 프로그램을 `Ctrl + C`로 종료합니다.

PowerShell에서 아래 명령을 실행하면 창을 닫아도 애플리케이션이 계속 실행됩니다.

```powershell
cd C:\HT-AX-Hackathon

Start-Process `
    -FilePath ".\jdk17\bin\java.exe" `
    -ArgumentList "-jar .\ht-ax-hackathon.jar" `
    -RedirectStandardOutput ".\logs\application.log" `
    -RedirectStandardError ".\logs\application-error.log" `
    -WindowStyle Hidden
```

실행 로그:

```text
C:\HT-AX-Hackathon\logs\application.log
C:\HT-AX-Hackathon\logs\application-error.log
```

로그 확인:

```powershell
Get-Content C:\HT-AX-Hackathon\logs\application.log -Wait
```

## 10. 휴대폰과 외부 PC에서 접속하도록 설정

`C:\HT-AX-Hackathon`에 JAR를 복사하고 실행하는 것만으로 외부 인터넷 접속이 자동으로
열리지는 않습니다.

휴대폰 LTE·5G, 집 PC, 외부 네트워크 등 어디서든 접속하려면 인프라 담당자가
도메인, HTTPS, 외부 방화벽, 리버스 프록시를 설정해야 합니다.

권장 연결 구조:

```text
외부 사용자 PC 또는 휴대폰
        ↓ HTTPS 443
https://발급도메인
        ↓ DNS + 방화벽 + 리버스 프록시
http://127.0.0.1:8080
        ↓
Spring Boot 애플리케이션
        ↓
SQL Server HT_AX DB
```

인프라 담당자에게 아래 내용을 전달합니다.

```text
HT AX 해커톤 신청 시스템 외부 공개를 요청드립니다.

Windows 서버의 C:\HT-AX-Hackathon에서 Spring Boot 애플리케이션이 실행되며,
내부적으로 0.0.0.0:8080 포트를 사용합니다.

외부 사용자가 PC와 휴대폰에서 접속할 수 있도록 외부 공개 도메인 발급,
DNS 연결, HTTPS 인증서 적용, 외부 방화벽 443 허용,
리버스 프록시에서 http://127.0.0.1:8080으로 전달 설정을 요청드립니다.

8080 포트는 외부에 직접 공개하지 않고 HTTPS 443을 통해서만 접근하도록 설정해 주세요.
관리자 경로 /admin은 사내 IP 또는 VPN에서만 접근 가능하도록 제한해 주세요.
```

설정 완료 후 외부 접속 주소:

```text
https://발급도메인/
https://발급도메인/apply
https://발급도메인/admin/applications
```

웹 화면은 모바일 반응형 CSS가 적용되어 있어 휴대폰 화면에서도 사용할 수 있습니다.

## 11. 서버 재부팅 후 자동 실행

`Start-Process` 방식은 서버가 재부팅되면 다시 실행해야 합니다.

운영 배포에서는 인프라 담당자에게 아래 작업을 요청합니다.

```text
Windows 서버 재부팅 후에도
C:\HT-AX-Hackathon\jdk17\bin\java.exe -jar C:\HT-AX-Hackathon\ht-ax-hackathon.jar
명령이 자동 실행되도록 Windows 서비스 또는 작업 스케줄러 등록을 요청드립니다.
작업 시작 위치는 C:\HT-AX-Hackathon으로 설정해 주세요.
```

시작 위치가 다르면 애플리케이션이 `.env` 파일을 찾지 못할 수 있습니다.

## 12. 종료

실행 중인 Java 프로세스를 확인합니다.

```powershell
Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -like "*ht-ax-hackathon.jar*" } |
    Select-Object ProcessId, CommandLine
```

확인한 프로세스 ID로 종료합니다.

```powershell
Stop-Process -Id 프로세스ID
```

## 13. 재배포

재배포할 때는 `.env`, `uploads`, `logs`, `jdk17` 폴더를 유지합니다.

1. 실행 중인 프로세스를 종료합니다.
2. 새로 빌드한 JAR만 교체합니다.
3. 백그라운드 실행 명령을 다시 실행합니다.
4. 로그와 접속 화면을 확인합니다.

교체 대상:

```text
C:\HT-AX-Hackathon\ht-ax-hackathon.jar
```

삭제하면 안 되는 항목:

```text
C:\HT-AX-Hackathon\.env
C:\HT-AX-Hackathon\uploads
C:\HT-AX-Hackathon\logs
C:\HT-AX-Hackathon\jdk17
```

## 14. 오류 확인 순서

실행에 실패하면 아래 순서로 확인합니다.

1. `C:\HT-AX-Hackathon\jdk17\bin\java.exe -version`이 정상 동작하는지 확인
2. Java 버전이 `17`인지 확인
3. `.env`가 `C:\HT-AX-Hackathon\.env` 경로에 있는지 확인
4. `.env` 파일명이 `.env.txt`가 아니라 정확히 `.env`인지 확인
5. DB IP, 아이디, 비밀번호가 맞는지 확인
6. `Test-NetConnection DB서버IP -Port 1433` 결과가 `True`인지 확인
7. `HT_AX` DB에 테이블 3개와 시퀀스 3개가 있는지 확인
8. `uploads`와 `logs` 폴더에 쓰기 권한이 있는지 확인

`Schema-validation` 오류가 나오면 DB 테이블 구조와 애플리케이션 엔티티 구조가
다른 상태입니다. `JPA_DDL_AUTO=update`로 임의 변경하지 말고 테이블 구조를 확인합니다.
