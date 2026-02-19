package mq_mapper.infra;

import utils.EntityMeta;
import utils.MParserUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityMetaRegistry {
    // Key: EntityClassName (예: MEntity3), Value: FieldMetadata Map (Key: Java 필드명, Value: DB 컬럼명)
    private static final Map<String, Map<String, String>> registry = new HashMap<>();


    private static final Map<String, Class<?>> classMap = new HashMap<>();
    // 클래스명 -> 테이블명 매핑 (선택 사항, 필요시 사용)
    private static final Map<String, String> tableRegistry = new HashMap<>();



    private static final Map<String, String> fieldToType = new HashMap<>(); // MFieldType은 String으로 저장


    public static String getFieldType(String fieldName) {
        return fieldToType.get(fieldName);
    }


    // MParserUtils의 결과를 Registry에 등록
    public static void register(String entityName, List<List<MParserUtils.Pair>> rawMeta) {
        Map<String, String> fieldToColumn = new HashMap<>();

        for (List<MParserUtils.Pair> fieldInfo : rawMeta) {
            String fieldName = null;  // Java 변수명 (예: level, isActive)
            String columnName = null; // DB 컬럼명 (예: user_level, is_active)
            String typeName = null;
            for (MParserUtils.Pair pair : fieldInfo) {
                // 1. 자바 변수명 (MParserUtils가 "fieldName"으로 넘겨준다고 가정)
                if ("fieldName".equals(pair.key)) {
                    fieldName = pair.value;
                }

                // 2. DB 컬럼명 (MField.builder().name("...") 또는 .column("...") 형태 모두 지원)
                if ("name".equals(pair.key) || "column".equals(pair.key)) {
                    columnName = pair.value.replace("\"", "").replace("'", "");
                }

                if ("type".equals(pair.key)) typeName = pair.value;
            }

            // 만약 .name() 이나 .column() 지정이 없으면 자바 필드명을 그대로 DB 컬럼명으로 사용
            if (columnName == null || columnName.trim().isEmpty()) {
                columnName = fieldName;
            }

            if (fieldName != null) {
                fieldToColumn.put(fieldName, columnName);
                if (typeName != null) fieldToType.put(fieldName, typeName);
            }



        }
        registry.put(entityName, fieldToColumn);
    }

    // Java 필드명으로 DB 컬럼명 조회
    public static String getColumn(String entityName, String fieldName) {
        if (!registry.containsKey(entityName)) {
            return fieldName; // 등록되지 않은 엔티티면 필드명 그대로 반환
        }
        return registry.get(entityName).getOrDefault(fieldName, fieldName);
    }



    public static EntityMeta getByTableName(String tableName) {
        // tableRegistry에서 테이블명으로 엔티티명 역조회
        for (Map.Entry<String, String> entry : tableRegistry.entrySet()) {
            if (entry.getValue().equals(tableName)) {
                return getEntityMeta(entry.getKey()); // 엔티티명으로 EntityMeta 생성
            }
        }
        return null;
    }


    // --- (옵션) 테이블명 관련 유틸 ---
    public static void registerTable(String entityName, String tableName) {
        tableRegistry.put(entityName, tableName.replace("\"", "").replace("'", ""));
    }

    public static String getTable(String entityName) {
        return tableRegistry.getOrDefault(entityName, entityName);
    }



    public static void registerEntity(Class<?> entityClass) {
        // entityClass.getSimpleName() 은 "UserEntity" 와 같은 짧은 이름을 반환합니다.
        classMap.put(entityClass.getSimpleName(), entityClass);

        // EntityMeta 객체 생성 및 저장 로직도 여기에 함께 구현...
    }
    public static Class<?> getEntityClass(String entityName) {
        return classMap.get(entityName);
    }



    public static utils.EntityMeta getEntityMeta(String entityName) {
        // 1. 레지스트리에 해당 엔티티 정보가 없으면 null 반환
        if (!registry.containsKey(entityName)) {
            return null;
        }

        // 2. 테이블명을 가져와서 EntityMeta 객체 생성
        String tableName = getTable(entityName);
        utils.EntityMeta meta = new utils.EntityMeta(tableName);

        // 3. 레지스트리에 저장된 필드-컬럼 매핑 정보를 EntityMeta 객체에 모두 주입
        Map<String, String> fieldMap = registry.get(entityName);
        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            meta.addMapping(entry.getKey(), entry.getValue());
        }

        return meta;
    }

}