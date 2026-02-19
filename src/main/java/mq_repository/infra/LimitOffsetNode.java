package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;

public class LimitOffsetNode implements SqlNode {
    private final String type;
    private final String value;

    public LimitOffsetNode(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        // 키워드 없이 값만 컨텍스트에 저장
        if ("LIMIT".equals(type)) ctx.limit = value;
        if ("OFFSET".equals(type)) ctx.offset = value;
    }

    @Override public String toSql(SqlMapperBinder.BuildContext ctx) { return ""; }
}
