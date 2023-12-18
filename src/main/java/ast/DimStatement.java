package ast;

import java.util.List;

public record DimStatement(List<ArrayInit> arrays) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
