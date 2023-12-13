package ast;

import java.util.List;

public record DataStatement(List<Object> constants) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
