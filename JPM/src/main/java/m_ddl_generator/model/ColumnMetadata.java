package m_ddl_generator.model;

public class ColumnMetadata {
    private final String name;
    private final String type;
    private final boolean isPrimaryKey;
    private final boolean isAutoIncrement;
    private final boolean isNullable;
    private final String defaultValue;
    private final boolean isIndexed;
    private final boolean isUnique;
    private final boolean isUUIDV7;

    // FK 정보 (Setter로 주입)
    private String fkTargetTable;
    private String fkTargetColumn;
    private String onDeleteAction;

    private ColumnMetadata(Builder builder) {
        this.name = builder.name;
        this.type = builder.dbType;
        this.isPrimaryKey = builder.isPrimaryKey;
        this.isAutoIncrement = builder.isAutoIncrement;
        this.isNullable = builder.isNullable;
        this.defaultValue = builder.defaultValue;
        this.isIndexed = builder.isIndexed;
        this.isUnique = builder.isUnique;
        this.isUUIDV7 = builder.isUUIDV7;
    }

    public void setForeignKey(String targetTable, String targetColumn, String onDeleteAction) {
        this.fkTargetTable = targetTable;
        this.fkTargetColumn = targetColumn;
        this.onDeleteAction = onDeleteAction;
    }

    // --- Getter Methods ---
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isContainPrimaryKey() { return isPrimaryKey; }
    public boolean isContainAutoIncrement() { return isAutoIncrement; }
    public boolean isContainNullable() { return isNullable; }
    public boolean isContainUUIDV7() { return isUUIDV7; }
    public String getDefaultValue() { return defaultValue; }
    public boolean isContainIndexed() { return isIndexed; }
    public boolean isContainUnique() { return isUnique; }


    // FK 관련 Getter
    public boolean isForeignKey() { return fkTargetTable != null; }
    public String getFkTargetTable() { return fkTargetTable; }
    public String getFkTargetColumn() { return fkTargetColumn; }
    public String getOnDeleteAction() { return onDeleteAction; }

    // --- 디버깅 및 로그용 toString ---
    @Override
    public String toString() {
        return "ColumnMetadata{" +
                "name='" + name + '\'' +
                ", dbType='" + type + '\'' +
                ", PK=" + isPrimaryKey +
                ", AI=" + isAutoIncrement +
                ", Nullable=" + isNullable +
                ", Default='" + defaultValue + '\'' +
                ", Indexed=" + isIndexed +
                ", Unique=" + isUnique +
                (isForeignKey() ? ", FK='" + fkTargetTable + "(" + fkTargetColumn + ")' ON DELETE " + onDeleteAction : "") +
                '}';
    }

    // --- Static Builder ---
    public static class Builder {
        private final String name;
        private final String dbType;
        private boolean isPrimaryKey = false;
        private boolean isAutoIncrement = false;
        private boolean isNullable = true;
        private String defaultValue = null;
        private boolean isIndexed = false;
        private boolean isUnique = false;
        private boolean isUUIDV7 = false;

        public Builder(String name, String dbType) {
            this.name = name;
            this.dbType = dbType;
        }

        public Builder primaryKey(boolean val) { this.isPrimaryKey = val; return this; }
        public Builder autoIncrement(boolean val) { this.isAutoIncrement = val; return this; }
        public Builder nullable(boolean val) { this.isNullable = val; return this; }
        public Builder defaultValue(String val) { this.defaultValue = val; return this; }
        public Builder indexed(boolean val) { this.isIndexed = val; return this; }
        public Builder unique(boolean val) { this.isUnique = val; return this; }
        public Builder isUUIDV7(boolean val) {this.isUUIDV7 = val; return this;}


        public ColumnMetadata build() {
            return new ColumnMetadata(this);
        }
    }
}