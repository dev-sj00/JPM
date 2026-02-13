package executor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseBootstrap {

    /**
     * 타겟 데이터베이스가 없으면 생성해주는 메서드
     * @param fullUrl  사용자가 접속하려는 최종 URL (예: jdbc:postgresql://localhost:5432/aips)
     * @param user     DB 유저 (DB 생성 권한 필요, 보통 root나 postgres)
     * @param password DB 비번
     */
    public static void createDatabaseIfNotExists(String fullUrl, String user, String password) {
        // 1. URL 파싱: "jdbc:postgresql://host:port/targetDB" -> targetDB 추출
        String targetDbName = extractDbName(fullUrl);

        // 2. 관리용 DB(postgres) 접속 URL 생성

        String maintenanceUrl = replaceDbNameInUrl(fullUrl, targetDbName, "postgres");

        System.out.println("🔍 [Bootstrap] Checking if database '" + targetDbName + "' exists...");

        try (Connection conn = DriverManager.getConnection(maintenanceUrl, user, password);
             Statement stmt = conn.createStatement()) {

            // 3. 이미 존재하는지 확인 (PostgreSQL 시스템 뷰 조회)
            String checkSql = "SELECT 1 FROM pg_database WHERE datname = '" + targetDbName + "'";
            ResultSet rs = stmt.executeQuery(checkSql);

            if (rs.next()) {
                System.out.println("✅ [Bootstrap] Database '" + targetDbName + "' already exists. Skipping creation.");
            } else {
                // 4. 없으면 생성 (CREATE DATABASE 문 실행)
                // 주의: CREATE DATABASE는 트랜잭션 블록 안에서 실행 불가하므로 auto-commit 모드여야 함 (기본값)
                System.out.println("✨ [Bootstrap] Creating database '" + targetDbName + "'...");
                stmt.executeUpdate("CREATE DATABASE " + targetDbName);
                System.out.println("✅ [Bootstrap] Database created successfully!");
            }

        } catch (Exception e) {
            System.err.println("❌ [Bootstrap] Failed to create database via JDBC.");
            System.err.println("   -> Reason: " + e.getMessage());
            // 여기서 에러가 나면 프로그램이 멈춰야 함 (DB 없이 진행 불가)
            throw new RuntimeException(e);
        }
    }

    // URL에서 맨 뒤의 DB 이름만 쏙 빼내는 로직
    private static String extractDbName(String url) {
        int lastSlash = url.lastIndexOf("/");
        int questionMark = url.indexOf("?");
        if (questionMark == -1) {
            return url.substring(lastSlash + 1);
        } else {
            return url.substring(lastSlash + 1, questionMark);
        }
    }


    private static String replaceDbNameInUrl(String url, String oldName, String newName) {
        return url.replace("/" + oldName, "/" + newName);
    }
}