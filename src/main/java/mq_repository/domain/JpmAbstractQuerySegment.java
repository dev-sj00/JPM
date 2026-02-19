package mq_repository.domain;

import dsl_variable.v2.MField;
import mq_mapper.domain.vo.DslStatement;

public abstract class JpmAbstractQuerySegment<ReturnType> {

    /**
     * 사용자가 target.id 처럼 필드에 접근할 수 있게 해주는 더미 객체입니다.
     */


    public static class AliasedField<E, R> {

        public final String tableAlias;   // u1
        public final MFieldRef<E, R> fieldRef;
        public final String selectAlias;  // u1_order

        public AliasedField(String tableAlias,
                            MFieldRef<E, R> fieldRef,
                            String selectAlias) {
            this.tableAlias = tableAlias;
            this.fieldRef = fieldRef;
            this.selectAlias = selectAlias;
        }

        public AliasedField<E, R> as(String selectAlias) {
            return new AliasedField<>(this.tableAlias, this.fieldRef, selectAlias);
        }
    }

    // 2. 사용자가 호출할 col 메서드 (정적 메서드로 선언하여 어디서든 사용)
    public static <E, R> AliasedField<E, R> col(String alias, MFieldRef<E, R> fieldRef) {
        return new AliasedField<>(alias, fieldRef, null);
    }


    // ★ 1. 메서드 참조(::)를 받기 위한 함수형 인터페이스 정의
    @FunctionalInterface
    public interface MFieldRef<E, R> {
        R apply(E entity);
    }



    // =======================================================
    // --- Selectable 구현 ---
    // =======================================================
    public void select(String... cols) {}

    // [수정] 조인된 다른 엔티티의 컬럼도 select 할 수 있도록 <?> 로 변경

    @SafeVarargs
    public final <E, R> void select(MFieldRef<E, R>... fieldRefs) {}


    public final  void select(AliasedField<?, ?>... aliasedFields) {
        // 파서에서 aliasedFields를 순회하며 "alias:MEntity::getMethod" 형태로 저장
    }

    public void selectRaw(String... rawSqls) {}
    public void from(Class<?> entityClass) {}
    public void from(String table) {}
    public void from(Class<?> entityClass, String alias) {}

    // =======================================================
    // --- Conditional 구현 ---
    // =======================================================


    public void where(String cond) {}
    public void whereExistsGroup() {}
    public void whereNotExistsGroup() {}




    public <E1, R1, E2, R2> void where(MFieldRef<E1, R1> leftField, String op, MFieldRef<E2, R2> rightField) {}
    public <E1, R1, E2, R2> void and(MFieldRef<E1, R1> leftField, String op, MFieldRef<E2, R2> rightField) {}
    public <E1, R1, E2, R2> void or(MFieldRef<E1, R1> leftField, String op, MFieldRef<E2, R2> rightField) {}


    // [수정] 모든 엔티티에 대해 조건절을 걸 수 있도록 제네릭 <E> 사용
    public <E, R> void where(MFieldRef<E, R> fieldRef, String op, Object value) {}
    public <E, R> void and(MFieldRef<E, R> fieldRef, String op, Object value) {}
    public <E, R> void or(MFieldRef<E, R> fieldRef, String op, Object value) {}



    public void where(AliasedField<?, ?> col, String op, Object value) {}
    public void and(AliasedField<?, ?> col, String op, Object value) {}
    public void or(AliasedField<?, ?> col, String op, Object value) {}


    public void andGroup() {}
    public void orGroup() {}
    public void endGroup() {}
    public void group() {}



    public <E, R> void whereInGroup(MFieldRef<E, R> fieldRef) {}


    @Deprecated
    public void fromGroup(Class<?> targetTable) {}


    // [수정] 양쪽 모두 메서드 참조 지원 (N중 조인을 위해 <L, R> 분리)
    public <R, LF, LFR, RF, RFR> void innerJoin(Class<R> targetTable, MFieldRef<LF, LFR> left, MFieldRef<RF, RFR> right) {}
    public <R, LF, LFR, RF, RFR> void leftJoin(Class<R> targetTable, MFieldRef<LF, LFR> left, MFieldRef<RF, RFR> right) {}


    public <L, R, C> void innerJoin(Class<R> targetTable, AliasedField<?, ?> left, AliasedField<?, ?> right) {}
    public <L, R, C> void leftJoin(Class<R> targetTable, AliasedField<?, ?> left, AliasedField<?, ?> right) {}


    public <R, LF, LFR, RF, RFR> void innerJoinGroup(Class<R> targetTable, MFieldRef<LF, LFR> left, MFieldRef<RF, RFR> right) {}
    public <R, LF, LFR, RF, RFR> void leftJoinGroup(Class<R> targetTable, MFieldRef<LF, LFR> left, MFieldRef<RF, RFR> right) {}
    public <L, R, C> void innerJoinGroup(Class<R> targetTable, AliasedField<?, ?> left, AliasedField<?, ?> right) {}
    public <L, R, C> void leftJoinGroup(Class<R> targetTable, AliasedField<?, ?> left, AliasedField<?, ?> right) {}







    // =======================================================
    // --- Modifiable 구현 ---
    // =======================================================
    public void insertInto(Class<?> entityClass) {}
    public void update(Class<?> entityClass) {}
    public void deleteFrom(Class<?> entityClass) {}


    // [수정] UPDATE, INSERT는 대상 테이블(T)에 종속적이지만 확장성을 고려해 <E>로 엽니다.
    public <E, R> void value(MFieldRef<E, R> fieldRef, Object value) {}
    public <E, R> void set(MFieldRef<E, R> fieldRef, Object value) {}

    public void set(AliasedField<?, ?> col, Object value) {}

    public <E, R> void setRaw(MFieldRef<E, R> fieldRef, String rawSql) {}


    // 1. GroupBy 세트
    @SafeVarargs
    public final <E, R> void groupBy(MFieldRef<E, R>... fieldRefs) {
        // 파서가 fieldRefs를 분석해 "Table.Column" 리스트로 변환하여 DslStatement 생성
    }

    protected void groupByRaw(String... rawSqls) {
        // 생쿼리 문자열 그대로 DslStatement 생성
    }

    // 2. OrderBy 세트
    protected <E, R> void orderBy(MFieldRef<E, R> fieldRef, String direction) {
        // direction은 "ASC" 또는 "DESC"
    }


    public final void groupBy(AliasedField<?, ?>... aliasedFields) { }

    public void orderBy(AliasedField<?, ?> col, String direction) {  }


    protected void orderByRaw(String... rawSqls) {
        // 예: orderByRaw("ISNULL(status) ASC", "id DESC")
    }

    protected void limit(int count) {}
    protected void offset(int start) {}
    protected void sql(String rawSql) {}




    public void mapTarget(Class<?> targetClass) {}


    public <E, R> void mapId(MFieldRef<E, R> fieldRef, String dbColumn) {}

    /**
     * [result] 일반 필드 매핑 (컬럼명과 필드명이 다를 때 명시적으로 사용)
     * 예: mapResult(MEntity3::getName, "user_name")
     */
    public <E, R> void mapResult(MFieldRef<E, R> fieldRef, String dbColumn) {}


    public <E, R> void mapJoin(MFieldRef<E, R> parentField, String alias) {}







    // 문자열 기반 간단한 CASE 문
    public void selectCase(String condition, Object thenValue, Object elseValue, String alias) {}

    // 메서드 참조(::)를 활용한 타입 세이프 CASE 문
    public <E, R> void selectCase(MFieldRef<E, R> fieldRef, String op, Object condValue, Object thenValue, Object elseValue, String alias) {}




    public <E, R> void having(MFieldRef<E, R> fieldRef, String op, Object value) {}
    public void havingRaw(String rawSql) {}

}