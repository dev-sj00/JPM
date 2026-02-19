package mq_repository.infra;

import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;
import mq_repository.domain.enums.GroupType;

import java.util.ArrayList;
import java.util.List;

public class GroupNode implements SqlNode {
    private final GroupType groupType;
    private final List<SqlNode> children = new ArrayList<>();

    public GroupNode(GroupType groupType) {
        this.groupType = groupType;
    }

    public void add(SqlNode node) { children.add(node); }
    public boolean isEmpty() { return children.isEmpty(); }

    @Override
    public String toSql(SqlMapperBinder.BuildContext ctx) {
        if (children.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for (SqlNode child : children) {
            String sql = child.toSql(ctx);
            if (sql == null || sql.isEmpty()) continue;

            if (isFirst) {
                sb.append(sql);
                isFirst = false;
            } else {
                // 🚀 핵심: 자식 노드가 ConditionNode인 경우, 해당 노드가 가진 고유의 logic(AND/OR)을 사용!
                // 만약 다른 타입의 노드라면 기본 그룹의 연산자(groupType.getOperator())를 폴백(Fallback)으로 사용
                if (child instanceof ConditionNode) { // 패키지명은 프로젝트에 맞게 맞춰주세요!
                    String logic = ((ConditionNode) child).getLogicOperator();
                    sb.append(" ").append(logic != null ? logic : groupType.getOperator()).append(" ");
                } else {
                    sb.append(" ").append(groupType.getOperator()).append(" ");
                }
                sb.append(sql);
            }
        }

        if (sb.length() == 0) return "";

        // 조립된 결과가 단일 조건이라도 그룹(괄호)으로 감쌀지 말지 결정 (현재 코드 기준으로는 감쌈)
        // 원치 않으시면 자식 개수에 따라 조건처리 하셔도 됩니다.
        return "( " + sb.toString() + " )";
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        // GroupNode는 단독으로 apply되지 않고 WhereClauseNode에 의해 관리됨
    }
}