package m_ddl_generator.column.enums;

@Deprecated
public enum ColumnType {
    INTEGER,
    LONG,
    STRING,
    BOOLEAN,
    FK; // 외래키도 하나의 타입으로 취급

    // 필요하다면 파싱할 때 쓸 별칭(alias)을 매핑하는 로직 추가
    public static ColumnType from(String typeName) {
        if (typeName.toUpperCase().contains("INT")) return INTEGER;
        if (typeName.toUpperCase().contains("STR")) return STRING;
        if (typeName.toUpperCase().contains("FK")) return FK;
        throw new IllegalArgumentException("Unknown type: " + typeName);
    }
}
