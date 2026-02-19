package mq_repository.domain;


import mq_mapper.infra.SqlMapperBinder;

// 1. 모든 SQL 처리 단위의 기본 인터페이스
public interface SqlNode {
    // Context를 받아 자신의 로직을 수행하고 Context를 업데이트함
    void apply(SqlMapperBinder.BuildContext ctx);
    String toSql(SqlMapperBinder.BuildContext ctx);
}