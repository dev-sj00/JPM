package m_ddl_generator.generator; // 패키지는 상황에 맞게 조정하세요

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MyBatisDirectExecutor {

    private final Messager messager;

    public MyBatisDirectExecutor(Messager messager) {
        this.messager = messager;
    }

    /**
     * 외부에서 호출하는 유일한 공개 메서드
     */
    public void execute(String sql, Map<String, String> aptOptions) throws Exception {
        log("🚀 Executing Generated DDL (Using application.properties keys)...");

        // 1. 설정 로딩 (Private로 은닉)
        Map<String, String> dbConfig = loadDbConfig(aptOptions);

        // 2. MyBatis 실행 (Private로 은닉)
        runMyBatisUpdate(sql, dbConfig);
    }

    // =================================================================================
    // Internal Helper Methods (Private)
    // =================================================================================

    private Map<String, String> loadDbConfig(Map<String, String> aptOptions) {
        Map<String, String> config = new HashMap<>(aptOptions);
        Properties props = new Properties();

        String projectDir = aptOptions.get("projectDir");

        // 1-1. application.properties 파일 읽기
        if (projectDir != null && !projectDir.isEmpty()) {
            try {
                Path path = Paths.get(projectDir, "src", "main", "resources", "application.properties");
                if (Files.exists(path)) {
                    try (InputStream is = Files.newInputStream(path)) {
                        props.load(is);
                        log("✅ application.properties 로드 성공: " + path.toAbsolutePath());
                    }
                } else {
                    log("ℹ️ 설정 파일이 존재하지 않습니다. (검색 경로: " + path.toAbsolutePath() + ")");
                }
            } catch (IOException e) {
                printError("파일 로드 중 오류: " + e.getMessage());
            }
        } else {
            log("⚠️ projectDir 옵션이 없습니다. 기본 옵션값만 사용합니다.");
        }

        // 1-2. 키 매핑 (Properties -> Map)
        String url = props.getProperty("spring.datasource.url", aptOptions.get("url"));
        String username = props.getProperty("spring.datasource.username", aptOptions.get("username"));
        String password = props.getProperty("spring.datasource.password", aptOptions.get("password"));

        // 1-3. 드라이버 결정 로직
        String defaultDriver = getDriverClassName(aptOptions.getOrDefault("dbType", "POSTGRES"));
        String driverClass = props.getProperty("spring.datasource.driver-class-name", defaultDriver);

        // 검증
        if (url == null || username == null) {
            throw new RuntimeException("DB 접속 정보(url, username)가 설정 파일이나 옵션에 누락되었습니다.");
        }

        config.put("url", url);
        config.put("username", username);
        config.put("password", password);
        config.put("driverClass", driverClass);

        return config;
    }

    private void runMyBatisUpdate(String sql, Map<String, String> config) throws Exception {
        try {
            // PooledDataSource 설정
            PooledDataSource ds = new PooledDataSource(
                    config.get("driverClass"),
                    config.get("url"),
                    config.get("username"),
                    config.get("password")
            );

            // MyBatis 핵심 설정
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("compile_time_runner", transactionFactory, ds);
            Configuration myBatisConfig = new Configuration(environment);
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(myBatisConfig);

            try (SqlSession session = factory.openSession()) {
                String tempStatementId = "ImmediateDDLRun";
                SqlSource sqlSource = new StaticSqlSource(myBatisConfig, sql);
                MappedStatement ms = new MappedStatement.Builder(myBatisConfig, tempStatementId, sqlSource, SqlCommandType.UPDATE).build();
                myBatisConfig.addMappedStatement(ms);

                session.update(tempStatementId);
                session.commit();
                log("✅ [MyBatis] DDL 실행 성공!");
            }
        } catch (Exception e) {
            printError("❌ [MyBatis] DDL 실행 실패: " + e.getMessage());
            throw e;
        }
    }

    private String getDriverClassName(String dbType) {
        return dbType != null && dbType.toUpperCase().contains("POSTGRES")
                ? "org.postgresql.Driver"
                : "com.mysql.cj.jdbc.Driver";
    }

    private void log(String msg) {
        if (messager != null) messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void printError(String msg) {
        if (messager != null) messager.printMessage(Diagnostic.Kind.ERROR, msg);
    }
}