package m_ddl_generator.model;

import java.util.ArrayList;
import java.util.List;

public class TableMetadata {
    private final String tableName;
    private final List<ColumnMetadata> columns;

    public TableMetadata(String tableName, List<ColumnMetadata> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }
    // Getters...
    public String getTableName() { return tableName; }
    public List<ColumnMetadata> getColumns() { return columns; }

    public List<String> getParentNames()
    {

        List<String> parentTables = new ArrayList<>();

        for(ColumnMetadata col : columns)
        {
            if(col.getFkTargetTable() !=null)
            {
               parentTables.add(col.getFkTargetTable());
            }
        }

        return parentTables;
    }
}