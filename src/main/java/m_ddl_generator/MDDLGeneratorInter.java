package m_ddl_generator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

public interface MDDLGeneratorInter {
    void generateXML(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv);
}
