package mq_mapper.infra;

import dsl_variable.v2.MField;
import mq_mapper.domain.EntityMetadata;
import mq_mapper.domain.EntityMetaProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리 기반의 엔티티 메타데이터 저장소.
 * MParserUtils로부터 분석된 정보를 전달받아 보관하고, Mapper에게 조회를 제공합니다.
 */

@Deprecated
public class MemoryEntityMetaProvider implements EntityMetaProvider {

    // Class별로 Metadata를 관리하여 타입 안전성 확보
    private final Map<Class<?>, EntityMetadata> registry = new ConcurrentHashMap<>();

    /**
     * 분석 완료된 엔티티 데이터를 등록합니다.
     */
    public void register(EntityMetadata metadata) {
        if (metadata == null || metadata.getEntityClass() == null) {
            throw new IllegalArgumentException("유효하지 않은 메타데이터 등록 시도입니다.");
        }
        registry.put(metadata.getEntityClass(), metadata);
    }

    @Override
    public boolean isRegistered(Class<?> entityClass) {
        return entityClass != null && registry.containsKey(entityClass);
    }

    @Override
    public Optional<String> getTableName(Class<?> entityClass) {
        return Optional.ofNullable(registry.get(entityClass))
                .map(EntityMetadata::getTableName);
    }

    @Override
    public Optional<String> getColumnName(Class<?> entityClass, MField field) {
        return Optional.ofNullable(registry.get(entityClass))
                .map(meta -> meta.getColumn(field));
    }

    @Override
    public Set<MField> getAllFields(Class<?> entityClass) {
        return Optional.ofNullable(registry.get(entityClass))
                .map(EntityMetadata::getFields)
                .orElse(Collections.emptySet());
    }

    @Override
    public Optional<EntityMetadata> getMetadata(Class<?> entityClass) {
        return Optional.ofNullable(registry.get(entityClass));
    }

    /**
     * 등록된 모든 엔티티의 클래스 목록을 반환합니다.
     */
    public Set<Class<?>> getRegisteredClasses() {
        return registry.keySet();
    }
}