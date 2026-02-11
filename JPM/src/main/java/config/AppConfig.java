package config;

import auto_ddl.AutoDDLPolicy;
import m_ddl_generator.dialect.MySqlDialect;
import m_ddl_generator.dialect.PostgreSqlDialect;
import m_ddl_generator.dialect.SqlDialect;

import java.util.Map;


public class AppConfig {

    private static SqlDialect sqlDialect;


    public static String MAPPER_NAME_SPACE = "dev.sj.jqm.mapper.";



    public static void sqlDialectInit(Map<String, String> options) {
        if(options.get("dbType").equals("MYSQL") )
        {
            sqlDialect = new MySqlDialect();
        }
        else
        {
            sqlDialect = new PostgreSqlDialect();
        }
    }
    public  static SqlDialect getSqlDialectImpl()
    {

        return sqlDialect;
    }





}
