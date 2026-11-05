# Oracle → PostgreSQL 전환

## 현재 상태

2026-11-05에 설치된 PostgreSQL 18 서버(`127.0.0.1:5432`)에 앱 전용 로그인 역할 `provit`과 데이터베이스 `provit_new`를 새로 만들었다. 사용자의 선택에 따라 기존 Oracle 업무 데이터는 이관하지 않았으며 Oracle DB·계정·데이터는 수정하지 않았다.

새 DB의 `public` 스키마에는 테이블 27개와 시퀀스 19개, 직군 21개·직무 2,178개·게시판 카테고리 4개(질문·정보·후기·공지)만 초기화했다. 회원·문서·첨삭·면접·채용공고·게시글·스터디 등 업무 데이터는 비어 있다. 테스트 데이터는 별도 임시 스키마에서 생성하고 검증 후 제거한다.

`provit`은 관리자 권한·역할 생성·DB 생성 권한이 없는 앱 계정이며 새 DB의 소유자다. 앱은 관리자 계정으로 연결하지 않는다. 생성한 앱 비밀번호와 접속 URL은 Git에서 제외되는 `backend/src/main/resources/database.properties`에 저장했다. 관리자 비밀번호는 프로젝트 파일에 저장하지 않았다.

## 코드와 SQL

Spring Boot 4.1.2·Java 17·Gradle 8.14.3·기존 MyBatis DAO를 사용한다. JDBC 드라이버는 Boot BOM이 관리하는 `org.postgresql:postgresql` 42.7.14로 전환했으며 실행 JAR에서 Oracle JDBC 의존성을 제거했다. JPA·TypeScript 전환은 후속 단계다.

| Oracle 구성 | PostgreSQL 구성 |
| --- | --- |
| `SEQ_T_*.NEXTVAL FROM DUAL` | `nextval('seq_t_*')`, `DUAL` 제거 |
| `NVL` | `COALESCE` |
| `SYSDATE`, `SYSTIMESTAMP` | 시각은 `LOCALTIMESTAMP`, 날짜 기준은 `CURRENT_DATE` |
| 날짜 ± 일수 | `INTERVAL '1 day'` 등의 interval 연산 |
| 채용공고 `MERGE` | 외부 공고 ID의 `INSERT ... ON CONFLICT ... DO UPDATE` |
| `ROWNUM` | 정렬된 결과에 `LIMIT` 적용 |
| `NUMBER(18)` / `NUMBER(9)` | `bigint` / `integer` |
| 작은 상태값·개수 / 소수 점수 | `smallint` / `numeric(5,2)` |
| `VARCHAR2(n CHAR)` | `varchar(n)` |
| `CLOB` / `BLOB` | `text` / `bytea` |
| CLOB·BLOB JDBC 바인딩 | `LONGVARCHAR`·`VARBINARY` (대형 객체 OID 사용 안 함) |

생년월일·입학일·졸업일·입사일·퇴사일·발급일은 `date`다. 생성·수정·만료·면접 시각 등은 `timestamp without time zone`으로 변환해 시간 부분을 보존한다. DB 기본 시간대와 Hikari 연결 시간대는 `Asia/Seoul`로 지정했다. 기존 한국 시간 JSON 날짜 표시 형식을 유지한다.

JSON 보관본은 원문 문자열을 유지하는 `text`와 `IS JSON` CHECK를 사용한다. PDF는 `bytea`로 보관해 기존 `byte[]` DTO·JDBC `getBytes`로 읽는다. 문서 제목 200자, 면접 질문 120자·답변 1,000자·피드백 250자, 게시글 본문 UTF-8 4,000바이트 제한을 유지한다. 게시글 본문에는 `octet_length` CHECK도 적용했다. 다른 `varchar(n)` 길이는 PostgreSQL의 문자 단위다.

Oracle의 빈 문자열→NULL 처리에 의존하던 선택 입력과 외래키는 `EmptyStringAsNullTypeHandler`가 보존한다. 공백 문자로만 이루어진 문자열을 임의로 잘라내지는 않는다. DTO·요청·응답 API 구조와 데이터 접근 인터페이스는 유지했다.

활성 회원 닉네임만 검사하는 부분 UNIQUE 인덱스, 첨삭 요청 ID 재사용, FK 삭제 전파·대표이력서 삭제 시 NULL 전환을 구현했다. 고정 카테고리 ID 초기화 후 시퀀스를 4로 맞춰 다음 ID가 5부터 생성되게 했다.

PostgreSQL SQLSTATE `23505`(중복), `23503`(외래키), `22001`(문자열 초과)로 기존 오류 분기를 전환했다. 스크랩 중복 INSERT는 `ON CONFLICT DO NOTHING`으로 처리해 충돌 후에도 PostgreSQL 트랜잭션이 정상적으로 이어지게 했다. 첨삭 예약 충돌은 기존처럼 트랜잭션을 롤백한 뒤 새 트랜잭션에서 저장된 결과를 재조회한다.

## 설정과 실행

현재 머신의 DB 초기화는 완료되어 있다. 접속 설정 예시는 `backend/src/main/resources/database.properties.example`에 있다. 새 머신에서는 이를 `database.properties`로 복사한 뒤 본인의 앱 비밀번호를 넣는다.

```properties
db.driverClassName=org.postgresql.Driver
db.url=jdbc:postgresql://127.0.0.1:5432/provit_new
db.username=provit
db.password=CHANGE_ME
```

기존 `api.properties`·JWT·메일·파일 저장 설정은 그대로 사용한다. 실행 디렉터리의 외부 `config/database.properties`가 있다면 리소스 설정보다 우선하므로 같은 PostgreSQL 연결을 지정한다.

JDK 17의 `JAVA_HOME`을 설정하고 `backend` 폴더에서 실행한다.

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

기존처럼 서버 기동 시 채용공고 자동 수집·예약 작업이 실행된다. 기준정보만 있는 상태에서 기동·조회만 확인하려면 다음과 같이 실행한다.

```powershell
.\gradlew.bat bootRun --args="--provit.scheduling.enabled=false"
```

실행 파일은 `build/libs/backend.jar`다. PostgreSQL 서버는 Windows 서비스로 계속 사용하며 별도의 테스트 서버는 필요하지 않다.

## 다른 빈 DB 초기화

이 절은 아직 초기화하지 않은 새 PostgreSQL DB에만 적용한다. 새 역할·DB는 관리자로 만들고 DB 이름·비밀번호는 환경에 맞게 정한다.

```sql
CREATE ROLE provit LOGIN PASSWORD 'CHANGE_ME' NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE DATABASE provit_new OWNER provit ENCODING 'UTF8' TEMPLATE template0;
ALTER DATABASE provit_new SET timezone TO 'Asia/Seoul';
```

`backend` 폴더에서 앱 계정으로 스키마와 기준정보를 한 트랜잭션으로 적용한다. 비밀번호는 psql의 입력 요청에서 넣는다.

```powershell
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h 127.0.0.1 -p 5432 -U provit -d provit_new -v ON_ERROR_STOP=1 --single-transaction -f src/main/resources/sql_query/schema.sql -f src/main/resources/sql_query/seed_saramin_jobs.sql
```

`schema.sql`은 새 DB용이며 기존 테이블에 다시 적용하지 않는다. 직군·직무 시드는 같은 코드의 이름·소속을 갱신하도록 작성해 반복 적용할 수 있다. `drop.sql`은 수동 초기화 자료로만 제공하며 이번 전환에서 기존 DB에 실행하지 않았다. Boot의 SQL 자동 초기화는 계속 꺼져 있다.

이전 Oracle 증분 스크립트는 `docs/legacy/oracle/`로 옮겨 현재 실행 리소스에서 제외했다. PostgreSQL에 실행하지 않는다. 최신 컬럼·제약조건은 통합 스키마에 포함되어 있으므로 별도 Oracle 업그레이드 스크립트는 필요하지 않다. 실제 생성된 203개 컬럼의 타입·기본값·제약조건은 [테이블 명세](../TABLE_SPECIFICATION.md)에 정리했다.

## 테스트

```powershell
# 외부 DB/API가 없어도 실행하는 기존 테스트와 Boot HTTP 테스트
.\gradlew.bat test

# PostgreSQL 실제 연결을 포함하는 전체 테스트
.\gradlew.bat "-Dprovit.postgres.tests=true" test

# DB 검사만 선택
.\gradlew.bat "-Dprovit.postgres.tests=true" test --tests "*Postgres*Test" --rerun-tasks
```

DB 검사는 앱 접속 설정을 읽지만 각 실행마다 `provit_test_<UUID>` 스키마를 만들어 격리하며, 종료 시 해당 스키마만 제거한다. 테스트 URL을 별도로 지정하려면 `provit.postgres.url`, `provit.postgres.username`, `provit.postgres.password` 시스템 속성을 사용한다. `currentSchema`는 테스트가 자체 관리하므로 별도로 지정하지 않는다.

기존 Oracle 전용 테스트를 PostgreSQL 검사로 전환했다. Oracle 증분 DDL의 바이트→문자 정규화 검사는 새 스키마의 실제 길이 경계 검사로 대체했다. 실제 외부 사주 API는 기존 `externalApiTest`로 분리되어 있고, 실제 유료 OpenAI 요청은 별도 옵션을 켜야 한다.

## 검증 결과

- PostgreSQL 옵션을 켠 전체 빌드 성공: 테스트 191개 중 190개 통과, 실제 유료 OpenAI 1개 건너뜀, 실패 0개.
- 기본 `clean build`도 성공: DB 옵션이 필요한 28개를 건너뛰고 163개 통과, 실패 0개.
- 실제 PostgreSQL 검사 27개 통과: 첨삭·PDF·JSON 저장, 실패 기록·부분 결과 롤백·동시 요청 재사용, 대표이력서, 회원·닉네임, 기준정보 반복 적용·시퀀스, 긴 원문·문서 제목·게시글 바이트·면접 길이 경계, 게시판·댓글·좋아요·신고, 채용 upsert·스크랩·정렬·만료 처리, 스터디, 토큰 만료·회원 정지.
- 최종 JAR에 pgJDBC 42.7.14와 매퍼 XML 19개 포함, Oracle JDBC 및 부속 라이브러리 제외 확인.
- 최종 JAR를 새 DB에 직접 연결해 health의 실제 DB 조회, 직군 21개, IT 직무 260개, 비어 있는 게시판·스터디·채용 목록의 HTTP 200, 비로그인 보호 경로의 HTTP 401 확인.
- 앱 기동 검증 중 자동 수집·예약 작업을 비활성화했고 임시 앱 서버를 종료했다. PostgreSQL 5432 서버는 실행 상태로 유지했다. 별도로 만들었던 테스트용 55432 서버는 종료했다.
- 기존 Oracle 데이터 이관, 실제 외부 API 호출, 화면 전체 수동 점검, 운영 배포는 수행하지 않았다.

공식 참고: [PostgreSQL 타입](https://www.postgresql.org/docs/current/datatype.html), [시퀀스](https://www.postgresql.org/docs/current/functions-sequence.html), [Oracle과 PostgreSQL의 차이](https://wiki.postgresql.org/wiki/Oracle_to_Postgres_Conversion), [pgJDBC](https://jdbc.postgresql.org/download/).
