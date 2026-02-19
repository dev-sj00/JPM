package mq_mapper.domain;

import dsl_variable.v2.MField;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class EntityMetadata {
    private final Class<?> entityClass;
    private final String tableName;
    // 필드 객체(MField) 자체를 키로 사용
    private final Map<MField, String> fieldColumnMap;

    public EntityMetadata(Class<?> entityClass, String tableName, Map<MField, String> fieldColumnMap) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.fieldColumnMap = Collections.unmodifiableMap(fieldColumnMap);
    }

    public Class<?> getEntityClass() { return entityClass; }
    public String getTableName() { return tableName; }
    public String getColumn(MField field) { return fieldColumnMap.get(field); }
    public Set<MField> getFields() { return fieldColumnMap.keySet(); }
    public boolean hasField(MField field) { return fieldColumnMap.containsKey(field); }
}
