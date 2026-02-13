import annotation.MColumn;
import annotation.MEntity;
import dsl_variable.v2.MFieldType;
import dsl_variable.v2.MField;

@MEntity(name = "wow")
public class Test {

    @MColumn
    private MField id = MField.builder()
            .type(MFieldType.LONG)
            .primaryKey(true)
            .autoIncrement(true)
            .build();


    public static void main(String[] args) {

    }
}
