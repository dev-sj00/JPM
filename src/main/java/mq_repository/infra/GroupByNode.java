package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;

import java.util.List;

public class GroupByNode implements SqlNode {
    private final List<String> columns;
    public GroupByNode(List<String> columns) { this.columns = columns; }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        ctx.groupBys.add(String.join(", ", columns));
    }
    @Override public String toSql(SqlMapperBinder.BuildContext ctx) { return ""; }
}
