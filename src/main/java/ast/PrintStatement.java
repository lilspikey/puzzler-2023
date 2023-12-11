package ast;

import java.util.List;

public record PrintStatement(List<Printable> printables) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
