package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;

public class SetNode implements SqlNode {
    private final String column;
    private final String value;

    public SetNode(String column, String value) {
        this.column = column;
        this.value = value;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        ctx.sets.add(column + " = " + value); // 필요시 column 별칭 해석 로직 추가
    }
    @Override public String toSql(SqlMapperBinder.BuildContext ctx) { return ""; }
}
