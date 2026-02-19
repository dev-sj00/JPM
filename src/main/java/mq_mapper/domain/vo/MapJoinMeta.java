package mq_mapper.domain.vo;

public class MapJoinMeta {
    public enum MappingType { ASSOCIATION, COLLECTION, AUTO }

    private final String parentField;
    private final String alias;
    private final MappingType mappingType;

    public MapJoinMeta(String parentField, String alias, MappingType mappingType) {
        this.parentField = parentField;
        this.alias = alias;
        this.mappingType = mappingType;
    }

    public MapJoinMeta(String parentField, String alias) {
        this(parentField, alias, MappingType.AUTO);
    }

    public String getParentField() { return parentField; }
    public String getAlias() { return alias; }
    public MappingType getMappingType() { return mappingType; }
    public boolean isList() { return mappingType == MappingType.COLLECTION; }
}