package mq_mapper.domain.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MethodMeta {
    private final String methodName;
    private final List<DslStatement> statements = new ArrayList<>();

    private String targetType;

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetType() {
        return targetType;
    }

    private final Map<String, String> parameters = new LinkedHashMap<>();

    public MethodMeta(String methodName) {
        this.methodName = methodName;
    }

    public void addStatement(DslStatement stmt) {
        this.statements.add(stmt);
    }

    public void addParameter(String paramName, String paramType) {
        this.parameters.put(paramName, paramType);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    // Getter...
    public String getMethodName() { return methodName; }
    public List<DslStatement> getStatements() { return statements; }



    private final List<MapJoinMeta> mapJoins = new ArrayList<>();

    public void addMapJoin(MapJoinMeta meta) {
        this.mapJoins.add(meta);
    }

    public List<MapJoinMeta> getMapJoins() {
        return mapJoins;
    }

}
