package ast;

import java.util.List;

public record DimStatement(String name, DataType dataType, List<Expression> sizes) implements Statement {
    public ArrayDim getArrayDimensions() {
        return new ArrayDim(name + "()", dataType, sizes.size());
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
