package m_ddl_generator.parser.annotation_metadata_loader.dto;

public class MEntityInfo {
    String tableName;
    String pkColumnName;


    public MEntityInfo(String t, String p) { tableName = t; pkColumnName = p; }

    public String getTableName() {
        return tableName;
    }

    public String getPkColumnName() {
        return pkColumnName;
    }
}
