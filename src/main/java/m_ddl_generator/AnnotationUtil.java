package m_ddl_generator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnnotationUtil {

    /**
     * 필드 중 지정한 어노테이션이 있는 필드만 반환
     *
     * @param fields     클래스 필드 리스트
     * @param annoClass  조회할 어노테이션 클래스
     * @param <T>        어노테이션 타입
     * @return 리스트 of Pair(VariableElement, Annotation)
     */

    public static <T extends java.lang.annotation.Annotation>
    List<Pair<VariableElement, T>> getFieldsWithAnnotation(
            List<? extends Element> fields,
            Class<T> annoClass
    ) {
        List<Pair<VariableElement, T>> result = new ArrayList<>();
        for (Element f : fields) {
            if (f.getKind() != ElementKind.FIELD) continue;
            VariableElement field = (VariableElement) f;
            System.out.print("field: " + field.getSimpleName());
            T annotation = field.getAnnotation(annoClass);
            if (annotation != null) {
                result.add(new Pair<>(field, annotation));
            }
        }
        return result;
    }


    public static String getRelativePath(VariableElement ve, boolean isTest) {
        // 1. 변수가 포함된 클래스(TypeElement) 찾기
        Element enclosing = ve.getEnclosingElement();
        while (enclosing != null && !(enclosing instanceof TypeElement)) {
            enclosing = enclosing.getEnclosingElement();
        }

        if (enclosing != null) {
            TypeElement typeElement = (TypeElement) enclosing;

            // 2. 패키지를 포함한 전체 이름 가져오기 (예: com.example.MyEntity)
            // 패키지 경로로 변환 (com.example -> com/example)
            String qualifiedName = typeElement.getQualifiedName().toString();
            String pathFromPackage = qualifiedName.replace(".", "/") + ".java";

            // 3. 베이스 경로 결정
            String basePath = isTest ? "src/test/java/" : "src/main/java/";

            // 4. 최종 경로 조합
            String finalPath = basePath + pathFromPackage;

            return new File(finalPath).getAbsolutePath();
        }

        throw new IllegalArgumentException("element not found");
    }

    // 단순 Pair 클래스 (없으면 Map.Entry로 대체 가능)
    public static class Pair<K, V> {
        public final K first;
        public final V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}
