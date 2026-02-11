package m_ddl_generator.generator;

public interface ExecutorSourceWriter {
    /**
     * Executor 자바 소스 파일을 생성합니다.
     * @param packageName 패키지명
     * @param className 클래스명
     */
    void write(String packageName, String className, AutoDDLGenerator.GeneratorCommand cmd);
}
