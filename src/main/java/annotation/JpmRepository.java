package annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // 파서로 소스만 읽으므로 SOURCE로 충분
public @interface JpmRepository {
    String name() default ""; // name 속성 추가
}
