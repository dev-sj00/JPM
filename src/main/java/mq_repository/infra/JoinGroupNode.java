package mq_repository.infra;



import mq_mapper.domain.vo.DslStatement;
import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;

import utils.EntityMeta;

import java.util.List;

public class JoinGroupNode implements SqlNode {
    private final String joinType;      // "INNER JOIN" 또는 "LEFT JOIN"
    private final String alias;         // 서브쿼리 별칭 (예: sub)
    private final String leftCol;       // ON 조건의 왼쪽 컬럼
    private final String rightCol;      // ON 조건의 오른쪽 컬럼
    private final List<DslStatement> subStatements;
    private final EntityMeta entityMeta;

    public JoinGroupNode(String cmd, List<String> args, List<DslStatement> subStatements, EntityMeta entityMeta) {
        this.joinType = cmd.startsWith("left") ? "LEFT JOIN" : "INNER JOIN";
        // args 구조: [alias, leftCol, rightCol] 가정
        this.alias = (args.size() > 0) ? args.get(0) : "sub_query";
        this.leftCol = (args.size() > 1) ? args.get(1) : "";
        this.rightCol = (args.size() > 2) ? args.get(2) : "";
        this.subStatements = subStatements;
        this.entityMeta = entityMeta;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        String joinSql = toSql(ctx);
        if (!joinSql.isEmpty()) {
            ctx.joins.add(joinSql);
        }
    }

    @Override
    public String toSql(SqlMapperBinder.BuildContext ctx) {
        // 1. 서브쿼리 내부를 별도로 빌드 (재귀적 호출)
        // 실제로는 SqlMapperBinder의 새로운 인스턴스를 만들거나
        // 전용 서브쿼리 빌더를 호출해야 합니다.
        String subQuerySql = buildSubQuery(ctx);

        // 2. ON 조건 해석
        String resolvedLeft = resolveColumn(leftCol, ctx);
        // 오른쪽 컬럼은 서브쿼리의 별칭(alias)을 따르도록 강제
        String resolvedRight = alias + "." + (rightCol.contains(".") ? rightCol.split("\\.")[1] : rightCol);

        // 3. 최종 조립: JOIN (SELECT ...) AS alias ON ...
        return String.format("%s (%s) AS %s ON %s = %s",
                joinType, subQuerySql, alias, resolvedLeft, resolvedRight);
    }

    private String buildSubQuery(SqlMapperBinder.BuildContext parentCtx) {
        // 💡 중요: 서브쿼리용 Binder를 새로 생성하여 독립적인 쿼리를 뽑아냅니다.
        // 이 로직은 프로젝트의 SqlMapperBinder 구조에 따라 달라질 수 있습니다.
        SqlMapperBinder subBinder = new SqlMapperBinder();
        return subBinder.generateSqlFromStatements(subStatements, entityMeta);
    }

    private String resolveColumn(String col, SqlMapperBinder.BuildContext ctx) {
        if (!col.contains(".") && ctx.requiresPrefix) {
            return ctx.tablePrefix + "." + col;
        }
        return col; // 상세한 Meta 치환 로직은 기존 ConditionNode와 동일하게 적용 가능
    }
}
