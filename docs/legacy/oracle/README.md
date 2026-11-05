# Oracle 시절의 증분 변경 기록

이 폴더의 SQL은 과거 Oracle DB 변경 이력을 참고하기 위한 자료다. 현재 PostgreSQL에는 실행하지 않는다.

현재 프로젝트는 기존 업무 데이터를 이관하지 않고 새 PostgreSQL DB에 테이블과 기준정보만 구성했다. 신규 초기화는 `backend/src/main/resources/sql_query/schema.sql`과 `seed_saramin_jobs.sql`을 사용한다. [현재 DB 안내](../../postgresql-migration.md)를 참고한다.
