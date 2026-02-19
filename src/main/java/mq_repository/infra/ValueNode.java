package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.BuildContext;
import mq_repository.domain.SqlNode;

public class ValueNode implements SqlNode {
    private final String column;
    private final String value;

    public ValueNode(String column, String value) {
        this.column = column;
        this.value = value;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        ctx.insertCols.add(column);
        ctx.insertVals.add(value); // 따옴표 처리 등의 포맷팅 로직 추가 가능
    }
    @Override public String toSql(SqlMapperBinder.BuildContext ctx) { return ""; }
}
