package mq_repository.infra;

import mq_mapper.domain.vo.DslStatement;
import mq_mapper.infra.EntityMetaRegistry;
import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;
import utils.EntityMeta;

import java.util.List;

public class ExistsNode implements SqlNode {
    private final String cmd; // "whereExistsGroup" 등
    private final List<DslStatement> subStatements;
    private final EntityMeta entityMeta;

    public ExistsNode(String cmd, List<DslStatement> subStatements, EntityMeta entityMeta) {
        this.cmd = cmd;
        this.subStatements = subStatements;
        this.entityMeta = entityMeta;
    }

    @Override
    public String toSql(SqlMapperBinder.BuildContext ctx) {
        if (subStatements == null || subStatements.isEmpty()) {
            return (cmd.contains("Not") ? "NOT EXISTS" : "EXISTS") + " (SELECT 1)";
        }

        // 서브쿼리 내부 from()에서 실제 EntityMeta 추출
        EntityMeta subMeta = entityMeta; // 기본값
        for (DslStatement stmt : subStatements) {
            if ("from".equals(stmt.getCommand()) && !stmt.getArgs().isEmpty()) {
                String rawClass = stmt.getArgs().get(0)
                        .replace(".class", "")
                        .replaceAll("^class .*\\.", ""); // 패키지명 제거
                EntityMeta found = EntityMetaRegistry.getEntityMeta(rawClass);
                if (found != null) {
                    subMeta = found;
                    break;
                }
            }
        }

        // 서브쿼리용 독립 컨텍스트 (부모 별칭 맵 상속)
        SqlMapperBinder subBinder = new SqlMapperBinder();
        String subSql = subBinder.generateSqlFromStatements(subStatements, subMeta);

        String operator = cmd.contains("Not") ? "NOT EXISTS" : "EXISTS";
        return operator + " (\n" + subSql + "\n)";
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        // WhereClauseNode 내부에서 toSql을 호출해 사용할 예정
    }


}
