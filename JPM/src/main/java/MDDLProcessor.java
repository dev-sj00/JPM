

import auto_ddl.AutoDDLPolicy;
import com.google.auto.service.AutoService;
import config.AppConfig;
import m_ddl_generator.dialect.MySqlDialect;
import m_ddl_generator.dialect.PostgreSqlDialect;
import m_ddl_generator.dialect.SqlDialect;
import m_ddl_generator.generator.AutoDDLGenerator;
import m_ddl_generator.generator.JpmExecutorSourceWriter;
import m_ddl_generator.parser.AnnotationMetadataLoader;
import m_ddl_generator.parser.MetadataLoader;
import m_ddl_generator.writer.DdlWriter;
import m_ddl_generator.writer.MyBatisXmlWriter;
import utils.LogPrinter;


import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
@SupportedOptions({ "url", "username", "password", "dbType", "auto", "projectDir" })
@SupportedAnnotationTypes({ "annotation.MEntity", "annotation.MColumn" })
public class MDDLProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 어노테이션이 없으면 처리 안 함
        if (annotations.isEmpty()) {
            return false;
        }

        try {
            // 1. Gradle이 주입한 옵션 가져오기 (파일 읽기 X, 오직 주입된 값만 신뢰)
            Map<String, String> options = processingEnv.getOptions();


            LogPrinter.init(processingEnv); //콘솔 찍기용

            // 2. Policy 파싱 (대소문자 무시 처리)
            String autoStr = options.getOrDefault("auto", "DISABLED").toUpperCase();
            AutoDDLPolicy policy;
            try {
                policy = AutoDDLPolicy.valueOf(autoStr);
            } catch (IllegalArgumentException e) {
                // 오타가 있거나 값이 이상하면 DISABLED 처리
                policy = AutoDDLPolicy.DISABLED;
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "⚠️ [JPM] 알 수 없는 auto 모드입니다 ('" + autoStr + "'). DISABLED로 설정합니다.");
            }

            // 3. DISABLED 상태면 즉시 종료 (로그만 남김)
            if (policy == AutoDDLPolicy.DISABLED) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "💤 [JPM] DDL Generator is DISABLED. (Skipping execution)");
                return true;
            }

            // 4. 실행 정보 로그 출력
            String dbType = options.getOrDefault("dbType", "MYSQL").toUpperCase();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "🚀 [JPM] Start DDL Generation! (Policy: " + policy + ", DB: " + dbType + ")");


            // 5. 컴포넌트 준비
            MetadataLoader metadataLoader = new AnnotationMetadataLoader(processingEnv, roundEnv);

            // DB 타입에 따른 방언 설정
            AppConfig.sqlDialectInit(options);

            DdlWriter ddlWriter = new MyBatisXmlWriter(processingEnv.getFiler(), "m_ddl_generator.ddl.AutoDDL");

            // 6. Generator 생성 및 실행
            AutoDDLGenerator generator = new AutoDDLGenerator(
                    metadataLoader,
                    ddlWriter,
                    processingEnv,
                    new JpmExecutorSourceWriter(processingEnv),
                    options // 전체 옵션 전달 (url, username, password 포함됨)
            );

            generator.generate();

        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "❌ [JPM] DDL 생성 중 오류 발생: " + e.getMessage());

        }

        return true;
    }
}