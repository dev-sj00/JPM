package m_ddl_generator.parser;

import m_ddl_generator.model.TableMetadata;
import javax.annotation.processing.RoundEnvironment;
import java.util.List;

public interface MetadataLoader {
    List<TableMetadata> load(RoundEnvironment roundEnv);
}