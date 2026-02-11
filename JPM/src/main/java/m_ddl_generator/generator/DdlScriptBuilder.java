package m_ddl_generator.generator;

import auto_ddl.AutoDDLPolicy;
import config.AppConfig;
import m_ddl_generator.dialect.SqlDialect;

import m_ddl_generator.model.ColumnMetadata;
import m_ddl_generator.model.TableMetadata;
import utils.LogPrinter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DdlScriptBuilder {

    private final String policy;

    private final SqlDialect dialect = AppConfig.getSqlDialectImpl();
    // 생성자: Dialect와 옵션을 받아서 초기화

    public DdlScriptBuilder(Map<String, String> options) {

        this.policy = options.getOrDefault("auto", "NONE");

    }

    /**
     * [Public] 전체 DDL 스크립트 생성 (Main Method)
     */
    public String build(List<TableMetadata> tables) {

        StringBuilder sb = new StringBuilder();

        sb.append("<![CDATA[\n");

        // 1. 테이블 삭제 및 생성 (DROP & CREATE)
        buildDropAndCreate(sb, tables);



        // 2. 컬럼 추가 (ALTER - UPDATE 정책일 때만)
        buildAddColumns(sb, tables);

        // 3. 외래키 제약조건 (FK)
        new ForeignKeyGenerator(dialect, tables).generate(sb);
      /*  buildForeignKeys(sb, tables);*/

        // 4. 인덱스 생성 (INDEX)
        buildIndexes(sb, tables);




        sb.append("]]>");

        return sb.toString();
    }

    // ---------------------------------------------------------
    // Private Helper Methods (로직 분리)
    // ---------------------------------------------------------

    /**
     * Step 1. 테이블 DROP 및 CREATE
     */


    private void buildDropAndCreate(StringBuilder sb, List<TableMetadata> tables) {
        sb.append("\t/* --- 1. TABLES (DROP & CREATE) --- */\n");

        boolean shouldDrop = isDropPolicy();

        for (TableMetadata table : tables) {
            // 1-1. DROP (정책이 맞을 경우)
            if (shouldDrop) {
                List<String> dropSqls = dialect.createDropTableSql(table);
                for (String sql : dropSqls) {
                    appendStatement(sb, sql);
                }
            }

            // 1-2. CREATE (IF NOT EXISTS 포함됨)
            String createSql = dialect.createTableDefinitionSql(table);
            appendStatement(sb, createSql);
            sb.append("\n");
        }
    }

    /**
     * Step 2. 컬럼 추가 (ALTER) - 테이블을 새로 만들지 않을 때만 실행
     */
    private void buildAddColumns(StringBuilder sb, List<TableMetadata> tables) {
        // DROP 정책이면 이미 CREATE에서 최신 컬럼이 만들어지므로 ALTER 불필요
        if (isDropPolicy()) {
            return;
        }

        sb.append("\t/* --- 2. UPDATE COLUMNS (If Not Exists) --- */\n");

        for (TableMetadata table : tables) {
            for (ColumnMetadata col : table.getColumns()) {
                // Dialect에게 위임 (PG: IF NOT EXISTS, MySQL: Procedure)
                String alterSql = dialect.createAddColumnIfNotExistsSql(table, col);

                // 유효한 SQL이 있을 경우에만 추가
                if (alterSql != null && !alterSql.trim().isEmpty()) {
                    appendStatement(sb, alterSql);
                }
            }
        }
        sb.append("\n");
    }

    /**
     * Step 3. 외래키 생성
     */

    private void buildForeignKeys(StringBuilder sb, List<TableMetadata> tables) {
        sb.append("\t/* --- 3. FOREIGN KEYS --- */\n");


        HashMap<String, List<String>> parentFieldTypes = new HashMap<>();

        for (TableMetadata table : tables) {

            //fk 부모 이름 정보들

            List<String> parentNames = table.getParentNames();

            
            /// parentName이 있을 경우 HashMap에 parent pk 필드 타입값을 넣음
            if( parentNames !=null)
            {
                addParentFieldTypes(parentNames, tables, parentFieldTypes);
            }

            for (ColumnMetadata col : table.getColumns()) {

                if (col.isForeignKey()) {

                    List<String> fkSqls = dialect.createAlterTableSql(table, col, parentFieldTypes);

                    for (String sql : fkSqls) {
                        appendStatement(sb, sql);

                    }
                }
            }
        }
        sb.append("\n");
    }

    private void addParentFieldTypes(List<String> parentNames, List<TableMetadata> tables, HashMap<String, List<String>> fieldTypeResult) {
        HashMap<String, TableMetadata> tableMap = (HashMap<String, TableMetadata>) tables.stream()
                .collect(Collectors.toMap(TableMetadata::getTableName
                        , t -> t));
        for(String parentName : parentNames) {
            tableMap.get(parentName).getColumns().forEach(col -> {

                fieldTypeResult.putIfAbsent(parentName, new ArrayList<>());

                if(col.isContainPrimaryKey()) {
                    fieldTypeResult.get(parentName).add(col.getType());
                }
            });
        }

    }

    /**
     * Step 4. 인덱스 생성
     */
    private void buildIndexes(StringBuilder sb, List<TableMetadata> tables) {
        sb.append("\t/* --- 4. INDEXES --- */\n");




        for (TableMetadata table : tables) {


            List<String> indexSqls = dialect.createIndexSql(table);
            for (String sql : indexSqls) {
                appendStatement(sb, sql);
            }
        }

    }




    // ---------------------------------------------------------
    // Utility Methods
    // ---------------------------------------------------------

    /**
     * SQL 문 뒤에 세미콜론(;)을 안전하게 붙여서 StringBuilder에 추가
     */
    private void appendStatement(StringBuilder sb, String sql) {
        if (sql == null || sql.trim().isEmpty()) return;

        sb.append("\t").append(sql.trim());

        // 끝에 ;가 없으면 붙여줌 (MySQL 프로시저 등은 이미 있을 수 있음)
        if (!sql.trim().endsWith(";")) {
            sb.append(";");
        }
        sb.append("\n");
    }

    /**
     * 현재 정책이 테이블 DROP을 포함하는지 확인
     */
    private boolean isDropPolicy() {
        return AutoDDLPolicy.DROP.name().equals(policy) ||
                AutoDDLPolicy.DROP_N_CREATE_EXE.name().equals(policy);
    }
}
