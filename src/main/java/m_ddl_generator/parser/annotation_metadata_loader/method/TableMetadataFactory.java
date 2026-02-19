package m_ddl_generator.parser.annotation_metadata_loader.method;

import config.AppConfig;
import dsl_variable.v2.MField;
import dsl_variable.v2.MFieldType;
import m_ddl_generator.dialect.SqlDialect;
import m_ddl_generator.model.ColumnMetadata;
import m_ddl_generator.model.TableMetadata;
import m_ddl_generator.parser.annotation_metadata_loader.dto.MEntityInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableMetadataFactory {

    // 🚨 [삭제] 이 정적 맵은 비어있어서 문제를 일으킵니다. 삭제합니다.
    // private static final Map<String, MEntityInfo> entityInfoMap = new HashMap<>();

    private static final SqlDialect dialect = AppConfig.getSqlDialectImpl();

    /**
     * 클래스 정보를 바탕으로 TableMetadata를 생성합니다.
     */
    public static TableMetadata create(List<MField> variables,
                                       MEntityInfo currentEntity,
                                       Map<String, MEntityInfo> globalEntityMap) { // 파라미터 이름 명확하게 변경
        if (variables == null || currentEntity == null) {
            return null;
        }

        List<ColumnMetadata> columns = new ArrayList<>();
        for (MField var : variables) {
            // 🚨 [수정] 전체 엔티티 맵(globalEntityMap)을 하위 메서드로 전달
            columns.add(buildColumnMetadata(var, globalEntityMap));
        }

        return new TableMetadata(currentEntity.getTableName(), columns);
    }

    /**
     * 개별 MField를 ColumnMetadata로 변환합니다.
     */
    // 🚨 [수정] 파라미터 추가 (Map<String, MEntityInfo> globalEntityMap)
    private static ColumnMetadata buildColumnMetadata(MField var, Map<String, MEntityInfo> globalEntityMap) {
        String type = resolveSqlType(var);
        String finalDefaultValue = resolveDefaultValue(var);
        boolean finalNullable = !var.isPrimaryKey() && var.isNullable();
        boolean isUUIDV7 = MFieldType.UUID_V_7.equals(var.getType());

        ColumnMetadata column = new ColumnMetadata.Builder(var.getName(), type)
                .primaryKey(var.isPrimaryKey())
                .autoIncrement(var.isAutoIncrement())
                .nullable(finalNullable)
                .defaultValue(finalDefaultValue)
                .indexed(var.isIndex())
                .unique(var.isUnique())
                .isUUIDV7(isUUIDV7)
                .build();

        // 🚨 [수정] 맵 전달
        applyForeignKey(column, var, globalEntityMap);

        return column;
    }

    /**
     * SQL 타입 결정 로직 (VARCHAR 길이 처리 등)
     */
    private static String resolveSqlType(MField var) {
        String type = MFieldToSqlType.resolveType(var);
        if (var.getType() == MFieldType.STRING) {
            return "VARCHAR(" + var.getLength() + ")";
        }

        if(var.getType() == MFieldType.JSON) {
            return dialect.getField(MFieldType.JSON);
        }

        if(var.getType() == MFieldType.UUID_V_7) {
            return dialect.getField(MFieldType.UUID_V_7);
        }

        return type;
    }

    /**
     * 기본값(Default Value) 처리 로직
     */
    private static String resolveDefaultValue(MField var) {
        if (var.isPrimaryKey() && var.isAutoIncrement()) {
            return null;
        }

        String rawDefault = var.getDefaultValue();
        if (rawDefault == null) {
            return null;
        }

        if (!rawDefault.trim().toUpperCase().startsWith("DEFAULT")) {
            return "DEFAULT " + rawDefault;
        }
        return rawDefault;
    }

    /**
     * 외래 키(FK) 설정 로직
     */
    // 🚨 [수정] 파라미터로 받은 맵을 사용하여 조회하도록 변경
    private static void applyForeignKey(ColumnMetadata column, MField var, Map<String, MEntityInfo> globalEntityMap) {
        if (var.getType() == MFieldType.FK) {
            String targetClassName = var.getParentClassName();

            // 기존의 빈 static map 대신 인자로 넘어온 map 사용
            MEntityInfo targetInfo = globalEntityMap.get(targetClassName);

            if (targetInfo != null) {
                column.setForeignKey(
                        targetInfo.getTableName(),
                        targetInfo.getPkColumnName(),
                        var.getOnDelete()
                );
            }
        }
    }
}