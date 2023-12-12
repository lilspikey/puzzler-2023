package ast;

public record GreaterThan(Expression lhs, Expression rhs) implements BinaryExpression {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
