package ast;

import java.util.List;

public record IfStatement(Expression predicate, List<Statement> trueStatements) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
