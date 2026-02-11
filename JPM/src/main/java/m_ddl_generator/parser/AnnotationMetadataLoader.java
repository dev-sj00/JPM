package m_ddl_generator.parser;

import annotation.MEntity;
import config.AppConfig;
import dsl_variable.v2.MFieldType;
import dsl_variable.v2.MObjectFactory;
import dsl_variable.v2.MParserUtils;
import dsl_variable.v2.MField;
import m_ddl_generator.AnnotationUtil; // 파일 경로 얻는 유틸 (기존 유지)
import m_ddl_generator.dialect.SqlDialect;
import m_ddl_generator.model.ColumnMetadata;
import m_ddl_generator.model.TableMetadata;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.*;


public class AnnotationMetadataLoader implements MetadataLoader {


    private final RoundEnvironment roundEnv;
    private final Messager messager;
    Map<String, String> options;
    SqlDialect sqlDialect = AppConfig.getSqlDialectImpl();

    // 1차 스캔 결과 저장소 (클래스명 -> 엔티티 정보)
    private final Map<String, EntityInfo> entityInfoMap = new HashMap<>();

    // 파싱된 변수 캐시 (파일 다시 읽지 않기 위함)
    private final Map<String, List<MField>> parsedVariablesCache = new HashMap<>();

    public AnnotationMetadataLoader(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv) {

        this.roundEnv = roundEnv;
        this.messager = processingEnv.getMessager();
        this.options = processingEnv.getOptions();
    }

    @Override
    public List<TableMetadata> load(RoundEnvironment ignored) {
        List<TableMetadata> tables = new ArrayList<>();
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(MEntity.class);

        // --- Step 1. 모든 엔티티 사전 스캔 (Parsing & Info Collecting) ---
        for (Element element : elements) {
            if (element instanceof TypeElement) {
                scanEntity((TypeElement) element);
            }
        }

        // --- Step 2. 메타데이터 생성 (Linking) ---
        for (String className : parsedVariablesCache.keySet()) {


            try {
                TableMetadata table = buildTableMetadata(className);
                if (table != null) {
                    tables.add(table);
                }
            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "DDL Generation Failed for " + className + ": " + e.getMessage());

            }
        }

        return tables;
    }

    // --- 내부 로직 ---

    /**
     * 1. 소스 코드를 파싱해서 MVariable 리스트를 만들고
     * 2. PK 정보와 테이블 이름을 entityInfoMap에 저장합니다.
     */
    private void scanEntity(TypeElement element) {
        try {
            String className = element.getSimpleName().toString();
            MEntity entityAnn = element.getAnnotation(MEntity.class);
            String tableName = entityAnn.name();

            // 🚨 [수정] AnnotationUtil이 TypeElement(클래스)를 처리 못하고 에러를 뱉으므로,
            // 클래스 내부의 첫 번째 필드를 찾아서 대신 넘겨줍니다. (같은 파일이므로 경로 동일)
            VariableElement firstField = javax.lang.model.util.ElementFilter.fieldsIn(element.getEnclosedElements())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("[AnnotationMetadataLoader: scanEntity] Entity must have at least one field: " + className));

            // 클래스(element) 대신 필드(firstField)를 넘김
            String filePath = AnnotationUtil.getRelativePath(firstField, false);




            // 2. 파싱 실행 (MParserUtils -> Pairs -> MVariable)
            List<List<MParserUtils.Pair>> rawDataList = MParserUtils.execute(filePath);

            List<MField> variables = new ArrayList<>();
            String pkColumnName = "id"; // fallback

            for (List<MParserUtils.Pair> rawData : rawDataList) {
                // 팩토리를 통해 MVariable 객체 생성
                MField var = MObjectFactory.createMVariable(rawData, element);


                variables.add(var);

                if (var.isPrimaryKey()) {
                    pkColumnName = var.getName(); // PK 발견

                }
            }

            // 3. 정보 캐싱
            parsedVariablesCache.put(className, variables);

            EntityInfo info = new EntityInfo(tableName, pkColumnName);
            entityInfoMap.put(className, info);

        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING, "Parsing skipped for " + element.getSimpleName() + ": " + e.getMessage());


        }
    }

    /**
     * 저장된 MVariable 정보를 바탕으로 최종 TableMetadata를 생성합니다.
     */
    private TableMetadata buildTableMetadata(String className) {
        List<MField> variables = parsedVariablesCache.get(className);
        EntityInfo currentEntity = entityInfoMap.get(className);

        if (variables == null || currentEntity == null) return null;

        List<ColumnMetadata> columns = new ArrayList<>();

        for (MField var : variables) {
            String type = mapToSqlType(var);

            if (var.getType() == MFieldType.STRING) {
                type = "VARCHAR(" + var.getLength() + ")";
            }

            boolean finalNullable = !var.isPrimaryKey() && var.isNullable();

            // 🔥 [수정 핵심] 기본값(Default Value) 처리 로직 강화
            String finalDefaultValue = null;
            if (var.getDefaultValue() != null) {
                // 사용자가 "DEFAULT"를 안 썼으면 자동으로 붙여줌
                if (!var.getDefaultValue().trim().toUpperCase().startsWith("DEFAULT")) {
                    finalDefaultValue = "DEFAULT " + var.getDefaultValue();
                } else {
                    finalDefaultValue = var.getDefaultValue();
                }
            }

            // PK인 경우 Auto Increment면 Default 값 제거 (충돌 방지)
            if (var.isPrimaryKey() && var.isAutoIncrement()) {
                finalDefaultValue = null;
            }



            boolean isIndexed = var.isIndex();
            boolean isUnique = var.isUnique();





            boolean isUUIDV7 = MFieldType.UUID_V_7.equals(var.getType());
            ColumnMetadata column = new ColumnMetadata.Builder(var.getName(), type) // 필수값
                    .primaryKey(var.isPrimaryKey())
                    .autoIncrement(var.isAutoIncrement())
                    .nullable(finalNullable)
                    .defaultValue(finalDefaultValue)
                    .indexed(isIndexed)
                    .unique(isUnique)
                    .isUUIDV7(isUUIDV7)
                    .build();

            // ... FK 처리 로직 (기존과 동일) ...
            if (var.getType() == MFieldType.FK) {
                String targetClassName = var.getParentClassName();
                EntityInfo targetInfo = entityInfoMap.get(targetClassName);
                if (targetInfo != null) {
                    column.setForeignKey(targetInfo.tableName, targetInfo.pkColumnName, var.getOnDelete());
                }
            }

            columns.add(column);
        }

        return new TableMetadata(currentEntity.tableName, columns);
    }


    private String mapToSqlType(MField var) {
        // 1. PK 처리 (PK가 숫자형일 경우 관례적으로 BIGINT 사용)
        if (var.isPrimaryKey() && (var.getType() == MFieldType.INTEGER || var.getType() == MFieldType.LONG)) {
            return "BIGINT";
        }

        SqlDialect sqlDialect = AppConfig.getSqlDialectImpl();

        switch (var.getType()) {
            case INTEGER:
                return "INT";

            case LONG:
            case FK:
                return "BIGINT";

            // 🔥 추가된 부분: 실수 타입
            case FLOAT:
                return "FLOAT";

            case DOUBLE:
                return "DOUBLE PRECISION"; // 대부분의 표준 SQL에서 고정밀도 실수형

            case STRING:
                // 길이가 4000보다 크면 TEXT로 변환 (MySQL 등에서 유용)
                if (var.getLength() > 4000) {
                    return "TEXT";
                }
                return "VARCHAR(" + var.getLength() + ")";

            case BOOLEAN:
                return "BOOLEAN";

            case LOCAL_DATE:
                return "DATE";

            case LOCAL_DATE_TIME:
                return "TIMESTAMP";

            case TEXT:
                return "TEXT";

            case JSON:
                return sqlDialect.getField(MFieldType.JSON);

            case UUID_V_7:
                return sqlDialect.getField(MFieldType.UUID_V_7);


            default:
                return "VARCHAR(255)";
        }
    }

    // --- Helper Class ---
    private static class EntityInfo {
        String tableName;
        String pkColumnName;
        EntityInfo(String t, String p) { tableName = t; pkColumnName = p; }
    }
}