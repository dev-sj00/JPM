package utils;

import mq_mapper.infra.EntityMetaRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EntityMeta {
    private final String tableName;


    // Key: 자바 변수명(level), Value: DB 컬럼명(user_level)
    private final Map<String, String> fieldToColumn = new HashMap<>();
    private final Map<String, String> fieldToType = new HashMap<>();
    // Key: 자바 변수명(orders), Value: 타겟 엔티티 클래스명(OrderEntity)
    private final Map<String, String> relationTargets = new HashMap<>();

    // 🚀 [수정] 생성자에서 Class<?> 정보도 함께 받도록 변경
    public EntityMeta(String tableName) {
        this.tableName = tableName;

    }




    public void addTypeMapping(String fieldName, String typeName) { // 🚀 추가
        this.fieldToType.put(fieldName, typeName);
    }

    public void addMapping(String fieldName, String columnName) {
        this.fieldToColumn.put(fieldName, columnName);
    }

    public void addRelation(String fieldName, String targetClassName) {
        this.relationTargets.put(fieldName, targetClassName);
    }

    public String getTableName() { return tableName; }

    public String getColumn(String fieldName) {
        return fieldToColumn.get(fieldName);
    }


    public String getFieldType(String fieldName) { // 🚀 추가
        return fieldToType.get(fieldName);
    }



    public EntityMeta getRelationTargetMeta(String fieldName) {
        String targetClassName = relationTargets.get(fieldName);
        if (targetClassName != null) {
            return EntityMetaRegistry.getEntityMeta(targetClassName);
        }
        return null;
    }

    public Collection<String> getAllColumnNames() {
        return fieldToColumn.values();
    }
}