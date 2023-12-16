package ast;

import java.util.Collections;
import java.util.List;

public record VarName(String name, DataType dataType, List<Expression> indexes) {

    public VarName(String name, DataType dataType) {
        this(name, dataType, Collections.emptyList());
    }

    public VarName(String name, DataType dataType, List<Expression> indexes) {
        this.name = name;
        this.dataType = dataType;
        this.indexes = indexes;
    }

    public boolean isArray() {
        return !indexes.isEmpty();
    }

    public ArrayDim getArrayDimensions() {
        return new ArrayDim(name, dataType, indexes.size());
    }
}
