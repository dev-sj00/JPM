package m_ddl_generator.writer;

import java.io.IOException;

public interface DdlWriter {
    void write(String content) throws IOException;
}
