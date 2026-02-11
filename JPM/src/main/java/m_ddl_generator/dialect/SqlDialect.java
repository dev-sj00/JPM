package m_ddl_generator.dialect;

import dsl_variable.v2.MFieldType;
import m_ddl_generator.model.ColumnMetadata;
import m_ddl_generator.model.TableMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface SqlDialect {
    String getField(MFieldType fieldType);

    List<String> createDropTableSql(TableMetadata table);
    String createTableDefinitionSql(TableMetadata table);
    List<String> createAlterTableSql(TableMetadata table, ColumnMetadata column, HashMap<String, List<String>> parentFieldTypes); // FK 등 추가용
    String createAddColumnIfNotExistsSql(TableMetadata table, ColumnMetadata col); //update policy
    default List<String> createIndexSql(TableMetadata table) {
        List<String> sqls = new ArrayList<>();

        for (ColumnMetadata col : table.getColumns()) {
            // 인덱스 or 유니크 설정이 있으면 생성
            if (col.isContainIndexed() || col.isContainUnique()) {
                String indexName = "idx_" + table.getTableName() + "_" + col.getName();

                // UNIQUE 키워드 처리 (뒤에 공백 포함)
                String uniquePart = col.isContainUnique() ? "UNIQUE " : "";

                // [핵심 수정] INDEX 뒤에 'IF NOT EXISTS' 추가
                // 문법: CREATE [UNIQUE] INDEX IF NOT EXISTS [인덱스명] ON [테이블명] ([컬럼명])
                String sql = String.format("CREATE %sINDEX IF NOT EXISTS %s ON %s (%s)",
                        uniquePart, indexName, table.getTableName(), col.getName());

                sqls.add(sql);
            }
        }
        return sqls;
    }
}