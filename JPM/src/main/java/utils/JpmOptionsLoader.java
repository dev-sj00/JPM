package utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JpmOptionsLoader {

    private JpmOptionsLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static Map<String, String> loadOptions(ProcessingEnvironment processingEnv) {
        Map<String, String> finalOptions = new HashMap<>();

        // 1. Gradle에서 넘겨준 프로젝트 경로 확인
        Map<String, String> compileOptions = processingEnv.getOptions();
        String projectDir = compileOptions.get("projectDir");

        // 경로가 없으면 현재 디렉토리(.) 사용
        if (projectDir == null || projectDir.isEmpty()) {
            projectDir = ".";
        }

        // 파일 객체 생성
        File propFile = new File(projectDir, "src/main/resources/application.properties");

        // 🔥 [디버깅 로그] 실제로 어디를 찾고 있는지 출력
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "\n========== [JPM DEBUG] ==========\n" +
                        "👉 Project Dir: " + projectDir + "\n" +
                        "👉 Target File: " + propFile.getAbsolutePath() + "\n" +
                        "👉 File Exists: " + propFile.exists() + "\n" +
                        "=================================\n");

        // 2. properties 파일 읽기
        if (propFile.exists()) {
            try (InputStream input = Files.newInputStream(propFile.toPath())) {
                Properties props = new Properties();
                props.load(input);

                // 값 매핑 확인 로그
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "ℹ️ Reading properties...");

                if (props.containsKey("spring.datasource.url")) {
                    finalOptions.put("url", props.getProperty("spring.datasource.url"));
                    // processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "   - Found URL");
                }

                if (props.containsKey("spring.datasource.username")) {
                    finalOptions.put("username", props.getProperty("spring.datasource.username"));
                }

                if (props.containsKey("spring.datasource.password")) {
                    finalOptions.put("password", props.getProperty("spring.datasource.password"));
                }

                if (props.containsKey("jpm.ddl.db-type")) {
                    finalOptions.put("dbType", props.getProperty("jpm.ddl.db-type").toUpperCase());
                }

                if (props.containsKey("jpm.ddl.auto")) {
                    finalOptions.put("auto", props.getProperty("jpm.ddl.auto"));
                }

            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "⚠️ [JPM] Properties file read error: " + e.getMessage());
            }
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "⚠️ [JPM] application.properties File NOT FOUND at: " + propFile.getAbsolutePath());
        }

        // 3. 컴파일 옵션 병합
        if (!compileOptions.isEmpty()) {
            finalOptions.putAll(compileOptions);
        }

        return finalOptions;
    }
}