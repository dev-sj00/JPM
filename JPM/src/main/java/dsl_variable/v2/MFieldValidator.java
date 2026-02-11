package dsl_variable.v2;

import utils.LogPrinter;

import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MFieldValidator {

    // PK로 사용할 수 없는 타입들 정의
    private static final List<MFieldType> UNSUITABLE_PK_TYPES = Arrays.asList(
            MFieldType.FLOAT, MFieldType.DOUBLE, MFieldType.BOOLEAN,
            MFieldType.JSON, MFieldType.TEXT
    );

    /**
     * 필드 유효성 검증 진입점 (Public Entry Point)
     */
    public static void validate(MField field, TypeElement element) {
        validatePrimaryKey(field, element);
        validateUuidConstraints(field, element);
        validateAutoIncrement(field, element);
        validateForeignKey(field, element);
        validateLogicalConstraints(field, element);
        validateDefaultValue(field, element);
    }


    @Deprecated
    public static void indexTypeValidate(Map<String, List<MField>> parsedVariablesCache, TypeElement element) {





        for(List<MField> pkFields: parsedVariablesCache.values()) {
            MFieldType childPkType = null;
            String parentClassName = null;

            for(MField child : pkFields) {
                if(child.isPrimaryKey()) {
                    childPkType = child.getType();
                }

                if(child.getType() == MFieldType.FK) {
                    parentClassName = child.getParentClassName();
                }



            }

            //parentClass 탐색 못할 시 아래 과정 스킵
            if(parentClassName == null) {
                continue;
            }



            List<MField> parentMFields = parsedVariablesCache.get(parentClassName);

            for(MField parent : parentMFields) {
                if(parent.isPrimaryKey()) {
                    MFieldType parentPkType = parent.getType();
                    if(childPkType != parentPkType) {

                        String colName = parent.getName();
                        MFieldType type = parent.getType();

                        LogPrinter.error(
                                "PK_INDEX_MISMATCH",  // 태그: 인덱스 불일치
                                colName,
                                // 에러 메시지: "PK 인덱스 타입 불일치: 'TYPE'은 인덱싱(PK)을 위한 타입으로 적절하지 않습니다."
                                "Primary Key Index Type Mismatch: Type '" + type + "' is invalid for indexing. (Required: LONG, INTEGER, STRING, UUID)",
                                element
                        );
                    }else{
                        parentClassName = null;
                        break;
                    }

                }
            }



        }
    }

    // =========================================================
    // 1. Primary Key(PK) 검증
    // =========================================================
    private static void validatePrimaryKey(MField field, TypeElement element) {
        if (!field.isPrimaryKey()) {
            return;
        }

        String colName = field.getName();
        MFieldType type = field.getType();

        // 1-1. Nullable 체크
        if (field.isNullable()) {
            LogPrinter.error("PK_CONST", colName, "Primary Key must be 'nullable=false'", element);
        }

        // 1-2. PK 부적절 타입 체크
        if (UNSUITABLE_PK_TYPES.contains(type)) {
            LogPrinter.error("PK_TYPE", colName, "Type '" + type + "' is unsuitable for Primary Key", element);
        }

        // 1-3. 문자열 길이 경고
        if (type == MFieldType.STRING && field.getLength() > 255) {
            LogPrinter.warn("PK_LEN", colName, "PK length > 255 is not recommended for index performance", element);
        }
    }

    // =========================================================
    // 2. UUID V7 제약 검증
    // =========================================================
    private static void validateUuidConstraints(MField field, TypeElement element) {
        // UUID V7은 반드시 PK여야 함 (일반 컬럼 사용 불가 정책)
        if (field.getType() == MFieldType.UUID_V_7 && !field.isPrimaryKey()) {
            LogPrinter.error(
                    "UUID_PK_ONLY",
                    field.getName(),
                    "UUID_V_7 type must be used as Primary Key.",
                    element
            );
        }
    }

    // =========================================================
    // 3. AutoIncrement 검증
    // =========================================================
    private static void validateAutoIncrement(MField field, TypeElement element) {
        if (!field.isAutoIncrement()) {
            return;
        }

        String colName = field.getName();
        MFieldType type = field.getType();

        // 3-1. 타입 제한 (정수형만 가능)
        if (type != MFieldType.INTEGER && type != MFieldType.LONG) {
            LogPrinter.error("AUTO_INC", colName, "Only INTEGER/LONG can be AutoIncrement", element);
        }

        // 3-2. PK 권장 경고
        if (!field.isPrimaryKey()) {
            LogPrinter.warn("AUTO_INC", colName, "AutoIncrement is typically used for Primary Keys", element);
        }

        // 3-3. 금지된 조합 (FK, UUID)
        if (type == MFieldType.FK || field.getParentClassName() != null) {
            LogPrinter.error("AUTO_INC", colName, "Foreign Key cannot be AutoIncrement", element);
        }

        if (type == MFieldType.UUID_V_7) {
            LogPrinter.error("AUTO_INC", colName, "UUID Key cannot be AutoIncrement", element);
        }
    }

    // =========================================================
    // 4. Foreign Key(FK) 검증
    // =========================================================
    private static void validateForeignKey(MField field, TypeElement element) {
        if (field.getType() != MFieldType.FK) {
            return;
        }

        if (field.getParentClassName() == null || field.getParentClassName().isEmpty()) {
            LogPrinter.error("FK_SYNTAX", field.getName(), "FK type requires 'parent' attribute (target class)", element);
        }
    }

    // =========================================================
    // 5. 기타 논리적 제약 검증
    // =========================================================
    private static void validateLogicalConstraints(MField field, TypeElement element) {
        // Boolean 타입에 Unique 제약조건은 논리적으로 무의미함 (값의 종류가 2개뿐이라 중복 필연적)
        if (field.getType() == MFieldType.BOOLEAN && field.isUnique()) {
            LogPrinter.error("LOGIC_ERR", field.getName(), "Unique constraint on Boolean is logically meaningless", element);
        }
    }

    // =========================================================
    // 6. Default Value 형변환 검증
    // =========================================================
    private static void validateDefaultValue(MField field, TypeElement element) {
        String defVal = field.getDefaultValue();
        if (defVal == null || defVal.isEmpty()) {
            return;
        }

        MFieldType type = field.getType();
        String colName = field.getName();

        try {
            if (type == MFieldType.INTEGER || type == MFieldType.LONG) {
                // 숫자로 변환 가능한지 체크
                Long.parseLong(defVal);
            } else if (type == MFieldType.BOOLEAN) {
                // true/false 문자열인지 체크
                if (!defVal.equalsIgnoreCase("true") && !defVal.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception e) {
            LogPrinter.error("TYPE_MIS", colName, "Default value '" + defVal + "' does not match type '" + type + "'", element);
        }
    }
}
