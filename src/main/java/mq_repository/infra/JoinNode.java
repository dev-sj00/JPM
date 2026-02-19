package mq_repository.infra;

import mq_mapper.infra.EntityMetaRegistry;
import mq_mapper.infra.SqlMapperBinder;

import mq_repository.domain.SqlNode;
import utils.EntityMeta;

import java.util.List;

public class JoinNode implements SqlNode {
    private final String joinType; // "INNER JOIN" 또는 "LEFT JOIN"
    private final String rawClass;
    private final String leftCol;
    private final String rightCol;

    // 1. 생성자에서는 넘어온 값들을 멤버 변수로 세팅만 해줍니다.
    public JoinNode(String cmd, List<String> args) {
        this.joinType = "leftJoin".equals(cmd) ? "LEFT JOIN" : "INNER JOIN";
        this.rawClass = args.get(0);
        this.leftCol = args.get(1);
        this.rightCol = args.get(2);
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        String cleanedClass = cleanClassName(this.rawClass);
        EntityMeta meta = EntityMetaRegistry.getEntityMeta(cleanedClass);
        String actualTable = (meta != null) ? meta.getTableName() : cleanedClass;

        String joinAlias = ctx.tableAliases.getOrDefault(actualTable, actualTable);

        // 🚀 leftCol에 접두어가 없으면 메인 테이블(tablePrefix) 접두어 추가
        String leftColWithPrefix = this.leftCol;
        if (!this.leftCol.contains(".")) {
            leftColWithPrefix = ctx.tablePrefix + "." + this.leftCol;
        }

        String resolvedLeftCol = forceResolveColumn(leftColWithPrefix, ctx);
        String resolvedRightCol = forceResolveColumn(correctRightCol(this.rightCol, joinAlias), ctx);

        String aliasSuffix = joinAlias.equals(actualTable) ? "" : " AS " + joinAlias;
        String joinStr = this.joinType + " " + actualTable + aliasSuffix + " ON " + resolvedLeftCol + " = " + resolvedRightCol;

        ctx.joins.add(joinStr);
    }

    @Override
    public String toSql(SqlMapperBinder.BuildContext ctx) {
        String cleanedClass = cleanClassName(this.rawClass);
        EntityMeta meta = EntityMetaRegistry.getEntityMeta(cleanedClass);
        String actualTable = (meta != null) ? meta.getTableName() : cleanedClass;

        String joinAlias = ctx.tableAliases.getOrDefault(actualTable, actualTable);

        // 🚀 leftCol에 접두어가 없으면 메인 테이블 접두어 추가
        String leftColWithPrefix = this.leftCol.contains(".")
                ? this.leftCol
                : ctx.tablePrefix + "." + this.leftCol;

        String resolvedLeftCol = forceResolveColumn(leftColWithPrefix, ctx);
        String resolvedRightCol = forceResolveColumn(correctRightCol(this.rightCol, joinAlias), ctx);

        String aliasSuffix = joinAlias.equals(actualTable) ? "" : " AS " + joinAlias;

        return String.format("%s %s%s ON %s = %s",
                this.joinType, actualTable, aliasSuffix, resolvedLeftCol, resolvedRightCol);
    }

    // -------------------------------------------------------------------------
    // 내부 헬퍼 메서드
    // -------------------------------------------------------------------------

    private String cleanClassName(String raw) {
        if (raw.startsWith("class ")) raw = raw.substring(raw.lastIndexOf('.') + 1);
        return raw.replace(".class", "");
    }

    private String forceResolveColumn(String colStr, SqlMapperBinder.BuildContext ctx) {
        if (!colStr.contains(".")) return colStr; // 단순 문자열이면 그냥 리턴

        String[] parts = colStr.split("\\.");
        String alias = parts[0];
        String fieldName = parts[1];

        // Alias를 통해 실제 테이블명 획득
        String tableName = ctx.tableAliases.get(alias);
        if (tableName != null) {
            EntityMeta meta = EntityMetaRegistry.getEntityMeta(tableName);
            if (meta != null) {
                String dbCol = meta.getColumn(fieldName);
                if (dbCol != null) {
                    return alias + "." + dbCol;
                }
            }
        }
        return colStr; // 못 찾으면 원래 문자열 리턴
    }

    // 🚀 [추가됨] 기존 원본 클래스에 있던 correctRightCol을 노드 안으로 가져왔습니다.
    private String correctRightCol(String rightCol, String rightAlias) {
        if (rightCol == null) return "";

        // 이미 "alias.column" 형태인 경우
        if (rightCol.contains(".")) {
            String[] parts = rightCol.split("\\.");
            // 만약 앞부분이 현재 정해진 별칭과 다르다면 교정
            if (!parts[0].equals(rightAlias)) {
                return rightAlias + "." + parts[1];
            }
            return rightCol;
        }

        // 별칭이 있는 경우에만 접두어 추가
        return (rightAlias != null && !rightAlias.isEmpty())
                ? rightAlias + "." + rightCol
                : rightCol;
    }


}