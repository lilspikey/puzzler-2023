package ast;

import java.util.List;

public record NextStatement(List<String> varnames) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}