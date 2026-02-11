package config.plugin; // 패키지명은 프로젝트에 맞게 수정하세요

import config.plugin.JpmDdlExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JpmPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // 1. Extension 생성
        JpmDdlExtension extension = project.getExtensions().create("jpm", JpmDdlExtension.class);

        // 2. JavaCompile 태스크 설정
        project.getTasks().withType(JavaCompile.class).configureEach(task -> {

            File propsFile = project.file("src/main/resources/application.properties");

            // [핵심] 파일 변경 감지
            if (propsFile.exists()) {
                task.getInputs().file(propsFile);
            }

            // --- [Step A] application.properties 파일 읽기 ---
            Properties props = new Properties();
            if (propsFile.exists()) {
                try (InputStream input = new FileInputStream(propsFile)) {
                    props.load(input);
                    // lifecycle: 일반 실행에서도 무조건 보임
                    project.getLogger().lifecycle("✅ [JPM Plugin] Loaded properties from: " + propsFile.getName());
                } catch (IOException e) {
                    project.getLogger().warn("⚠️ [JPM Plugin] Failed to read properties: " + e.getMessage());
                }
            }

            // --- [Step B] 값 병합 (Extension > Properties > Default) ---

            String url = getVal(extension.getUrl().getOrNull(), props.getProperty("spring.datasource.url"), "");
            String username = getVal(extension.getUsername().getOrNull(), props.getProperty("spring.datasource.username"), "");
            String password = getVal(extension.getPassword().getOrNull(), props.getProperty("spring.datasource.password"), "");

            // DB Type (기본값 MYSQL)
            String dbType = getVal(extension.getDbType().getOrNull(), props.getProperty("jpm.ddl.db-type"), "MYSQL");

            // Auto Mode (기본값 NONE)
            String auto = getVal(extension.getAuto().getOrNull(), props.getProperty("jpm.ddl.auto"), "NONE");

            String projectDir = project.getProjectDir().getAbsolutePath();

            // --- [Step C] 로그 출력 (logger.lifecycle 사용) ---
            project.getLogger().lifecycle("\n========== 🛠️ [JPM Plugin Config] ==========");
            project.getLogger().lifecycle("   👉 URL       : " + url);
            project.getLogger().lifecycle("   👉 Username  : " + username);
            project.getLogger().lifecycle("   👉 Password  : " + (password.isEmpty() ? "(empty)" : "****"));
            project.getLogger().lifecycle("   👉 DB Type   : " + dbType);
            project.getLogger().lifecycle("   👉 Auto Mode : " + auto);
            project.getLogger().lifecycle("===========================================\n");

            // --- [Step D] 컴파일러 옵션 주입 ---
            List<String> compilerArgs = new ArrayList<>();
            compilerArgs.add("-AprojectDir=" + projectDir);
            compilerArgs.add("-Aurl=" + url);
            compilerArgs.add("-Ausername=" + username);
            compilerArgs.add("-Apassword=" + password);
            compilerArgs.add("-AdbType=" + dbType);
            compilerArgs.add("-Aauto=" + auto);

            task.getOptions().getCompilerArgs().addAll(compilerArgs);
        });
    }

    // 헬퍼 메소드: 우선순위 처리 (Extension -> Property -> Default)
    private String getVal(String extVal, String propVal, String defVal) {
        if (extVal != null && !extVal.isEmpty()) return extVal;
        if (propVal != null && !propVal.isEmpty()) return propVal;
        return defVal;
    }
}