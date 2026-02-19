package mq_mapper.domain;


import dsl_variable.v2.MField;
import java.util.Optional;
import java.util.Set;

public interface EntityMetaProvider {

    /**
     * 클래스 정보를 바탕으로 등록된 엔티티인지 확인
     */
    boolean isRegistered(Class<?> entityClass);

    /**
     * 클래스 정보를 바탕으로 실제 DB 테이블명 조회
     */
    Optional<String> getTableName(Class<?> entityClass);

    /**
     * MField 객체 인스턴스를 직접 전달받아 매핑된 DB 컬럼명 조회
     * (사용자가 select(target.getId()) 처럼 던진 객체를 그대로 사용)
     */
    Optional<String> getColumnName(Class<?> entityClass, MField field);

    /**
     * 특정 엔티티의 모든 MField 객체 목록 조회 (검증용)
     */
    Set<MField> getAllFields(Class<?> entityClass);

    /**
     * 전체 메타데이터 묶음 조회
     */
    Optional<EntityMetadata> getMetadata(Class<?> entityClass);
}