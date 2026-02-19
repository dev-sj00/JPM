package mq_repository.domain.enums;



public enum GroupType {
    AND(" AND "),
    OR(" OR ");

    private final String operator;

    GroupType(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }
}
