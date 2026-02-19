package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;

import java.util.List;

public class OrderByNode implements SqlNode {
    private final List<String> args;
    public OrderByNode(List<String> args) { this.args = args; }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        String col = args.get(0);
        // 기존 isBareName 로직 대체 (단순 문자열에 점(.)이 없으면 접두어 추가)
        if (ctx.requiresPrefix && !col.contains(".")) {
            col = ctx.tablePrefix + "." + col;
        }
        String spec = col + (args.size() > 1 ? " " + args.get(1) : "");
        ctx.orderBys.add(spec); // 키워드 없이 순수 조건만 리스트에 담기
    }

    @Override public String toSql(SqlMapperBinder.BuildContext ctx) { return ""; }
}
