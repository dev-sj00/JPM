package m_ddl_generator.generator;

import auto_ddl.AutoDDLPolicy;
import m_ddl_generator.dialect.SqlDialect;
import m_ddl_generator.model.TableMetadata;
import m_ddl_generator.parser.MetadataLoader;
import m_ddl_generator.writer.DdlWriter;
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
import utils.JpmOptionsLoader;
import utils.LogPrinter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AutoDDLGenerator {
    private final MetadataLoader loader;
    private final DdlWriter writer;
    private final ProcessingEnvironment processingEnv;
    private final Map<String, String> options;
    ExecutorSourceWriter executorWriter;
    // 상수 정의
    private static final String AUTO_EXECUTOR_PACKAGE = "m_ddl_generator.executor";
    private static final String EXECUTOR_CLASS_NAME = "JpmAutoSQLExecutor";

    public static class GeneratorCommand {
        public String sql;
        public String url;
        public String username;
        public String password;
        public String dbType;
        public String sqlCommandType;
    }

    public AutoDDLGenerator(MetadataLoader loader,
                            DdlWriter writer,
                            ProcessingEnvironment processingEnv,
                            ExecutorSourceWriter executorWriter,
                            Map<String, String> options) { // 👈 파라미터 추가
        this.loader = loader;

        this.writer = writer;
        this.processingEnv = processingEnv;
        this.executorWriter = executorWriter;
        this.options = options; // 👈 저장
    }

    // ===================================================================================
    // 1. Main Entry Point
    // ===================================================================================
    public void generate() {
        try {
            // 1-1. 메타데이터 로드
            List<TableMetadata> tables = loader.load(null);
            if (tables.isEmpty()) return;

            // 1-2. SQL 생성

            String finalSql = buildSql(tables);

            // 1-3. XML 파일 기록
            writer.write(finalSql);



            String cleanedSql = finalSql
                    .replace("<![CDATA[", "")  // 시작 태그 삭제
                    .replace("]]>", "");        // 끝 태그 삭제

            // 1-4. DB 연결 옵션 가져오기
            Map<String, String> options = JpmOptionsLoader.loadOptions(processingEnv);
            validateOptions(options);

            // 1-5. 즉시 DDL 실행 (실패 시 여기서 중단됨)


            String auto = options.get("auto");
            boolean isCreateExec = AutoDDLPolicy.CREATE_N_EXE.name().equals(auto);
            boolean isDropExec   = AutoDDLPolicy.DROP_N_CREATE_EXE.name().equals(auto);
            boolean isAlterExec  = AutoDDLPolicy.ALTER_N_EXE.name().equals(auto);


// 3. 하나라도 해당되면 '실행해야 하는 상태'로 판단
            boolean shouldExecute = isCreateExec || isDropExec || isAlterExec;

            if(shouldExecute)
            {
                executeImmediateDdl(cleanedSql, options);
            }
            // 1-6. Executor 소스 코드 생성

            GeneratorCommand cmd = createCommand(cleanedSql, options);

            generateExecutorSource(cmd);

        } catch (Exception e) {
            logError("AutoDDL Generation Error: " + e.getMessage());
        }
    }

    // ===================================================================================
    // 2. Helper Methods for Logic
    // ===================================================================================
    private String buildSql(List<TableMetadata> tables) {

        return new DdlScriptBuilder(options).build(tables);
    }

    private void validateOptions(Map<String, String> options) {
        if (options.get("url") == null || options.get("username") == null) {
            throw new RuntimeException("DB Connection options (url, username) are missing.");
        }
    }

    private String getDriverClassName(String dbType) {
        return dbType != null && dbType.contains("POSTGRES")
                ? "org.postgresql.Driver"
                : "com.mysql.cj.jdbc.Driver";
    }

    private GeneratorCommand createCommand(String sql, Map<String, String> options) {
        GeneratorCommand cmd = new GeneratorCommand();
        cmd.sql = sql;
        cmd.url = options.get("url");
        cmd.username = options.get("username");
        cmd.password = options.get("password");
        cmd.dbType = options.getOrDefault("dbType", "POSTGRES");
        cmd.sqlCommandType = options.getOrDefault("sqlType", "UPDATE");
        return cmd;
    }

    private void executeImmediateDdl(String sql, Map<String, String> options) throws Exception {
        logNote("🚀 Executing Generated DDL (Using application.properties keys)...");

        Properties props = new Properties();
        // src/main/resources/application.properties 파일을 읽어옵니다.

        String projectDir = options.get("projectDir");

        if (projectDir == null || projectDir.isEmpty()) {
            logNote("⚠️ projectDir 옵션이 없습니다. 기본 옵션값만 사용합니다.");
            return;
        }


        try {
            // 현재 프로젝트의 작업 디렉토리를 기준으로 경로 설정
            Path path = Paths.get(projectDir, "src", "main", "resources", "application.properties");

            if (Files.exists(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    props.load(is);
                    logNote("✅ application.properties 로드 성공: " + path.toAbsolutePath());
                }
            } else {
                // 파일이 없을 경우 상세 경로를 출력하여 디버깅을 돕습니다.
                logNote("ℹ️ 설정 파일이 존재하지 않습니다. (검색 경로: " + path.toAbsolutePath() + ")");
            }
        } catch (IOException e) {
            logError("파일 로드 중 오류: " + e.getMessage());
        }

        // MyBatis/Spring에서 흔히 사용하는 설정 키값으로 매핑
        String url = props.getProperty("spring.datasource.url", options.get("url"));
        String username = props.getProperty("spring.datasource.username", options.get("username"));
        String password = props.getProperty("spring.datasource.password", options.get("password"));

        // DB 타입도 관습에 따라 driver-class-name으로 가져오거나 기존 옵션 사용
        String driverClass = props.getProperty("spring.datasource.driver-class-name",
                getDriverClassName(options.getOrDefault("dbType", "POSTGRES")));

        if (url == null || username == null) {
            throw new RuntimeException("DB 접속 정보(url, username)가 설정 파일이나 옵션에 누락되었습니다.");
        }

        try {
            // PooledDataSource 설정
            PooledDataSource ds = new PooledDataSource(driverClass, url, username, password);

            // MyBatis 핵심 설정
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("compile_time_runner", transactionFactory, ds);
            Configuration config = new Configuration(environment);
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession()) {
                String tempStatementId = "ImmediateDDLRun";
                SqlSource sqlSource = new StaticSqlSource(config, sql);
                MappedStatement ms = new MappedStatement.Builder(config, tempStatementId, sqlSource, SqlCommandType.UPDATE).build();
                config.addMappedStatement(ms);

                session.update(tempStatementId);
                session.commit();
                logNote("✅ [MyBatis] DDL 실행 성공!");
            }
        } catch (Exception e) {
            logError("❌ [MyBatis] DDL 실행 실패: " + e.getMessage());
            throw e;
        }
    }





    // ===================================================================================
    // 3. Source Code Generation (Writer)
    // ===================================================================================
    private void generateExecutorSource(GeneratorCommand cmd) throws IOException {
        try {
            ExecutorSourceWriter execute = new JpmExecutorSourceWriter(processingEnv);
            execute.write(AUTO_EXECUTOR_PACKAGE, EXECUTOR_CLASS_NAME, cmd);
        } catch (Exception e) {
            logError("Executor 소스 생성 실패: " + e.getMessage());
        }
    }


    // ===================================================================================
    // 4. Utility Methods
    // ===================================================================================
    private void logNote(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void logError(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }
}