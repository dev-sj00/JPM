package auto_ddl;

public enum AutoDDLPolicy {
    DISABLED,        // 자동 DDL 수행 안 함
    CREATE,
    DROP,//만들어야함
    ALTER,
    ALTER_N_EXE,
    CREATE_N_EXE,          // 테이블 생성
    DROP_N_CREATE_EXE // 기존 테이블 삭제 후 생성
}
