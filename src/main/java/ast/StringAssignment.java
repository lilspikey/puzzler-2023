package ast;

public record StringAssignment(String name, Expression expression) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
