package com.provit.dao;

import com.provit.persistence.JpaStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;

/** 팀원 참고용 샘플 DAO (생성자 주입 방식) */
@Repository
public class TestDao {

    private final JpaStore store;

    // 생성자 주입 (Constructor Injection)
    @Autowired
    public TestDao(JpaStore store) {
        this.store = store;
    }

    /** 데이터베이스 현재 시각 조회 */
    public String selectCurrentTime() {
        try {
            return store.scalar(
                    "select to_char(localtimestamp,'YYYY-MM-DD HH24:MI:SS')",
                    String.class,
                    Map.of());
        } catch (Exception e) {
            return "DB 연결 대기 중 (미연결 시 기본값 반환)";
        }
    }
}
