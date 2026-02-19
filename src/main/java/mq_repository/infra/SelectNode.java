package mq_repository.infra;

import mq_mapper.infra.EntityMetaRegistry;
import mq_mapper.infra.SqlMapperBinder;
import mq_repository.domain.SqlNode;
import utils.EntityMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelectNode implements SqlNode {
    private final List<String> columns;

    public SelectNode(List<String> columns) {
        this.columns = columns;
    }

    @Override
    public void apply(SqlMapperBinder.BuildContext ctx) {
        List<String> resolved = new ArrayList<>();
        for (String col : columns) {
            resolved.add(resolveSelectColumn(col, ctx));
        }
        ctx.action = "SELECT";
        // 덮어쓰기 대신 누적
        if (ctx.columns.isEmpty()) {
            ctx.columns = String.join(", ", resolved);
        } else {
            ctx.columns = ctx.columns + ", " + String.join(", ", resolved);
        }
    }

    private String resolveSelectColumn(String colStr, SqlMapperBinder.BuildContext ctx) {
        String targetCol = colStr;

        if (!targetCol.contains(".")) {
            // requiresPrefix이거나, JOIN이 있으면 무조건 접두어 추가
            if (ctx.requiresPrefix || !ctx.joins.isEmpty()) {
                targetCol = ctx.tablePrefix + "." + targetCol;
            }
        }

        if (targetCol.contains(".")) {
            String[] parts = targetCol.split("\\.");
            String alias = parts[0];
            String fieldName = parts[1];

            String tableName = ctx.tableAliases.get(alias);
            if (tableName != null) {
                EntityMeta meta = EntityMetaRegistry.getEntityMeta(tableName);
                if (meta != null) {
                    String dbCol = meta.getColumn(fieldName);
                    if (dbCol != null) return alias + "." + dbCol;
                }
            }
        }
        return targetCol;
    }

    @Override public String toSql(SqlMapperBinder.BuildContext ctx) { return ""; }
}