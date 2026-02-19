package utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import mq_mapper.domain.vo.DslStatement;
import mq_mapper.domain.vo.MapJoinMeta;
import mq_mapper.domain.vo.MethodMeta;
import mq_mapper.domain.vo.RepoMeta;
import mq_mapper.infra.EntityMetaRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class MqRepoParser {

    private static final Set<String> DSL_KEYWORDS = new HashSet<>(Arrays.asList(
            // 기존 SQL 키워드
            "select", "from", "where", "and", "or", "andGroup", "orGroup", "endGroup",
            "innerJoin", "leftJoin", "hashJoin", "mergeJoin", "loopJoin",
            "insertInto", "update", "deleteFrom", "value", "set", "setRaw",
            "orderBy", "groupBy", "limit", "offset", "sql", "selectRaw", "orderByRaw", "groupByRaw",
            "whereInGroup", "group", "fromGroup",

            "selectCase", // 🚀 [추가] CASE 문법을 파서가 인식하도록 추가!


            // 신규 매핑 키워드 추가
            "mapTarget", "mapId", "mapResult", "mapJoin", "innerJoinGroup", "leftJoinGroup", "whereExistsGroup",
            "whereNotExistsGroup"

            /*"mapAssociation", "mapCollection", "mapDiscriminator"*/
    ));

    public static Map<String, RepoMeta> parseFile(String filePath) {
        Map<String, RepoMeta> repoMap = new LinkedHashMap<>();
        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("File not found: " + filePath);
            return repoMap;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            CompilationUnit cu = StaticJavaParser.parse(in);

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                String className = classDecl.getNameAsString();
                String namespace = className;

                // ★ @JpmRepository 또는 @MqRepository 둘 다 호환되도록 수정
                Optional<AnnotationExpr> annotationOpt = classDecl.getAnnotationByName("JpmRepository");
                if (!annotationOpt.isPresent()) {
                    annotationOpt = classDecl.getAnnotationByName("MqRepository");
                }

                if (annotationOpt.isPresent()) {
                    AnnotationExpr annotation = annotationOpt.get();
                    if (annotation.isNormalAnnotationExpr()) {
                        NormalAnnotationExpr normalExpr = annotation.asNormalAnnotationExpr();
                        for (MemberValuePair pair : normalExpr.getPairs()) {
                            if ("name".equals(pair.getNameAsString()) && pair.getValue().isStringLiteralExpr()) {
                                String extractedName = pair.getValue().asStringLiteralExpr().getValue();
                                if (extractedName != null && !extractedName.trim().isEmpty()) {
                                    namespace = extractedName;
                                }
                            }
                        }
                    }
                }

                RepoMeta repoMeta = new RepoMeta(className, namespace);

                classDecl.findAll(MethodDeclaration.class).forEach(method -> {
                    MethodMeta methodMeta = new MethodMeta(method.getNameAsString());

                    method.getParameters().forEach(param -> {
                        String paramType = param.getTypeAsString();
                        String paramName = param.getNameAsString();
                        methodMeta.addParameter(paramName, paramType);
                    });

                    method.getBody().ifPresent(body -> {
                        body.findAll(ExpressionStmt.class).forEach(stmt -> {
                            if (stmt.getExpression().isMethodCallExpr()) {
                                MethodCallExpr call = stmt.getExpression().asMethodCallExpr();
                                String command = call.getNameAsString();

                                if (DSL_KEYWORDS.contains(command)) {
                                    List<String> rawArgs = extractTokens(call, cu);

                                    // ========================================================
                                    // 🚀 [핵심 수정] 명령어(command) 종류에 따라 완벽하게 분기 처리!
                                    // ========================================================

                                    // 1. mapJoin 처리
                                    if ("mapJoin".equals(command)) {
                                        String raw = rawArgs.get(0);
                                        String fieldName = extractFieldNameFromMethodRef(raw);
                                        String alias = rawArgs.size() > 1 ? rawArgs.get(1) : null;

                                        // 어노테이션으로 매핑 타입 결정
                                        MapJoinMeta.MappingType mappingType = resolveMappingType(raw, fieldName);

                                        methodMeta.addMapJoin(new MapJoinMeta(fieldName, alias, mappingType));
                                        methodMeta.addStatement(new DslStatement(command, rawArgs));
                                    }
                                    // 2. JOIN 관련 명령어 처리 (중복 추가 방지!)
                                    else if (Arrays.asList("innerJoin", "leftJoin", "hashJoin", "mergeJoin", "loopJoin").contains(command)) {
                                        List<String> joinArgs = new ArrayList<>();

                                        // 0: Target Table (MEntity2.class)
                                        if (!rawArgs.isEmpty()) joinArgs.add(rawArgs.get(0));

                                        // 1: Left Column (col("m", MEntity3::getOwnerId))
                                        if (rawArgs.size() > 1) {
                                            String leftRaw = rawArgs.get(1);
                                            // "m|MEntity3::getOwnerId" 형태로 들어온다고 가정하면 그대로 저장하거나
                                            // SqlMapperBinder가 이해할 수 있는 col("m", ...) 형태로 보존해야 합니다.
                                            joinArgs.add(leftRaw);
                                        }

                                        // 2: Right Column (col("u", MEntity2::getOrder))
                                        if (rawArgs.size() > 2) {
                                            String rightRaw = rawArgs.get(2);
                                            joinArgs.add(rightRaw); // 여기서 "u|MEntity2::getOrder" 전체를 넘깁니다.
                                        }

                                        // 3. ✨ Alias 추출 및 추가 저장 (선택 사항)
                                        // SqlMapperBinder에서 2번 인자를 파싱해서 써도 되지만,
                                        // 여기서 아예 u만 뽑아서 5번째 인자로 넣어주면 처리가 훨씬 쉽습니다.
                                        String extractedAlias = "";
                                        if (rawArgs.size() > 2 && rawArgs.get(2).contains("|")) {
                                            extractedAlias = rawArgs.get(2).split("\\|")[0]; // "u" 추출
                                        }

                                        // 기존 Binder와의 호환성을 위해 인자 리스트를 구성
                                        // [TargetTable, LeftCol, RightCol, (임시), ExtractedAlias]
                                        while (joinArgs.size() < 4) joinArgs.add("");
                                        joinArgs.add(extractedAlias);

                                        methodMeta.addStatement(new DslStatement(command, joinArgs));
                                    }
                                    // 3. 그 외 일반 명령어 처리 (select, from, where 등)
                                    else {
                                        // ★ 여기서 딱 한 번만 statement 추가!
                                        methodMeta.addStatement(new DslStatement(command, rawArgs));

                                        // 타겟 타입 추론 (from, mapTarget)
                                        if ("from".equals(command) && !rawArgs.isEmpty()) {
                                            String typeName = rawArgs.get(0).replace(".class", "");
                                            methodMeta.setTargetType(typeName);
                                        } else if ("mapTarget".equals(command) && !rawArgs.isEmpty()) {
                                            String dtoName = rawArgs.get(0).replace(".class", "");
                                            methodMeta.setTargetType(dtoName);
                                        }
                                    }
                                }
                            }
                        });
                    });

                    if (!methodMeta.getStatements().isEmpty() || !methodMeta.getParameters().isEmpty()) {
                        repoMeta.addMethod(methodMeta);
                    }
                });

                repoMap.put(className, repoMeta);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return repoMap;
    }

    private static List<String> extractTokens(MethodCallExpr call, CompilationUnit cu) {
        List<String> args = new ArrayList<>();
        call.getArguments().forEach(arg -> {

            if (arg.isStringLiteralExpr()) {
                args.add(arg.asStringLiteralExpr().getValue());
            }

            else if (arg.isLiteralExpr()) {
                args.add(arg.toString());
            }

            // 🚀 [추가 1] OrderEntity.class 같은 클래스 표현식 추출
            else if (arg.isClassExpr()) {
                args.add(arg.asClassExpr().getTypeAsString());
            }
            // 🚀 [추가 2] col("별칭", 메서드참조) 형태 추출
            else if (arg.isMethodCallExpr() && "col".equals(arg.asMethodCallExpr().getNameAsString())) {
                MethodCallExpr colCall = arg.asMethodCallExpr();

                // 1. 별칭(Alias) 추출 (예: "u")
                String alias = "";
                if (colCall.getArgument(0).isStringLiteralExpr()) {
                    alias = colCall.getArgument(0).asStringLiteralExpr().getValue();
                }

                // 2. 필드(Field) 추출 (예: UserEntity::getId)
                String fieldStr = "";
                com.github.javaparser.ast.expr.Expression ref = colCall.getArgument(1);
                if (ref.isMethodReferenceExpr()) {
                    String scope = ref.asMethodReferenceExpr().getScope().toString();
                    String identifier = ref.asMethodReferenceExpr().getIdentifier();
                    fieldStr = scope + "::" + identifier;
                } else if (ref.isStringLiteralExpr()) {
                    fieldStr = ref.asStringLiteralExpr().getValue();
                } else {
                    fieldStr = ref.toString();
                }

                // Binder에게 전달하기 쉽게 "별칭|필드" 형태로 묶어줍니다. (예: "u|UserEntity::getId")
                args.add(alias + "|" + fieldStr);
            }

            else if (arg.isMethodCallExpr()
                    && arg.asMethodCallExpr().getNameAsString().equals("as")) {

                MethodCallExpr asCall = arg.asMethodCallExpr();

                // 1️⃣ 내부 col(...) 추출
                MethodCallExpr colCall = asCall.getScope().get().asMethodCallExpr();

                String alias = "";
                if (colCall.getArgument(0).isStringLiteralExpr()) {
                    alias = colCall.getArgument(0).asStringLiteralExpr().getValue();
                }

                String fieldStr = "";
                Expression ref = colCall.getArgument(1);

                if (ref.isMethodReferenceExpr()) {
                    String scope = ref.asMethodReferenceExpr().getScope().toString();
                    String identifier = ref.asMethodReferenceExpr().getIdentifier();
                    fieldStr = scope + "::" + identifier;
                }

                // 2️⃣ select alias 추출
                String selectAlias = "";
                if (asCall.getArgument(0).isStringLiteralExpr()) {
                    selectAlias = asCall.getArgument(0).asStringLiteralExpr().getValue();
                }

                // 3️⃣ Binder에 넘기기 쉽게 3단 구성
                args.add(alias + "|" + fieldStr + "|" + selectAlias);
            }



            // 기존 일반 메서드 콜 처리 (get... 등)
            else if (arg.isMethodCallExpr()) {
                String methodName = arg.asMethodCallExpr().getNameAsString();
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    String propName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                    args.add("#{" + propName + "}");
                } else {
                    args.add("#{" + methodName + "}");
                }
            }
            // 기존 메서드 참조(::) 처리
            else if (arg.isMethodReferenceExpr()) {
                String scope = arg.asMethodReferenceExpr().getScope().toString();
                String identifier = arg.asMethodReferenceExpr().getIdentifier();
                args.add(scope + "::" + identifier);
            }
            else if (arg.isNameExpr()) {
                String name = arg.asNameExpr().getNameAsString();

                // 메서드 파라미터 목록에 있으면 #{} 바인딩, 아니면 로컬 변수로 간주
                boolean isMethodParam = call.findAncestor(com.github.javaparser.ast.body.MethodDeclaration.class)
                        .map(m -> m.getParameters().stream()
                                .anyMatch(p -> p.getNameAsString().equals(name)))
                        .orElse(false);

                args.add(isMethodParam ? "#{" + name + "}" : name);
            }
        });
        return args;
    }


    private static String extractFieldNameFromMethodRef(String raw) {

        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        String cleaned = raw.trim();

        // 🔥 1️⃣ alias|UserEntity::getOrders 형태 처리
        if (cleaned.contains("|")) {
            String[] pipeParts = cleaned.split("\\|");
            cleaned = pipeParts[pipeParts.length - 1];
            // 마지막 파트가 실제 MethodRef
        }

        // 🔥 2️⃣ :: 기준으로 메서드명 추출
        if (cleaned.contains("::")) {
            String[] parts = cleaned.split("::");
            String methodName = parts[1].trim();

            return convertGetterToField(methodName);
        }

        return cleaned;
    }


    private static String convertGetterToField(String methodName) {

        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }

        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }

        return methodName;
    }


    /**
     * "MEntity1::getOrders" 같은 메서드 참조에서
     * 해당 필드가 List 타입인지 EntityMetaRegistry를 통해 확인합니다.
     */
    private static MapJoinMeta.MappingType resolveMappingType(String raw, String fieldName) {
        // "alias|ClassName::getField" 또는 "ClassName::getField" 에서 클래스명 추출
        String classNamePart = raw.contains("|")
                ? raw.split("\\|")[1].split("::")[0].trim()
                : raw.contains("::") ? raw.split("::")[0].trim() : null;

        if (classNamePart == null) return MapJoinMeta.MappingType.AUTO;

        try {
            // EntityMetaRegistry에서 실제 클래스 가져오기
            utils.EntityMeta meta = EntityMetaRegistry.getEntityMeta(classNamePart);
            if (meta == null) return MapJoinMeta.MappingType.AUTO;

            Class<?> entityClass = EntityMetaRegistry.getEntityClass(classNamePart);
            java.lang.reflect.Field field = entityClass.getDeclaredField(fieldName);

            if (field.isAnnotationPresent(annotation.MqCollection.class)) {
                return MapJoinMeta.MappingType.COLLECTION;
            }
            if (field.isAnnotationPresent(annotation.MqAssociation.class)) {
                return MapJoinMeta.MappingType.ASSOCIATION;
            }

            // 어노테이션 없으면 타입으로 자동 판별 (fallback)
            return java.util.List.class.isAssignableFrom(field.getType())
                    ? MapJoinMeta.MappingType.COLLECTION
                    : MapJoinMeta.MappingType.ASSOCIATION;

        } catch (Exception e) {
            return MapJoinMeta.MappingType.AUTO;
        }
    }



}