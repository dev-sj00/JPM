package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;
import mq_repository.domain.enums.GroupType;
import mq_repository.infra.GroupNode;

public class WhereClauseNode implements SqlNode {
    private final GroupNode root = new GroupNode(GroupType.AND);

    public void addCondition(SqlNode condition) { root.add(condition); }
    public void addGroup(GroupNode group) { root.add(group); }
    public boolean isEmpty() { return root.isEmpty(); }

    @Override
    public String toSql(SqlMapperBinder.BuildContext ctx) {
        if (root.isEmpty()) return "";
        // 최하위 root는 괄호를 제거하고 "WHERE " 접두사만 붙임
        String content = root.toSql(ctx);
        // root.toSql 결과가 "( A AND B )" 형태라면 가장 바깥 괄호는 제거하는게 깔끔함
        if (content.startsWith("(") && content.endsWith(")")) {
            content = content.substring(1, content.length() - 1).trim();
        }
        return content;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        if (!isEmpty()) {
            // Binder의 wheres 리스트에 조립된 SQL을 통째로 넣음
            ctx.wheres.add(this.toSql(ctx));
        }
    }
}