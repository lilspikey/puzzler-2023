package runtime;

import ast.DataType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionDefTest {

    @Test
    void verifyFunctionDefs() {
        assertEquals(
            Set.of(
                new FunctionDef("INT", DataType.FLOAT, List.of(DataType.FLOAT)),
                new FunctionDef("SIN", DataType.FLOAT, List.of(DataType.FLOAT)),
                new FunctionDef("TAB", DataType.STRING, List.of(DataType.FLOAT)),
                new FunctionDef("RND", DataType.FLOAT, List.of(DataType.FLOAT)),
                new FunctionDef("ASC", DataType.FLOAT, List.of(DataType.STRING)),
                new FunctionDef("LEFT$", DataType.STRING, List.of(DataType.STRING, DataType.FLOAT))
            ),
            FunctionDef.getFunctionDefs()
        );
    }

}