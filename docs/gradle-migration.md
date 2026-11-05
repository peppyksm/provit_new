# Maven → Gradle 전환

> 이 문서는 Spring Boot 도입 전 Gradle 전환 단계의 기록이다. 현재 실행 방식은 [Spring Boot 안내](spring-boot-migration.md), DB 구성·통합 테스트는 [PostgreSQL 안내](postgresql-migration.md)를 따른다.

## 범위

백엔드 빌드 도구를 Gradle 8.14.3으로 변경한다. Java 17, Spring Framework 5.3.39, MyBatis, Oracle JDBC, 기존 Java 소스·XML 설정·리소스, Tomcat 9 WAR 배포를 유지한다. Spring Boot, PostgreSQL, JPA, TypeScript 전환은 다음 단계에서 진행한다.

`backend/build.gradle`이 의존성과 빌드를 관리하며, `backend/settings.gradle`과 Wrapper를 사용한다. 루트의 npm 명령은 기존 프론트엔드 작업을 그대로 수행한다.

## 기존 설정 대응

| Maven 설정 | Gradle 설정 |
| --- | --- |
| Java 17 / UTF-8 | JDK 17 toolchain / `options.release = 17` / UTF-8 |
| `src/main/java`, `src/main/resources`, `src/main/webapp` | 같은 표준 소스 경로 |
| WAR / finalName `backend` | `war` 플러그인 / `build/libs/backend.war` |
| Servlet·JSP `provided` | `providedCompile` (테스트에서 사용, WAR에서 제외) |
| Lombok `provided` 및 annotation processor | `compileOnly` / `annotationProcessor` (테스트도 설정) |
| JJWT 구현체·Jackson 어댑터·JCL 브리지 `runtime` | `runtimeOnly` |
| JUnit 4·Spring Test | `testImplementation` / `useJUnit()` |
| Eclipse WTP | `eclipse-wtp`, 컨텍스트 경로 `/` |

직접 선언된 라이브러리 버전은 유지한다. Maven과 Gradle의 충돌 해결 방식 차이로 HikariCP가 참조하는 SLF4J 2 alpha가 선택되지 않도록 SLF4J 1.7.36을 명시적으로 고정한다.

## 실행

명령은 `backend` 폴더의 PowerShell 기준이다. `JAVA_HOME`은 JDK 17을 가리켜야 한다. macOS / Linux에서는 `.\gradlew.bat` 대신 `sh ./gradlew`를 사용한다.

```powershell
.\gradlew.bat clean build
.\gradlew.bat test --tests "*AuthHttpStatusTest"
.\gradlew.bat dependencies --configuration runtimeClasspath
```

기본 `build`는 컴파일, 외부 API를 호출하지 않는 테스트, WAR 생성을 실행한다. 테스트에서 상대 경로를 사용하는 기존 코드를 위해 작업 디렉터리는 `backend`로 고정한다. 실제 Oracle을 사용하는 테스트는 기본 실행 시 기존 `Assume` 조건으로 건너뛴다.

Gradle의 테스트 JVM은 별도 프로세스이므로 `provit.*` 시스템 속성을 전달하도록 설정했다. Oracle 테스트는 로컬 `database.properties`와 실행 중인 Oracle이 필요하며 격리된 테스트 객체를 생성·정리한다. 옵션을 바꿔 재검증할 때는 `--rerun-tasks`를 사용한다.

```powershell
.\gradlew.bat "-Dprovit.oracle.documentReviewTests=true" test --tests "*OracleDocumentReviewPersistenceTest" --rerun-tasks
.\gradlew.bat "-Dprovit.oracle.documentTitleTests=true" test --tests "*OracleDocumentTitleLengthTest" --rerun-tasks
.\gradlew.bat "-Dprovit.oracle.mainResumeTests=true" test --tests "*OracleMainResumeTest" --rerun-tasks
.\gradlew.bat "-Dprovit.oracle.lengthTests=true" test --tests "*OracleInterviewTextLengthTest" --rerun-tasks
```

실제 외부 사주 API를 호출하는 기존 `SazuApiClientTest`, `FortuneServiceTest`는 `test`에서 분리했다. 유효한 로컬 `api.properties`로 아래 작업을 명시적으로 실행한다. 이 작업은 기본 `build`에 포함되지 않는다.

```powershell
.\gradlew.bat externalApiTest
```

문서 첨삭의 실제 유료 OpenAI 테스트는 기존 두 옵션을 모두 켜야 실행한다: `provit.oracle.documentReviewTests=true`, `provit.openai.documentReviewTests=true`. 일반 빌드에서는 실행하지 않는다.

## 배포 및 IDE

Spring Boot 실행 파일이 아니므로 `bootRun`이나 `java -jar` 대신 Tomcat 9에 WAR를 배포한다. 기존 `/api` 주소를 유지하려면 컨텍스트 경로를 `/`로 설정한다. 파일 배포 시 `backend.war`를 `ROOT.war`로 배포한다. 설정 파일은 기존처럼 WAR의 `WEB-INF/classes`에 포함되므로 WAR도 접속정보가 포함된 배포 파일로 관리한다.

Eclipse / STS는 Buildship의 Existing Gradle Project로 `backend`를 가져온다. 과거 Maven용 `.project`, `.classpath`, `.settings`는 Git에서 제외된 로컬 파일이므로 IDE에서 기존 프로젝트를 제거할 때 디스크 파일 삭제를 선택하지 않는다. `.\gradlew.bat eclipse` 실행 후 새로고침하면 WTP 배포 설정을 생성할 수 있다.

Wrapper 배포본에는 SHA-256 검증을 설정했다. 개발 머신별 JDK 경로와 Gradle 캐시는 커밋하지 않는다.

## 전환 검증 (2026-11-05)

- 검증용 Temurin JDK 17로 Wrapper의 `clean build` 성공.
- 기본 테스트: 154개 통과, Oracle·실제 유료 OpenAI 옵션이 필요한 22개 건너뜀, 실패 0개.
- 네 가지 Oracle 옵션을 켠 별도 실행: 21개 통과, 실제 유료 OpenAI 요청 1개 건너뜀, 실패 0개.
- 기존 `target/backend.war`와 새 WAR의 라이브러리 목록 대조: 41개 JAR의 이름·버전 일치.
- MyBatis 매퍼 19개와 Spring·Servlet 설정 포함, Servlet/JSP API·Lombok·테스트 라이브러리는 WAR에서 제외됨.
- Tomcat 재배포 및 화면 전체 재점검은 이번 빌드 검증에 포함하지 않았다.

현재 머신의 기본 JDK 26 설정은 변경하지 않았다. 검증용 JDK 17은 Git에서 제외되는 `backend/target/gradle-migration-tools/jdk-17.0.21+9`에 준비했다. 이 머신에서 같은 환경으로 실행하려면 저장소 루트에서 다음과 같이 지정할 수 있다. 다른 머신에서는 설치한 JDK 17 경로를 사용한다.

```powershell
$env:JAVA_HOME = (Resolve-Path .\backend\target\gradle-migration-tools\jdk-17.0.21+9).Path
cd backend
.\gradlew.bat clean build
```

공식 참고: [Gradle WAR 플러그인](https://docs.gradle.org/current/userguide/war_plugin.html), [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).
