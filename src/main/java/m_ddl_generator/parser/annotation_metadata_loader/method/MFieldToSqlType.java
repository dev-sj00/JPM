package m_ddl_generator.parser.annotation_metadata_loader.method;


import dsl_variable.v2.MField;
import dsl_variable.v2.MFieldType;

import java.util.Objects;


public class MFieldToSqlType {



    public static String resolveType(MField handleField) {

        ResolveType defaultResolveType = ResolveType.valueOf(handleField.getType().name());
        boolean ifPrimaryKeyReturnBigInt = handleField.isPrimaryKey()
                && (handleField.getType())
                == MFieldType.INTEGER
                || handleField.getType() == MFieldType.LONG;

        if (ifPrimaryKeyReturnBigInt) {

            return "BIGINT";
        }

        if (Objects.requireNonNull(handleField.getType()) == MFieldType.STRING) {// 길이가 4000보다 크면 TEXT로 변환 (MySQL 등에서 유용)
            if (handleField.getLength() > 4000) {
                return "TEXT";
            }
            return "VARCHAR(" + handleField.getLength() + ")";
        }

        return defaultResolveType.getSqlType();

    }

}
