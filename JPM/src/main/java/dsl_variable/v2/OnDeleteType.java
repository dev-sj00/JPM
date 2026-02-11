package dsl_variable.v2;



public enum OnDeleteType {
    CASCADE("CASCADE"),       // 같이 삭제
    SET_NULL("SET NULL"),     // NULL로 변경 (고아 객체)
    RESTRICT("RESTRICT"),     // 삭제 막음 (즉시)
    NO_ACTION("NO ACTION"),   // 삭제 막음 (지연) - 보통 기본값
    SET_DEFAULT("SET DEFAULT"); // 기본값으로 변경

    private final String sql;



    OnDeleteType(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
