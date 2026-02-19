package mq_repository.infra;

import mq_mapper.domain.vo.DslStatement;
import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;

import utils.EntityMeta;

import java.util.List;

public class FromSubQueryNode implements SqlNode {
    private final List<DslStatement> subStatements;
    private final EntityMeta entityMeta;
    private final String alias;

    public FromSubQueryNode(DslStatement stmt, List<DslStatement> subStatements, EntityMeta entityMeta) {
        this.subStatements = subStatements;
        this.entityMeta = entityMeta;

        // 🚀 수정한 부분: List<Object>를 List<String>으로 변경
        List<String> args = stmt.getArgs();
        this.alias = (args != null && !args.isEmpty()) ? args.get(0) : "sub";
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        String subQuerySql = toSql(ctx);
        if (subQuerySql != null && !subQuerySql.isEmpty()) {
            ctx.tables.add(subQuerySql);
            // 서브쿼리 별칭 등록
            ctx.tableAliases.put(this.alias, "SUBQUERY");
        }
    }

    @Override
    public String toSql(SqlMapperBinder.BuildContext ctx) {
        SqlMapperBinder subBinder = new SqlMapperBinder();
        // 앞서 추가한 generateSqlFromStatements 메서드 호출
        String innerSql = subBinder.generateSqlFromStatements(subStatements, entityMeta);

        if (innerSql == null || innerSql.isEmpty()) return "";

        return "(" + innerSql + ") AS " + alias;
    }
}