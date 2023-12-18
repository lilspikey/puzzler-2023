package ast;

import java.util.List;

public record ArrayInit(String name, DataType dataType, List<Expression> sizes) {
    public ArrayDim getArrayDimensions() {
        return new ArrayDim(name + "()", dataType, sizes.size());
    }
}
