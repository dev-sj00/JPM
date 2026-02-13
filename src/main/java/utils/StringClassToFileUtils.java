package utils;

import java.io.File;

public class StringClassToFileUtils {

    /**
     * className: "MyEntity.class" 또는 "dsl_variable.MyEntity.class"
     * sourceRoot: "src/main/java"
     */
    public static String getSourceFilePath(String className, String sourceRoot) {
        // 1. ".class" 제거
        if (className.endsWith(".class")) {
            className = className.substring(0, className.length() - 6);
        }

        // 2. 패키지 구분자를 OS 경로 구분자로 변환
        String relativePath = className.replace('.', File.separatorChar) + ".java";

        // 3. sourceRoot와 결합

        return sourceRoot + File.separator + relativePath;
    }
}
