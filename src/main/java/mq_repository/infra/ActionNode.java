package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;

public class ActionNode implements SqlNode {
    private final String action;

    public ActionNode(String action) {
        this.action = action;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        ctx.action = this.action;
    }

    @Override public String toSql(SqlMapperBinder.BuildContext ctx) { return ""; }
}
