import annotation.MColumn;
import dsl_variable.v2.MFieldType;
import dsl_variable.v2.MField;


package m_ddl_generator.test;

import m_ddl_generator.parser.EntityMeta;
import m_ddl_generator.parser.MqParserUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MqParserTest {

    public static void main(String[] args) {
        System.out.println("🚀 MQ DSL Parser Test 시작 (Spring-free mode)\n");

        try {
            // 1. 엔티티 소스 파일 경로 설정 (실제 파일이 있는 경로로 수정 필요)
            // 프로젝트 구조에 따라 "src/main/java/..." 형태가 됩니다.
            Path entityPath = Paths.get("src/main/java/com/example/entity/MEntity3.java");

            System.out.println("🔍 분석 대상 파일: " + entityPath.toAbsolutePath());

            // 2. MqParserUtils를 이용한 정적 분석 실행
            long startTime = System.currentTimeMillis();
            EntityMeta meta = MqParserUtils.getMetadata(entityPath);
            long endTime = System.currentTimeMillis();

            // 3. 결과 출력 및 검증
            System.out.println("\n==========================================");
            System.out.println("✅ 분석 완료! (소요 시간: " + (endTime - startTime) + "ms)");
            System.out.println("------------------------------------------");
            System.out.println("📍 Table Name  : " + meta.getTableName());
            System.out.println("------------------------------------------");

            // 핵심 케이스별 출력
            printColumnInfo(meta, "id");          // PK, 자동 생성 이름 확인
            printColumnInfo(meta, "level");       // .name("user_level") 명시적 이름 확인
            printColumnInfo(meta, "description"); // .name("description") 명시적 이름 확인
            printColumnInfo(meta, "isActive");    // 스네이크 케이스 자동 변환 확인 (is_active)
            printColumnInfo(meta, "createdAt");   // 스네이크 케이스 자동 변환 확인 (created_at)

            System.out.println("==========================================");

        } catch (Exception e) {
            System.err.println("❌ 테스트 도중 에러 발생!");
            e.printStackTrace();
        }
    }

    private static void printColumnInfo(EntityMeta meta, String javaFieldName) {
        String dbColumn = meta.getColumn(javaFieldName);
        System.out.printf("👉 Java Field: [%-12s]  --->  DB Column: [%s]\n", javaFieldName, dbColumn);
    }
}
