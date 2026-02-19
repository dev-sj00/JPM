package mq_mapper.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class RepoMeta {
    private final String className;  // 실제 자바 클래스 이름
    private final String namespace;  // 어노테이션의 name 또는 클래스 이름
    private final List<MethodMeta> methods = new ArrayList<>();

    public RepoMeta(String className, String namespace) {
        this.className = className;
        this.namespace = namespace;
    }

    public void addMethod(MethodMeta method) {
        this.methods.add(method);
    }

    // Getters
    public String getClassName() { return className; }
    public String getNamespace() { return namespace; }
    public List<MethodMeta> getMethods() { return methods; }
}
