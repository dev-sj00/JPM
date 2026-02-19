package mq_repository.infra;

import mq_mapper.infra.EntityMetaRegistry;
import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.BuildContext;
import mq_repository.domain.SqlNode;
import utils.EntityMeta;

import java.util.List;


public class TableNode implements SqlNode {
    private final List<String> args;

    public TableNode(List<String> args) {
        this.args = args;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        String inputName = args.get(0); // "UsersInfoEntity" 또는 "users_info"
        String alias = args.size() > 1 ? args.get(1) : null;

        // 🚀 1. 클래스명으로 Meta를 찾아서 실제 테이블명 추출
        EntityMeta meta = EntityMetaRegistry.getEntityMeta(inputName);
        String finalTableName = (meta != null) ? meta.getTableName() : inputName;

        // 2. 테이블 표현식 조립
        String tableExpr = finalTableName;
        if (alias != null) {
            tableExpr += " " + alias;
            ctx.tableAliases.put(alias, finalTableName);
        } else {
            ctx.tableAliases.put(finalTableName, finalTableName);
        }

        // 3. Set에 추가 (중복 방지)
        ctx.tables.add(tableExpr);

        // 4. 접두어 설정
        if (ctx.tablePrefix == null || ctx.tablePrefix.isEmpty()) {
            ctx.tablePrefix = alias != null ? alias : finalTableName;
        }
    }

    @Override
    public String toSql(SqlMapperBinder.BuildContext ctx) {
        return "";
    }
}
