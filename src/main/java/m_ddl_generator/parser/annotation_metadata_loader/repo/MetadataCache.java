package m_ddl_generator.parser.annotation_metadata_loader.repo;

import annotation.MEntity;
import dsl_variable.v2.MField;
import dsl_variable.v2.MObjectFactory;
import utils.MParserUtils;
import m_ddl_generator.parser.annotation_metadata_loader.dto.MEntityInfo;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MetadataCache {
    public static final Map<String, MEntityInfo> entityInfoMap = new HashMap<>();

    // 파싱된 변수 캐시 (파일 다시 읽지 않기 위함)
    public static final Map<String, List<MField>> parsedVariablesCache = new HashMap<>();





    public static void saveMetadataCache(TypeElement element, List<List<MParserUtils.Pair>> rawData)
    {
        MEntity entityAnn = element.getAnnotation(MEntity.class);
        String tableName = entityAnn.name();

        Pair varAndCol = addFieldAndFindPkColumn(element, rawData);


        MEntityInfo info = new MEntityInfo(tableName, varAndCol.getSecond());
        String className = element.getSimpleName().toString();


        entityInfoMap.put(className, info);
        parsedVariablesCache.put(className, varAndCol.getFirst());



    }


    private static Pair addFieldAndFindPkColumn(TypeElement element, List<List<MParserUtils.Pair>> rawData) {
        String pkColumnName = "id"; // fallback



        List<MField> variables = new ArrayList<>();


        for (List<MParserUtils.Pair> ignored : rawData) {
            // 팩토리를 통해 MVariable 객체 생성
            MField var = MObjectFactory.createMVariable(ignored, element);

            variables.add(var);

            if (var.isPrimaryKey()) {
                pkColumnName = var.getName(); // PK 발견

            }
        }


        return new Pair(variables, pkColumnName);

    }






    private static class Pair{
        List<MField> variables;
        String columnName;

        private Pair(List<MField> variables, String columnName) {
            this.variables = variables;
            this.columnName = columnName;
        }


        private List<MField> getFirst()
        {
            return this.variables;
        }
        private String getSecond()
        {
            return this.columnName;
        }
    }


}
