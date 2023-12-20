package ast;

import java.util.List;

public record OnGotoStatement(Expression expression, List<String> destinationLabels) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
