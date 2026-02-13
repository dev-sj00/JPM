package dsl_variable.v2;

import utils.LogPrinter;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class MObjectFactory {

    public static MField createMVariable(List<MParserUtils.Pair> pairs, TypeElement element) {
        MField.Builder builder = MField.builder();



        for (MParserUtils.Pair pair : pairs) {
            String key = pair.key;
            String val = pair.value;


            switch (key) {
                case "fieldName": // 파서에서 넣어준 변수명
                    // .name() 설정이 없으면 변수명을 사용하도록 로직 처리 필요
                    builder.name(val);
                    break;
                case "name": // 명시적 컬럼명
                    builder.name(val);
                    break;
                case "type":
                    // String으로 된 Enum 이름을 실제 Enum으로 변환
                    builder.type(MFieldType.valueOf(val));
                    break;
                case "primaryKey":
                    builder.primaryKey(Boolean.parseBoolean(val));
                    break;
                case "autoIncrement":
                    builder.autoIncrement(Boolean.parseBoolean(val));
                    break;
                case "nullable":
                    builder.nullable(Boolean.parseBoolean(val));
                    break;
                case "defaultValue":
                    builder.defaultValue(val);
                    break;
                case "length":
                    builder.length(Integer.parseInt(val));
                    break;
                case "parent": // FK 타겟 클래스
                    builder.parent(val); // 클래스명 문자열 저장
                    break;
                case "onDelete":
                    builder.onDelete(OnDeleteType.valueOf(val));
                    break;

                case "index":
                    builder.index(Boolean.parseBoolean(val));
                    break;

                case "unique":
                    builder.unique(Boolean.parseBoolean(val));
                    break;

            }
        }


        MField var = builder.build();
        MFieldValidator.validate(var, element);

        return var;
    }



    private void validateIndex()
    {

    }

    /*private static void validateMVariable(MField var, TypeElement element) {
        String colName = var.getName();
        MFieldType type = var.getType();

        // =========================================================
        // 1. Primary Key(PK) 제약 검증
        // =========================================================
        if (var.isPrimaryKey()) {
            // 1-1. Nullable 체크
            if (var.isNullable()) {
                LogPrinter.error("PK_CONST", colName, "Primary Key must be 'nullable=false'", element);
            }

            // 1-2. PK로 부적절한 타입 체크 (로직 위치 수정!)
            if (type == MFieldType.FLOAT || type == MFieldType.DOUBLE || type == MFieldType.BOOLEAN || type == MFieldType.JSON || type == MFieldType.TEXT) {
                LogPrinter.error("PK_TYPE", colName, "Type '" + type + "' is unsuitable for Primary Key", element);
            }

            // 1-3. 문자열 길이 경고
            if (type == MFieldType.STRING && var.getLength() > 255) {
                LogPrinter.warn("PK_LEN", colName, "PK length > 255 is not recommended for index performance", element);
            }
        }
        else{
            if (type == MFieldType.UUID_V_7) {
                LogPrinter.error(
                        "UUID_PK_ONLY",
                        colName,
                        "UUID_V_7 type must be used as Primary Key.",
                        element
                );
            }
        }

        // =========================================================
        // 2. AutoIncrement 논리 검증
        // =========================================================
        if (var.isAutoIncrement()) {
            if (type != MFieldType.INTEGER && type != MFieldType.LONG) {
                LogPrinter.error("AUTO_INC", colName, "Only INTEGER/LONG can be AutoIncrement", element);
            }

            if (!var.isPrimaryKey()) {
                LogPrinter.warn("AUTO_INC", colName, "AutoIncrement is typically used for Primary Keys", element);
            }

            if (type == MFieldType.FK || var.getTargetClassName() != null) {
                LogPrinter.error("AUTO_INC", colName, "Foreign Key cannot be AutoIncrement", element);
            }
            if(type == MFieldType.UUID_V_7)
            {
                LogPrinter.error("AUTO_INC", colName, "UUID Key cannot be AutoIncrement", element);
            }

        }

        // =========================================================
        // 3. Foreign Key(FK) 구문 검증
        // =========================================================
        if (type == MFieldType.FK) {
            if (var.getTargetClassName() == null || var.getTargetClassName().isEmpty()) {
                LogPrinter.error("FK_SYNTAX", colName, "FK type requires 'parent' attribute (target class)", element);
            }
        }

        // =========================================================
        // 4. 기타 논리 오류
        // =========================================================
        if (type == MFieldType.BOOLEAN && var.isUnique()) {
            LogPrinter.error("LOGIC_ERR", colName, "Unique constraint on Boolean is logically meaningless", element);
        }

        // 4-1. Default Value 형변환 체크
        String defVal = var.getDefaultValue();
        if (defVal != null && !defVal.isEmpty()) {
            try {
                if (type == MFieldType.INTEGER || type == MFieldType.LONG) {
                    Long.parseLong(defVal);
                } else if (type == MFieldType.BOOLEAN) {
                    if (!defVal.equalsIgnoreCase("true") && !defVal.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException();
                    }
                }
            } catch (Exception e) {
                LogPrinter.error("TYPE_MIS", colName, "Default value '" + defVal + "' does not match type '" + type + "'", element);
            }
        }
    }*/
}