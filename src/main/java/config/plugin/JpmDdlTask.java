package config.plugin;


import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class JpmDdlTask extends DefaultTask {

    @Input
    public abstract Property<String> getUrl();

    @Input
    public abstract Property<String> getUsername();

    @Input
    public abstract Property<String> getPassword();

    @Input
    public abstract Property<String> getDbType();

    @Input
    public abstract Property<String> getAuto();

    @TaskAction
    public void run() {
        String url  = getUrl().getOrNull();
        String user = getUsername().getOrNull();
        String pass = getPassword().getOrNull();
        String db   = getDbType().getOrNull();
        String auto = getAuto().getOrNull();

        getLogger().lifecycle("====== JPM DDL CONFIG ======");
        getLogger().lifecycle("url      : {}", url);
        getLogger().lifecycle("username : {}", user);
        getLogger().lifecycle("password : {}", pass);
        getLogger().lifecycle("dbType   : {}", db);
        getLogger().lifecycle("auto     : {}", auto);
        getLogger().lifecycle("============================");
    }
}
