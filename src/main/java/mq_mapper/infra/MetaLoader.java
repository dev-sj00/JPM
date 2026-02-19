package mq_mapper.infra;

import dsl_variable.v2.MField;
import mq_mapper.domain.EntityMetadata;
import utils.MParserUtils; // 이전에 만든 엔티티 파서

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Deprecated
public class MetaLoader {

    public static void loadEntity(MemoryEntityMetaProvider provider, Class<?> clazz, String filePath) throws Exception {
        // 1. MParserUtils를 사용하여 파일 분석
        List<List<MParserUtils.Pair>> rawData = MParserUtils.execute(filePath);

        Map<MField, String> fieldColumnMap = new HashMap<>();
        String tableName = clazz.getSimpleName().toLowerCase(); // 기본값: 클래스명 (추후 @MEntity 파싱 필요)

        // 2. 파싱된 결과를 순회하며 MField 인스턴스와 DB 컬럼명 매핑
        for (List<MParserUtils.Pair> columnInfo : rawData) {
            String fieldName = "";
            String dbColumnName = "";

            for (MParserUtils.Pair pair : columnInfo) {
                if ("fieldName".equals(pair.key)) fieldName = pair.value;
                if ("name".equals(pair.key)) dbColumnName = pair.value;
            }

            // DB 컬럼명이 명시되지 않았다면 필드명을 사용
            if (dbColumnName.isEmpty()) dbColumnName = fieldName;

            // 실제 MField 객체 생성 (엔티티 내 필드와 동일한 이름을 갖도록)
            MField fieldInstance = MField.builder().name(fieldName).build();
            fieldColumnMap.put(fieldInstance, dbColumnName);
        }

        // 3. Provider에 등록
        EntityMetadata metadata = new EntityMetadata(clazz, tableName, fieldColumnMap);
        provider.register(metadata);

        System.out.println("✅ 엔티티 로드 완료: " + clazz.getSimpleName() + " -> [필드수: " + fieldColumnMap.size() + "]");
    }
}