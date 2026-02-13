package config.plugin;

import org.gradle.api.provider.Property;

public abstract class JpmDdlExtension {

    // Gradle 5.0+ 부터는 abstract 클래스로 만들면
    // Gradle이 내부적으로 구현체를 생성해 주므로 관리가 편합니다.

    public abstract Property<String> getUrl();

    public abstract Property<String> getUsername();

    public abstract Property<String> getPassword();

    public abstract Property<String> getDbType();

    public abstract Property<String> getAuto();
}
