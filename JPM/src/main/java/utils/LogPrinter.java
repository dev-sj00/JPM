package utils;



import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class LogPrinter {

    private static Messager messager;

    private static final String TABLE_FORMAT = "%-7s | %-12s | %-12s | %s";



    private static String repeat(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static void printHeader() {
        // Messager가 문자열만 받으므로, 하나로 합쳐서 출력하는 게 깔끔합니다.
        String header = "\n" + repeat("=") + "\n" +
                String.format(TABLE_FORMAT, "LEVEL", "CATEGORY", "COLUMN", "MESSAGE") + "\n" +
                repeat("-");

        // Messager로 출력 (Diagnostic.Kind.NOTE 사용)
        // 만약 static messager가 없다면 아래처럼 바로 찍어도 되지만,
        // 빌드 타임에 보려면 messager 활용을 권장합니다.
        info("INIT SYSTEM JPM Validation started...");
        System.out.println(header);
    }

    // 프로세서 시작할 때 딱 한 번 호출
    public static void init(ProcessingEnvironment env) {
        messager = env.getMessager();
    }

    // 1. 일반 로그 (NOTE)
    public static void info(String message) {
        if (messager != null) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Build Log: " + message);
        }
    }

    // 2. 경고 (WARNING) - 소스코드 위치 포함
    public static void warn(String message, Element element) {
        if (messager != null) {
            messager.printMessage(Diagnostic.Kind.WARNING, message, element);
        }
    }

    public static void error(String tag, String column, String msg, Element e) {
        String formatted = String.format("\n"+TABLE_FORMAT, "[ERROR]", tag, column, "\n"+msg);
        // Messager를 사용한다면 Diagnostic.Kind.ERROR와 함께 출력
        messager.printMessage(Diagnostic.Kind.ERROR, formatted, e);
    }

    public static void warn(String tag, String column, String msg, Element e) {
        String formatted = String.format("\n"+TABLE_FORMAT, "[WARN]", tag, column, "\n"+msg);
        messager.printMessage(Diagnostic.Kind.WARNING, formatted, e);
    }


}

