package m_ddl_generator.column.column_metadata;

import m_ddl_generator.column.enums.ColumnType;

@Deprecated
public class ColumnMetadata {
    private String name;
    private ColumnType type; // 🔥 핵심! String typeName 대신 이거 사용

    // 타입별로 필요한 속성들을 그냥 다 때려 넣으세요 (Optional fields)
    private int length;          // String용
    private String fkTargetTable;// FK용
    private String onDelete;     // FK용

    // 생성자나 빌더로 필요한 것만 세팅
}
