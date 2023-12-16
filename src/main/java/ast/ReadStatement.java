package ast;

import java.util.List;

public record ReadStatement(List<VarName> names) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
