package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class DefaultValueUtil {

    /**
     * typeName: MParserUtils.TARGET_TYPES 안에 있는 이름
     * value: builder.defaultValue
     */


    public static boolean validateDefaultValue(String typeName, String value) {
        // MFk는 검증하지 않음
        if ("MFk".equals(typeName)) return true;



        return Optional.ofNullable(value)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    switch (typeName) {
                        case "MLong":
                        case "MInteger":
                            // 정수만 허용, 2L이나 2.0은 false
                            return s.matches("-?\\d+");

                        case "MNumber":
                        case "MDouble":
                            try {
                                Double.parseDouble(s);
                                return true;
                            } catch (NumberFormatException e) {
                                return false;
                            }

                        case "MBoolean":
                            return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");

                        case "MLocalDate":
                            try {
                                LocalDate.parse(s); // yyyy-MM-dd
                                return true;
                            } catch (DateTimeParseException e) {
                                return false;
                            }

                        case "MLocalDateTime":
                            try {
                                LocalDateTime.parse(s); // ISO_LOCAL_DATE_TIME
                                return true;
                            } catch (DateTimeParseException e) {
                                return false;
                            }

                        case "MString":
                        case "MEnumType":
                        default:
                            return true; // 문자열, enum은 아무 값이나 허용
                    }
                })
                .orElse(true); // null 또는 빈 문자열은 true
    }
}
