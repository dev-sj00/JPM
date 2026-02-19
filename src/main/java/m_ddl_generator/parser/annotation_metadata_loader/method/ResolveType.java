package m_ddl_generator.parser.annotation_metadata_loader.method;

public enum ResolveType {

    INTEGER("INT"),
    LONG("BIGINT"),
    FK("BIGINT"),
    FLOAT("FLOAT"),
    DOUBLE("DOUBLE PRECISION"),
    BOOLEAN("BOOLEAN"),
    LOCAL_DATE("DATE"),
    LOCAL_DATE_TIME("TIMESTAMP"),
    TEXT("TEXT"),
    STRING("VARCHAR(255)"),
    UUID_V_7("UUID");

    private final String sqlType;

    ResolveType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getSqlType() {
        return sqlType;
    }
}