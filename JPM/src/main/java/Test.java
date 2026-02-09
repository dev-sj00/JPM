import annotation.MColumn;
import annotation.MEntity;
import dsl_variable.v2.ColumnType;
import dsl_variable.v2.MVariable;

@MEntity(name = "wow")
public class Test {

    @MColumn
    private MVariable id = MVariable.builder()
            .type(ColumnType.LONG)
            .primaryKey(true)
            .autoIncrement(true)
            .build();
}
