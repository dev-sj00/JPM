package m_ddl_generator.dialect;

import dsl_variable.v2.MFieldType;
import m_ddl_generator.model.ColumnMetadata;
import m_ddl_generator.model.TableMetadata;
import utils.LogPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MySqlDialect implements SqlDialect {

    @Override
    public String getField(MFieldType fieldType) {


        if(fieldType.equals(MFieldType.JSON)) {
            return "JSON";
        }
        else if(fieldType.equals(MFieldType.UUID_V_7)) {

            return "CHAR(36)";
        }

        LogPrinter.error(fieldType.toString(), "", "FieldTypeError MysqlDialect class", null);
        return null;
    }

    @Override
    public List<String> createDropTableSql(TableMetadata table) {
        List<String> sqls = new ArrayList<>();
        // MySQL DROP (세미콜론 제거)
        sqls.add("DROP TABLE IF EXISTS " + table.getTableName());
        return sqls;
    }

    @Override
    public String createTableDefinitionSql(TableMetadata table) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(table.getTableName()).append(" (\n");

        List<String> pkColumns = new ArrayList<>();
        List<String> definitions = new ArrayList<>();

        for (ColumnMetadata col : table.getColumns()) {
            if (col.isForeignKey()) continue;

            if (col.isContainPrimaryKey()) pkColumns.add(col.getName());

            // [수정] 탭(\t)으로 변경
            definitions.add("\t" + buildColumnSql(col));
        }

        sb.append(String.join(",\n", definitions));

        if (!pkColumns.isEmpty()) {
            sb.append(",\n\tPRIMARY KEY (").append(String.join(", ", pkColumns)).append(")");
        }



        // [수정] 세미콜론 제거 & 엔진 설정 유지
        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        return sb.toString();
    }

    private String buildColumnSql(ColumnMetadata col) {
        StringBuilder sb = new StringBuilder();
        sb.append(col.getName()).append(" ").append(col.getType());

        if (!col.isContainNullable()) sb.append(" NOT NULL");

        if (col.isContainAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        } else if (col.getDefaultValue() != null && !col.getDefaultValue().isEmpty()) {
            sb.append(" ").append(col.getDefaultValue());
        }

        if(col.isContainUUIDV7())
        {
            sb.append(" CONSTRAINT "+col.getName()+"_v7_chk CHECK (SUBSTRING(id, 15, 1) = '7')");
        }

        return sb.toString();
    }

    @Override
    public List<String> createAlterTableSql(TableMetadata table, ColumnMetadata col, HashMap<String, List<String>> parentFieldTypes) {

        List<String> sql = new ArrayList<>();






        if (col.isForeignKey()) {
            String parentFieldType = parentFieldTypes.get(col.getFkTargetTable())
                    .stream()
                    .filter(type -> type != null && !type.isEmpty()
                    ).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("[MySqlDialect] Index Parent Field Type is NULL"));




            sql.add(String.format("ALTER TABLE %s ADD COLUMN %s %s", table.getTableName(), col.getName(), parentFieldType));
            String constraintName = "fk_" + table.getTableName() + "_" + col.getName();
            sql.add(String.format("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s) ON DELETE %s",
                    table.getTableName(), constraintName, col.getName(),
                    col.getFkTargetTable(), col.getFkTargetColumn(), col.getOnDeleteAction()));
        }
        return sql;
    }




    @Override
    public String createAddColumnIfNotExistsSql(TableMetadata table, ColumnMetadata col) {
        String tableName = table.getTableName();
        String colName = col.getName();
        // 프로시저 이름이 겹치지 않게 테이블_컬럼명으로 생성
        String procName = "AddCol_" + tableName + "_" + colName;
        // 실제 실행할 ALTER 문 (예: ALTER TABLE job ADD COLUMN good VARCHAR(255)...)
        String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + buildColumnSql(col);
        StringBuilder sb = new StringBuilder();
        // 1. 기존에 같은 이름의 프로시저가 있으면 삭제
        sb.append("DROP PROCEDURE IF EXISTS ").append(procName).append(";\n");
        // 2. 프로시저 정의 (DELIMITER 없이 MyBatis에서 실행 가능하도록 작성)
        sb.append("CREATE PROCEDURE ").append(procName).append("() BEGIN\n");
        //    3. IF NOT EXISTS 체크 (information_schema 활용)
        sb.append("\tIF NOT EXISTS (\n");
        sb.append("\t\tSELECT * FROM information_schema.COLUMNS\n");
        sb.append("\t\tWHERE TABLE_SCHEMA = DATABASE()\n");
        sb.append("\t\tAND TABLE_NAME = '").append(tableName).append("'\n");
        sb.append("\t\tAND COLUMN_NAME = '").append(colName).append("'\n");
        sb.append("\t) THEN\n");
        //    4. 없으면 ALTER 문 실행
        sb.append("\t\t").append(alterSql).append(";\n");
        sb.append("\tEND IF;\n");
        sb.append("END;\n");
        // 5. 프로시저 실행
        sb.append("CALL ").append(procName).append("();\n");
        // 6. 프로시저 삭제 (청소)
        sb.append("DROP PROCEDURE ").append(procName).append(";");

        return sb.toString();
    }




}