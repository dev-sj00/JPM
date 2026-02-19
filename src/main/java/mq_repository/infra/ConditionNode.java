package mq_repository.infra;


import mq_repository.domain.SqlNode;



import mq_mapper.infra.EntityMetaRegistry;
import mq_mapper.infra.SqlMapperBinder;

import utils.EntityMeta;



public class ConditionNode implements SqlNode {
    private final String column;   // Java 필드명 또는 "별칭.필드명"
    private final String operator; // =, !=, LIKE, IN 등
    private final Object value;    // 비교할 값 (String, Number, 또는 ?)
    private final String logicOperator;

    public ConditionNode(String logicOperator, String column, String operator, Object value) {
        this.logicOperator = logicOperator;
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    public String toSql(SqlMapperBinder.BuildContext ctx) {
        String resolvedColumn = resolveColumn(this.column, ctx);

        // 🚀 value가 컬럼 참조("Entity::getField" 형태)면 컬럼명으로 변환
        String formattedValue;
        String valueStr = this.value.toString();
        if (valueStr.contains("::")) {
            formattedValue = resolveColumn(
                    resolveArgToColumn(valueStr, ctx), ctx
            );
        } else {
            formattedValue = formatValue(this.value, this.column, ctx);
        }

        return resolvedColumn + " " + this.operator + " " + formattedValue;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        // WhereClauseNode가 이 노드의 toSql()과 logicOperator를 사용해 조립할 것입니다.
    }

    // getter 추가 (WhereClauseNode에서 AND/OR 판단용)
    public String getLogicOperator() {
        return logicOperator;
    }

    // -------------------------------------------------------------------------
    // 내부 헬퍼 메서드 (JoinNode의 로직과 유사)
    // -------------------------------------------------------------------------

    private String resolveColumn(String colStr, SqlMapperBinder.BuildContext ctx) {
        String targetCol = colStr;

        // 1. 접두어가 없고(BareName) 테이블 접두어가 필요한 경우 자동 추가
        if (!targetCol.contains(".") && ctx.requiresPrefix) {
            targetCol = ctx.tablePrefix + "." + targetCol;
        }

        // 2. "별칭.필드명" 형태인 경우 실제 DB 컬럼명으로 치환
        if (targetCol.contains(".")) {
            String[] parts = targetCol.split("\\.");
            String alias = parts[0];
            String fieldName = parts[1];

            String tableName = ctx.tableAliases.get(alias);
            if (tableName != null) {
                EntityMeta meta = EntityMetaRegistry.getEntityMeta(tableName);
                if (meta != null) {
                    String dbCol = meta.getColumn(fieldName);
                    if (dbCol != null) return alias + "." + dbCol;
                }
            }
        }
        return targetCol;
    }

    private String formatValue(Object val, String column, SqlMapperBinder.BuildContext ctx) {
        if (val == null) return "NULL";

        // 1. 이미 따옴표가 붙어있거나 바인딩 변수(?)인 경우 그대로 반환 (이중 방지)
        String s = val.toString();
        if (s.startsWith("'") && s.endsWith("'")) return s;
        if (s.equals("?")) return s;



        if ( s.contains("#{") ) {
            return s;
        }

        // 2. 컬럼의 실제 타입 정보(MFieldType) 확인
        String fieldType = getFieldType(column, ctx);

        // 🚀 [해결 포인트 1] BOOLEAN 타입이면 대문자로 변환하고 따옴표 없이 반환
        if ("BOOLEAN".equalsIgnoreCase(fieldType) ||
                "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
            return s.toUpperCase();
        }

        // 🚀 [해결 포인트 2] 숫자 타입(LONG, INTEGER 등) 처리
        if (fieldType != null) {
            switch (fieldType.toUpperCase()) {
                case "LONG": case "INTEGER": case "FLOAT": case "DOUBLE": case "FK":
                    return s.replace("L", "").replace("l", "");
            }
        }

        // 3. 타입 정보가 없더라도 순수 숫자인 경우 따옴표 생략
        if (s.matches("-?\\d+(\\.\\d+)?")) return s;
        if (s.matches("-?\\d+[Ll]")) return s.replaceAll("(?i)L", "");

        // 4. 그 외 나머지만 따옴표를 붙임
        return "'" + s + "'";
    }

    private String getFieldType(String column, SqlMapperBinder.BuildContext ctx) {
        // "users_info.id" → "id" 추출
        String fieldName = column.contains(".") ? column.split("\\.")[1] : column;
        String tableName = column.contains(".")
                ? ctx.tableAliases.getOrDefault(column.split("\\.")[0], column.split("\\.")[0])
                : ctx.tablePrefix;

        EntityMeta meta = EntityMetaRegistry.getEntityMeta(tableName);
        if (meta == null) meta = EntityMetaRegistry.getByTableName(tableName);
        if (meta != null) return meta.getFieldType(fieldName);
        return null;
    }



    private String resolveArgToColumn(String arg, SqlMapperBinder.BuildContext ctx) {
        String[] parts = arg.split("::");
        String entityName = parts[0].trim();
        String fieldName = extractFieldName(parts[1].trim());

        EntityMeta meta = EntityMetaRegistry.getEntityMeta(entityName);
        if (meta != null) {
            String dbCol = meta.getColumn(fieldName);
            String alias = ctx.tableAliases.getOrDefault(meta.getTableName(), meta.getTableName());
            return alias + "." + (dbCol != null ? dbCol : fieldName);
        }
        return fieldName;
    }

    private String extractFieldName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        return methodName;
    }
}
