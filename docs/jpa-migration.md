# MyBatis → JPA 전환

## 현재 저장 계층

Spring Boot의 JPA/Hibernate 자동 설정을 사용한다. 기존 DAO 인터페이스는 서비스·API와의 계약으로 유지하고 구현체를 `EntityManager` 기반으로 교체했다. 공통 저장소 `JpaStore`가 Entity CRUD·JPQL·DTO 프로젝션·잠금을 처리한다. MyBatis 의존성과 실행 매퍼 XML·설정·타입 핸들러는 제거했다. 이전 SQL은 `docs/legacy/mybatis/`에 비교 자료로 보관한다.

실행 JAR 기준 Spring Data JPA 4.1.2·Hibernate ORM 7.4.13.Final을 사용한다. 저장소 구현은 `EntityManager`를 사용하는 커스텀 JPA 저장소 방식이다.

`persistence/entity`의 Entity 27개가 기존 PostgreSQL 테이블 203개 컬럼에 대응한다. 기본키·복합키·시퀀스·숫자 정밀도·문자 길이·날짜·긴 원문·PDF 타입을 매핑했다. 외래키는 식별자 필드로 매핑하고 기존 DB의 제약조건과 삭제 전파 정책을 사용한다. DTO를 응답으로 반환하므로 Entity와 영속성 상태가 API로 노출되지 않는다.

| 영역 | 전환 내용 |
| --- | --- |
| 회원·토큰 | Entity 저장, JPQL 조회·부분 수정·토큰 버전 증가·만료 삭제 |
| 이력서·학력·경력·자격증·자소서·포트폴리오 | Entity CRUD, 소유자 조건, 날짜 변환, 문서 목록 DTO 프로젝션 |
| 첨삭 | Entity 저장·JPQL 완료/실패 전환·비관적 잠금, JSON·PDF 보관, 동시 요청 ID 재사용 |
| 면접 | 기존 세션 번호 사전 발급, 질문/답변·점수 Entity 저장, 평가 목록 조회 |
| 게시판·댓글·좋아요·신고 | Entity 저장·JPQL 조건부 수정, 기존 검색·페이징·집계 DTO 조회 |
| 채용공고·스크랩 | JPA 조회, PostgreSQL upsert·중복 스크랩·만료 정리 네이티브 쿼리 |
| 스터디 | Entity 저장·멤버 복합키, `PESSIMISTIC_WRITE` 잠금 후 참여 수 확인 |
| 파일 소유권 | `FileOwnershipDAO`의 JPA 조회로 서비스의 직접 SqlSession 의존 제거 |

복잡한 검색·집계·권한 투영과 PostgreSQL의 원자적 upsert는 JPA 네이티브 쿼리로 실행한다. 기본 CRUD·상태 변경·잠금은 Entity·JPQL을 사용한다. 이전 XML 쿼리를 런타임에 해석하는 계층은 없다.

## 스키마와 식별자

`spring.jpa.hibernate.ddl-auto=validate`로 기존 스키마를 검사한다. JPA가 테이블을 생성·변경·삭제하지 않는다. 신규 DB를 만들 때는 기존 PostgreSQL `schema.sql`과 기준정보 시드를 적용한다. 이번 JPA 전환에서 `public` 스키마의 구조와 업무 데이터는 변경하지 않았다.

일반 식별자는 기존 시퀀스와 `@SequenceGenerator(allocationSize=1)`로 발급한다. 포트폴리오·면접 기록은 파일 경로와 진행 중 세션에서 번호를 먼저 사용하는 기존 계약에 맞춰 사전 발급 후 지정 ID로 저장한다. 사전 시퀀스 발급은 읽기 전용이 아닌 쓰기 트랜잭션에서 실행한다. 복합키 테이블은 `@IdClass`로 매핑한다.

`text`와 `bytea`는 각각 String·byte[]로 매핑한다. PostgreSQL 대형 객체 OID를 사용하는 `@Lob`은 쓰지 않는다. 문서 상세의 PDF·원문 스냅샷은 저장 시 보관하지만 일반 첨삭 문서 목록 프로젝션에서는 로드하지 않는다.

`EmptyStringConverter`는 기존 빈 문자열→NULL 규칙을 보존한다. Entity의 `LocalDate`·`LocalDateTime`과 기존 DTO의 Date·날짜 문자열은 `DtoMapper`에서 한국 시간 기준으로 변환한다. 기존 JSON 형식은 유지한다.

## 트랜잭션과 동시성

`JpaTransactionManager`가 기존 서비스의 `@Transactional`과 `TransactionTemplate`을 처리한다. 기존 JDBC 전용 트랜잭션 매니저 Bean은 제거했다.

- DAO 쓰기 작업은 서비스 트랜잭션에 참여하며, 독립 호출도 쓰기 트랜잭션을 연다.
- 첨삭 예약·결과 저장·실패 기록은 기존처럼 짧은 트랜잭션으로 나뉜다. AI 호출은 트랜잭션 밖에서 실행한다.
- 첨삭 결과 저장과 스터디 가입은 JPA 비관적 쓰기 잠금을 사용한다.
- 중복 첨삭 요청은 예약 트랜잭션 롤백 후 새 영속성 컨텍스트에서 기존 결과를 읽는다. Hibernate의 중복 오류는 SQLSTATE `23505`로 구별한다.
- 벌크 수정·삭제 전에 보류 중인 변경을 flush하고 실행 후 영속성 컨텍스트를 clear해 오래된 상태를 다시 사용하지 않는다.
- 사용자 프로필·댓글·블라인드 게시글 등 조건부 수정은 기존 소유자·상태 조건을 포함한 JPQL로 처리한다. Entity 변경에는 `@DynamicUpdate`를 사용해 수정 대상 외 컬럼을 덮어쓰지 않는다.
- 등록 시 조회수·좋아요 수·삭제 플래그·토큰 버전·처리 상태·서버 날짜 등은 서버가 설정한다. 요청 DTO에 같은 필드가 있어도 이전 INSERT SQL이 허용하던 값만 저장한다.
- 같은 내용으로 문서를 다시 저장해도 수정 시각을 갱신한다.

## 조회와 성능

`spring.jpa.open-in-view=false`를 사용한다. 서비스가 반환한 DTO는 요청 종료 이후의 Entity 로딩에 의존하지 않는다.

게시판·채용·신고 검색과 스터디 목록은 필요한 조인·집계·페이징을 쿼리 안에서 처리한다. 문서 요약과 면접 문서 선택 목록은 ID·제목 등 필요한 컬럼만 조회한다. 별도의 Entity 그래프를 행마다 순회하지 않는다. 회귀 테스트는 게시판 2페이지의 실제 항목과 목록+총건수 SQL 2회, 이력서 요약 5개의 SQL 1회를 검증한다.

## 실행과 테스트

PostgreSQL 18의 기존 `provit_new` DB·`provit` 계정·로컬 설정을 사용한다. JDK 17의 `JAVA_HOME`을 지정한 뒤 `backend` 폴더에서 실행한다.

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

기동 시 수집·예약 작업을 끄고 확인하려면 아래 옵션을 사용한다.

```powershell
.\gradlew.bat bootRun --args="--provit.scheduling.enabled=false"
```

기본 빌드는 DB와 실제 외부 API 없이 기존 단위 테스트와 Boot HTTP 통합 테스트를 실행한다. JPA 실제 DB 검사는 이전과 같은 옵션을 쓴다.

```powershell
.\gradlew.bat "-Dprovit.postgres.tests=true" test
.\gradlew.bat "-Dprovit.postgres.tests=true" test --tests "*Postgres*Test" --rerun-tasks
```

검사는 `provit_test_<UUID>` 스키마에 실제 JPA EntityManagerFactory와 트랜잭션 매니저를 구성하고 종료 시 해당 스키마만 제거한다. 앱의 `public` 테이블은 테스트에 사용하지 않는다.

XML 모양·MyBatis 파라미터 핸들러를 확인하던 테스트는 제거하고, 실제 JPA DAO 회귀·스키마 검증·DTO 계약 검사로 대체했다. 기존 첨삭·대표이력서·동시 요청·실패 롤백 검증을 유지하고, 부분 수정·서버 소유 필드·벌크 수정의 flush/clear·SQL 수·면접 기록/결과의 원자적 롤백을 추가했다. 실제 유료 OpenAI 및 외부 사주 API는 명시적인 별도 옵션/작업에서만 호출한다.

## 검증 결과 (2026-11-05)

- JDK 17·Gradle Wrapper로 PostgreSQL 검사 옵션을 켠 `clean build` 성공: 190개 중 189개 통과, 실제 유료 OpenAI 1개 건너뜀, 실패 0개.
- 기본 빌드도 성공: 단위·Boot HTTP 검사 156개 통과, 실제 DB 옵션이 필요한 34개 건너뜀, 실패 0개.
- 스키마 검증을 켠 실제 JPA DAO 검사에서 기존 CRUD·권한·FK·동시 요청·실패 기록·JSON·PDF·길이 경계와 추가 JPA 회귀 조건을 확인했다.
- 최종 JAR에 Entity 클래스 27개와 Hibernate 포함, MyBatis 라이브러리·실행 매퍼 XML 제외 확인.
- JAR를 `provit_new`에 직접 연결해 스키마 검증과 EntityManagerFactory 초기화, 실제 DB health 조회, 직군 21개·IT 직무 260개, 빈 게시판·스터디·채용 목록 HTTP 200, 보호 경로 HTTP 401 확인.
- 기동 확인 중 자동 수집·예약 작업은 비활성화했고 임시 앱 서버를 종료했다. 기존 PostgreSQL 서버는 실행 상태로 유지했다.
- 실제 외부 API 호출, 화면 전체 수동 점검, 운영 배포는 수행하지 않았다.

공식 참고: [Spring Boot 데이터 접근](https://docs.spring.io/spring-boot/how-to/data-access.html), [JPA 조회·수정 쿼리와 영속성 컨텍스트](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).
