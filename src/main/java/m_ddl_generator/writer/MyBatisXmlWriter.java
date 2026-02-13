package m_ddl_generator.writer;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;

public class MyBatisXmlWriter implements DdlWriter {
    private final Filer filer;
    private final String namespace;

    public MyBatisXmlWriter(Filer filer, String namespace) {
        this.filer = filer;
        this.namespace = namespace;
    }

    @Override
    public void write(String sqlBody) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        xml.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        xml.append("<mapper namespace=\"").append(namespace).append("\">\n");
        xml.append("    <update id=\"execute_auto_ddl\" statementType=\"STATEMENT\">\n");
        xml.append(sqlBody);
        xml.append("\n    </update>\n");
        xml.append("</mapper>");

        FileObject file = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "ddl/ddl.xml");
        try (Writer writer = file.openWriter()) {
            writer.write(xml.toString());
        }
    }
}