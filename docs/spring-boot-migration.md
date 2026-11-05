# Spring Framework → Spring Boot 전환

> 이 문서는 Boot 전환 당시 Oracle을 유지한 단계의 기록이다. 현재 DB 설정·테스트 실행은 [PostgreSQL 전환 안내](postgresql-migration.md)를 따른다.

## 현재 구성

Spring Boot 4.1.2, Spring Framework 7.0.10, 내장 Tomcat 11.0.27, Java 17, Gradle 8.14.3을 사용한다. Oracle 19c와 기존 MyBatis DAO·SQL·DTO를 사용하며 프론트엔드는 기존 React JavaScript 구성이다. PostgreSQL·JPA·TypeScript 전환은 후속 단계다.

기존 외부 Tomcat용 WAR 대신 `backend/build/libs/backend.jar`를 실행한다. `ProvitApplication`이 단일 애플리케이션 컨텍스트를 구성한다. 기존 `web.xml`, `root-context.xml`, `servlet-context.xml`은 제거했다.

## 설정 대응

| 기존 설정 | Spring Boot 구성 |
| --- | --- |
| ContextLoaderListener·DispatcherServlet | `@SpringBootApplication`, MVC 자동 설정 |
| Oracle DataSource·HikariCP | `spring.datasource.*`, 기존 `db.*` 키 연결 |
| SqlSessionFactory·SqlSessionTemplate | MyBatis Boot Starter 4.1.0, 기존 매퍼 19개·`mybatis-config.xml` |
| 트랜잭션 매니저 | `ApplicationConfiguration`의 `DataSourceTransactionManager`, 기존 `@Transactional` |
| BCryptPasswordEncoder | `ApplicationConfiguration` |
| 로그인 사용자 인자 해석·CORS·공개 파일 경로 | `WebConfiguration` (`WebMvcConfigurer`) |
| 업로드 제한 | 파일 20MiB·전체 요청 50MiB·임시 파일 기준 0바이트 |
| 문자 인코딩 필터 | `server.servlet.encoding.*`, UTF-8 강제 |
| JavaMailSender | Mail Starter, 기존 `mail.*` 키 연결 |
| 스케줄러 2개 스레드 | `SchedulingConfiguration`, `spring.task.scheduling.pool.size=2` |

Spring·Servlet·메일·로깅·Lombok 등의 호환 버전은 Boot BOM으로 관리한다. Spring 관련 `javax.servlet`, `javax.annotation`, `javax.mail` 참조를 `jakarta.*`로 변경했다. Java 표준 API인 `javax.sql`, `javax.crypto`, `javax.xml`은 그대로 사용한다. 기존 JUnit 4 테스트는 JUnit Platform의 Vintage 엔진으로 실행하며 새 Boot 통합 테스트는 Jupiter를 사용한다.

MyBatis는 스타터가 지정하는 3.5.19 / MyBatis-Spring 4.1.0을 사용한다. Oracle JDBC는 Boot BOM의 `ojdbc8` 23.26.3.0.0으로 관리하며 기존 Oracle 19c에서 실제 조회와 격리 테이블 테스트를 검증했다. DB 스키마·운영 데이터 이관은 수행하지 않았다.

## API 호환

요청 경로, DTO, 응답의 `responseCode`·`data` 구조, 인증 쿠키 이름·HttpOnly·SameSite·만료 시간, 인증 실패의 HTTP 401·403 처리를 유지한다. `-parameters`로 컴파일해 이름을 생략한 `@RequestParam`·`@PathVariable`이 Spring 7에서도 해석되게 한다.

Boot 4의 기본 Jackson 3 대신 공식 `spring-boot-jackson2` 호환 모듈을 사용한다. 기존 Jackson 2 `JsonNode` 요청, `@JsonIgnore`, `@JsonFormat`, 날짜 밀리초 응답과 첨삭 JSON 처리 방식을 유지하기 위한 선택이다. Jackson 2 호환 모듈은 deprecated 상태이므로 향후 Jackson 3 전환은 별도 작업으로 다룬다.

공개 파일은 `/uploads/post_uploadfile/**`, `/uploads/profile_uploadfile/**`에만 연결한다. 비공개 포트폴리오는 인증 API로 제공한다. 없는 경로·파일은 Spring 7의 예외 처리 방식에 맞춰 HTTP 404로 반환한다.

## 로컬 실행

JDK 17을 `JAVA_HOME`으로 지정하고 `backend` 폴더에서 실행한다.

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

빌드한 파일은 같은 폴더에서 실행한다.

```powershell
java -jar build/libs/backend.jar
```

IDE에서는 Gradle 프로젝트로 가져온 뒤 `com.provit.ProvitApplication`을 실행한다. 기본 포트는 8080이고 컨텍스트 경로는 루트이므로 기존 Vite 프록시 설정을 사용한다. 외부 Tomcat 설치·WAR 배포 단계가 필요하지 않다.

`GET http://localhost:8080/api/health`의 HTTP 200과 `data.framework`를 확인한다. `data.dbTime`이 실제 Oracle 조회 결과인지도 확인하면 DB 연결을 점검할 수 있다. 기존 health API는 DB 연결 실패도 메시지로 반환하므로 HTTP 200만으로 DB 정상 여부를 판정하지 않는다.

현재 머신에 이전 단계에서 준비한 JDK 17을 사용할 때는 저장소 루트에서 다음과 같이 지정할 수 있다.

```powershell
$env:JAVA_HOME = (Resolve-Path .\backend\target\gradle-migration-tools\jdk-17.0.21+9).Path
cd backend
.\gradlew.bat bootRun
```

## 설정 파일과 스케줄러

기존 `backend/src/main/resources/database.properties`, `api.properties`를 읽는다. 실행 디렉터리의 `config/database.properties`, `config/api.properties`도 선택적으로 읽으며 뒤에 읽은 외부 파일이 우선한다. 기존 `db.*`, `api.*`, `jwt.*`, `mail.*`, `auth.*`, 파일 저장 경로 키를 사용한다. `jwt.secret`의 기존 최소 길이 검증도 유지한다.

Spring Boot 환경변수·명령줄 옵션으로 설정을 덮어쓸 수 있다. 예를 들어 포트는 `--server.port=8081`로 바꾼다. SQL 자동 초기화는 꺼져 있으며 기존 Oracle 스키마를 사용한다. 로컬 설정 파일을 리소스에 두면 JAR에도 포함되므로 배포 시에는 외부 `config` 파일 사용을 권장한다.

기본 기동 시 기존처럼 채용공고 초기 수집과 예약 작업을 실행한다. 자동 수집이나 예약 작업 없이 기동만 확인하려면 아래 옵션을 사용한다.

```powershell
.\gradlew.bat bootRun --args="--provit.scheduling.enabled=false"
java -jar build/libs/backend.jar --provit.scheduling.enabled=false
```

이 옵션은 채용공고 초기 수집 빈과 모든 `@Scheduled` 처리를 비활성화한다. 기본값은 `true`다.

## 검증 실행

기본 `build`에서 외부 DB·API 없이 기존 테스트와 실제 내장 서버 HTTP 통합 테스트를 실행한다. Boot 통합 테스트는 DB와 일부 업무 서비스를 대체하며 별도 임시 파일 경로를 사용한다. 기존 SQL의 실제 Oracle 동작은 별도 옵션으로 검증한다.

```powershell
.\gradlew.bat test --tests "com.provit.BootApplicationTest"
.\gradlew.bat "-Dprovit.oracle.documentReviewTests=true" "-Dprovit.oracle.documentTitleTests=true" "-Dprovit.oracle.mainResumeTests=true" "-Dprovit.oracle.lengthTests=true" test --tests "*Oracle*Test" --rerun-tasks
```

실제 외부 사주 API 테스트는 `externalApiTest`로 분리되어 있다. 실제 유료 OpenAI 테스트는 `provit.openai.documentReviewTests=true`까지 명시해야 한다. 기본 검증에는 둘 다 포함되지 않는다.

## 검증 결과 (2026-11-05)

- JDK 17·Gradle Wrapper로 `clean build` 성공, 실행 파일 `build/libs/backend.jar` 생성.
- 기본 테스트 185개 중 163개 통과, Oracle·유료 OpenAI 옵션이 필요한 22개 건너뜀, 실패 0개.
- 기본 테스트에 실제 내장 Tomcat HTTP 통합 검사 9개 포함: 전체 빈 구성·매퍼·트랜잭션 프록시, health JSON, CORS, 비로그인 401, 로그인 쿠키 및 사용자 식별, 한글·날짜·토큰 숨김 JSON, 공개 이미지·비공개 파일 경로, multipart 정상 처리, 20MiB 초과 차단.
- 별도 Oracle 테스트 22개 중 21개 통과, 실제 유료 OpenAI 요청 1개 건너뜀, 실패 0개. 테스트가 생성한 격리 객체는 기존 테스트의 종료 처리에서 정리한다.
- 실행 JAR를 임시 포트에서 직접 기동해 실제 Oracle 조회, health HTTP 200, 비로그인 HTTP 401, CORS 응답 확인. 이 검증은 자동 수집·예약 작업을 끈 상태로 수행하고 임시 서버를 종료했다.
- 실행 JAR에 MyBatis 매퍼 19개·Boot 실행 진입점 포함, 이전 Spring 5·Servlet `javax` 라이브러리 중복 없음.
- 실제 외부 API 호출, 일반 계정의 화면 전체 흐름, 운영 배포는 이번 검증 범위에 포함하지 않았다.

공식 참고: [Boot 시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html), [Boot 4 전환 안내](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [Jackson 2 호환 설정](https://docs.spring.io/spring-boot/reference/features/json.html), [MyBatis Boot 호환표](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/).
