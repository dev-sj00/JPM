package utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class MParserUtils {

    // 이제 타겟은 오직 하나입니다.
    private static final String TARGET_TYPE = "MField";

    public static class Pair {
        public String key;
        public String value;
        public Pair(String key, String value) { this.key = key; this.value = value; }
        @Override public String toString() { return key + "=" + value; }
    }

    public static List<List<Pair>> execute(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) throw new Exception("File not found: " + filePath);

        FileInputStream in = new FileInputStream(file);
        CompilationUnit cu = StaticJavaParser.parse(in);
        List<List<Pair>> result = new ArrayList<>();

        cu.findAll(VariableDeclarator.class).forEach(v -> {
            // 1. 변수 타입이 "MVariable" 인지 확인
            if (TARGET_TYPE.equals(v.getType().asString())) {
                List<Pair> columnInfo = new ArrayList<>();

                // 2. 변수명 저장 (기본 컬럼명으로 사용됨)
                columnInfo.add(new Pair("fieldName", v.getNameAsString()));

                // 3. 메서드 체인 파싱 (.builder().type(...).build())
                v.getInitializer().ifPresent(expr -> {
                    parseMethodChain(expr, columnInfo);
                });

                // 체인 순서가 거꾸로 들어갈 수 있으므로 정렬이 필요하면 여기서 처리
                // (지금 구조에선 순서가 크게 상관없음)
                result.add(columnInfo);
            }
        });





        return result;
    }



    // ★ 변경점: 리턴 타입을 EntityMeta로 변경하여 정보를 한방에 정리해서 반환 MqMapper용
    public static EntityMeta executeMq(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) throw new Exception("File not found: " + filePath);

        FileInputStream in = new FileInputStream(file);
        CompilationUnit cu = StaticJavaParser.parse(in);

        // 1. 클래스 정보 읽어서 테이블명(Table Name) 추출
        ClassOrInterfaceDeclaration classDecl = cu.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new Exception("No class found in file"));

        String tableName = classDecl.getNameAsString(); // 기본값: 클래스명
        Optional<AnnotationExpr> anno = classDecl.getAnnotationByName("MEntity");
        if(anno.isPresent() && anno.get().isNormalAnnotationExpr()) {
            for(MemberValuePair pair : anno.get().asNormalAnnotationExpr().getPairs()) {
                if("name".equals(pair.getNameAsString())) {
                    tableName = pair.getValue().asStringLiteralExpr().getValue();
                }
            }
        }

        // 메타 객체 생성
        EntityMeta entityMeta = new EntityMeta(tableName);

        // =========================================================
        // 🚀 2. [수정된 핵심 로직] 필드(변수) 파싱 - JavaParser 방식
        // =========================================================
        cu.findAll(VariableDeclarator.class).forEach(v -> {
            String fieldTypeString = v.getType().asString();
            String javaFieldName = v.getNameAsString(); // 자바 변수명 (예: "mEntity2", "description")

            // [Case A] MField 로 선언된 일반 컬럼들 처리
            if (TARGET_TYPE.equals(fieldTypeString)) {
                List<Pair> columnInfo = new ArrayList<>();
                v.getInitializer().ifPresent(expr -> parseMethodChain(expr, columnInfo));

                String dbColumnName = javaFieldName;
                String typeName = null; // 🚀 추가

                for (Pair p : columnInfo) {
                    if ("name".equals(p.key)) {
                        dbColumnName = p.value;
                    }
                    if ("type".equals(p.key)) { // 🚀 추가
                        typeName = p.value;
                    }
                }

                entityMeta.addMapping(javaFieldName, dbColumnName);

                if (typeName != null) { // 🚀 추가
                    entityMeta.addTypeMapping(javaFieldName, typeName);
                }
            }

            // [Case B] 1:N 관계 처리 (List<...>, Set<...>, Collection<...>)
            else if (fieldTypeString.startsWith("List<") || fieldTypeString.startsWith("Set<") || fieldTypeString.startsWith("Collection<")) {
                // 예: "List<MEntity2>" -> 제네릭 타입인 "MEntity2" 추출
                int startIdx = fieldTypeString.indexOf("<") + 1;
                int endIdx = fieldTypeString.indexOf(">");
                if (startIdx > 0 && endIdx > startIdx) {
                    String targetClassName = fieldTypeString.substring(startIdx, endIdx).trim();

                    // 💡 만약 List<String> 같이 기본 타입 리스트는 무시하고 싶다면 여기서 조건 추가 가능
                    if (isBasicJavaType(targetClassName)) {
                        entityMeta.addRelation(javaFieldName, targetClassName);
                    }
                }
            }

            // [Case C] 1:1 관계 처리 (자기 자신이 정의한 엔티티 타입인 경우)
            // 기본 자바 타입(String, int, Long 등)이 아닌 객체 타입을 관계(Relation)로 간주합니다.
            else if (isBasicJavaType(fieldTypeString)) {
                // 예: 타입이 "MEntity2" 인 경우
                entityMeta.addRelation(javaFieldName, fieldTypeString);
            }
        });

        return entityMeta;
    }

    // 🚀 [추가 유틸 메서드] JavaParser에서 읽은 타입 문자열이 기본 자바 타입인지 판별
    private static boolean isBasicJavaType(String typeStr) {
        // 배열 기호([]) 제거 후 비교
        String cleanType = typeStr.replace("[]", "").trim();

        List<String> basicTypes = Arrays.asList(
                "String", "Integer", "int", "Long", "long", "Boolean", "boolean",
                "Double", "double", "Float", "float", "Short", "short", "Byte", "byte",
                "Character", "char", "BigDecimal", "BigInteger", "LocalDate", "LocalDateTime", "Date"
        );
        return !basicTypes.contains(cleanType);
    }












    // 재귀적으로 메서드 체인을 파고들어 정보를 추출
    private static void parseMethodChain(Expression expr, List<Pair> info) {
        if (expr instanceof MethodCallExpr) {
            MethodCallExpr methodCall = (MethodCallExpr) expr;
            String methodName = methodCall.getNameAsString();

            // builder()와 build()는 제외하고 실제 설정값만 추출
            if (!"builder".equals(methodName) && !"build".equals(methodName)) {
                String argValue = extractArgValue(methodCall);
                // 키(메서드명)와 값(인자) 저장


                info.add(new Pair(methodName, argValue));
            }

            // 다음 체인(Scope)으로 이동 (재귀)
            methodCall.getScope().ifPresent(scope -> parseMethodChain(scope, info));
        }
    }

    // 🔥 핵심: 인자 타입별 값 추출 로직 (Enum, Class, String 등)
    private static String extractArgValue(MethodCallExpr methodCall) {
        if (methodCall.getArguments().isEmpty()) return "";

        Expression arg = methodCall.getArgument(0);

        if (arg.isStringLiteralExpr()) {
            return arg.asStringLiteralExpr().getValue(); // "문자열" -> 문자열
        }
        else if (arg.isBooleanLiteralExpr()) {
            return String.valueOf(arg.asBooleanLiteralExpr().getValue()); // true -> "true"
        }
        else if (arg.isIntegerLiteralExpr()) {
            return arg.asIntegerLiteralExpr().getValue(); // 100 -> "100"
        }
        else if (arg.isFieldAccessExpr()) {
             // Enum 처리: ColumnType.STRING -> "STRING" 추출
            return arg.asFieldAccessExpr().getNameAsString();
        }
        else if (arg.isClassExpr()) {
            // Class 처리: UserEntity.class -> "UserEntity" 추출
            return arg.asClassExpr().getType().asString();
        }
        else {
            // 그 외 (상수나 변수 등) -> 텍스트 그대로 반환
            return arg.toString();
        }
    }
}