package mq_mapper.domain.vo;

import java.util.List;


public class DslStatement {
    private final String command;       // 예: "where", "select", "whereExistsGroup"
    private final List<String> args;    // 예: ["target.id", "=", "10"]
    private final List<DslStatement> subStatements; // 그룹/서브쿼리용

    public DslStatement(String command, List<String> args) {
        this(command, args, null);
    }

    public DslStatement(String command, List<String> args, List<DslStatement> subStatements) {
        this.command = command;
        this.args = args;
        this.subStatements = subStatements;
    }

    public String getCommand() { return command; }
    public List<String> getArgs() { return args; }
    public List<DslStatement> getSubStatements() { return subStatements; }

    // 편의 메서드
    public String getArg(int index) {
        if (args == null || index >= args.size()) return null;
        return args.get(index);
    }
}
